package com.problemsolving.platform.importing.hf;

/** Thrown when the Hugging Face datasets-server cannot be reached or returns unusable data. */
public class HuggingFaceClientException extends RuntimeException {

    public HuggingFaceClientException(String message) {
        super(message);
    }

    public HuggingFaceClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
