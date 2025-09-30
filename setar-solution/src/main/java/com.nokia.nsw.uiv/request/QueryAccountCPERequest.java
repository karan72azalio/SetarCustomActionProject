package com.nokia.nsw.uiv.request;

import javax.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request DTO for QueryAccountCPE
 */
@Data
public class QueryAccountCPERequest {

    @NotNull
    private String subscriberName;

    @NotNull
    private String serviceId;
}
