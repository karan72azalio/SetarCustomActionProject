package com.nokia.nsw.uiv.action;

import com.nokia.nsw.uiv.exception.BadRequestException;
import com.nokia.nsw.uiv.framework.action.Action;
import com.nokia.nsw.uiv.framework.action.ActionContext;
import com.nokia.nsw.uiv.framework.action.HttpAction;
import com.nokia.nsw.uiv.model.common.party.Customer;
import com.nokia.nsw.uiv.model.common.party.CustomerRepository;
import com.nokia.nsw.uiv.model.resource.logical.LogicalDevice;
import com.nokia.nsw.uiv.model.resource.logical.LogicalDeviceRepository;
import com.nokia.nsw.uiv.model.service.Subscription;
import com.nokia.nsw.uiv.model.service.SubscriptionRepository;
import com.nokia.nsw.uiv.repository.*;
import com.nokia.nsw.uiv.request.DetachResourcesRequest;
import com.nokia.nsw.uiv.response.DetachResourcesResponse;
import com.nokia.nsw.uiv.utils.Constants;
import com.nokia.nsw.uiv.utils.Validations;
import com.setar.uiv.model.product.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Component
@RestController
@Action
@Slf4j
public class DetachResources implements HttpAction {

    protected static final String ACTION_LABEL = Constants.DETACH_RESOURCES;
    private static final String ERROR_PREFIX = "UIV action DetachResources execution failed - ";

    @Autowired private CustomerCustomRepository subscriberRepository;
    @Autowired private SubscriptionCustomRepository subscriptionRepository;
    @Autowired private ProductCustomRepository productRepository;
    @Autowired private CustomerFacingServiceCustomRepository cfsRepository;
    @Autowired private ResourceFacingServiceCustomRepository rfsRepository;
    @Autowired private LogicalDeviceCustomRepository deviceRepository;

    @Override
    public Class<?> getActionClass() {
        return DetachResourcesRequest.class;
    }

    @Override
    public Object doPatch(ActionContext actionContext) throws Exception {
        log.info("Executing action: {}", ACTION_LABEL);

        DetachResourcesRequest request = (DetachResourcesRequest) actionContext.getObject();
        String subscriptionName = request.getSubscriberName() + "_" + request.getServiceID();
        String cfsName = "CFS_" + subscriptionName;
        String rfsName = "RFS_" + subscriptionName;
        String productName = request.getSubscriberName()+ request.getProductSubType()+ request.getServiceID();

        try {
            // 1. Mandatory validation
            log.info("Validating mandatory parameters...");
            Validations.validateMandatoryParams(request.getSubscriberName(), "subscriberName");
            Validations.validateMandatoryParams(request.getOntSN(), "ontSN");
            Validations.validateMandatoryParams(request.getServiceID(), "serviceID");
            Validations.validateMandatoryParams(request.getProductSubType(), "productSubType");

            // 2. Fetch entities
            Optional<Customer> subscriber = subscriberRepository.findByDiscoveredName(request.getSubscriberName());
            Optional<Subscription> subscription = subscriptionRepository.findByDiscoveredName(subscriptionName);
            Optional<Product> product = productRepository.findByDiscoveredName(productName);
            Optional<CustomerFacingService> cfs = cfsRepository.findByDiscoveredName(cfsName);
            Optional<ResourceFacingService> rfs = rfsRepository.findByDiscoveredName(rfsName);

            if (!subscriber.isPresent() || !subscription.isPresent() || !product.isPresent() || !cfs.isPresent() || !rfs.isPresent()) {
                return new DetachResourcesResponse("404", ERROR_PREFIX + "No entry found for Delete.",
                        getCurrentTimestamp(), subscriptionName);
            }

            ResourceFacingService rfsEntity = rfs.get();
            if (request.getFxOrderId() != null) {
                rfsEntity.getProperties().put("transactionId", request.getFxOrderId());
            }
            rfsEntity.getProperties().put("transactionType", "DetachResources");

            boolean deviceUpdated = false;

            // 3. Handle devices
            List<String> stbSerials = Arrays.asList(request.getStbSN1(), request.getStbSN2(),
                    request.getStbSN3(), request.getStbSN4(), request.getStbSN5());
            List<String> apSerials = Arrays.asList(request.getApSN1(), request.getApSN2(),
                    request.getApSN3(), request.getApSN4(), request.getApSN5());

            for (String serial : stbSerials) {
                if (serial != null && !serial.equalsIgnoreCase("NA")) {
                    String devName = "STB_" + serial;
                    deviceUpdated |= detachDevice(devName, rfsEntity, true);
                }
            }

            for (String serial : apSerials) {
                if (serial != null && !serial.equalsIgnoreCase("NA")) {
                    String devName = "AP_" + serial;
                    deviceUpdated |= detachDevice(devName, rfsEntity, true);
                }
            }

            if (deviceUpdated) {
                rfsRepository.save(rfsEntity, 2);
                return new DetachResourcesResponse("200",
                        "UIV action DetachResources executed successfully.",
                        getCurrentTimestamp(),
                        subscriptionName);
            } else {
                return new DetachResourcesResponse("409", ERROR_PREFIX + "Error, Resources not detached.",
                        getCurrentTimestamp(), subscriptionName);
            }

        } catch (BadRequestException bre) {
            log.error("Validation error: {}", bre.getMessage());
            return new DetachResourcesResponse("400", ERROR_PREFIX + "Missing mandatory parameter : " + bre.getMessage(),
                    getCurrentTimestamp(), "");
        } catch (Exception ex) {
            log.error("Unhandled exception in DetachResources", ex);
            return new DetachResourcesResponse("500", ERROR_PREFIX + "Internal server error occurred",
                    getCurrentTimestamp(), "");
        }
    }

    private boolean detachDevice(String devName, ResourceFacingService rfsEntity, boolean isSTB) {
        Optional<LogicalDevice> optDevice = deviceRepository.findByDiscoveredName(devName);
        if (optDevice.isPresent()) {
            LogicalDevice device = optDevice.get();
            if (isSTB) {
                device.getProperties().put("deviceGroupId", "");
            }
            device.getProperties().put("administrativeState", "Available");
            device.getProperties().put("description", "");
            device.setContained(null);
            deviceRepository.save(device, 2);
            return true;
        }
        return false;
    }

    private String getCurrentTimestamp() {
        return Instant.now().toString();
    }
}
