package com.nokia.nsw.uiv.request;

import javax.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AccountTransferByServiceIDRequest {

    @NotNull
    private String subscriberNameOld;

    @NotNull
    private String subscriberName;

    @NotNull
    private String serviceId;

    private String kenanUidNo;
}
