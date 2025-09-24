package com.nokia.nsw.uiv.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response class for CreateServiceVoIP action.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateServiceVoIPResponse {

    private String status;
    private String message;
    private String timestamp;

    private String subscriptionName;
    private String ontName;
}
