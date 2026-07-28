import javax.crypto.*;
import javax.crypto.spec.*;
import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.security.spec.*;
import java.util.*;

/**
 * FileCryptor - A secure file encryption & decryption tool (Java 21)
 *
 * Algorithm : AES-256-GCM (Authenticated Encryption)
 * Key Derivation: PBKDF2WithHmacSHA256 (310,000 iterations, OWASP 2023 recommendation)
 * Salt         : 16 bytes (cryptographically random)
 * IV/Nonce     : 12 bytes (cryptographically random, GCM standard)
 *
 * Encrypted file layout:
 * ┌──────────┬──────────┬──────────────────────────────┐
 * │ Salt     │ IV       │ Ciphertext + GCM Auth Tag    │
 * │ 16 bytes │ 12 bytes │ variable length              │
 * └──────────┴──────────┴──────────────────────────────┘
 *
 * Usage:
 *   java FileCryptor.java encrypt <input-file> <output-file>
 *   java FileCryptor.java decrypt <input-file> <output-file>
 *
 * Or run without arguments for interactive mode.
 */
public class FileCryptor {

    // ── Constants ────────────────────────────────────────────────────────────
    private static final String ALGORITHM      = "AES/GCM/NoPadding";
    private static final String KEY_ALGORITHM  = "AES";
    private static final String KDF_ALGORITHM  = "PBKDF2WithHmacSHA256";
    private static final int    KEY_LENGTH     = 256;          // bits
    private static final int    GCM_TAG_LENGTH = 128;          // bits
    private static final int    IV_LENGTH      = 12;           // bytes (96 bits – GCM standard)
    private static final int    SALT_LENGTH    = 16;           // bytes (128 bits)
    private static final int    KDF_ITERATIONS = 310_000;      // OWASP 2023 recommendation
    private static final int    BUFFER_SIZE    = 8 * 1024;     // 8 KB read buffer
    private static final String BANNER = """
            
            ╔══════════════════════════════════════════════════════════════╗
            ║                                                              ║
            ║    ███████╗██╗██╗     ███████╗                               ║
            ║    ██╔════╝██║██║     ██╔════╝                               ║
            ║    █████╗  ██║██║     █████╗                                 ║
            ║    ██╔══╝  ██║██║     ██╔══╝                                 ║
            ║    ██║     ██║███████╗███████╗                               ║
            ║    ╚═╝     ╚═╝╚══════╝╚══════╝                              ║
            ║     ██████╗██████╗ ██╗   ██╗██████╗ ████████╗ ██████╗ ██████╗║
            ║    ██╔════╝██╔══██╗╚██╗ ██╔╝██╔══██╗╚══██╔══╝██╔═══██╗██╔══██╗
            ║    ██║     ██████╔╝ ╚████╔╝ ██████╔╝   ██║   ██║   ██║██████╔╝
            ║    ██║     ██╔══██╗  ╚██╔╝  ██╔═══╝    ██║   ██║   ██║██╔══██╗
            ║    ╚██████╗██║  ██║   ██║   ██║        ██║   ╚██████╔╝██║  ██║
            ║     ╚═════╝╚═╝  ╚═╝   ╚═╝   ╚═╝        ╚═╝    ╚═════╝ ╚═╝  ╚═╝
            ║                                                              ║
            ║          AES-256-GCM  •  PBKDF2  •  Java 21                  ║
            ╚══════════════════════════════════════════════════════════════╝
            """;

    // ── ANSI Colors ──────────────────────────────────────────────────────────
    private static final String RESET   = "\u001B[0m";
    private static final String BOLD    = "\u001B[1m";
    private static final String RED     = "\u001B[31m";
    private static final String GREEN   = "\u001B[32m";
    private static final String YELLOW  = "\u001B[33m";
    private static final String CYAN    = "\u001B[36m";
    private static final String MAGENTA = "\u001B[35m";
    private static final String DIM     = "\u001B[2m";

    // ── Entry Point ──────────────────────────────────────────────────────────
    public static void main(String[] args) {
        try {
            if (args.length >= 3) {
                runCli(args);
            } else {
                runInteractive();
            }
        } catch (Exception e) {
            printError(e.getMessage());
            System.exit(1);
        }
    }

    // ── CLI Mode ─────────────────────────────────────────────────────────────
    private static void runCli(String[] args) throws Exception {
        String mode       = args[0].toLowerCase();
        Path   inputPath  = Path.of(args[1]);
        Path   outputPath = Path.of(args[2]);

        validateInput(inputPath);
        confirmOverwrite(outputPath);

        Console console = System.console();
        char[] password;
        if (console != null) {
            password = console.readPassword(CYAN + "🔑 Enter password: " + RESET);
        } else {
            System.out.print(CYAN + "🔑 Enter password: " + RESET);
            password = new Scanner(System.in).nextLine().toCharArray();
        }
        validatePassword(password);

        switch (mode) {
            case "encrypt", "enc", "e" -> {
                encryptFile(inputPath, outputPath, password);
                printSuccess("Encryption complete! → " + outputPath);
            }
            case "decrypt", "dec", "d" -> {
                decryptFile(inputPath, outputPath, password);
                printSuccess("Decryption complete! → " + outputPath);
            }
            default -> {
                printError("Unknown mode: " + mode + ". Use 'encrypt' or 'decrypt'.");
                System.exit(1);
            }
        }
        Arrays.fill(password, '\0'); // clear sensitive data
    }

    // ── Interactive Mode ─────────────────────────────────────────────────────
    private static void runInteractive() throws Exception {
        System.out.println(CYAN + BANNER + RESET);

        Scanner scanner = new Scanner(System.in);
        Console console = System.console();

        while (true) {
            System.out.println(BOLD + "  Choose an operation:" + RESET);
            System.out.println(GREEN  + "    [1]" + RESET + "  🔒  Encrypt a file");
            System.out.println(YELLOW + "    [2]" + RESET + "  🔓  Decrypt a file");
            System.out.println(RED    + "    [3]" + RESET + "  🚪  Exit");
            System.out.println();
            System.out.print(BOLD + "  ➤ " + RESET);

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1", "encrypt", "enc" -> processOperation(scanner, console, true);
                case "2", "decrypt", "dec" -> processOperation(scanner, console, false);
                case "3", "exit", "quit", "q" -> {
                    System.out.println(DIM + "\n  Goodbye! Stay secure. 🔐\n" + RESET);
                    return;
                }
                default -> printError("Invalid choice. Please enter 1, 2, or 3.");
            }
            System.out.println();
        }
    }

    private static void processOperation(Scanner scanner, Console console, boolean encrypt)
            throws Exception {
        System.out.println();

        // Input file
        System.out.print(CYAN + "  📄 Input file path: " + RESET);
        Path inputPath = Path.of(scanner.nextLine().trim().replace("\"", ""));
        validateInput(inputPath);

        // Output file
        String defaultOutput = encrypt
                ? inputPath + ".encrypted"
                : inputPath.toString().replace(".encrypted", ".decrypted");
        System.out.print(CYAN + "  📄 Output file path " + DIM + "[" + defaultOutput + "]" + RESET + ": ");
        String outStr = scanner.nextLine().trim().replace("\"", "");
        Path outputPath = outStr.isEmpty() ? Path.of(defaultOutput) : Path.of(outStr);
        confirmOverwrite(outputPath);

        // Password
        char[] password;
        if (console != null) {
            password = console.readPassword(CYAN + "  🔑 Enter password: " + RESET);
            if (encrypt) {
                char[] confirm = console.readPassword(CYAN + "  🔑 Confirm password: " + RESET);
                if (!Arrays.equals(password, confirm)) {
                    Arrays.fill(confirm, '\0');
                    Arrays.fill(password, '\0');
                    throw new SecurityException("Passwords do not match!");
                }
                Arrays.fill(confirm, '\0');
            }
        } else {
            System.out.print(CYAN + "  🔑 Enter password: " + RESET);
            password = scanner.nextLine().toCharArray();
            if (encrypt) {
                System.out.print(CYAN + "  🔑 Confirm password: " + RESET);
                char[] confirm = scanner.nextLine().toCharArray();
                if (!Arrays.equals(password, confirm)) {
                    Arrays.fill(confirm, '\0');
                    Arrays.fill(password, '\0');
                    throw new SecurityException("Passwords do not match!");
                }
                Arrays.fill(confirm, '\0');
            }
        }
        validatePassword(password);

        System.out.println();
        long start = System.nanoTime();

        if (encrypt) {
            encryptFile(inputPath, outputPath, password);
        } else {
            decryptFile(inputPath, outputPath, password);
        }

        long elapsed = (System.nanoTime() - start) / 1_000_000;
        long outSize = Files.size(outputPath);

        Arrays.fill(password, '\0');

        System.out.println();
        printSuccess((encrypt ? "Encryption" : "Decryption") + " complete!");
        System.out.println(DIM + "  ├─ Output : " + outputPath.toAbsolutePath() + RESET);
        System.out.println(DIM + "  ├─ Size   : " + formatSize(outSize) + RESET);
        System.out.println(DIM + "  └─ Time   : " + elapsed + " ms" + RESET);
        System.out.println();
    }

    // ── Core Encryption ──────────────────────────────────────────────────────
    private static void encryptFile(Path input, Path output, char[] password) throws Exception {
        printInfo("Deriving encryption key (PBKDF2, " + KDF_ITERATIONS + " iterations)...");

        SecureRandom random = SecureRandom.getInstanceStrong();

        // Generate salt & IV
        byte[] salt = new byte[SALT_LENGTH];
        random.nextBytes(salt);
        byte[] iv = new byte[IV_LENGTH];
        random.nextBytes(iv);

        // Derive key
        SecretKey key = deriveKey(password, salt);

        // Init cipher
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec);

        printInfo("Encrypting " + formatSize(Files.size(input)) + "...");

        // Write: salt | iv | ciphertext
        try (OutputStream fos = new BufferedOutputStream(Files.newOutputStream(output));
             InputStream fis  = new BufferedInputStream(Files.newInputStream(input))) {

            fos.write(salt);
            fos.write(iv);

            byte[] buffer = new byte[BUFFER_SIZE];
            long totalRead = 0;
            long fileSize  = Files.size(input);
            int  bytesRead;

            while ((bytesRead = fis.read(buffer)) != -1) {
                byte[] encrypted = cipher.update(buffer, 0, bytesRead);
                if (encrypted != null) fos.write(encrypted);
                totalRead += bytesRead;
                printProgress("Encrypting", totalRead, fileSize);
            }

            byte[] finalBlock = cipher.doFinal();
            if (finalBlock != null) fos.write(finalBlock);
            clearProgressLine();
        }
    }

    // ── Core Decryption ──────────────────────────────────────────────────────
    private static void decryptFile(Path input, Path output, char[] password) throws Exception {
        long fileSize = Files.size(input);
        if (fileSize < SALT_LENGTH + IV_LENGTH + GCM_TAG_LENGTH / 8) {
            throw new IllegalArgumentException(
                    "File is too small to be a valid encrypted file (" + fileSize + " bytes).");
        }

        try (InputStream fis = new BufferedInputStream(Files.newInputStream(input))) {
            // Read salt & IV
            byte[] salt = fis.readNBytes(SALT_LENGTH);
            byte[] iv   = fis.readNBytes(IV_LENGTH);

            printInfo("Deriving decryption key (PBKDF2, " + KDF_ITERATIONS + " iterations)...");
            SecretKey key = deriveKey(password, salt);

            // Init cipher
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec);

            long cipherSize = fileSize - SALT_LENGTH - IV_LENGTH;
            printInfo("Decrypting " + formatSize(cipherSize) + "...");

            try (OutputStream fos = new BufferedOutputStream(Files.newOutputStream(output))) {
                byte[] buffer = new byte[BUFFER_SIZE];
                long totalRead = 0;
                int  bytesRead;

                while ((bytesRead = fis.read(buffer)) != -1) {
                    byte[] decrypted = cipher.update(buffer, 0, bytesRead);
                    if (decrypted != null) fos.write(decrypted);
                    totalRead += bytesRead;
                    printProgress("Decrypting", totalRead, cipherSize);
                }

                try {
                    byte[] finalBlock = cipher.doFinal();
                    if (finalBlock != null) fos.write(finalBlock);
                } catch (AEADBadTagException e) {
                    clearProgressLine();
                    // Clean up the partial output
                    Files.deleteIfExists(output);
                    throw new SecurityException(
                            "Authentication failed! Wrong password or corrupted file.");
                }
                clearProgressLine();
            }
        }
    }

    // ── Key Derivation ───────────────────────────────────────────────────────
    private static SecretKey deriveKey(char[] password, byte[] salt) throws Exception {
        KeySpec spec = new PBEKeySpec(password, salt, KDF_ITERATIONS, KEY_LENGTH);
        SecretKeyFactory factory = SecretKeyFactory.getInstance(KDF_ALGORITHM);
        byte[] keyBytes = factory.generateSecret(spec).getEncoded();
        return new SecretKeySpec(keyBytes, KEY_ALGORITHM);
    }

    // ── Validation ───────────────────────────────────────────────────────────
    private static void validateInput(Path path) {
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("Input file not found: " + path.toAbsolutePath());
        }
        if (!Files.isRegularFile(path)) {
            throw new IllegalArgumentException("Not a regular file: " + path.toAbsolutePath());
        }
        if (!Files.isReadable(path)) {
            throw new IllegalArgumentException("Cannot read file: " + path.toAbsolutePath());
        }
    }

    private static void validatePassword(char[] password) {
        if (password == null || password.length == 0) {
            throw new IllegalArgumentException("Password cannot be empty.");
        }
        if (password.length < 8) {
            printWarning("Password is short (< 8 chars). Consider using a stronger password.");
        }
    }

    private static void confirmOverwrite(Path path) {
        if (Files.exists(path)) {
            System.out.print(YELLOW + "  ⚠ File '" + path.getFileName()
                    + "' already exists. Overwrite? [y/N]: " + RESET);
            String answer = new Scanner(System.in).nextLine().trim().toLowerCase();
            if (!answer.equals("y") && !answer.equals("yes")) {
                System.out.println(DIM + "  Aborted." + RESET);
                System.exit(0);
            }
        }
    }

    // ── Progress & Output ────────────────────────────────────────────────────
    private static void printProgress(String label, long current, long total) {
        if (total <= 0) return;
        int percent = (int) (current * 100 / total);
        int barWidth = 30;
        int filled   = (int) (current * barWidth / total);

        StringBuilder bar = new StringBuilder("  ");
        bar.append(CYAN).append(label).append(" [");
        bar.append("█".repeat(filled));
        bar.append("░".repeat(barWidth - filled));
        bar.append("] ").append(String.format("%3d%%", percent));
        bar.append("  ").append(formatSize(current)).append(" / ").append(formatSize(total));
        bar.append(RESET);

        System.out.print("\r" + bar);
    }

    private static void clearProgressLine() {
        System.out.print("\r" + " ".repeat(80) + "\r");
    }

    private static void printSuccess(String msg) {
        System.out.println(GREEN + BOLD + "  ✔ " + msg + RESET);
    }

    private static void printError(String msg) {
        System.out.println(RED + BOLD + "  ✖ Error: " + msg + RESET);
    }

    private static void printWarning(String msg) {
        System.out.println(YELLOW + "  ⚠ " + msg + RESET);
    }

    private static void printInfo(String msg) {
        System.out.println(DIM + "  ℹ " + msg + RESET);
    }

    private static String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        String[] units = {"KB", "MB", "GB", "TB"};
        int i = 0;
        double size = bytes;
        while (size >= 1024 && i < units.length - 1) {
            size /= 1024;
            i++;
        }
        return String.format("%.2f %s", size, units[i]);
    }

    // ── Help ─────────────────────────────────────────────────────────────────
    static {
        // Print usage if --help is present
        if (System.getProperty("sun.java.command", "").contains("--help")) {
            System.out.println("""
                    
                    FileCryptor - Secure File Encryption & Decryption (Java 21)
                    
                    USAGE:
                      java FileCryptor.java encrypt <input> <output>
                      java FileCryptor.java decrypt <input> <output>
                      java FileCryptor.java                            (interactive mode)
                    
                    SECURITY:
                      • AES-256-GCM authenticated encryption
                      • PBKDF2WithHmacSHA256 key derivation (310,000 iterations)
                      • 128-bit random salt per file
                      • 96-bit random IV/nonce per file
                      • GCM authentication tag prevents tampering
                    
                    EXAMPLES:
                      java FileCryptor.java encrypt secret.pdf secret.pdf.enc
                      java FileCryptor.java decrypt secret.pdf.enc secret.pdf
                    """);
            System.exit(0);
        }
    }
}
