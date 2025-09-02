package com.nokia.nsw.uiv.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ModifySPRResponse {
    private String status;
    private String message;
    private String timestamp;
    private String ontName;
    private String subscriptionId;
}
