package com.nokia.nsw.uiv.action;

import com.nokia.nsw.uiv.exception.BadRequestException;
import com.nokia.nsw.uiv.framework.action.Action;
import com.nokia.nsw.uiv.framework.action.ActionContext;
import com.nokia.nsw.uiv.framework.action.HttpAction;
import com.nokia.nsw.uiv.model.resource.logical.LogicalDevice;
import com.nokia.nsw.uiv.model.resource.logical.LogicalDeviceRepository;
import com.nokia.nsw.uiv.repository.LogicalDeviceCustomRepository;
import com.nokia.nsw.uiv.request.QueryResourceRequest;
import com.nokia.nsw.uiv.response.QueryResourceResponse;
import com.nokia.nsw.uiv.utils.Constants;
import com.nokia.nsw.uiv.utils.Validations;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Optional;

@Component
@RestController
@Action
@Slf4j
public class QueryResource implements HttpAction {
    protected static final String ACTION_LABEL = Constants.QUERY_RESOURCE;
    private static final String ERROR_PREFIX = "UIV action QueryResource execution failed - ";

    @Autowired
    private LogicalDeviceCustomRepository deviceRepository;

    @Override
    public Class getActionClass() {
        return QueryResourceRequest.class;
    }

    @Override
    public Object doPost(ActionContext actionContext) throws Exception {
        log.error(Constants.EXECUTING_ACTION, ACTION_LABEL);
        QueryResourceRequest request = (QueryResourceRequest) actionContext.getObject();
        String resourceSN = request.getResourceSN();
        String resourceType = request.getResourceType();

        try {
            log.error(Constants.MANDATORY_PARAMS_VALIDATION_STARTED);
            // Step 1: Mandatory Validations
            Validations.validateMandatoryParams(resourceSN, "resourceSN");
            Validations.validateMandatoryParams(resourceType, "resourceType");
            log.error(Constants.MANDATORY_PARAMS_VALIDATION_COMPLETED);
            // Step 2: Construct Device Name
            String devName;
            if ("CBM".equalsIgnoreCase(resourceType) || "ONT".equalsIgnoreCase(resourceType)) {
                devName = resourceType + resourceSN;
            } else {
                devName = resourceType + Constants.UNDER_SCORE  + resourceSN;
            }

            // Step 3: Search Device
            Optional<LogicalDevice> optDev = deviceRepository.findByDiscoveredName(devName);
            if (optDev.isEmpty()) {
                return errorResponse("404", ERROR_PREFIX + "Resource not found, SN is: " + resourceSN);
            }

            LogicalDevice device = optDev.get();
            String devModel = (String) device.getProperties().getOrDefault("deviceModel", "");
            String devMan = (String) device.getProperties().getOrDefault("manufacturer", "");
            String devSN = (String) device.getProperties().getOrDefault("serialNo", resourceSN);
            String devMAC = (String) device.getProperties().getOrDefault("macAddress", "");
            String gatewayMac = (String) device.getProperties().getOrDefault("gatewayMacAddress", "");
            String devStatus = (String) device.getProperties().getOrDefault("AdministrativeState", "");
            String devKEY = (String) device.getProperties().getOrDefault("presharedKey", "");
            String devDesc = (String) device.getProperties().getOrDefault("description", "");
            if(devDesc.isEmpty() && !device.getDescription().isEmpty())
            {
                devDesc=device.getDescription();
            }

            String devGroupID = "NA";
            String devSubTYPE = "";

            // Step 3b: Handle subtype/groupID for AP/STB
            if ("AP".equalsIgnoreCase(resourceType)) {
                devSubTYPE = "Not Applicable";
            } else if ("STB".equalsIgnoreCase(resourceType)) {
                devGroupID = (String) device.getProperties().getOrDefault("deviceGroupId", "NA");
                devSubTYPE = (String) device.getProperties().getOrDefault("modelSubType", "");
            }
            log.error(Constants.ACTION_COMPLETED);
            // Step 4: Final Success Response
            return new QueryResourceResponse(
                    "200",
                    "UIV action QueryResource executed successfully.",
                    getCurrentTimestamp(),
                    devSN,
                    devMAC,
                    devStatus,
                    devModel,
                    devSubTYPE,
                    devKEY,
                    resourceType,
                    devMan,
                    devGroupID,
                    devDesc,
                    gatewayMac
            );

        } catch (BadRequestException bre) {
            return errorResponse("400", ERROR_PREFIX + "Missing mandatory parameter : " + bre.getMessage());
        } catch (Exception ex) {
            return errorResponse("500", ERROR_PREFIX + "Internal server error occurred - " + ex.getMessage());
        }
    }

    private QueryResourceResponse errorResponse(String code, String message) {
        return new QueryResourceResponse(
                code,
                message,
                getCurrentTimestamp(),
                "", "", "", "", "", "", "", "", "", "", ""
        );
    }

    private String getCurrentTimestamp() {
        return Instant.now().toString();
    }
}
