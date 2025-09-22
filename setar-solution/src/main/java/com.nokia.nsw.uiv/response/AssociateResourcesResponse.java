package com.nokia.nsw.uiv.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response class for AssociateResources custom action.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AssociateResourcesResponse {

    private String status;       // HTTP code (200, 400, 404, 409, 500)
    private String message;      // Success or error message
    private String timestamp;    // ISO timestamp
    private String subscriptionId; // Subscription identifier

}
