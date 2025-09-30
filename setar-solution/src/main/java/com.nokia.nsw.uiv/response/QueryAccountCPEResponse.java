package com.nokia.nsw.uiv.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for QueryAccountCPE.
 * Flat object – no maps, only fields explicitly listed in spec.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class QueryAccountCPEResponse {

    private String status;
    private String message;
    private String timestamp;

    private String broadbandOntSn;
    private String broadbandCpeSn;
    private String broadbandCbmMac;
    private String broadbandServiceLink;
    private String broadbandInventoryType;
    private String broadbandGatewayMac;
    private String accountNumber;
    private String broadbandServiceId;
    private String broadbandDeviceModel;
    private String voiceMtaMac;
    private String voice1ServiceId;
    private String voice2ServiceId;

    public QueryAccountCPEResponse(String number, String s, String string, Object o, Object o1, Object o2, Object o3, Object o4, Object o5, Object o6, Object o7, Object o8, Object o9, Object o10) {
    }
}
