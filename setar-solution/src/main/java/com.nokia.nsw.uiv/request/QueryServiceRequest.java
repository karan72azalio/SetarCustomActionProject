package com.nokia.nsw.uiv.request;

import javax.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request DTO for QueryService
 */
@Data
public class QueryServiceRequest {

    @NotNull
    private String serviceId;
}
