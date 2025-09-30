package com.nokia.nsw.uiv.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChangeTechnologyResponse {

    private String status;
    private String message;
    private String timestamp;
    private String subscriptionName;
    private String ontName;
}
