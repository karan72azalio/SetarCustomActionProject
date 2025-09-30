package com.nokia.nsw.uiv.request;

import javax.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request DTO for ModifySubscriber
 */
@Data
public class ModifySubscriberRequest {

    @NotNull
    private String subscriberName; // new subscriber name

    @NotNull
    private String subscriberNameOld; // old subscriber name
}
