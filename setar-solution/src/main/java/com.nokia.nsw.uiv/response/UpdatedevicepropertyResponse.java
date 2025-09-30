package com.nokia.nsw.uiv.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for Updatedeviceproperty
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdatedevicepropertyResponse {
    private String status;
    private String message;
    private String timestamp;
    private String stbSn1;
    private String customerGroupId;
}
