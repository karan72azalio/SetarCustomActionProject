package com.nokia.nsw.uiv.response;

import lombok.Getter;
import lombok.Setter;
import javax.validation.constraints.NotNull;
import java.util.List;

@Getter
@Setter
public class QueryEquipmentResponse {

    @NotNull
    private String status;            // M

    @NotNull
    private String message;           // M

    @NotNull
    private String timestamp;         // M

    @NotNull
    private String subscriptionId;    // M

    // For repeating apSn_1 … apSn_5
    private List<String> apSns;       // C

    // For repeating stbSn_1 … stbSn_5
    private List<String> stbSns;      // C
}
