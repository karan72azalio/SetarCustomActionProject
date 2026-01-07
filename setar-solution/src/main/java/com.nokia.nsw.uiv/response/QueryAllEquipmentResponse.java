package com.nokia.nsw.uiv.response;

import co.elastic.clients.util.AllowForbiddenApis;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class QueryAllEquipmentResponse {
    private String status;
    private String message;
    private String timestamp;
    private Map<String,String> equipmentsInfo;

    public QueryAllEquipmentResponse(String status, String message, String timestamp, Map<String,String> equipmentsInfo) {
        this.status = status;
        this.message = message;
        this.timestamp = timestamp;
        this.equipmentsInfo = equipmentsInfo;
    }

    public QueryAllEquipmentResponse() {

    }
}
