package com.nokia.nsw.uiv.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QueryTemplateNameVLANRequest {

    private Integer vlanRangeStart;
    private Integer vlanRangeEnd;

    // Mandatory inputs
    private String ontSN;
    private String ontPort;
    private String menm;
    private String templateNameVlan;
    private String templateNameVlanCreate;

    // Optional
    private String templateNameVpls;
}
