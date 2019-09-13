package de.huemmerich.web.storesthal;

/**
 * Exception thrown during JSON-HAL-object retrieval
 */
public class StoresthalException extends Exception {

    /**
     * "Direct" Exception without root cause
     * @param errorMessage The message of the exception
     */
    public StoresthalException(String errorMessage) {
        super(errorMessage);
    }

    /**
     * "Indirect" exception with root cause
     * @param errorMessage The message of the exception
     * @param cause The root cause
     */
    public StoresthalException(String errorMessage, Throwable cause) {
        super(errorMessage,cause);
    }

}
