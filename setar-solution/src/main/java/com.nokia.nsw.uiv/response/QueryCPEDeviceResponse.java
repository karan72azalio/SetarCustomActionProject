package com.nokia.nsw.uiv.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QueryCPEDeviceResponse {
    private String status;
    private String message;
    private long timestamp;
    private String resourceModel;
    private String resourceModelMTA;
    private String resourceGWMac;
    private String resourceInventoryType;
    private String resourceMac;
    private String resourceMacMTA;
    private String resourceManufacturer;
    private String resourceSN;
    private String resourceModelSubtype;
    private String resourceStatus;
    private String resourceDescription;
    private String resourceVoicePort1;
    private String resourceVoicePort2;
    private String resourceDataPort1;
    private String resourceDataPort2;
    private String resourceDataPort3;
    private String resourceDataPort4;
    private String resourceDataPort5;

    public void setDataPortStatus(int portNumber, String status) {
        switch (portNumber) {
            case 1:
                this.resourceDataPort1 = status;
                break;
            case 2:
                this.resourceDataPort2 = status;
                break;
            case 3:
                this.resourceDataPort3 = status;
                break;
            case 4:
                this.resourceDataPort4 = status;
                break;
            case 5:
                this.resourceDataPort5 = status;
                break;
        }
    }

    public QueryCPEDeviceResponse(String status, String message, long timestamp) {
        this.status = status;
        this.message = message;
        this.timestamp = timestamp;
    }

    public QueryCPEDeviceResponse() {
    }
}
