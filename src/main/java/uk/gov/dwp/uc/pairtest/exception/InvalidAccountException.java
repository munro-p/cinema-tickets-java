package uk.gov.dwp.uc.pairtest.exception;

public class InvalidAccountException extends InvalidPurchaseException {

    // Stacktrace/error details perhaps not necessary since most of these (barring maybe the null
    // checks) would be used to pass feedback to user/log failure details
    public InvalidAccountException() {
        super("Invalid account or funds. Cannot purchase tickets using this account.");
    }
}
