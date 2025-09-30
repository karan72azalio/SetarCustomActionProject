package com.nokia.nsw.uiv.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for ModifySubscriber
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ModifySubscriberResponse {
    private String status;
    private String message;
    private String timestamp;
}
