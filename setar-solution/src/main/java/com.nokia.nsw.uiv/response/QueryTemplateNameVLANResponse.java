package com.nokia.nsw.uiv.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QueryTemplateNameVLANResponse {
    private String status;
    private String message;
    private String timestamp;

    private String vlanId;
    private String vlanTemplateName;
    private String vlanTemplateCreateName;
    private String vplsTemplateName;
}
