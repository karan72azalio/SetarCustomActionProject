package com.nokia.nsw.uiv.request;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;

@Getter
@Setter
public class QueryFlagsRequest {
    @NotNull
    private String subscriberName;
    @NotNull
    private String productType;
    @NotNull
    private String productSubtype;
    private String actionType;
    @NotNull
    private String ontSN;
    @NotNull
    private String ontPort;
    private String serviceId;
}
