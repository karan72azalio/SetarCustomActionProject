package com.nokia.nsw.uiv.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QueryAddrByServiceIDRequest {
    private String serviceId; // SERVICE_ID (mandatory)
}
