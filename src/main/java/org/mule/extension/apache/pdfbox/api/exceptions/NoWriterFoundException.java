package org.mule.extension.apache.pdfbox.api.exceptions;

public class NoWriterFoundException extends Exception {
    public NoWriterFoundException(String errorMessage) {
        super(errorMessage);
    }
}