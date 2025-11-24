package com.nokia.nsw.uiv.action;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.nokia.nsw.uiv.exception.BadRequestException;
import com.nokia.nsw.uiv.exception.InternalServerErrorException;
import com.nokia.nsw.uiv.framework.action.Action;
import com.nokia.nsw.uiv.framework.action.ActionContext;
import com.nokia.nsw.uiv.framework.action.HttpAction;
import com.nokia.nsw.uiv.model.common.party.Customer;
import com.nokia.nsw.uiv.repository.CustomerCustomRepository;
import com.nokia.nsw.uiv.model.resource.logical.LogicalDevice;
import com.nokia.nsw.uiv.repository.CustomerCustomRepository;
import com.nokia.nsw.uiv.repository.LogicalDeviceCustomRepository;
import com.nokia.nsw.uiv.model.resource.logical.LogicalDeviceRepository;
import com.nokia.nsw.uiv.model.resource.logical.LogicalComponent;
import com.nokia.nsw.uiv.model.resource.logical.LogicalComponentRepository;
import com.nokia.nsw.uiv.model.service.ServiceRepository;
import com.nokia.nsw.uiv.model.service.Subscription;
import com.nokia.nsw.uiv.model.service.SubscriptionRepository;
import com.nokia.nsw.uiv.repository.*;
import com.nokia.nsw.uiv.repository.LogicalDeviceCustomRepository;
import com.nokia.nsw.uiv.request.DeleteCBMRequest;
import com.nokia.nsw.uiv.response.DeleteCBMResponse;
import com.nokia.nsw.uiv.utils.Constants;
import com.nokia.nsw.uiv.utils.Validations;
import com.setar.uiv.model.product.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RestController;

@Component
@RestController
@Action
@Slf4j
public class DeleteCBM implements HttpAction {

    private static final String ACTION_LABEL = "DeleteCBM";

    @Autowired
    private LogicalDeviceCustomRepository cbmDeviceRepository;

    @Autowired
    private SubscriptionCustomRepository subscriptionRepository;

    @Autowired
    private ProductCustomRepository productRepository;

    @Autowired
    private ResourceFacingServiceCustomRepository rfsRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private CustomerCustomRepository subscriberRepository;

    @Autowired
    private CustomerFacingServiceCustomRepository cfsRepository;

    @Override
    public Class getActionClass() {
        return DeleteCBMRequest.class;
    }

    @Override
    public Object doPost(ActionContext actionContext) throws Exception {
        log.info("Executing action {}", ACTION_LABEL);

        DeleteCBMRequest request = (DeleteCBMRequest) actionContext.getObject();

        // 1. Validate mandatory params (including CBM_SN)
        try {
            Validations.validateMandatoryParams(request.getSubscriberName(), "subscriberName");
            Validations.validateMandatoryParams(request.getProductType(), "productType");
            Validations.validateMandatoryParams(request.getProductSubtype(), "productSubtype");
            Validations.validateMandatoryParams(request.getServiceId(), "serviceId");
            Validations.validateMandatoryParams(request.getCbmSN(), "cbmSN"); // <-- added
            // serviceFlag was previously validated in your code; it's optional in spec — validate only if required.
        } catch (BadRequestException bre) {
            return new DeleteCBMResponse("400", Constants.ERROR_PREFIX + "Missing mandatory parameter : " + bre.getMessage(),
                    java.time.Instant.now().toString(), "", "");
        }

        // 2. Construct names
        String subscriptionName = request.getSubscriberName() + Constants.UNDER_SCORE + request.getServiceId();
        String cfsName = "CFS" + Constants.UNDER_SCORE + subscriptionName;
        String rfsName = "RFS" + Constants.UNDER_SCORE + subscriptionName;
        String productName = request.getSubscriberName() + Constants.UNDER_SCORE + request.getProductSubtype() + Constants.UNDER_SCORE + request.getServiceId();
        String cbmName = "CBM" + request.getCbmSN();
        String subscriberName = request.getSubscriberName();

        // 6. Validate CBM name length early
        if (cbmName.length() > 100) {
            return new DeleteCBMResponse("400", Constants.ERROR_PREFIX + "CBM name too long",
                    java.time.Instant.now().toString(), cbmName, subscriptionName);
        }

        try {
            Optional<LogicalDevice> optCbmDevice = Optional.empty();
            Optional<Subscription> optSubscription = Optional.empty();
            Optional<Product> optProduct = Optional.empty();
            Optional<CustomerFacingService> optCfs = Optional.empty();
            Optional<ResourceFacingService> optRfs = Optional.empty();
            int subscriptionCount = 0;

            // --- 3. Fetch CBM Device ---
            try {
                optCbmDevice = cbmDeviceRepository.findByDiscoveredName(cbmName);
                if (!optCbmDevice.isPresent()) {
                    log.warn("CBM device {} not found", cbmName);
                }
            } catch (Exception e) {
                log.error("Error fetching CBM device {}", cbmName, e);
            }

            // If IPTV: direct subscriber name, else add MAC
            try {
                if ("IPTV".equalsIgnoreCase(request.getProductType())) {
                    Optional<Customer> subOpt = subscriberRepository.findByDiscoveredName(subscriberName);
                    if (subOpt.isPresent()) {
                        Customer setarSubscriber = subOpt.get();
                        if (setarSubscriber.getSubscription() != null) {
                            subscriptionCount = setarSubscriber.getSubscription().size();
                        } else {
                            subscriptionCount = 0;
                        }
                    } else {
                        log.warn("Subscriber {} not found (IPTV)", subscriberName);
                        subscriptionCount = 0;
                    }
                } else {
                    // Non-IPTV: need MAC from CBM device
                    if (!optCbmDevice.isPresent()) {
                        log.warn("CBM device required to derive subscriber name for non-IPTV product but CBM not found: {}", cbmName);
                        // If CBM required, return or continue depending on business decision.
                        return new DeleteCBMResponse("404", Constants.ERROR_PREFIX + "CBM device not found for non-IPTV product",
                                java.time.Instant.now().toString(), cbmName, subscriptionName);
                    }
                    LogicalDevice cbm = optCbmDevice.get();
                    Object macObj = cbm.getProperties() != null ? cbm.getProperties().get("macAddress") : null;
                    if (macObj == null) {
                        log.warn("CBM {} has no macAddress property", cbmName);
                        return new DeleteCBMResponse("400", Constants.ERROR_PREFIX + "CBM missing macAddress",
                                java.time.Instant.now().toString(), cbmName, subscriptionName);
                    }
                    String cbmMacAddr = macObj.toString();
                    String macWithoutColons = cbmMacAddr.replaceAll(":", "");
                    String newSubscriberName = subscriberName + Constants.UNDER_SCORE + macWithoutColons;

                    Optional<Customer> subscriberOpt = subscriberRepository.findByDiscoveredName(newSubscriberName);
                    if (subscriberOpt.isPresent()) {
                        Customer setarSubscriber = subscriberOpt.get();
                        if (setarSubscriber.getSubscription() != null) {
                            subscriptionCount = setarSubscriber.getSubscription().size();
                        } else {
                            subscriptionCount = 0;
                        }
                        subscriberName = newSubscriberName; // update for later deletion if necessary
                    } else {
                        log.warn("Subscriber {} not found (derived using MAC)", newSubscriberName);
                        subscriptionCount = 0;
                    }
                }
            } catch (Exception e) {
                log.error("Error while deriving subscriber or subscriptionCount", e);
            }

            // --- 4. Retrieve Associated Entities ---
            try {
                optSubscription = subscriptionRepository.findByDiscoveredName(subscriptionName);
            } catch (Exception e) {
                log.error("Error fetching subscription {}", subscriptionName, e);
            }

            try {
                optProduct = productRepository.findByDiscoveredName(productName);
            } catch (Exception e) {
                log.error("Error fetching product {}", productName, e);
            }

            try {
                optCfs = cfsRepository.findByDiscoveredName(cfsName);
            } catch (Exception e) {
                log.error("Error fetching CFS {}", cfsName, e);
            }

            try {
                optRfs = rfsRepository.findByDiscoveredName(rfsName);
                if (!optRfs.isPresent()) {
                    log.warn("RFS {} not found; continuing but skipping RFS-specific updates", rfsName);
                }
            } catch (Exception e) {
                log.error("Error fetching RFS {}", rfsName, e);
            }

            // --- 5. CPE Device logic (Voice/Broadband) ---
            if (optSubscription.isPresent() && optCbmDevice.isPresent()) {
                Subscription subscription = optSubscription.get();
                LogicalDevice cbmDevice = optCbmDevice.get();

                if ("Voice".equalsIgnoreCase(request.getProductSubtype())) {
                    // reset voip ports if matching
                    resetVoipPorts(subscription, cbmDevice);
                } else if ("Broadband".equalsIgnoreCase(request.getProductSubtype())) {
                    // clear description
                    cbmDevice.setDescription("");
                    // check voip ports availability - if both available set admin state available
                    try {
                        Object vp1 = cbmDevice.getProperties() != null ? cbmDevice.getProperties().get("voipPort1") : null;
                        Object vp2 = cbmDevice.getProperties() != null ? cbmDevice.getProperties().get("voipPort2") : null;
                        if ("Available".equals(vp1) && "Available".equals(vp2)) {
                            if (cbmDevice.getProperties() == null) {
                                cbmDevice.setProperties(new HashMap<>());
                            }
                            cbmDevice.getProperties().put("administrativeState", "Available");
                        }
                    } catch (Exception e) {
                        log.warn("Error while evaluating voip ports for broadband CBM {}", cbmName, e);
                    }
                    cbmDeviceRepository.save(cbmDevice, 2);
                }
            } else {
                log.info("Subscription or CBM device not present; skipping CPE device logic.");
            }

            // --- 7. Delete CBM device if exists ---
            try {
                optCbmDevice.ifPresent(device -> {
                    try {
                        cbmDeviceRepository.delete(device);
                        log.info("Deleted CBM device {}", cbmName);
                    } catch (Exception e) {
                        log.error("Error deleting CBM device {}", cbmName, e);
                    }
                });
            } catch (Exception e) {
                log.error("Exception while attempting to delete CBM", e);
            }

            // --- 8. Reset and Update RFS-Linked Resources ---
            if (optRfs.isPresent()) {
                ResourceFacingService setarRFS = optRfs.get();
                if (setarRFS.getUsedResource() != null) {
                    setarRFS.getUsedResource().forEach(resource -> {
                        try {
                            String localName = resource.getLocalName();
                            if (localName == null) {
                                return;
                            }
                            boolean isAP = localName.startsWith("AP");
                            boolean isSTB = localName.startsWith("STB");

                            if (isAP || isSTB) {
                                Map<String, Object> props = resource.getProperties() != null
                                        ? new HashMap<>(resource.getProperties())
                                        : new HashMap<>();
                                props.put("administrativeState", "Available");

                                if (isSTB) {
                                    // clear deviceGroupId for STB
                                    props.put("DeviceGroupId", null);
                                }

                                resource.setProperties(props);

                                // Persist resource - resource may be LogicalDevice
                                try {
                                    cbmDeviceRepository.save((LogicalDevice) resource, 2);
                                    log.info("Updated resource {} administrative state to Available", localName);
                                } catch (ClassCastException cce) {
                                    log.warn("Resource {} is not a LogicalDevice; skipping save via cbmDeviceRepository", localName);
                                } catch (Exception saveEx) {
                                    log.error("Failed to save resource {}", localName, saveEx);
                                }
                            }
                        } catch (Exception e) {
                            log.error("Error processing resource in RFS", e);
                        }
                    });
                }
                // Delete RFS after resources processed
                try {
                    rfsRepository.delete(setarRFS);
                    log.info("Deleted RFS {}", rfsName);
                } catch (Exception e) {
                    log.error("Error deleting RFS {}", rfsName, e);
                }
            } else {
                log.info("No RFS to process for {}", rfsName);
            }

            // --- 9. Delete Product & Subscription & CFS (SPR objects) ---
            try {
                optProduct.ifPresent(productRepository::delete);
            } catch (Exception e) {
                log.error("Error deleting Product {}", productName, e);
            }

            try {
                optSubscription.ifPresent(subscriptionRepository::delete);
            } catch (Exception e) {
                log.error("Error deleting Subscription {}", subscriptionName, e);
            }

            try {
                optCfs.ifPresent(cfsRepository::delete);
            } catch (Exception e) {
                log.error("Error deleting CFS {}", cfsName, e);
            }

            // --- 10. Delete Subscriber if last subscription ---
            if (subscriptionCount == 1) {
                try {
                    // subscriberName may have been updated for non-IPTV case
                    final String subscriberNameToDelete = subscriberName;
                    Optional<Customer> subscriberToDelete = subscriberRepository.findByDiscoveredName(subscriberNameToDelete);
                    subscriberToDelete.ifPresent(subscriber -> {
                        try {
                            subscriberRepository.delete(subscriber);
                            log.info("Deleted subscriber {}", subscriberNameToDelete);
                        } catch (Exception e) {
                            log.error("Error deleting subscriber {}", subscriberNameToDelete, e);
                        }
                    });
                } catch (Exception e) {
                    log.error("Error while attempting to delete subscriber {}", subscriberName, e);
                }
            } else {
                log.info("Not deleting subscriber {} because subscriptionCount != 1 (count={})", subscriberName, subscriptionCount);
            }

            // --- 11. Return success response ---
            return new DeleteCBMResponse("200", "CBM objects Deleted", java.time.Instant.now().toString(),
                    cbmName, subscriptionName);

        } catch (Exception e) {
            log.error("DeleteCBM action failed", e);
            throw new InternalServerErrorException("UIV action DeleteCBM execution failed - " + e.getMessage());
        }
    }

    private void resetVoipPorts(Subscription subscription, LogicalDevice cbmDevice) {
        if (subscription == null || cbmDevice == null) {
            return;
        }
        Map<String, Object> subProps = subscription.getProperties();
        Map<String, Object> devProps = cbmDevice.getProperties();
        if (devProps == null) {
            devProps = new HashMap<>();
            cbmDevice.setProperties(devProps);
        }

        try {
            // Correct property names: "voipNumber1"/"voipNumber2" on subscription; "voipPort1"/"voipPort2" on device.
            Object sVoip1 = subProps != null ? subProps.get("voipNumber1") : null;
            Object sVoip2 = subProps != null ? subProps.get("voipNumber2") : null;
            Object dPort1 = devProps.get("voipPort1");
            Object dPort2 = devProps.get("voipPort2");

            if (sVoip1 != null) {
                if (sVoip1.equals(dPort1)) {
                    devProps.put("voipPort1", "Available");
                }
                if (sVoip1.equals(dPort2)) {
                    devProps.put("voipPort2", "Available");
                }
            }

            if (sVoip2 != null) {
                if (sVoip2.equals(dPort1)) {
                    devProps.put("voipPort1", "Available");
                }
                if (sVoip2.equals(dPort2)) {
                    devProps.put("voipPort2", "Available");
                }
            }

            cbmDeviceRepository.save(cbmDevice, 2);
        } catch (Exception e) {
            log.error("Error resetting VOIP ports for device {}", cbmDevice != null ? cbmDevice.getDiscoveredName() : "unknown", e);
        }
    }
}
