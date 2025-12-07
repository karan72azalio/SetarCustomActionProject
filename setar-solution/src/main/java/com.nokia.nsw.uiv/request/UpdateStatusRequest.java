package com.nokia.nsw.uiv.request;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;

@Getter
@Setter
public class UpdateStatusRequest {
    @NotNull
    private String serialNumber;
    @NotNull
    private String portType;
    @NotNull
    private String portNumber;
    @NotNull
    private String portStatus;
}
