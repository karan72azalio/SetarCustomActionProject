package com.nokia.nsw.uiv.action;

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
import com.setar.uiv.model.product.CustomerFacingService;
import com.setar.uiv.model.product.CustomerFacingServiceRepository;
import com.setar.uiv.model.product.Product;
import com.setar.uiv.model.product.ProductRepository;
import com.setar.uiv.model.product.ResourceFacingService;
import com.setar.uiv.model.product.ResourceFacingServiceRepository;
import com.nokia.nsw.uiv.request.ModifyCBMRequest;
import com.nokia.nsw.uiv.response.ModifyCBMResponse;
import com.nokia.nsw.uiv.utils.Constants;
import com.nokia.nsw.uiv.utils.Validations;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@Component
@RestController
@Action
@Slf4j
public class ModifyCBM implements HttpAction {

    protected static final String ACTION_LABEL = Constants.MODIFY_CBM;
    private static final String ERROR_PREFIX = "UIV action ModifyCBM execution failed - ";

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CustomerFacingServiceRepository cfsRepository;

    @Autowired
    private ResourceFacingServiceRepository rfsRepository;

    @Autowired
    private LogicalDeviceRepository logicalDeviceRepository;

    @Autowired
    private LogicalComponentRepository logicalComponentRepository;

    @Autowired
    private LogicalInterfaceRepository logicalInterfaceRepository;

    @Override
    public Class getActionClass() {
        return ModifyCBMRequest.class;
    }

    @Override
    public Object doPost(ActionContext actionContext) throws Exception {
        log.warn(Constants.EXECUTING_ACTION, ACTION_LABEL);

        ModifyCBMRequest input = (ModifyCBMRequest) actionContext.getObject();
        String context = "";

        try {
            // 1. Mandatory validations
            try {
                Validations.validateMandatoryParams(input.getSubscriberName(), "subscriberName");
                Validations.validateMandatoryParams(input.getResourceSN(), "resourceSN");
                Validations.validateMandatoryParams(input.getProductType(), "productType");
                Validations.validateMandatoryParams(input.getProductSubtype(), "productSubtype");
                Validations.validateMandatoryParams(input.getServiceId(), "serviceId");
                Validations.validateMandatoryParams(input.getModifyType(), "modifyType");
            } catch (BadRequestException bre) {
                return new ModifyCBMResponse("400",
                        ERROR_PREFIX + "Missing mandatory parameter : " + bre.getMessage(),
                        String.valueOf(System.currentTimeMillis()), "", "");
            }

            // Extract optional parameters
            String modifyParam1 = input.getModifyParam1();
            String modifyParam2 = input.getModifyParam2();
            String fxOrderId = input.getFxOrderId();
            String cbmModelInput = input.getCbmModel();

            // 2. Derive subscriberName
            String subscriberNameDerived = deriveSubscriberName(input.getProductType(), input.getSubscriberName(),
                    input.getResourceSN(), input.getModifyType(), modifyParam1);

            // 3. Compose Entity Names
            String subscriptionName = input.getSubscriberName()+Constants.UNDER_SCORE + input.getServiceId();
            String cfsName = "CFS"+Constants.UNDER_SCORE + subscriptionName;
            String rfsName = "RFS"+Constants.UNDER_SCORE + subscriptionName;
            String productName = input.getSubscriberName()+Constants.UNDER_SCORE + input.getProductSubtype()+Constants.UNDER_SCORE + input.getServiceId();
            String cbmDeviceName = "CBM"+Constants.UNDER_SCORE + input.getServiceId();

            // 4. Retrieve and update Key Entities
            // If modifyType includes package/components/products/contracts skip subscriber retrieval as per spec
            boolean skipEntities = containsAny(input.getModifyType(),
                    "Package", "Components", "Products", "Contracts");

            Customer subscriber = null;
            if (!skipEntities) {
                Optional<Customer> optSub = customerRepository.uivFindByGdn(subscriberNameDerived);
                if (!optSub.isPresent()) {
                    String msg = ERROR_PREFIX + "Object with UOR \"" + subscriberNameDerived + "\" not found";
                    return new ModifyCBMResponse("409", msg, String.valueOf(System.currentTimeMillis()), "", "");
                }
                subscriber = optSub.get();
            }

            Optional<Subscription> optSubscription = subscriptionRepository.uivFindByGdn(subscriptionName);
            if (!optSubscription.isPresent()) {
                String msg = ERROR_PREFIX + "Object with UOR \"" + subscriptionName + "\" not found";
                return new ModifyCBMResponse("409", msg, String.valueOf(System.currentTimeMillis()), "", "");
            }
            Subscription subscription = optSubscription.get();

            Optional<CustomerFacingService> optCfs = cfsRepository.uivFindByGdn(cfsName);
            if (!optCfs.isPresent()) {
                String msg = ERROR_PREFIX + "Object with UOR \"" + cfsName + "\" not found";
                return new ModifyCBMResponse("409", msg, String.valueOf(System.currentTimeMillis()), "", "");
            }
            CustomerFacingService cfs = optCfs.get();

            Optional<ResourceFacingService> optRfs = rfsRepository.uivFindByGdn(rfsName);
            if (!optRfs.isPresent()) {
                String msg = ERROR_PREFIX + "Object with UOR \"" + rfsName + "\" not found";
                return new ModifyCBMResponse("409", msg, String.valueOf(System.currentTimeMillis()), "", "");
            }
            ResourceFacingService rfs = optRfs.get();

            Optional<LogicalDevice> optCbm = logicalDeviceRepository.uivFindByGdn(cbmDeviceName);
            if (!optCbm.isPresent()) {
                String msg = ERROR_PREFIX + "Object with UOR \"" + cbmDeviceName + "\" not found";
                return new ModifyCBMResponse("409", msg, String.valueOf(System.currentTimeMillis()), "", "");
            }
            LogicalDevice cbmDevice = optCbm.get();

            // Update RFS metadata if fxOrderId present
            if (fxOrderId != null && !fxOrderId.trim().isEmpty()) {
                Map<String, Object> rfsProps = rfs.getProperties() == null ? new HashMap<>() : rfs.getProperties();
                rfsProps.put("transactionId", fxOrderId);
                rfsProps.put("transactionType", input.getModifyType());
                rfs.setProperties(rfsProps);
                rfsRepository.save(rfs, 2);
            }

            // 4 (continued). Update Service MAC or Gateway MAC flow
            if (!"IPTV".equalsIgnoreCase(input.getProductType())
                    && containsAny(input.getModifyType(), "ModfiyCableModem", "Cable_Modem")) {

                // _subscriberWithMAC_ is subscriberName + resourceSN (without colons?) spec says:
                String subscriberWithMAC = input.getSubscriberName() + sanitizeForName(input.getResourceSN());
                Optional<Customer> optSubscriberWithMac = customerRepository.uivFindByGdn(subscriberWithMAC);
                Customer subscriberWithMac = optSubscriberWithMac.orElse(null);

                // Try retrieving CBM device for modifyParam1 (if present)
                String cbmForParam1Name = null;
                LogicalDevice cbmForParam1 = null;
                if (modifyParam1 != null && !modifyParam1.trim().isEmpty()) {
                    cbmForParam1Name = "CBM" + removeColons(modifyParam1);
                    Optional<LogicalDevice> opt = logicalDeviceRepository.uivFindByGdn(cbmForParam1Name);
                    if (opt.isPresent()) cbmForParam1 = opt.get();
                }

                // find subscription by subscriptionName - already done above
                // Now update fields
                try {
                    // If subscription.serviceMAC equals input.resourceSN update / else try other logic
                    Map<String, Object> subProps = subscription.getProperties() == null ? new HashMap<>() : subscription.getProperties();
                    String svcMac = (String) subProps.getOrDefault("serviceMAC", "");

                    if (svcMac != null && svcMac.equalsIgnoreCase(input.getResourceSN())) {
                        // IPTV special-case
                        if ("IPTV".equalsIgnoreCase(subscription.getProperties() != null ? (String)subscription.getProperties().getOrDefault("subType", "") : "")) {
                            if (modifyParam1 != null && !modifyParam1.trim().isEmpty()) {
                                subProps.put("serviceMAC", modifyParam1);
                                cbmDevice.getProperties().put("macAddress", modifyParam1);
                                if (cbmModelInput != null) cbmDevice.getProperties().put("deviceModel", cbmModelInput);
                            }
                            if (modifyParam2 != null && !modifyParam2.trim().isEmpty()) {
                                subProps.put("gatewayMAC", modifyParam2);
                                cbmDevice.getProperties().put("gatewayMAC", modifyParam2);
                            }
                            subscription.setProperties(subProps);
                            subscriptionRepository.save(subscription, 2);
                        } else {
                            // Generic flow - update serviceMAC/gatewayMAC and cbm
                            if (modifyParam1 != null && !modifyParam1.trim().isEmpty()) {
                                subProps.put("serviceMAC", modifyParam1);
                                cbmDevice.getProperties().put("macAddress", modifyParam1);
                                if (cbmModelInput != null) cbmDevice.getProperties().put("deviceModel", cbmModelInput);
                            }
                            if (modifyParam2 != null && !modifyParam2.trim().isEmpty()) {
                                subProps.put("gatewayMAC", modifyParam2);
                                cbmDevice.getProperties().put("gatewayMAC", modifyParam2);
                            }
                            // Voice subtype special-case: update serviceSN
                            String subType = (String) subProps.getOrDefault("subType", "");
                            if ("Voice".equalsIgnoreCase(subType) && modifyParam1 != null && !modifyParam1.trim().isEmpty()) {
                                subProps.put("serviceSN", modifyParam1);
                            }

                            subscription.setProperties(subProps);
                            subscriptionRepository.save(subscription, 2);
                            logicalDeviceRepository.save(cbmDevice, 2);

                            // Replace resourceSN with modifyParam1 in subscriber name if needed
                            if (modifyParam1 != null && !modifyParam1.trim().isEmpty() && subscriber != null) {
                                String oldSubscriberName = subscriber.getLocalName();
                                String newSubscriberName = input.getSubscriberName() + "_" + removeColons(modifyParam1);
                                subscriber.setLocalName(newSubscriberName);
                                Map<String, Object> custProps = subscriber.getProperties() == null ? new HashMap<>() : subscriber.getProperties();
                                custProps.put("name", newSubscriberName);
                                subscriber.setProperties(custProps);
                                customerRepository.save(subscriber, 2);
                            }
                        }
                    } else {
                        // serviceMAC mismatch: try to find CBM by serviceID (from subscription) and update that device
                        String serviceID = (String) subscription.getProperties().getOrDefault("serviceID", "");
                        String newCBMName = "CBM" + serviceID;
                        Optional<LogicalDevice> optNewCbm = logicalDeviceRepository.uivFindByGdn(newCBMName);
                        if (optNewCbm.isPresent()) {
                            LogicalDevice newCBM = optNewCbm.get();
                            Map<String, Object> sProps = subscription.getProperties() == null ? new HashMap<>() : subscription.getProperties();
                            if (modifyParam1 != null && !modifyParam1.trim().isEmpty()) {
                                sProps.put("serviceMAC", modifyParam1);
                                newCBM.getProperties().put("macAddress", modifyParam1);
                                if (cbmModelInput != null) newCBM.getProperties().put("deviceModel", cbmModelInput);
                            }
                            if (modifyParam2 != null && !modifyParam2.trim().isEmpty()) {
                                sProps.put("gatewayMAC", modifyParam2);
                                newCBM.getProperties().put("gatewayMAC", modifyParam2);
                            }
                            // Voice subtype handling
                            String sType = (String) sProps.getOrDefault("subType", "");
                            if ("Voice".equalsIgnoreCase(sType) && modifyParam1 != null && !modifyParam1.trim().isEmpty()) {
                                sProps.put("serviceSN", modifyParam1);
                            }
                            subscription.setProperties(sProps);
                            subscriptionRepository.save(subscription, 2);
                            logicalDeviceRepository.save(newCBM, 2);

                            // update subscriber name replace resourceSN with modifyParam1
                            if (modifyParam1 != null && !modifyParam1.trim().isEmpty() && subscriber != null) {
                                String newSubscriberName = input.getSubscriberName() + "_" + removeColons(modifyParam1);
                                subscriber.setLocalName(newSubscriberName);
                                Map<String, Object> custProps = subscriber.getProperties() == null ? new HashMap<>() : subscriber.getProperties();
                                custProps.put("name", newSubscriberName);
                                subscriber.setProperties(custProps);
                                customerRepository.save(subscriber, 2);
                            }
                        } else {
                            // nothing to do - proceed
                        }
                    }
                } catch (Exception ex) {
                    log.error("Error updating MAC/Gateway", ex);
                    String msg = ERROR_PREFIX + "Error, ModifyCableModem request " + input.getModifyType() + " not executed";
                    return new ModifyCBMResponse("500", msg + " - " + ex.getMessage(), String.valueOf(System.currentTimeMillis()), "", "");
                }
            }

            // 5. Migrate Broadband Port Assignments
            if ("Broadband".equalsIgnoreCase(input.getProductType()) && modifyParam1 != null && !modifyParam1.trim().isEmpty()) {
                try {
                    String oldCbmName = "CBM_" + sanitizeForName(input.getResourceSN());
                    String newCbmName = "CBM_" + sanitizeForName(modifyParam1);

                    Optional<LogicalDevice> optOldCbm = logicalDeviceRepository.uivFindByGdn(oldCbmName);
                    Optional<LogicalDevice> optNewCbm = logicalDeviceRepository.uivFindByGdn(newCbmName);

                    if (optOldCbm.isPresent() && optNewCbm.isPresent()) {
                        LogicalDevice oldCbm = optOldCbm.get();
                        LogicalDevice newCbm = optNewCbm.get();

                        Map<String, Object> oldProps = oldCbm.getProperties() == null ? new HashMap<>() : oldCbm.getProperties();
                        Map<String, Object> newProps = newCbm.getProperties() == null ? new HashMap<>() : newCbm.getProperties();

                        // Copy VOIP_PORT1, VOIP_PORT2 if present
                        if (oldProps.containsKey("VOIP_PORT1")) newProps.put("VOIP_PORT1", oldProps.get("VOIP_PORT1"));
                        if (oldProps.containsKey("VOIP_PORT2")) newProps.put("VOIP_PORT2", oldProps.get("VOIP_PORT2"));

                        // 6. Set new CBM fields
                        newProps.put("administrativeState", "Allocated");
                        newProps.put("description", "Internet");
                        newProps.put("modelSubtype", "HFC");
                        newProps.put("voipPorts", newProps.getOrDefault("VOIP_PORT1", "Available") + "," + newProps.getOrDefault("VOIP_PORT2", "Available"));

                        newCbm.setProperties(newProps);
                        logicalDeviceRepository.save(newCbm, 2);

                        // Reset old CBM
                        oldProps.put("administrativeState", "Available");
                        oldProps.put("description", "");
                        oldProps.put("modelSubtype", "");
                        oldProps.put("voipPorts", "Available");
                        oldCbm.setProperties(oldProps);
                        logicalDeviceRepository.save(oldCbm, 2);
                    }
                } catch (Exception ex) {
                    log.error("Error migrating broadband ports", ex);
                    String msg = ERROR_PREFIX + "Error while migrating broadband ports";
                    return new ModifyCBMResponse("500", msg + " - " + ex.getMessage(), String.valueOf(System.currentTimeMillis()), "", "");
                }
            }

            // 7. Modify Profile, Package, or Components
            if (containsAny(input.getModifyType(), "Package", "Components", "Products", "Contracts")) {
                try {
                    Map<String, Object> sProps = subscription.getProperties() == null ? new HashMap<>() : subscription.getProperties();
                    if (modifyParam1 != null && !modifyParam1.trim().isEmpty()) {
                        sProps.put("veipQosSessionProfile", modifyParam1);
                        sProps.put("servicePackage", modifyParam1);
                        sProps.put("voipPackage1", modifyParam1);
                    }
                    subscription.setProperties(sProps);
                    subscriptionRepository.save(subscription, 2);
                } catch (Exception ex) {
                    log.error("Error updating package/components", ex);
                    String msg = ERROR_PREFIX + "Error updating package/components";
                    return new ModifyCBMResponse("500", msg + " - " + ex.getMessage(), String.valueOf(System.currentTimeMillis()), "", "");
                }
            }

            // 8. Update Email Password
            if (input.getModifyType() != null && input.getModifyType().contains("Password")) {
                try {
                    if (subscriber == null) {
                        // try to get subscriber by original subscriberName
                        Optional<Customer> alt = customerRepository.uivFindByGdn(input.getSubscriberName());
                        if (alt.isPresent()) subscriber = alt.get();
                    }
                    if (subscriber != null) {
                        Map<String, Object> custProps = subscriber.getProperties() == null ? new HashMap<>() : subscriber.getProperties();
                        custProps.put("email_pwd", modifyParam1 != null ? modifyParam1 : "");
                        subscriber.setProperties(custProps);
                        customerRepository.save(subscriber, 2);
                    } else {
                        String msg = ERROR_PREFIX + "Subscriber not found for password update";
                        return new ModifyCBMResponse("409", msg, String.valueOf(System.currentTimeMillis()), "", "");
                    }
                } catch (Exception ex) {
                    log.error("Error updating email password", ex);
                    String msg = ERROR_PREFIX + "Error updating email password";
                    return new ModifyCBMResponse("500", msg + " - " + ex.getMessage(), String.valueOf(System.currentTimeMillis()), "", "");
                }
            }

            // 9 & 10. Update Service ID and VOIP Number and Rename entities if serviceID changed
            if (input.getModifyType() != null && input.getModifyType().contains("Modify_Number")) {
                try {
                    String newServiceId = modifyParam1;
                    if (newServiceId != null && !newServiceId.trim().isEmpty()) {
                        // Update subscription fields: serviceID, voipNumber1
                        Map<String, Object> sProps = subscription.getProperties() == null ? new HashMap<>() : subscription.getProperties();
                        sProps.put("serviceID", newServiceId);
                        sProps.put("voipNumber1", newServiceId);
                        subscription.setProperties(sProps);

                        // If service ID changed, reconstruct names
                        if (!input.getServiceId().equals(newServiceId)) {
                            String subscriptionNameNew = input.getSubscriberName() + newServiceId;
                            String cfsNameNew = "CFS" + subscriptionNameNew;
                            String rfsNameNew = "RFS_" + subscriptionNameNew;
                            String productNameNew = input.getSubscriberName() + input.getProductSubtype() + newServiceId;
                            String cbmDeviceNameNew = "CBM" + newServiceId;

                            // rename subscription
                            subscription.setLocalName(subscriptionNameNew);
                            subscriptionRepository.save(subscription, 2);

                            // rename product
                            Optional<Product> optProduct = productRepository.uivFindByGdn(productName);
                            if (optProduct.isPresent()) {
                                Product prod = optProduct.get();
                                prod.setLocalName(productNameNew);
                                Map<String,Object> pProps = prod.getProperties() == null ? new HashMap<>() : prod.getProperties();
                                pProps.put("name", productNameNew);
                                prod.setProperties(pProps);
                                productRepository.save(prod, 2);
                            }

                            // rename CFS
                            if (cfs != null) {
                                cfs.setLocalName(cfsNameNew);
                                Map<String,Object> cfsProps = cfs.getProperties() == null ? new HashMap<>() : cfs.getProperties();
                                cfsProps.put("name", cfsNameNew);
                                cfs.setProperties(cfsProps);
                                cfs.setEndDate(Date.from(java.time.Instant.now()));
                                cfsRepository.save(cfs, 2);
                            }

                            // rename RFS
                            if (rfs != null) {
                                rfs.setLocalName(rfsNameNew);
                                Map<String,Object> rfsProps = rfs.getProperties() == null ? new HashMap<>() : rfs.getProperties();
                                rfsProps.put("name", rfsNameNew);
                                if (fxOrderId != null) rfsProps.put("transactionId", fxOrderId);
                                rfs.setProperties(rfsProps);
                                rfsRepository.save(rfs, 2);
                            }

                            // rename CBM
                            Optional<LogicalDevice> optOldCbmDevice = logicalDeviceRepository.uivFindByGdn(cbmDeviceName);
                            if (optOldCbmDevice.isPresent()) {
                                LogicalDevice oldCbmDevice = optOldCbmDevice.get();
                                oldCbmDevice.setLocalName(cbmDeviceNameNew);
                                Map<String,Object> cbmProps = oldCbmDevice.getProperties() == null ? new HashMap<>() : oldCbmDevice.getProperties();
                                cbmProps.put("name", cbmDeviceNameNew);
                                oldCbmDevice.setProperties(cbmProps);
                                logicalDeviceRepository.save(oldCbmDevice, 2);

                                // If old serviceID matches CBM VOIP port update that port
                                if (cbmProps.containsKey("VOIP_PORT1")) {
                                    String port = (String) cbmProps.get("VOIP_PORT1");
                                    if (port != null && port.contains(input.getServiceId())) {
                                        cbmProps.put("VOIP_PORT1", newServiceId);
                                        oldCbmDevice.setProperties(cbmProps);
                                        logicalDeviceRepository.save(oldCbmDevice, 2);
                                    }
                                }
                            }
                        } else {
                            subscriptionRepository.save(subscription, 2);
                        }
                    }
                } catch (Exception ex) {
                    log.error("Error modifying service ID", ex);
                    String msg = ERROR_PREFIX + "Error modifying service ID";
                    return new ModifyCBMResponse("500", msg + " - " + ex.getMessage(), String.valueOf(System.currentTimeMillis()), "", "");
                }
            }

            // 11. Final Response (success)
            String outSubscriberId = (subscriber != null) ? subscriber.getLocalName() : input.getSubscriberName();
            String outSubscriptionId = subscription.getLocalName();

            return new ModifyCBMResponse("200", "UIV action ModifyCBM executed successfully.",
                    java.time.Instant.now().toString(), outSubscriberId, outSubscriptionId);

        } catch (Exception ex) {
            log.error("Unhandled exception during ModifyCBM", ex);
            String msg = ERROR_PREFIX + "Internal server error occurred";
            return new ModifyCBMResponse("500", msg + " - " + ex.getMessage(), String.valueOf(System.currentTimeMillis()), "", "");
        }
    }

    // --- helper utilities ---

    private boolean containsAny(String source, String... toks) {
        if (source == null) return false;
        for (String t : toks) {
            if (source.contains(t)) return true;
        }
        return false;
    }

    private String removeColons(String s) {
        if (s == null) return null;
        return s.replace(":", "").replace("-", "").trim();
    }

    // sanitize for names: remove colons and non-alphanumerics except underscore and dot
    private String sanitizeForName(String s) {
        if (s == null) return "";
        return s.replace(":", "").replace("-", "").trim();
    }

    private String deriveSubscriberName(String productType, String origSubscriber, String resourceSN, String modifyType, String modifyParam1) {
        if ("IPTV".equalsIgnoreCase(productType)) {
            return origSubscriber;
        } else if (resourceSN == null || resourceSN.trim().isEmpty() || "NA".equalsIgnoreCase(resourceSN.trim())) {
            if (containsAny(modifyType, "Package", "Components", "Products", "Contracts")) {
                return origSubscriber;
            } else {
                if (modifyParam1 != null && !modifyParam1.trim().isEmpty()) {
                    return origSubscriber + "_" + removeColons(modifyParam1);
                } else {
                    return origSubscriber;
                }
            }
        } else {
            return origSubscriber + "_" + removeColons(resourceSN);
        }
    }
}
