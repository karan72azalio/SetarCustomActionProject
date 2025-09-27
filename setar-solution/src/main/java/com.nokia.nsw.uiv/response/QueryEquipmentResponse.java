package com.nokia.nsw.uiv.response;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;

@Getter
@Setter
public class QueryEquipmentResponse {

    @NotNull
    private String status;            // M - HTTP status code

    @NotNull
    private String message;           // M - Success or error message

    @NotNull
    private String timestamp;         // M - Timestamp when response was generated

    @NotNull
    private String subscriptionId;    // M - Subscription identifier

    // Conditional AP serial numbers (up to 5)
    private String apSn1;             // C
    private String apSn2;             // C
    private String apSn3;             // C
    private String apSn4;             // C
    private String apSn5;             // C

    // Conditional STB serial numbers (up to 5)
    private String stbSn1;            // C
    private String stbSn2;            // C
    private String stbSn3;            // C
    private String stbSn4;            // C
    private String stbSn5;            // C
}
