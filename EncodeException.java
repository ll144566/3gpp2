package com.quectel.jnitestexec.cdma2;

public class EncodeException extends Exception {

    private int mError = ERROR_UNENCODABLE;

    public static final int ERROR_UNENCODABLE = 0;
    public static final int ERROR_EXCEED_SIZE = 1;

    public EncodeException() {
        super();
    }

    public EncodeException(String s) {
        super(s);
    }

    public EncodeException(String s, int error) {
        super(s);
        mError = error;
    }

    public EncodeException(char c) {
        super("Unencodable char: '" + c + "'");
    }

    public int getError() {
        return mError;
    }
}
