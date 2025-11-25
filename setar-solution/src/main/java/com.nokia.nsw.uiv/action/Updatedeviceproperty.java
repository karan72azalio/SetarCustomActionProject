package com.nokia.nsw.uiv.action;

import com.nokia.nsw.uiv.framework.action.Action;
import com.nokia.nsw.uiv.framework.action.ActionContext;
import com.nokia.nsw.uiv.framework.action.HttpAction;
import com.nokia.nsw.uiv.model.resource.logical.LogicalDevice;
import com.nokia.nsw.uiv.model.resource.logical.LogicalDeviceRepository;
import com.nokia.nsw.uiv.repository.LogicalDeviceCustomRepository;
import com.nokia.nsw.uiv.request.UpdatedevicepropertyRequest;
import com.nokia.nsw.uiv.response.UpdatedevicepropertyResponse;
import com.nokia.nsw.uiv.utils.Validations;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
@RestController
@Action
@Slf4j
public class Updatedeviceproperty implements HttpAction {

    private static final String ERROR_PREFIX = "UIV action Updatedeviceproperty execution failed - ";

    @Autowired
    private LogicalDeviceCustomRepository stbRepo;

    @Override
    public Class<?> getActionClass() {
        return UpdatedevicepropertyRequest.class;
    }

    @Override
    public Object doPost(ActionContext actionContext) {
        System.out.println("------------Test Trace # 1--------------- Updatedeviceproperty started");
        UpdatedevicepropertyRequest req = (UpdatedevicepropertyRequest) actionContext.getObject();

        try {
            // 1. Mandatory validation
            try {
                Validations.validateMandatory(req.getStbSn1(), "stbSn1");
                Validations.validateMandatory(req.getCustomerGroupId(), "customerGroupId");
            } catch (Exception bre) {
                System.out.println("------------Test Trace # 2--------------- Missing param: " + bre.getMessage());
                return new UpdatedevicepropertyResponse(
                        "400",
                        ERROR_PREFIX + "Missing mandatory parameter: " + bre.getMessage(),
                        Instant.now().toString(),
                        "", ""
                );
            }

            String stbSn = req.getStbSn1();
            String custGroupId = req.getCustomerGroupId();
            System.out.println("------------Test Trace # 3--------------- Inputs: stbSn=" + stbSn + ", customerGroupId=" + custGroupId);

            // 2. Derive device name
            String stbName = "STB_" + stbSn;
            System.out.println("------------Test Trace # 4--------------- Derived device name: " + stbName);

            // 3. Locate STB
            Optional<LogicalDevice> stbOpt = stbRepo.findByDiscoveredName(stbName);
            if (!stbOpt.isPresent()) {
                System.out.println("------------Test Trace # 5--------------- STB not found: " + stbName);
                return new UpdatedevicepropertyResponse(
                        "404",
                        ERROR_PREFIX + "Error, No STB found with Allocated state to update.",
                        Instant.now().toString(),
                        stbSn,
                        custGroupId
                );
            }

            LogicalDevice stb = stbOpt.get();
            String currentState = stb.getProperties().get("administrativeState")!=null?stb.getProperties().get("administrativeState").toString():null;
            System.out.println("------------Test Trace # 6--------------- STB found. Current state=" + currentState);

            // 4. Validate Allocated state
            if (!"Available".equalsIgnoreCase(currentState)) {
                System.out.println("------------Test Trace # 7--------------- STB not in Allocated state");
                return new UpdatedevicepropertyResponse(
                        "404",
                        ERROR_PREFIX + "Error, No STB found with Activated state to update.",
                        Instant.now().toString(),
                        stbSn,
                        custGroupId
                );
            }

            // 5. Update Device Property
            Map<String,Object>props=new HashMap<>();
            props.put("DeviceGroupId",custGroupId);
            stb.setProperties(props);
            stbRepo.save(stb);
            System.out.println("------------Test Trace # 8--------------- CustomerGroupId updated to " + custGroupId);

            // 6. Success response
            return new UpdatedevicepropertyResponse(
                    "200",
                    "UIV action Updatedeviceproperty executed successfully.",
                    Instant.now().toString(),
                    stbSn,
                    custGroupId
            );

        } catch (Exception ex) {
            log.error("Unhandled exception in Updatedeviceproperty", ex);
            return new UpdatedevicepropertyResponse(
                    "500",
                    ERROR_PREFIX + "Internal server error occurred - " + ex.getMessage(),
                    Instant.now().toString(),
                    "", ""
            );
        }
    }
}
