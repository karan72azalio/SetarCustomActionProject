package com.nokia.nsw.uiv.request;

import javax.validation.constraints.NotNull;
import lombok.Data;

@Data
public class QueryAllServicesByCPERequest {

    @NotNull
    private String ontSn;
}
