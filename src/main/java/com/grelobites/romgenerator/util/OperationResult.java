package com.grelobites.romgenerator.util;

public class OperationResult {
    public static final int OK = 0;
    public static final int ERROR = -1;

    private int code;
    private String context;
    private String message;
    private String detail;

    public OperationResult(int code) {
        this.code = code;
    }

    public OperationResult(int code, String context, String message) {
        this(code);
        this.context = context;
        this.message = message;
    }

    public OperationResult(int code, String context, String message, String detail) {
        this(code, context, message);
        this.detail = detail;
    }

    public static OperationResult successResult() {
        return new OperationResult(OK);
    }

    public static OperationResult errorResult(String context, String message) {
        return new OperationResult(ERROR, context, message);
    }

    public static OperationResult errorWithDetailResult(String context, String message, String detail) {
        return new OperationResult(ERROR, context, message, detail);
    }

    public boolean isError() {
        return code == ERROR;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }
}
