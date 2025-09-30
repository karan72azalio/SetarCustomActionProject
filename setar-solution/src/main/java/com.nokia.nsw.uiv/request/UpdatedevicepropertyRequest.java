package com.nokia.nsw.uiv.request;

import javax.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request DTO for Updatedeviceproperty
 */
@Data
public class UpdatedevicepropertyRequest {

    @NotNull
    private String stbSn1; // STB Serial Number

    @NotNull
    private String customerGroupId; // Customer Group ID to update
}
