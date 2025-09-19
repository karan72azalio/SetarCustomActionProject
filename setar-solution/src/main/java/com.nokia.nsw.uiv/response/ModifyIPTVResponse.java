package com.nokia.nsw.uiv.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ModifyIPTVResponse {

    private String status;           // STATUS
    private String message;          // MESSAGE
    private String timestamp;        // TIMESTAMP
    private String subscriberId;     // SUBSCRIBER_ID
    private String subscriptionId;   // SUBSCRIPTION_ID

    public ModifyIPTVResponse(String number, String s, String s1, String s2, String s3) {
    }
}
