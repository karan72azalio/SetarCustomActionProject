package com.nokia.nsw.uiv.action;

import com.nokia.nsw.uiv.exception.BadRequestException;
import com.nokia.nsw.uiv.framework.action.Action;
import com.nokia.nsw.uiv.framework.action.ActionContext;
import com.nokia.nsw.uiv.framework.action.HttpAction;

import com.nokia.nsw.uiv.model.common.party.CustomerRepository;

import com.nokia.nsw.uiv.model.service.Subscription;
import com.nokia.nsw.uiv.model.service.SubscriptionRepository;

import com.nokia.nsw.uiv.model.resource.logical.LogicalDevice;
import com.nokia.nsw.uiv.model.resource.logical.LogicalDeviceRepository;

import com.nokia.nsw.uiv.repository.CustomerCustomRepository;
import com.nokia.nsw.uiv.repository.LogicalDeviceCustomRepository;
import com.nokia.nsw.uiv.repository.ResourceFacingServiceCustomRepository;
import com.nokia.nsw.uiv.repository.SubscriptionCustomRepository;
import com.nokia.nsw.uiv.request.ChangeStateRequest;
import com.nokia.nsw.uiv.response.ChangeStateResponse;

import com.nokia.nsw.uiv.utils.Constants;
import com.nokia.nsw.uiv.utils.Validations;
import com.setar.uiv.model.product.ResourceFacingService;
import com.setar.uiv.model.product.ResourceFacingServiceRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@Component
@RestController
@Action
@Slf4j
public class ChangeState implements HttpAction {

    private static final String ACTION_LABEL = "ChangeState";

    @Autowired
    private SubscriptionCustomRepository subscriptionRepository;

    @Autowired
    private ResourceFacingServiceCustomRepository rfsRepository;

    @Autowired
    private LogicalDeviceCustomRepository logicalDeviceRepository;

    @Autowired
    private CustomerCustomRepository customerRepository;

    @Override
    public Class<?> getActionClass() {
        return ChangeStateRequest.class;
    }

    @Override
    public Object doPost(ActionContext actionContext) throws Exception {
        log.info("Executing action {}", ACTION_LABEL);
        ChangeStateRequest req = (ChangeStateRequest) actionContext.getObject();

        // 1. Mandatory validation
        try {
            validateMandatory(req.getSubscriberName(), "subscriberName");
            validateMandatory(req.getServiceId(), "serviceId");
            validateMandatory(req.getActionType(), "actionType");
        } catch (BadRequestException bre) {
            return new ChangeStateResponse("400", Constants.ERROR_PREFIX + "Missing mandatory parameter : " + bre.getMessage(),
                    java.time.Instant.now().toString(), "","","");
        }

        // 2. Prepare names based on product type and service link
        String subscriptionName;
        String rfsName;
        String ontName = null;
        String cbmName = null;
        String productType = nullSafe(req.getProductType());
        String productSubType = nullSafe(req.getProductSubtype());
        String serviceLink = nullSafe(req.getServiceLink());

        // IPTV case
        if (!isEmpty(productType) && productType.toUpperCase().contains("IPTV")) {
            subscriptionName = req.getSubscriberName() + "_" + req.getServiceId();
            rfsName = "RFS_" + req.getSubscriberName() + "_" + req.getServiceId();
        }
        // Broadband/Voice with serviceLink provided and not Cloudstarter/Bridged
        else if ((equalsAny(productType, "Broadband", "Voice") && !equalsAny(productSubType, "Cloudstarter", "Bridged"))
                && !isEmpty(serviceLink)) {

            if (serviceLink.equalsIgnoreCase("ONT")) {
                subscriptionName = req.getSubscriberName() + "_" + req.getServiceId()+ "_" + nullSafe(req.getOntSN());
                rfsName = "RFS_" + req.getSubscriberName()+ "_" + req.getServiceId() + "_" + nullSafe(req.getOntSN());
            } else { // Cable_Modem
                subscriptionName = req.getSubscriberName() +"_" + req.getServiceId();
                rfsName = "RFS" + req.getSubscriberName() + "_" + req.getServiceId();
            }
        }
        // fallback when ontSN present
        else if (!isEmpty(req.getOntSN())) {
            subscriptionName = req.getSubscriberName()+ "_" + req.getServiceId()+ "_" + req.getOntSN();
            rfsName = "RFS_" + req.getSubscriberName()+ "_" + req.getServiceId()+ "_" + req.getOntSN();
        } else {
            // default fallback: subscriber + serviceId
            subscriptionName = req.getSubscriberName()+ "_" + req.getServiceId();
            rfsName = "RFS_" + subscriptionName;
        }

        // 3. Check ONT name length if present
        if (!isEmpty(req.getOntSN())) {
            ontName = "ONT" + req.getOntSN();
            if (ontName.length() > 100) {
                return new ChangeStateResponse("400", Constants.ERROR_PREFIX + "ONT name too long",
                        java.time.Instant.now().toString(), "", ontName, "");
            }
        }

        // 4. Search for subscription, rfs, ontd & cbm device
        try {

            Optional<Subscription> optSubscription = subscriptionRepository.findByDiscoveredName(subscriptionName);
            Optional<ResourceFacingService> optRfs = rfsRepository.findByDiscoveredName(rfsName);
            Optional<LogicalDevice> optOnt = Optional.empty();
            Optional<LogicalDevice> optCbm = Optional.empty();

            if (!isEmpty(ontName)) {
                optOnt = logicalDeviceRepository.findByDiscoveredName(ontName);
            }

            if (!isEmpty(req.getCbmMac())) {
                // attempt find by GDN "CBM" + mac (as per naming in your system)
                cbmName = "CBM" + req.getCbmMac();
                optCbm = logicalDeviceRepository.findByDiscoveredName(cbmName);
            }
            if (!optSubscription.isPresent() || !optRfs.isPresent()) {
                return new ChangeStateResponse("500", Constants.ERROR_PREFIX + "No entry found for Suspend/Resume",
                        java.time.Instant.now().toString(), (cbmName == null ? "" : cbmName),
                        (ontName == null ? "" : ontName), subscriptionName);
            }

            Subscription subscription = optSubscription.get();
            ResourceFacingService rfs = optRfs.get();

            // 5. Perform state change
            String actionType = req.getActionType().trim();
            String newStatus;
            if ("Suspend".equalsIgnoreCase(actionType)) {
                newStatus = "Suspended";
            } else if ("Resume".equalsIgnoreCase(actionType)) {
                newStatus = "Active";
            } else {
                return new ChangeStateResponse("400", Constants.ERROR_PREFIX + "Unsupported actionType: " + actionType,
                        java.time.Instant.now().toString(), (cbmName == null ? "" : cbmName),
                        (ontName == null ? "" : ontName), subscriptionName);
            }

            // Update subscription property - store as subscriptionStatus
            if (subscription.getProperties() == null) subscription.setProperties(new java.util.HashMap<>());
            subscription.getProperties().put("subscriptionStatus", newStatus);

            // 6. Update RFS transaction info if fxOrderId present
            if (!isEmpty(req.getFxOrderId())) {
                if (rfs.getProperties() == null) rfs.setProperties(new java.util.HashMap<>());
                rfs.getProperties().put("transactionId", req.getFxOrderId());
                rfs.getProperties().put("transactionType", actionType);
            }

            // Persist changes
            subscriptionRepository.save(subscription, 2);
            rfsRepository.save(rfs, 2);

            // Also persist ONT/CBM if we located and want to reflect state (optional)
//            if (optOnt.isPresent()) {
//                LogicalDevice ont = optOnt.get();
//                logicalDeviceRepository.save(ont, 2);
//            }
//            if (optCbm.isPresent()) {
//                LogicalDevice cbm = optCbm.get();
//                logicalDeviceRepository.save(cbm, 2);
//            }

            // 7. Final response
            return new ChangeStateResponse("200",
                    "UIV action ChangeState executed successfully.",
                    java.time.Instant.now().toString(),
                    (cbmName == null ? "" : cbmName),
                    (ontName == null ? "" : ontName),
                    subscriptionName
            );

        } catch (Exception ex) {
            log.error("ChangeState failed", ex);
            return new ChangeStateResponse("500", Constants.ERROR_PREFIX + "Internal server error occurred - " + ex.getMessage(),
                    java.time.Instant.now().toString(),
                    (cbmName == null ? "" : cbmName),
                    (ontName == null ? "" : ontName),
                    subscriptionName);
        }
    }

    // ---------- helpers ----------
    private void validateMandatory(String val, String name) throws BadRequestException {
        if (val == null || val.trim().isEmpty()) throw new BadRequestException(name);
    }

    private boolean isEmpty(String s) { return s == null || s.trim().isEmpty(); }

    private boolean equalsAny(String s, String... opts) {
        if (s == null) return false;
        for (String o : opts) if (o != null && s.equalsIgnoreCase(o)) return true;
        return false;
    }

    private String nullSafe(String s) { return s == null ? "" : s; }
}
