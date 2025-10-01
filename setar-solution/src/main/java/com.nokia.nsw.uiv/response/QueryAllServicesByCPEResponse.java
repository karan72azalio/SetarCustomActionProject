package com.nokia.nsw.uiv.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Flat response for QueryAllServicesByCPE.
 * All fields use counters and prefixes per service family.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class QueryAllServicesByCPEResponse {

    private String status;
    private String message;
    private String timestamp;

    // Service counters
    private String bbCount;
    private String voiceCount;
    private String entCount;
    private String iptvCount;

    // --- Broadband sample fields ---
    private String broadband1ServiceId;
    private String broadband1ServiceSubtype;
    private String broadband1ServiceType;
    private String broadband1QosProfile;
    private String broadband1OntTemplate;
    private String broadband1ServiceTemplateHsi;
    private String broadband1ServiceTemplateVeip;
    private String broadband1Hhid;
    private String broadband1AccountNumber;
    private String broadband1FirstName;
    private String broadband1LastName;
    private String broadband1Email;
    private String broadband1EmailPassword;
    private String broadband1CompanyName;
    private String broadband1ContactPhone;
    private String broadband1SubsAddress;

    // --- Voice sample fields ---
    private String voice1ServiceId;
    private String voice1ServiceSubtype;
    private String voice1ServiceType;
    private String voice1CustomerId;
    private String voice1SimaSubsId;
    private String voice1SimaEndpointId;
    private String voice1VoipNumber1;
    private String voice1VoipCode1;
    private String voice1QosProfile;
    private String voice1OntTemplate;
    private String voice1ServiceTemplateVoip;
    private String voice1ServiceTemplatePots1;
    private String voice1ServiceTemplatePots2;
    private String voice1FirstName;
    private String voice1LastName;

    // --- Enterprise sample fields ---
    private String enterprise1ServiceId;
    private String enterprise1ServiceSubtype;
    private String enterprise1ServiceType;
    private String enterprise1QosProfile;
    private String enterprise1KenanSubsId;
    private String enterprise1Port;
    private String enterprise1Vlan;
    private String enterprise1TemplateNameVlan;
    private String enterprise1TemplateNameVlanCreate;
    private String enterprise1TemplateNameVpls;

    // --- IPTV sample fields ---
    private String iptv1ServiceId;
    private String iptv1ServiceSubtype;
    private String iptv1ServiceType;
    private String iptv1QosProfile;
    private String iptv1CustomerGroupId;
    private String iptv1TemplateNameIptv;
    private String iptv1TemplateNameIgmp;
    private String iptv1Vlan;

    // Example for IPTV devices
    private String iptv1StbSn1;
    private String iptv1StbMac1;
    private String iptv1StbModel1;
    private String iptv1ApSn1;
    private String iptv1ApMac1;
    private String iptv1ApModel1;

    // Example for IPTV catalog items
    private String iptv1ProdName1;
    private String iptv1ProdVariant1;
}
