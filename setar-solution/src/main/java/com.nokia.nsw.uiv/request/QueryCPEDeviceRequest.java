package com.nokia.nsw.uiv.request;

import lombok.Getter;
import lombok.Setter;
import javax.validation.constraints.NotNull;

@Getter
@Setter
public class QueryCPEDeviceRequest {
    @NotNull
    private String resourceSN;
    @NotNull
    private String resourceType;
}
