package com.nokia.nsw.uiv.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UivActionEnvelope<T> {

    @JsonProperty("task_type")
    private String taskType;

    @JsonProperty("product_version")
    private String productVersion;

    @JsonProperty("product_name")
    private String productName;

    @JsonProperty("ne_type")
    private String neType;

    @JsonProperty("ne_id")
    private String neId;

    @JsonProperty("REQ_TYPE")
    private String reqType;

    @JsonProperty("RUNACTION_USERIDENTIFIER")
    private String runActionUserIdentifier;

    @JsonProperty("ACTION")
    private String action;

    @JsonProperty("RUNACTION_ACTIONNAME")
    private String runActionActionName;

    // The actual action-specific payload
    @JsonProperty("parameters")
    private T parameters;
}
