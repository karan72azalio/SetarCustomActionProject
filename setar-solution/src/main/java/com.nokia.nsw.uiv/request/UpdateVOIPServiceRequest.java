package com.nokia.nsw.uiv.request;

import javax.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateVOIPServiceRequest {

    @NotNull
    private String subscriberName;

    @NotNull
    private String ontSN;

    @NotNull
    private String simaSubsId;

    private String simaCustId;
    private String simaEndpointId;

    @NotNull
    private String serviceId;
}
