package com.nokia.nsw.uiv.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for ChangeResourceStatus
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChangeResourceStatusResponse {
    private String status;
    private String message;
    private String timestamp;
    private String resourceSn;
    private String resourceMac;
    private String resourceStatus;
    private String resourceModel;
    private String resourceType;
}
