package com.nokia.nsw.uiv.request;

import javax.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request DTO for ModifyServiceId
 */
@Data
public class ModifyServiceIdRequest {

    @NotNull
    private String serviceId; // old service ID

    @NotNull
    private String serviceIdNew; // new service ID
}
