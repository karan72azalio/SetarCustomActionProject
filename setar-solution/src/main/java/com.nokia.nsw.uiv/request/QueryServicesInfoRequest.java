package com.nokia.nsw.uiv.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QueryServicesInfoRequest {
    private String subscriberName; // SUBSCRIBER_NAME
    private String ontSn;          // ONT_SN
}
