package com.nokia.nsw.uiv.request;


import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;

@Getter
@Setter
public class ChangeStateRequest {
    @NotNull
    private String subscriberName;   // M
    private String productType;      // O
    private String productSubtype;   // O
    @NotNull
    private String serviceId;        // M
    @NotNull
    private String actionType;       // M (Suspend | Resume)
    private String fxOrderId;        // O
    private String serviceLink;      // O (ONT | Cable_Modem)
    private String cbmMac;           // O
    private String ontSN;            // O
}

