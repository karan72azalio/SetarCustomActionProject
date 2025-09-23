package com.nokia.nsw.uiv.response;

import co.elastic.clients.util.AllowForbiddenApis;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QueryAllEquipmentResponse {
    private String status;
    private String message;
    private String timestamp;
    private String stbSerialNo1;
    private String stbSerialNo2;
    private String stbSerialNo3;
    private String stbCustomerGroupID1;
    private String stbModel2;
    private String stbModel3;
    private String stbCustomerGroupID2;
    private String stbCustomerGroupID3;
    private String stbPreSharedKey3;
    private String apSerialNo1;
    private String stbModel1;
    private String stbPreSharedKey2;
    private String stbPreSharedKey1;
    private String apSerialNo2;
    private String apPreShareKey1;
    private String apPreShareKey2;
    private String apModel1;
    private String stbMacAddr2;
    private String stbMacAddr1;
    private String stbMacAddr3;
    private String apModel2;
    private String apMacAddr2;
    private String apMacAddr1;

    public QueryAllEquipmentResponse(String status, String message, String timestamp, String stbSerialNo1, String stbSerialNo2, String stbSerialNo3, String stbCustomerGroupID1, String stbModel2, String stbModel3, String stbCustomerGroupID2, String stbCustomerGroupID3, String stbPreSharedKey3, String apSerialNo1, String stbModel1, String stbPreSharedKey2, String stbPreSharedKey1, String apSerialNo2, String apPreShareKey1, String apPreShareKey2, String apModel1, String stbMacAddr2, String stbMacAddr1, String stbMacAddr3, String apModel2, String apMacAddr2, String apMacAddr1) {
        this.status = status;
        this.message = message;
        this.timestamp = timestamp;
        this.stbSerialNo1 = stbSerialNo1;
        this.stbSerialNo2 = stbSerialNo2;
        this.stbSerialNo3 = stbSerialNo3;
        this.stbCustomerGroupID1 = stbCustomerGroupID1;
        this.stbModel2 = stbModel2;
        this.stbModel3 = stbModel3;
        this.stbCustomerGroupID2 = stbCustomerGroupID2;
        this.stbCustomerGroupID3 = stbCustomerGroupID3;
        this.stbPreSharedKey3 = stbPreSharedKey3;
        this.apSerialNo1 = apSerialNo1;
        this.stbModel1 = stbModel1;
        this.stbPreSharedKey2 = stbPreSharedKey2;
        this.stbPreSharedKey1 = stbPreSharedKey1;
        this.apSerialNo2 = apSerialNo2;
        this.apPreShareKey1 = apPreShareKey1;
        this.apPreShareKey2 = apPreShareKey2;
        this.apModel1 = apModel1;
        this.stbMacAddr2 = stbMacAddr2;
        this.stbMacAddr1 = stbMacAddr1;
        this.stbMacAddr3 = stbMacAddr3;
        this.apModel2 = apModel2;
        this.apMacAddr2 = apMacAddr2;
        this.apMacAddr1 = apMacAddr1;
    }

    public QueryAllEquipmentResponse() {

    }
}
