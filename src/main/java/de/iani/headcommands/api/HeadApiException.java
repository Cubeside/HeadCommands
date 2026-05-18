package de.iani.headcommands.api;

public class HeadApiException extends Exception {
    private static final long serialVersionUID = 3181295755763334301L;

    public HeadApiException(String message) {
        super(message);
    }

    public HeadApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
