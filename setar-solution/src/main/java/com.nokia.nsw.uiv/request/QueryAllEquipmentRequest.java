package com.nokia.nsw.uiv.request;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;

@Getter
@Setter
public class QueryAllEquipmentRequest {
    @NotNull
    private String subscriberName;
    @NotNull
    private String serviceId;
}
