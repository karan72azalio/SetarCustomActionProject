package com.nokia.nsw.uiv.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for ModifyServiceId
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ModifyServiceIdResponse {
    private String status;
    private String message;
    private String timestamp;
}
