package com.nokia.nsw.uiv.request;

import javax.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request DTO for ChangeTechnologyVoice
 */
@Data
public class ChangeTechnologyVoiceRequest {

    @NotNull
    private String subscriberName;

    @NotNull
    private String productSubtype;

    @NotNull
    private String serviceId;

    private String fxOrderId;

    @NotNull
    private String ontSN;

    @NotNull
    private String ontMacAddr;

    @NotNull
    private String cbmSn;

    @NotNull
    private String oltName;

    @NotNull
    private String templateNamePots1;

    @NotNull
    private String templateNamePots2;

    @NotNull
    private String voipPackage;

    @NotNull
    private String voipServiceCode;

    private String voipServiceTemplate; // optional

    @NotNull
    private String ontModel;

    @NotNull
    private String cbmMac;

    private String hhid;

    @NotNull
    private String ontPort; // "1" or "2"

    private String simaCustId;     // optional
    private String simaSubsId;     // optional
    private String serviceEndpointNumber1; // optional
    private String serviceEndpointNumber2; // optional
}
