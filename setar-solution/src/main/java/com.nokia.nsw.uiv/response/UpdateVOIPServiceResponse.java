package com.nokia.nsw.uiv.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UpdateVOIPServiceResponse {
    private String status;
    private String message;
    private String timestamp;
    private String subscriberName;
    private String serviceId;
}
