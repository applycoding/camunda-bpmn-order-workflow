package com.arpanbom.orderworkflow.workflow;

public class WorkflowUnavailableException extends RuntimeException {

    public WorkflowUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
