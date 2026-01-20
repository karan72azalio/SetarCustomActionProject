package com.nokia.nsw.uiv.response;

import lombok.Getter;
import lombok.Setter;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Getter
@Setter
public class QueryIPTVByServiceIDResponse {

    @NotNull
    private String status;                         // M
    @NotNull
    private String message;                        // M
    @NotNull
    private String timestamp;                      // M
    @NotNull
    private Map<String, String> iptvInfo;
}
