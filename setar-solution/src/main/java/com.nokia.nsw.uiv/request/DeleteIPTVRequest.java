package com.nokia.nsw.uiv.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeleteIPTVRequest {
    private String subscriberName;   // Mandatory
    private String productType;      // Mandatory
    private String productSubtype;   // Mandatory
    private String serviceId;        // Mandatory
    private String serviceFlag;      // Optional
    private String ontSN;            // Mandatory
}
