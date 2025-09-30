package com.nokia.nsw.uiv.request;

import javax.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request DTO for ChangeResourceStatus
 */
@Data
public class ChangeResourceStatusRequest {

    @NotNull
    private String resourceSn;   // Serial Number

    @NotNull
    private String resourceType; // Resource Type

    @NotNull
    private String resourceStatus; // Target Status (Available/Allocated)
}
