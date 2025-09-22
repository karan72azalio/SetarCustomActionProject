package com.nokia.nsw.uiv.request;

import lombok.Getter;
import lombok.Setter;
import javax.validation.constraints.NotNull;

/**
 * Request class for AssociateResources custom action.
 */
@Getter
@Setter
public class AssociateResourcesRequest {

    @NotNull
    private String subscriberName;

    @NotNull
    private String serviceId;

    @NotNull
    private String productSubType;

    private String ontSN; // optional
    private String cbmSN; // optional
    private String fxOrderID; // optional

    // Mandatory STB and AP serials for IPTV
    @NotNull private String stbSN1;
    @NotNull private String stbSN2;
    @NotNull private String stbSN3;
    @NotNull private String stbSN4;
    @NotNull private String stbSN5;

    @NotNull private String apSN1;
    @NotNull private String apSN2;
    @NotNull private String apSN3;
    @NotNull private String apSN4;
    @NotNull private String apSN5;

    // Optional STB 6–20
    private String stbSN6;
    private String stbSN7;
    private String stbSN8;
    private String stbSN9;
    private String stbSN10;
    private String stbSN11;
    private String stbSN12;
    private String stbSN13;
    private String stbSN14;
    private String stbSN15;
    private String stbSN16;
    private String stbSN17;
    private String stbSN18;
    private String stbSN19;
    private String stbSN20;

    // Optional AP 6–20
    private String apSN6;
    private String apSN7;
    private String apSN8;
    private String apSN9;
    private String apSN10;
    private String apSN11;
    private String apSN12;
    private String apSN13;
    private String apSN14;
    private String apSN15;
    private String apSN16;
    private String apSN17;
    private String apSN18;
    private String apSN19;
    private String apSN20;

    // Optional Customer Group IDs (for STBs)
    private String customerGroupID1;
    private String customerGroupID2;
    private String customerGroupID3;
    private String customerGroupID4;
    private String customerGroupID5;
    private String customerGroupID6;
    private String customerGroupID7;
    private String customerGroupID8;
    private String customerGroupID9;
    private String customerGroupID10;
    private String customerGroupID11;
    private String customerGroupID12;
    private String customerGroupID13;
    private String customerGroupID14;
    private String customerGroupID15;
    private String customerGroupID16;
    private String customerGroupID17;
    private String customerGroupID18;
    private String customerGroupID19;
    private String customerGroupID20;
}
