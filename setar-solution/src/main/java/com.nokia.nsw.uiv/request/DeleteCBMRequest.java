package com.nokia.nsw.uiv.request;


import lombok.Getter;
import lombok.Setter;
import javax.validation.constraints.NotNull;

@Getter
@Setter
public class DeleteCBMRequest {

    @NotNull
    private String subscriberName;

    @NotNull
    private String serviceId;

    @NotNull
    private String productType;

    @NotNull
    private String productSubtype;

    private String serviceFlag;
    private String serviceLink;
    private String cbmSN; // Optional
}
