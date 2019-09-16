package com.gipl.sendemail.sendemail;

/**
 * Created by Ankit on 16-Sep-19.
 */
public class EmailSendResult {
    private STATUS status;
    private String extraInfo;
    private Exception exception;

    public EmailSendResult(STATUS status,  Exception... exception) {
        this.status = status;
        this.exception = exception[0];
    }

    public EmailSendResult(STATUS status, String extraInfo , Exception... exception) {
        this.status = status;
        this.extraInfo = extraInfo;
        this.exception = exception[0];
    }

    public STATUS getStatus() {
        return status;
    }

    public Exception getException() {
        return exception;
    }

    public String getExtraInfo() {
        return extraInfo;
    }


    public enum STATUS{
        EMAIL_SEND_STARTED,
        ACCOUNT_PICKUP,
        ACCOUNT_PICK_UP_ERROR,
        EMAIL_SEND_SUCCESS,
        EMAIL_SEND_FAILED,
    }
}
