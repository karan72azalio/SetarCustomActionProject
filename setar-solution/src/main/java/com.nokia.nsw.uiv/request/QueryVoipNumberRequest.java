package com.nokia.nsw.uiv.request;

import lombok.Data;
import javax.validation.constraints.NotNull;

@Data
public class QueryVoipNumberRequest {

    private String subscriberName;  // optional
    private String serviceId;       // optional

    @NotNull(message = "ontSN is mandatory")
    private String ontSN;

    private String serviceLink;     // optional
}
