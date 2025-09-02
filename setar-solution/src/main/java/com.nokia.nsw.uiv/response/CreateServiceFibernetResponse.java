package com.nokia.nsw.uiv.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateServiceFibernetResponse {

    private String status;
    private String message;
    private String timestamp;
    private String subscriptionName;
    private String ontName;

}
