package com.grelobites.romgenerator.util;


import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class CompletedTask implements Future<OperationResult> {

    private static final Future<OperationResult> SUCCESS_TASK = new CompletedTask(OperationResult.successResult());
    private OperationResult operationResult;

    public static Future<OperationResult> successTask() {
        return SUCCESS_TASK;
    }

    public CompletedTask(OperationResult operationResult) {
        this.operationResult = operationResult;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return true;
    }

    @Override
    public OperationResult get() throws InterruptedException, ExecutionException {
        return operationResult;
    }

    @Override
    public OperationResult get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return operationResult;
    }
}
