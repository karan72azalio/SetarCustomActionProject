package com.nokia.nsw.uiv.action;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.nokia.nsw.uiv.exception.AccessForbiddenException;
import com.nokia.nsw.uiv.exception.BadRequestException;
import com.nokia.nsw.uiv.exception.ModificationNotAllowedException;
import com.nokia.nsw.uiv.framework.action.Action;
import com.nokia.nsw.uiv.framework.action.ActionContext;
import com.nokia.nsw.uiv.framework.action.HttpAction;
import com.nokia.nsw.uiv.model.common.party.Customer;
import com.nokia.nsw.uiv.model.common.party.CustomerRepository;
import com.nokia.nsw.uiv.model.resource.logical.LogicalComponent;
import com.nokia.nsw.uiv.model.resource.logical.LogicalComponentRepository;
import com.nokia.nsw.uiv.model.resource.logical.LogicalDevice;
import com.nokia.nsw.uiv.model.resource.logical.LogicalDeviceRepository;
import com.nokia.nsw.uiv.model.resource.logical.LogicalInterface;
import com.nokia.nsw.uiv.model.resource.logical.LogicalInterfaceRepository;
import com.nokia.nsw.uiv.model.service.Subscription;
import com.nokia.nsw.uiv.model.service.SubscriptionRepository;
import com.nokia.nsw.uiv.request.ModifySPRRequest;
import com.nokia.nsw.uiv.response.ModifySPRResponse;
import com.nokia.nsw.uiv.utils.Constants;
import com.nokia.nsw.uiv.utils.Validations;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RestController;

@Component
@RestController
@Action
@Slf4j
public class ModifySPR implements HttpAction {

    protected static final String ACTION_LABEL = Constants.MODIFY_SPR;
    private static final String ERROR_PREFIX = "UIV action ModifySPR execution failed - ";

    @Autowired
    private LogicalDeviceRepository logicalDeviceRepository;

    @Autowired
    private LogicalComponentRepository logicalComponentRepository;

    @Autowired
    private LogicalInterfaceRepository logicalInterfaceRepository;
    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;
    @Override
    public Class getActionClass() {
        return ModifySPRRequest.class;
    }

    @Override
    public Object doPost(ActionContext actionContext) {
        log.warn(Constants.EXECUTING_ACTION, ACTION_LABEL);

        ModifySPRRequest request = (ModifySPRRequest) actionContext.getObject();
        boolean success = false;

        try {
            // 1. Mandatory Validations
            log.info(Constants.MANDATORY_PARAMS_VALIDATION_STARTED);
            Validations.validateMandatoryParams(request.getSubscriberName(), "SUBSCRIBER_NAME");
            Validations.validateMandatoryParams(request.getProductType(), "PRODUCT_TYPE");
            Validations.validateMandatoryParams(request.getProductSubtype(), "PRODUCT_SUB_TYPE");
            Validations.validateMandatoryParams(request.getOntSN(), "ONT_SN");
            Validations.validateMandatoryParams(request.getServiceId(), "SERVICE_ID");
            Validations.validateMandatoryParams(request.getModifyType(), "MODIFY_TYPE");
            log.info(Constants.MANDATORY_PARAMS_VALIDATION_COMPLETED);

            // 2. Name Construction
            String subscriberName = request.getSubscriberName() + "_" + request.getOntSN();
            String subscriptionName = request.getSubscriberName() + "_" + request.getServiceId() + "_" + request.getOntSN();
            String ontName = "ONT_" + request.getOntSN();
            String subscriberGdn = subscriberName;
            String subscriptionContext = subscriberGdn;

            if (ontName.length() > 100) {
                throw new BadRequestException("ONT name too long");
            }

            // 3. Fetch Entities
            Customer subscriber = customerRepository.uivFindByGdn(subscriberGdn)
                    .orElseThrow(() -> new BadRequestException("Subscriber not found: " + subscriberName));

            String subscriptionGdn = Validations.getGlobalName(subscriptionContext,subscriptionName);
            Subscription subscription = subscriptionRepository.uivFindByGdn(subscriptionGdn)
                    .orElseThrow(() -> new BadRequestException("Subscription not found: " + subscriptionName));

            // 4. Route to correct handler
            if (isBroadband(request)) {
                success = handleFibernetOrBroadband(request, subscriber, subscription);
            } else if (isEnterprise(request)) {
                success = handleEVPN(request, subscription);
            } else if (isVoip(request)) {
                success = handleVOIP(request, subscription, ontName);
            } else if (isOntModification(request)) {
                success = handleModifyONT(request, ontName);
            }

            // 5. Response
            if (success) {
                log.info(Constants.ACTION_COMPLETED);
                return new ModifySPRResponse("200", "UIV action ModifySPR executed successfully.", getCurrentTimestamp(),
                        ontName, subscriptionName);
            } else {
                throw new Exception("Modify operation failed");
            }

        } catch (BadRequestException bre) {
            log.error("Validation or not found error: {}", bre.getMessage(), bre);
            String msg = ERROR_PREFIX + bre.getMessage();
            return new ModifySPRResponse("400", msg, getCurrentTimestamp(), "", "");
        } catch (ModificationNotAllowedException ex) {
            log.error("Persistence error: {}", ex.getMessage(), ex);
            String msg = ERROR_PREFIX + ex.getMessage();
            return new ModifySPRResponse("500", msg, getCurrentTimestamp(), "", "");
        } catch (Exception ex) {
            log.error("Unhandled exception during ModifySPR", ex);
            String msg = ERROR_PREFIX + "Internal server error occurred";
            return new ModifySPRResponse("500", msg, getCurrentTimestamp(), "", "");
        }
    }

    // ------------------ HANDLERS ------------------

    private boolean handleFibernetOrBroadband(ModifySPRRequest request,
                                              Customer subscriber,
                                              Subscription subscription) throws ModificationNotAllowedException, BadRequestException, AccessForbiddenException {
        if ("Username".equalsIgnoreCase(request.getModifyType())) {
            Map<String, Object> subProps = subscription.getProperties();
            subProps.put("subscriptionDetails", request.getModifyParam1());
            subProps.put("serviceID", request.getModifyParam3());
            subscription.setProperties(subProps);

            Map<String, Object> subrProps = subscriber.getProperties();
            subrProps.put("email_username", request.getModifyParam2());
            subscriber.setProperties(subrProps);

            if (!request.getServiceId().equals(request.getModifyParam3())) {
                updateSubscriptionAndChildren(request, subscription, request.getModifyParam3());
            }

            subscriptionRepository.save(subscription, 2);
            customerRepository.save(subscriber, 2);
            return true;

        } else if ("Password".equalsIgnoreCase(request.getModifyType())) {
            try {
                Map<String, Object> subrProps = subscriber.getProperties();
                subrProps.put("email_pwd", request.getModifyParam1());
                subscriber.setProperties(subrProps);
                customerRepository.save(subscriber, 2);
                return true;
            } catch (Exception e) {
                throw new ModificationNotAllowedException("Failed to persist password update " + e.getMessage());
            }
        } else if (List.of("Package", "Component", "Product", "Contract").contains(request.getModifyType())) {
            try {
                Map<String, Object> subProps = subscription.getProperties();
                if ("Cloudstarter".equalsIgnoreCase(request.getProductSubtype())
                        || "Bridged".equalsIgnoreCase(request.getProductSubtype())) {
                    subProps.put("evpnQosSessionProfile", request.getModifyParam1());
                } else {
                    subProps.put("veipQosSessionProfile", request.getModifyParam1());
                }
                subscription.setProperties(subProps);
                subscriptionRepository.save(subscription, 2);
                return true;
            } catch (Exception e) {
                throw new ModificationNotAllowedException("Failed to update QoS profile " + e.getMessage());
            }
        }
        return false;
    }

    private boolean handleEVPN(ModifySPRRequest request, Subscription subscription) throws ModificationNotAllowedException, BadRequestException, AccessForbiddenException {
        if ("Username".equalsIgnoreCase(request.getModifyType())) {
            Map<String, Object> subProps = subscription.getProperties();
            subProps.put("subscriptionDetails", "FTTB-" + request.getModifyParam1());
            subProps.put("serviceID", request.getModifyParam1());
            subscription.setProperties(subProps);

            if (!request.getServiceId().equals(request.getModifyParam1())) {
                updateSubscriptionAndChildren(request, subscription, request.getModifyParam1());
            }

            subscriptionRepository.save(subscription, 2);
            return true;

        } else if ("Component".equalsIgnoreCase(request.getModifyType())) {
            try {
                Map<String, Object> subProps = subscription.getProperties();
                subProps.put("evpnQosSessionProfile", request.getModifyParam1());
                subscription.setProperties(subProps);
                subscriptionRepository.save(subscription, 2);
                return true;
            } catch (Exception e) {
                throw new ModificationNotAllowedException("Failed to update EVPN component " + e.getMessage());
            }
        }
        return false;
    }

    private boolean handleVOIP(ModifySPRRequest request, Subscription subscription, String ontName) throws ModificationNotAllowedException {
        if (List.of("Package", "Product").contains(request.getModifyType())) {
            try {
                Map<String, Object> subProps = subscription.getProperties();
                subProps.put("voipPackage1", request.getModifyParam1());
                subProps.put("voipServiceCode1", request.getModifyParam2());
                subscription.setProperties(subProps);
                subscriptionRepository.save(subscription, 2);
                return true;
            } catch (Exception e) {
                throw new ModificationNotAllowedException("Failed to update VoIP package " + e.getMessage());
            }
        } else if ("Modify_Number".equalsIgnoreCase(request.getModifyType())) {
            try {
                Map<String, Object> subProps = subscription.getProperties();
                subProps.put("serviceID", request.getModifyParam1());
                subscription.setProperties(subProps);

                LogicalDevice ont = logicalDeviceRepository.uivFindByGdn(ontName)
                        .orElseThrow(() -> new BadRequestException("ONT not found"));

                Map<String, Object> ontProps = ont.getProperties();
                ontProps.put("potsPort1Number", request.getModifyParam1());
                ont.setProperties(ontProps);

                if (!request.getServiceId().equals(request.getModifyParam1())) {
                    updateSubscriptionAndChildren(request, subscription, request.getModifyParam1());
                }

                logicalDeviceRepository.save(ont, 2);
                subscriptionRepository.save(subscription, 2);
                return true;
            } catch (Exception e) {
                throw new ModificationNotAllowedException("Failed to modify VOIP number: " + e.getMessage());
            }
        }
        return false;
    }

    private boolean handleModifyONT(ModifySPRRequest request, String ontName) throws BadRequestException, AccessForbiddenException {
        LogicalDevice ont = logicalDeviceRepository.uivFindByGdn(ontName)
                .orElseThrow(() -> new BadRequestException("ONT not found"));

        // update subscriptions linked by simaCustomerId
        List<LogicalComponent> allComponents = (List<LogicalComponent>) logicalComponentRepository.findAll();
        for (LogicalComponent sub : allComponents) {
            Map<String, Object> subProps = sub.getProperties();
            if (subProps != null && request.getSubscriberName().equals(subProps.get("simaCustomerId"))) {
                subProps.put("serviceSN", request.getModifyParam1());
                sub.setProperties(subProps);
                logicalComponentRepository.save(sub, 2);
            }
        }

        // update ONT
        Map<String, Object> ontProps = ont.getProperties();
        ontProps.put("serialNo", request.getModifyParam1());
        ont.setLocalName("resourceName " + request.getModifyParam1());
        ont.setProperties(ontProps);

        // update VLAN interfaces
        List<LogicalInterface> allIfaces = (List<LogicalInterface>) logicalInterfaceRepository.findAll();
        for (LogicalInterface vlan : allIfaces) {
            if (vlan.getLocalName() != null && vlan.getLocalName().contains(request.getOntSN())) {
                String newVlanName = vlan.getLocalName().replace(request.getOntSN(), request.getModifyParam1());
                vlan.setLocalName(newVlanName);
                logicalInterfaceRepository.save(vlan, 2);
            }
        }

        // update CPE Device
        String cpeDeviceName = request.getProductType() + "_" + request.getOntSN();
        logicalDeviceRepository.uivFindByGdn(cpeDeviceName).ifPresent(cpe -> {
            Map<String, Object> cpeProps = cpe.getProperties();
            cpeProps.put("serialNo", request.getModifyParam1());
            try {
                cpe.setLocalName(request.getProductType() + "_" + request.getModifyParam1());
            } catch (AccessForbiddenException e) {
                throw new RuntimeException(e);
            } catch (BadRequestException e) {
                throw new RuntimeException(e);
            }
            cpe.setProperties(cpeProps);
            logicalDeviceRepository.save(cpe, 2);
        });

        logicalDeviceRepository.save(ont, 2);
        return true;
    }

    // ------------------ HELPERS ------------------

    private void updateSubscriptionAndChildren(ModifySPRRequest request,
                                               Subscription subscription,
                                               String newServiceId) throws BadRequestException, AccessForbiddenException {
        String oldSubscriptionName = request.getSubscriberName() + request.getServiceId() + request.getOntSN();
        String productName = request.getSubscriberName() + request.getProductSubtype() + request.getServiceId();
        String cfsName = "CFS_" + oldSubscriptionName;
        String rfsName = "RFS_" + oldSubscriptionName;

        String subscriptionNameNew = request.getSubscriberName() + newServiceId + request.getOntSN();
        String productNameNew = request.getSubscriberName() + request.getProductSubtype() + newServiceId;
        String cfsNameNew = "CFS_" + subscriptionNameNew;
        String rfsNameNew = "RFS_" + subscriptionNameNew;

        logicalComponentRepository.uivFindByGdn(productName).ifPresent(product -> {
            try {
                product.setLocalName(productNameNew);
            } catch (AccessForbiddenException e) {
                throw new RuntimeException(e);
            } catch (BadRequestException e) {
                throw new RuntimeException(e);
            }
            logicalComponentRepository.save(product, 2);
        });

        logicalComponentRepository.uivFindByGdn(cfsName).ifPresent(cfs -> {
            try {
                cfs.setLocalName(cfsNameNew);
            } catch (AccessForbiddenException e) {
                throw new RuntimeException(e);
            } catch (BadRequestException e) {
                throw new RuntimeException(e);
            }
            logicalComponentRepository.save(cfs, 2);
        });

        logicalComponentRepository.uivFindByGdn(rfsName).ifPresent(rfs -> {
            try {
                rfs.setLocalName(rfsNameNew);
            } catch (AccessForbiddenException e) {
                throw new RuntimeException(e);
            } catch (BadRequestException e) {
                throw new RuntimeException(e);
            }
            rfs.getProperties().put("transactionType", request.getModifyType());
            if (request.getFxOrderId() != null) {
                rfs.getProperties().put("transactionId", request.getFxOrderId());
            }
            logicalComponentRepository.save(rfs, 2);
        });

        subscription.setLocalName(subscriptionNameNew);
    }

    private boolean isBroadband(ModifySPRRequest request) {
        return "Fibernet".equalsIgnoreCase(request.getProductType())
                || "Broadband".equalsIgnoreCase(request.getProductType());
    }

    private boolean isEnterprise(ModifySPRRequest request) {
        return "EVPN".equalsIgnoreCase(request.getProductType())
                || "ENTERPRISE".equalsIgnoreCase(request.getProductType());
    }

    private boolean isVoip(ModifySPRRequest request) {
        return "VOIP".equalsIgnoreCase(request.getProductType())
                || "Voice".equalsIgnoreCase(request.getProductType());
    }

    private boolean isOntModification(ModifySPRRequest request) {
        return "Modify_ONT".equalsIgnoreCase(request.getModifyType())
                || "ONT".equalsIgnoreCase(request.getModifyType());
    }

    private String getCurrentTimestamp() {
        return java.time.Instant.now().toString();
    }
}
