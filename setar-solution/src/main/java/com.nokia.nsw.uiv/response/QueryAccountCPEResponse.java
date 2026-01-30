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
}
