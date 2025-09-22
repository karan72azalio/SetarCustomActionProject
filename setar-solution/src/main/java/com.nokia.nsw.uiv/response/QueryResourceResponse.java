package com.nokia.nsw.uiv.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class QueryResourceResponse {
    private String status;
    private String message;
    private String timestamp;

    private String resourceSN;
    private String resourceMAC;
    private String resourceStatus;
    private String resourceModel;
    private String resourceModelSubType;
    private String resourcePKey;
    private String resourceType;
    private String resourceManufacturer;
    private String resourceGroupID;
    private String resourceDescription;
    private String resourceGwMAC;
}
