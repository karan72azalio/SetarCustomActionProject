package com.nokia.nsw.uiv.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QueryAddrByServiceIDResponse {
    private String status;
    private String message;
    private String timestamp;

    // Output parameters
    private String productName;
    private String address;
    private String serviceLink;
}
