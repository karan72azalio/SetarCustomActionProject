package com.nokia.nsw.uiv.request;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;

@Getter
@Setter
public class ModifyIPTVRequest {

    @NotNull
    private String subscriberName;   // SUBSCRIBER_NAME
    @NotNull
    private String productType;      // PRODUCT_TYPE
    @NotNull
    private String productSubtype;   // PRODUCT_SUB_TYPE
    @NotNull
    private String serviceId;        // SERVICE_ID
    @NotNull
    private String modifyType;       // MODIFY_TYPE

    private String modifyParam1;     // MODIFY_PARAM1
    private String modifyParam2;     // MODIFY_PARAM2
    private String modifyParam3;     // MODIFY_PARAM3
    private String gatewayMac;       // GATEWAY_MAC
    private String fxOrderID;        // FX_ORDERID
}
