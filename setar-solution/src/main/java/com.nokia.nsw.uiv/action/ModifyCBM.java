package com.nokia.nsw.uiv.action;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.nokia.nsw.uiv.exception.InternalServerErrorException;
import com.nokia.nsw.uiv.exception.BadRequestException;
import com.nokia.nsw.uiv.framework.action.Action;
import com.nokia.nsw.uiv.framework.action.ActionContext;
import com.nokia.nsw.uiv.framework.action.HttpAction;
import com.nokia.nsw.uiv.model.common.party.CustomerRepository;
import com.nokia.nsw.uiv.model.resource.logical.LogicalDevice;
import com.nokia.nsw.uiv.model.resource.logical.LogicalDeviceRepository;
import com.nokia.nsw.uiv.request.ModifyCBMRequest;
import com.nokia.nsw.uiv.response.ModifyCBMResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

@Component
@RestController
@Action
@Slf4j
public class ModifyCBM implements HttpAction {

    @Autowired
    private LogicalDeviceRepository cbmDeviceRepository;

    @Autowired
    private CustomerRepository subscriberRepository; // Or actual subscriber repo

    @Override
    public Class getActionClass() {
        return ModifyCBMRequest.class;
    }

    @Override
    public Object doPost(ActionContext actionContext) throws Exception {
        ModifyCBMRequest request = (ModifyCBMRequest) actionContext.getObject();
        validateMandatoryParameters(request);

        // Derive subscriber name
        String subscriberName = deriveSubscriberName(request);

        // Compose entity names
        String subscriptionName = request.getSubscriberName() + request.getServiceId();
        String cfsName = "CFS" + subscriptionName;
        String rfsName = "RFS_" + subscriptionName;
        String productName = request.getSubscriberName() + request.getProductSubtype() + request.getServiceId();
        String cbmDeviceName = "CBM" + request.getServiceId();

        // Retrieve CBM device
        Optional<LogicalDevice> optCbm = cbmDeviceRepository.uivFindByGdn(cbmDeviceName);
        if (!optCbm.isPresent()) {
            throw new BadRequestException("CBM device not found: " + cbmDeviceName);
        }
        LogicalDevice cbmDevice = optCbm.get();

        // Update CBM based on modify type
        if ("ModifyCableModem".equalsIgnoreCase(request.getModifyType()) ||
                "Cable_Modem".equalsIgnoreCase(request.getModifyType())) {

            if (request.getModifyParam1() != null) {
                cbmDevice.getProperties().put("macAddress", request.getModifyParam1());
            }
            if (request.getModifyParam2() != null) {
                cbmDevice.getProperties().put("gatewayMacAddress", request.getModifyParam2());
            }
            if (request.getCbmModel() != null) {
                cbmDevice.getProperties().put("deviceModel", request.getCbmModel());
            }

            cbmDeviceRepository.save(cbmDevice, 2);
        }

        // Optional: Handle other modify types like "Package", "Components", "Products", "Contracts", "Password", "Modify_Number"

        // Final response
        return new ModifyCBMResponse(
                "200",
                "UIV action ModifyCBM executed successfully.",
                getCurrentTimestamp(),
                subscriberName,
                subscriptionName
        );
    }

    private void validateMandatoryParameters(ModifyCBMRequest request) throws InternalServerErrorException {
        if (request.getSubscriberName() == null || request.getResourceSN() == null ||
                request.getProductType() == null || request.getProductSubtype() == null ||
                request.getServiceId() == null || request.getModifyType() == null) {
            throw new InternalServerErrorException("Missing mandatory parameter(s)");
        }
    }

    private String deriveSubscriberName(ModifyCBMRequest request) {
        if ("IPTV".equalsIgnoreCase(request.getProductType())) {
            return request.getSubscriberName();
        }
        if (request.getResourceSN() == null || request.getResourceSN().isEmpty()) {
            if (request.getModifyType().matches("Package|Components|Products|Contracts")) {
                return request.getSubscriberName();
            } else {
                return request.getSubscriberName() + "_" + request.getModifyParam1().replace(":", "");
            }
        }
        return request.getSubscriberName() + "_" + request.getResourceSN().replace(":", "");
    }

    private String getCurrentTimestamp() {
        return java.time.Instant.now().toString();
    }
}

