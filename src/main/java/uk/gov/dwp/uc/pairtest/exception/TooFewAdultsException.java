package uk.gov.dwp.uc.pairtest.exception;

public class TooFewAdultsException extends InvalidPurchaseException {

    public TooFewAdultsException() {
        super("There must be at least one adult ticket per transaction, and one adult ticket per infant ticket");
    }
}
