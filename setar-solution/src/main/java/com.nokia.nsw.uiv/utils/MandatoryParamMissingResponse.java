package com.nokia.nsw.uiv.utils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MandatoryParamMissingResponse {

    private String status;
    private String message;
    private String timestamp;
    private String missingParam;
}

