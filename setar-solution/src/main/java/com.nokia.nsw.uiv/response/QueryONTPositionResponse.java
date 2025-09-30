package com.nokia.nsw.uiv.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for QueryONTPosition
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class QueryONTPositionResponse {
    private String status;
    private String message;
    private String timestamp;
    private String ontObjectId;
}
