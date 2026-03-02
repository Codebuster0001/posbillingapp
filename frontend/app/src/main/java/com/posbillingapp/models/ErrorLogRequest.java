package com.posbillingapp.models;

public class ErrorLogRequest {
    public String logType;
    public String referenceId;
    public String errorMessage;
    public String stackTrace;
    public String deviceInfo;
    public String createdBy;

    public ErrorLogRequest(String logType, String referenceId, String errorMessage, String stackTrace, String deviceInfo, String createdBy) {
        this.logType = logType;
        this.referenceId = referenceId;
        this.errorMessage = errorMessage;
        this.stackTrace = stackTrace;
        this.deviceInfo = deviceInfo;
        this.createdBy = createdBy;
    }
}
