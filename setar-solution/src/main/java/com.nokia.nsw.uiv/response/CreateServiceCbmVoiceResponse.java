package com.nokia.nsw.uiv.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import javax.validation.constraints.NotNull;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
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
