package com.nokia.nsw.uiv.response;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class QueryServiceResponse {
    private String status;
    private String message;
    private String timestamp;
    private boolean success;
    private Map<String, Object> iptvinfo;

    public QueryServiceResponse() {}

    public QueryServiceResponse(String status, String message, String timestamp, boolean success, Object details) {
        this.status = status;
        this.message = message;
        this.timestamp = timestamp;
        this.success = success;
        if (details instanceof Map) {
            this.iptvinfo = (Map<String, Object>) details;
        }
    }
}
