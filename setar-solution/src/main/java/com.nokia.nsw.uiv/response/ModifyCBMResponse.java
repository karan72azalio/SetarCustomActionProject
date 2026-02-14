package com.nokia.nsw.uiv.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ModifyCBMResponse {
    private String status;
    private String message;
    private String timestamp;
    private String subscriberId;
    private String subscriptionId;

    public ModifyCBMResponse() {
    }
}

