package uk.gov.dwp.uc.pairtest.exception;

public class TooManyTicketsException extends InvalidPurchaseException {

    public TooManyTicketsException() {
        super("Cannot purchase more than 20 tickets in one transaction");
    }
}
