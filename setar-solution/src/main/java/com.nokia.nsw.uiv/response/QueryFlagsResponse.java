package com.nokia.nsw.uiv.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
public class QueryFlagsResponse {
    private String status;
    private String message;
    private String timestamp;
    private Map<String, String> flags;
}
