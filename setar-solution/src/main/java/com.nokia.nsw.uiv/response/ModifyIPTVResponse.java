package com.nokia.nsw.uiv.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ModifyIPTVResponse {

    private String status;           // STATUS
    private String message;          // MESSAGE
    private String timestamp;        // TIMESTAMP
    private String subscriberId;     // SUBSCRIBER_ID
    private String subscriptionId;   // SUBSCRIPTION_ID
}
