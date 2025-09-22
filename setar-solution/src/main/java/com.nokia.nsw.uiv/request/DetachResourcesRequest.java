package com.nokia.nsw.uiv.request;

import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DetachResourcesRequest {

    @NotNull
    private String subscriberName;

    @NotNull
    private String ontSN;

    @NotNull
    private String serviceID;

    @NotNull
    private String productSubType;

    @NotNull
    private String stbSN1;

    @NotNull
    private String stbSN2;

    @NotNull
    private String stbSN3;

    @NotNull
    private String stbSN4;

    @NotNull
    private String stbSN5;

    @NotNull
    private String apSN1;

    @NotNull
    private String apSN2;

    @NotNull
    private String apSN3;

    @NotNull
    private String apSN4;

    @NotNull
    private String apSN5;

    private String fxOrderId; // Optional
}
