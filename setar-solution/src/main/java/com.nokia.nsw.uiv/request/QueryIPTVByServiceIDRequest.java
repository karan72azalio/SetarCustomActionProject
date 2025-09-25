package com.nokia.nsw.uiv.request;

import lombok.Getter;
import lombok.Setter;
import javax.validation.constraints.NotNull;

@Getter
@Setter
public class QueryIPTVByServiceIDRequest {

    @NotNull
    private String serviceID;   // M
}
