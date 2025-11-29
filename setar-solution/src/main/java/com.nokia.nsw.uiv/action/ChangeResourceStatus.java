package com.nokia.nsw.uiv.action;

import com.nokia.nsw.uiv.framework.action.Action;
import com.nokia.nsw.uiv.framework.action.ActionContext;
import com.nokia.nsw.uiv.framework.action.HttpAction;
import com.nokia.nsw.uiv.model.resource.AdministrativeState;
import com.nokia.nsw.uiv.model.resource.logical.LogicalDevice;
import com.nokia.nsw.uiv.model.resource.logical.LogicalDeviceRepository;
import com.nokia.nsw.uiv.repository.LogicalDeviceCustomRepository;
import com.nokia.nsw.uiv.request.ChangeResourceStatusRequest;
import com.nokia.nsw.uiv.response.ChangeResourceStatusResponse;
import com.nokia.nsw.uiv.utils.Constants;
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
public class ChangeResourceStatus implements HttpAction {
    protected static final String ACTION_LABEL = Constants.CHANGE_RESOURCE_STATUS;
    private static final String ERROR_PREFIX = "UIV action ChangeResourceStatus execution failed - ";

    @Autowired
    private LogicalDeviceCustomRepository stbRepo;

    @Override
    public Class<?> getActionClass() {
        return ChangeResourceStatusRequest.class;
    }

    @Override
    public Object doPost(ActionContext actionContext) {
        log.warn(Constants.EXECUTING_ACTION, ACTION_LABEL);
        System.out.println("------------Test Trace # 1--------------- ChangeResourceStatus started");
        ChangeResourceStatusRequest req = (ChangeResourceStatusRequest) actionContext.getObject();

        try {
            // 1. Mandatory validation
            try {
                log.info(Constants.MANDATORY_PARAMS_VALIDATION_STARTED);
                Validations.validateMandatory(req.getResourceSn(), "resourceSn");
                Validations.validateMandatory(req.getResourceType(), "resourceType");
                Validations.validateMandatory(req.getResourceStatus(), "resourceStatus");
                log.info(Constants.MANDATORY_PARAMS_VALIDATION_COMPLETED);
            } catch (Exception bre) {
                System.out.println("------------Test Trace # 2--------------- Missing param: " + bre.getMessage());
                return new ChangeResourceStatusResponse(
                        "400",
                        ERROR_PREFIX + "Missing mandatory parameter: " + bre.getMessage(),
                        Instant.now().toString(),
                        "", "", "", "", ""
                );
            }

            String sn = req.getResourceSn();
            String type = req.getResourceType();
            String targetStatus = req.getResourceStatus();

            System.out.println("------------Test Trace # 3--------------- Inputs: SN=" + sn + ", Type=" + type + ", TargetStatus=" + targetStatus);

            // 2. Resolve states
            if (!("Available".equalsIgnoreCase(targetStatus) || "Deallocated".equalsIgnoreCase(targetStatus) || "Unknown".equalsIgnoreCase(targetStatus) || "NotApplicabe".equalsIgnoreCase(targetStatus))) {
                return new ChangeResourceStatusResponse(
                        "400",
                        ERROR_PREFIX + "Invalid resourceStatus: " + targetStatus,
                        Instant.now().toString(),
                        sn, "", targetStatus, "", type
                );
            }

            // 3. Derive Device Name
            String devName = type + Constants.UNDER_SCORE  + sn;
            System.out.println("------------Test Trace # 4--------------- Derived device name: " + devName);

            // 4. Locate device
            Optional<LogicalDevice> devOpt = stbRepo.findByDiscoveredName(devName);
            if (!devOpt.isPresent()) {
                System.out.println("------------Test Trace # 5--------------- Device not found: " + devName);
                return new ChangeResourceStatusResponse(
                        "404",
                        ERROR_PREFIX + type + sn + " not found",
                        Instant.now().toString(),
                        sn, "", "", "", type
                );
            }

            LogicalDevice device = devOpt.get();
            String currentStatus = device.getProperties().get("administrativeState")!=null?device.getProperties().get("administrativeState").toString():null;
            String model = device.getProperties().get("Model") == null ? "" : device.getProperties().get("Model").toString();
            String mac = device.getProperties().get("MacAddress") == null ? "" : device.getProperties().get("MacAddress").toString() ;

            System.out.println("------------Test Trace # 6--------------- Device found. Current status=" + currentStatus);

            // 5. Apply status change
            if (targetStatus.equalsIgnoreCase(currentStatus)) {
                System.out.println("------------Test Trace # 7--------------- No change required");
                return new ChangeResourceStatusResponse(
                        "404",
                        ERROR_PREFIX + "No change required. Resource already in status " + targetStatus,
                        Instant.now().toString(),
                        sn, mac, currentStatus, model, type
                );
            }

            Map<String, Object> deviceProps = device.getProperties();
            deviceProps.put("administrativeStatus",targetStatus);
            device.setProperties(deviceProps);
            stbRepo.save(device);

            System.out.println("------------Test Trace # 8--------------- Device status updated to " + targetStatus);
            log.info(Constants.ACTION_COMPLETED);
            // 6. Success response
            return new ChangeResourceStatusResponse(
                    "200",
                    "UIV action ChangeResourceStatus executed successfully.",
                    Instant.now().toString(),
                    sn,
                    mac,
                    targetStatus,
                    model,
                    type
            );

        } catch (Exception ex) {
            log.error("Unhandled exception in ChangeResourceStatus", ex);
            return new ChangeResourceStatusResponse(
                    "500",
                    ERROR_PREFIX + "Internal server error occurred - " + ex.getMessage(),
                    Instant.now().toString(),
                    "", "", "", "", ""
            );
        }
    }
}
