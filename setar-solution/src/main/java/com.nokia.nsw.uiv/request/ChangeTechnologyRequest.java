package com.nokia.nsw.uiv.request;

import javax.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ChangeTechnologyRequest {

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

    private String qosProfile;

    @NotNull
    private String oltName;

    private String templateNameOnt;
    private String templateNameVeip;
    private String templateNameHsi;
    private String templateNameIptv;
    private String templateNameIgmp;

    @NotNull
    private String menm;

    @NotNull
    private String vlanId;

    @NotNull
    private String ontModel;

    @NotNull
    private String cbmMac;

    private String hhid;
}
