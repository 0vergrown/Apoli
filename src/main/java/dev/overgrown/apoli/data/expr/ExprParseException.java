package dev.overgrown.apoli.data.expr;

public final class ExprParseException extends Exception {

    public ExprParseException(String message, int position) {
        super(message + " (at index " + position + ")");
    }
}
