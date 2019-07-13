package de.huemmerich.web.wsobjectstore;

/**
 * Exception thrown during JSON-HAL-object retrieval
 */
public class WSObjectStoreException extends Exception {

    /**
     * "Direct" Exception without root cause
     * @param errorMessage The message of the exception
     */
    public WSObjectStoreException(String errorMessage) {
        super(errorMessage);
    }

    /**
     * "Indirect" exception with root cause
     * @param errorMessage The message of the exception
     * @param cause The root cause
     */
    public WSObjectStoreException(String errorMessage, Throwable cause) {
        super(errorMessage,cause);
    }

}
