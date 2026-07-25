
import java.util.Scanner;

// ===================== PIECE (Abstraction + Encapsulation) =====================
// abstract class -> we can never make "new Piece()", only its children (King, Pawn, etc.)
abstract class Piece {
    private boolean isWhite;   // encapsulation: private field
    private boolean hasMoved;

    public Piece(boolean isWhite) {
        this.isWhite = isWhite;
        this.hasMoved = false;
    }

    public boolean isWhite() { return isWhite; }
    public boolean hasMoved() { return hasMoved; }
    public void setMoved() { this.hasMoved = true; }

    // Polymorphism: every piece will define canMove in its OWN way
    public abstract boolean canMove(Board board, Spot start, Spot end);

    // Polymorphism: every piece prints a different letter
    public abstract char getSymbol();
}

// ===================== KING =====================
class King extends Piece {
    public King(boolean isWhite) { super(isWhite); }

    @Override
    public boolean canMove(Board board, Spot start, Spot end) {
        int dx = Math.abs(start.getX() - end.getX());
        int dy = Math.abs(start.getY() - end.getY());
        if (dx > 1 || dy > 1) return false;                 // king moves only 1 step
        return Board.destinationOk(this, end);
    }

    @Override
    public char getSymbol() { return isWhite() ? 'K' : 'k'; }
}

// ===================== QUEEN =====================
