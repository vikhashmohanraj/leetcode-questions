class Solution {
    public boolean isPalindrome(int x) {
        // Negative numbers are never palindromes (e.g., -121 reversed is 121-)
        if (x < 0) {
            return false;
        }

        int original = x;
        long reversed = 0; // Use long to prevent integer overflow during reversal

        // Reverse the integer mathematically
        while (x != 0) {
            int pop = x % 10;
            reversed = reversed * 10 + pop;
            x /= 10;
        }

        // It is a palindrome if the reversed number equals the original
        return original == reversed;
    }
}
