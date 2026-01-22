package com.nokia.nsw.uiv.response;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Response for QueryAllServicesByCPE.
 * Uses a dynamic properties map to support variable service count fields
 * (e.g., Broadband_1_..., Broadband_2_..., IPTV_1_STB_SN_1, etc.).
 */
@Getter
@Setter
@NoArgsConstructor
public class QueryAllServicesByCPEResponse {

    private String status;
    private String message;
    private String timestamp;

    // Dynamic output properties (flattened into the root JSON object)
    private Map<String, Object> outputProperties = new LinkedHashMap<>();

    public QueryAllServicesByCPEResponse(String status, String message, String timestamp,
            Map<String, Object> properties) {
        this.status = status;
        this.message = message;
        this.timestamp = timestamp;
        if (properties != null) {
            this.outputProperties = properties;
        }
    }

    @JsonAnyGetter
    public Map<String, Object> getOutputProperties() {
        return outputProperties;
    }

    @JsonAnySetter
    public void setOutputProperty(String key, Object value) {
        this.outputProperties.put(key, value);
    }

    public void put(String key, Object value) {
        if (value != null) {
            this.outputProperties.put(key, value);
        }
    }
}
