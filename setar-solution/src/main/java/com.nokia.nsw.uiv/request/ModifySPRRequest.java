package com.nokia.nsw.uiv.request;


import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;

@Getter
@Setter
public class ModifySPRRequest {
    @NotNull
    private String subscriberName;
    @NotNull
    private String productType;
    @NotNull
    private String productSubtype;
    @NotNull
    private String ontSN;
    @NotNull
    private String serviceId;
    @NotNull
    private String modifyType;
    private String modifyParam1;
    private String modifyParam2;
    private String modifyParam3;
    private String fxOrderId;
    private String templateNameVEIP;
    private String ontModel;
    private String templateNameVLAN;
}

