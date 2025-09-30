package com.nokia.nsw.uiv.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Flat response DTO for CreateServiceEVPN
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateServiceEVPNResponse {
    private String status;
    private String message;
    private String timestamp;
    private String subscriptionName;
    private String ontName;
}
