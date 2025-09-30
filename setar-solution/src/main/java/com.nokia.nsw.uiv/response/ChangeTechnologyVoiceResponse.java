package com.nokia.nsw.uiv.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for ChangeTechnologyVoice
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChangeTechnologyVoiceResponse {
    private String status;
    private String message;
    private String timestamp;
    private String subscriptionName;
    private String ontName;
}
