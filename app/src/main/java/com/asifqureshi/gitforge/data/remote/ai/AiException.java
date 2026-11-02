package com.asifqureshi.gitforge.data.remote.ai;

/** An AI request failure with a message that is safe to show (no keys, no request content). */
public class AiException extends Exception {
    public AiException(String safeMessage) {
        super(safeMessage);
    }
}
