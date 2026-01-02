package com.nokia.nsw.uiv.request;

import lombok.Getter;
import lombok.Setter;
import javax.validation.constraints.NotNull;

@Getter
@Setter
public class QueryEquipmentRequest {

    @NotNull
    private String subscriberName;    // M

    @NotNull
    private String serviceId;         // M

    @NotNull
    private String resourceSn;        // M

    @NotNull
    private String productType;       // M

    @NotNull
    private String productSubtype;    // M

    private String serviceLink;       // O
}
