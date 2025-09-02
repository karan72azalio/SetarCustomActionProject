package com.nokia.nsw.uiv.request;

import lombok.Getter;
import lombok.Setter;
import javax.validation.constraints.NotNull;

@Getter
@Setter
public class ModifyCBMRequest {

    @NotNull
    private String subscriberName;
    @NotNull
    private String serviceId;
    @NotNull
    private String productType;
    @NotNull
    private String productSubtype;

    @NotNull
    private String resourceSN;
    private String modifyType;
    private String modifyParam1;
    private String modifyParam2;
    private String cbmModel;
    private String fxOrderId;
}

