package com.nokia.nsw.uiv.request;

import javax.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request DTO for QueryONTPosition
 */
@Data
public class QueryONTPositionRequest {

    @NotNull
    private String ontSn; // Serial number of the ONT device
}
