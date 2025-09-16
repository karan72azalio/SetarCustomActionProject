package com.nokia.nsw.uiv.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class CreateProductSubscriptionResponse {
    private String status;
    private String message;
    private String timestamp;
    private String subscriptionName;
    private String productName;
}
