package com.nokia.nsw.uiv.request;

import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QueryResourceRequest {

    @NotNull
    private String resourceSN;

    @NotNull
    private String resourceType;
}
