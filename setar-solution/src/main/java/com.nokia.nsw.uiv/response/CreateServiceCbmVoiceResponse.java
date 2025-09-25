package com.nokia.nsw.uiv.response;

import lombok.Getter;
import lombok.Setter;
import javax.validation.constraints.NotNull;

@Getter
@Setter
public class CreateServiceCbmVoiceResponse {

    @NotNull
    private String status;              // M
    @NotNull
    private String message;             // M
    @NotNull
    private String timestamp;           // M
    @NotNull
    private String subscriptionName;    // M
    @NotNull
    private String cbmName;             // M
}
