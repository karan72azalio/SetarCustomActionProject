package com.nokia.nsw.uiv.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

/**
 * Response DTO for QueryService (flat fields but flexible map for IPTV details)
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class QueryServiceResponse {
    private String status;
    private String message;
    private String timestamp;
    private Map<String, String> iptvinfo;
}
