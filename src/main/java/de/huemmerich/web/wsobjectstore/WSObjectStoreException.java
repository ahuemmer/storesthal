package de.huemmerich.web.wsobjectstore;

public class WSObjectStoreException extends Exception {

    public WSObjectStoreException(String errorMessage) {
        super(errorMessage);
    }

    public WSObjectStoreException(String errorMessage, Throwable cause) {
        super(errorMessage,cause);
    }

}
