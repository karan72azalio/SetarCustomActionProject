package com.nokia.nsw.uiv.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
public class DeleteIPTVResponse {
    private String status;
    private String message;
    private String timestamp;
    private String subscriptionId;
    private String ontName;

    public DeleteIPTVResponse(String status, String message, String timestamp) {
        this.status = status;
        this.message = message;
        this.timestamp = timestamp;
    }
}
