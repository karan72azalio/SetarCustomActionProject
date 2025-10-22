package com.nokia.nsw.uiv.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QueryServicesInfoResponse {
    private String status;
    private String message;
    private String timestamp;

    private Map<String, Object> structuredObject;
}
