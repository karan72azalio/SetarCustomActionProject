// ============================================================================
// DTO: DeleteSPRResponse
// Package: com.nokia.nsw.uiv.response
// ============================================================================
package com.nokia.nsw.uiv.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter @AllArgsConstructor
public class DeleteSPRResponse {
    private String status;        // e.g., "200", "400", "500"
    private String message;       // success or error message (with ERROR_PREFIX for errors)
    private String timestamp;     // ISO-8601
    private String ontName;       // ONT_NAME
    private String subscriptionId; // SUBSCRIPTION_ID
}

