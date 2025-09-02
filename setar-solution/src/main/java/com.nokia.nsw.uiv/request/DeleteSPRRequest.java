// ============================================================================
// DTO: DeleteSPRRequest
// Package: com.nokia.nsw.uiv.request
// ============================================================================
package com.nokia.nsw.uiv.request;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;

@Getter @Setter
public class DeleteSPRRequest {

    @NotNull
    private String subscriberName;   // SUBSCRIBER_NAME

    @NotNull
    private String productType;      // PRODUCT_TYPE

    @NotNull
    private String productSubtype;   // PRODUCT_SUB_TYPE

    @NotNull
    private String serviceId;        // SERVICE_ID

    private String serviceFlag;      // SERVICE_FLAG (O)

    private String ontPort;          // ONT_PORT (O)

    @NotNull
    private String ontSN;            // OTN_SN (ONT serial number)
}

