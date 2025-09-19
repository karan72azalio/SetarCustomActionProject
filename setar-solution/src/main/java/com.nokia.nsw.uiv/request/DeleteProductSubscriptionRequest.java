package com.nokia.nsw.uiv.request;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;

@Getter
@Setter
public class DeleteProductSubscriptionRequest {
    @NotNull
    private String subscriberName;
    @NotNull
    private String serviceID;
    @NotNull
    private String productType;
    @NotNull
    private String componentName;
    private String fxOrderID; // optional
}
