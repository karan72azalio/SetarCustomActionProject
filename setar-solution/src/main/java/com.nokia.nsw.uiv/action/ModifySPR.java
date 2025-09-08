package com.nokia.nsw.uiv.action;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.nokia.nsw.uiv.exception.BadRequestException;
import com.nokia.nsw.uiv.exception.ModificationNotAllowedException;
import com.nokia.nsw.uiv.framework.action.Action;
import com.nokia.nsw.uiv.framework.action.ActionContext;
import com.nokia.nsw.uiv.framework.action.HttpAction;
import com.nokia.nsw.uiv.model.resource.logical.LogicalComponent;
import com.nokia.nsw.uiv.model.resource.logical.LogicalComponentRepository;
import com.nokia.nsw.uiv.model.resource.logical.LogicalDevice;
import com.nokia.nsw.uiv.model.resource.logical.LogicalDeviceRepository;
import com.nokia.nsw.uiv.model.resource.logical.LogicalInterface;
import com.nokia.nsw.uiv.model.resource.logical.LogicalInterfaceRepository;
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

    @Override
    public Class getActionClass() {
        return ModifySPRRequest.class;
    }

    @Override
    public Object doPost(ActionContext actionContext) throws Exception {
        log.warn(Constants.EXECUTING_ACTION, ACTION_LABEL);

        ModifySPRRequest request = (ModifySPRRequest) actionContext.getObject();
        boolean success = false;

        try {
            // 1. Perform Mandatory Validations
            log.info(Constants.MANDATORY_PARAMS_VALIDATION_STARTED);
            Validations.validateMandatoryParams(request.getSubscriberName(), "SUBSCRIBER_NAME");
            Validations.validateMandatoryParams(request.getProductType(), "PRODUCT_TYPE");
            Validations.validateMandatoryParams(request.getProductSubtype(), "PRODUCT_SUB_TYPE");
            Validations.validateMandatoryParams(request.getOntSN(), "ONT_SN");
            Validations.validateMandatoryParams(request.getServiceId(), "SERVICE_ID");
            Validations.validateMandatoryParams(request.getModifyType(), "MODIFY_TYPE");
            log.info(Constants.MANDATORY_PARAMS_VALIDATION_COMPLETED);

            // 2. Name Construction
            String subscriberName = request.getSubscriberName();
            String subscriptionName = request.getSubscriberName() + request.getServiceId() + request.getOntSN();
            String ontName = "ONT" + request.getOntSN();

            if (ontName.length() > 100) {
                throw new BadRequestException("ONT name too long");
            }

// 3. Fetch Required Entities
            Optional<LogicalDevice> optSubscriber = logicalDeviceRepository.uivFindByGdn(subscriberName);
            if (!optSubscriber.isPresent()) {
                throw new BadRequestException("Object with name \"" + subscriberName + "\" not found");
            }
            LogicalDevice subscriber = optSubscriber.get();

            Optional<LogicalComponent> optSubscription = logicalComponentRepository.uivFindByGdn(subscriptionName);
            if (!optSubscription.isPresent()) {
                throw new BadRequestException("Subscription not found");
            }
            LogicalComponent subscription = optSubscription.get();


            // 4. Modify Logic for Fibernet/Broadband
            if ("Fibernet".equalsIgnoreCase(request.getProductType()) || "Broadband".equalsIgnoreCase(request.getProductType())) {
                if ("Username".equalsIgnoreCase(request.getModifyType())) {
                    Map<String, Object> subProps = subscription.getProperties();
                    subProps.put("subscriptionDetails", request.getModifyParam1());
                    subProps.put("serviceID", request.getModifyParam3());
                    subscription.setProperties(subProps);

                    Map<String, Object> subrProps = subscriber.getProperties();
                    subrProps.put("email_username", request.getModifyParam2());
                    subscriber.setProperties(subrProps);

                    if (!request.getServiceId().equals(request.getModifyParam3())) {
                        String oldSubscriptionName = request.getSubscriberName() + request.getServiceId() + request.getOntSN();
                        String productName = request.getSubscriberName() + request.getProductSubtype() + request.getServiceId();
                        String cfsName = "CFS_" + oldSubscriptionName;
                        String rfsName = "RFS_" + oldSubscriptionName;

                        Optional<LogicalComponent> optProduct = logicalComponentRepository.uivFindByGdn(productName);
                        Optional<LogicalComponent> optCFS = logicalComponentRepository.uivFindByGdn(cfsName);
                        Optional<LogicalComponent> optRFS = logicalComponentRepository.uivFindByGdn(rfsName);

                        String subscriptionNameNew = request.getSubscriberName() + request.getModifyParam3() + request.getOntSN();
                        String productNameNew = request.getSubscriberName() + request.getProductSubtype() + request.getModifyParam3();
                        String cfsNameNew = "CFS_" + subscriptionNameNew;
                        String rfsNameNew = "RFS_" + subscriptionNameNew;

                        if (optProduct.isPresent()) {
                            LogicalComponent product = optProduct.get();
                            product.setLocalName(productNameNew);
                            logicalComponentRepository.save(product, 2);
                        }

                        if (optCFS.isPresent()) {
                            LogicalComponent cfs = optCFS.get();
                            cfs.setLocalName(cfsNameNew);
                            cfs.getProperties().put("serviceEndDate", getCurrentTimestamp());
                            logicalComponentRepository.save(cfs, 2);
                        }

                        if (optRFS.isPresent()) {
                            LogicalComponent rfs = optRFS.get();
                            rfs.setLocalName(rfsNameNew);
                            rfs.getProperties().put("transactionType", request.getModifyType());
                            if (request.getFxOrderId() != null) {
                                rfs.getProperties().put("transactionId", request.getFxOrderId());
                            }
                            logicalComponentRepository.save(rfs, 2);
                        }

                        subscription.setLocalName(subscriptionNameNew);
                    }

                    logicalComponentRepository.save(subscription, 2);
                    logicalDeviceRepository.save(subscriber, 2);
                    success = true;

                } else if ("Password".equalsIgnoreCase(request.getModifyType())) {
                    try {
                        Map<String, Object> subrProps = subscriber.getProperties();
                        subrProps.put("email_pwd", request.getModifyParam1());
                        subscriber.setProperties(subrProps);
                        logicalDeviceRepository.save(subscriber, 2);
                        success = true;
                    } catch (Exception e) {
                        throw new ModificationNotAllowedException("Failed to persist password update"+e.getMessage());
                    }
                }

                else if (List.of("Package", "Component", "Product", "Contract").contains(request.getModifyType())) {
                    Map<String, Object> subProps = subscription.getProperties();
                    try {
                        if ("Cloudstarter".equalsIgnoreCase(request.getProductSubtype()) || "Bridged".equalsIgnoreCase(request.getProductSubtype())) {
                            subProps.put("evpnQosSessionProfile", request.getModifyParam1());
                        } else {
                            subProps.put("veipQosSessionProfile", request.getModifyParam1());
                        }
                        subscription.setProperties(subProps);
                        logicalComponentRepository.save(subscription, 2);
                        success = true;
                    } catch (Exception e) {
                    throw new ModificationNotAllowedException("Failed to update QoS profile "+e.getMessage());
                }

            }
            }

            // 5. Modify Logic for EVPN/ENTERPRISE
            else if ("EVPN".equalsIgnoreCase(request.getProductType()) || "ENTERPRISE".equalsIgnoreCase(request.getProductType())) {
                if ("Username".equalsIgnoreCase(request.getModifyType())) {
                    Map<String, Object> subProps = subscription.getProperties();
                    subProps.put("subscriptionDetails", "FTTB-" + request.getModifyParam1());
                    subProps.put("serviceID", request.getModifyParam1());
                    subscription.setProperties(subProps);

                    if (!request.getServiceId().equals(request.getModifyParam1())) {
                        String oldSubscriptionName = request.getSubscriberName() + request.getServiceId() + request.getOntSN();
                        String productName = request.getSubscriberName() + request.getProductSubtype() + request.getServiceId();
                        String cfsName = "CFS_" + oldSubscriptionName;
                        String rfsName = "RFS_" + oldSubscriptionName;

                        String subscriptionNameNew = request.getSubscriberName() + request.getModifyParam1() + request.getOntSN();
                        String productNameNew = request.getSubscriberName() + request.getProductSubtype() + request.getModifyParam1();
                        String cfsNameNew = "CFS_" + subscriptionNameNew;
                        String rfsNameNew = "RFS_" + subscriptionNameNew;

                        Optional<LogicalComponent> optProduct = logicalComponentRepository.uivFindByGdn(productName);
                        Optional<LogicalComponent> optCFS = logicalComponentRepository.uivFindByGdn(cfsName);
                        Optional<LogicalComponent> optRFS = logicalComponentRepository.uivFindByGdn(rfsName);

                        if (optProduct.isPresent()) {
                            LogicalComponent product = optProduct.get();
                            product.setLocalName(productNameNew);
                            logicalComponentRepository.save(product, 2);
                        }

                        if (optCFS.isPresent()) {
                            LogicalComponent cfs = optCFS.get();
                            cfs.setLocalName(cfsNameNew);
                            logicalComponentRepository.save(cfs, 2);
                        }

                        if (optRFS.isPresent()) {
                            LogicalComponent rfs = optRFS.get();
                            rfs.setLocalName(rfsNameNew);
                            rfs.getProperties().put("transactionType", request.getModifyType());
                            logicalComponentRepository.save(rfs, 2);
                        }

                        subscription.setLocalName(subscriptionNameNew);
                    }

                    logicalComponentRepository.save(subscription, 2);
                    success = true;

                } else if ("Component".equalsIgnoreCase(request.getModifyType())) {
                    try {
                        Map<String, Object> subProps = subscription.getProperties();
                        subProps.put("evpnQosSessionProfile", request.getModifyParam1());
                        subscription.setProperties(subProps);
                        logicalComponentRepository.save(subscription, 2);
                        success=true;
                    } catch (Exception e) {
                        throw new ModificationNotAllowedException("Failed to update EVPN component "+ e);
                    }

                }
            }

            // 6. Modify Logic for VOIP/Voice
            else if ("VOIP".equalsIgnoreCase(request.getProductType()) || "Voice".equalsIgnoreCase(request.getProductType())) {
                try {
                    if (List.of("Package", "Product").contains(request.getModifyType())) {
                        Map<String, Object> subProps = subscription.getProperties();
                        subProps.put("voipPackage1", request.getModifyParam1());
                        subProps.put("voipServiceCode1", request.getModifyParam2());
                        subscription.setProperties(subProps);
                        logicalComponentRepository.save(subscription, 2);
                        success = true;
                    }
                } catch (Exception e) {
                    throw new ModificationNotAllowedException("Failed to update VoIP package "+e.getMessage());
                }

                } else if ("Modify_Number".equalsIgnoreCase(request.getModifyType())) {
                try {
                    Map<String, Object> subProps = subscription.getProperties();
                    subProps.put("serviceID", request.getModifyParam1());
                    subscription.setProperties(subProps);

                    Optional<LogicalDevice> optOnt = logicalDeviceRepository.uivFindByGdn(ontName);
                    if (!optOnt.isPresent()) {
                        throw new BadRequestException("ONT not found");
                    }
                    LogicalDevice ont = optOnt.get();

                    Map<String, Object> ontProps = ont.getProperties();
                    ontProps.put("potsPort1Number", request.getModifyParam1());
                    ont.setProperties(ontProps);

                    if (!request.getServiceId().equals(request.getModifyParam1())) {
                        String oldSubscriptionName = request.getSubscriberName() + request.getServiceId() + request.getOntSN();
                        String subscriptionNameNew = request.getSubscriberName() + request.getModifyParam1() + request.getOntSN();
                        subscription.setLocalName(subscriptionNameNew);

                        String cfsName = "CFS_" + oldSubscriptionName;
                        String rfsName = "RFS_" + oldSubscriptionName;
                        String cfsNameNew = "CFS_" + subscriptionNameNew;
                        String rfsNameNew = "RFS_" + subscriptionNameNew;

                        Optional<LogicalComponent> optCFS = logicalComponentRepository.uivFindByGdn(cfsName);
                        Optional<LogicalComponent> optRFS = logicalComponentRepository.uivFindByGdn(rfsName);

                        if (optCFS.isPresent()) {
                            LogicalComponent cfs = optCFS.get();
                            cfs.setLocalName(cfsNameNew);
                            logicalComponentRepository.save(cfs, 2);
                        }

                        if (optRFS.isPresent()) {
                            LogicalComponent rfs = optRFS.get();
                            rfs.setLocalName(rfsNameNew);
                            logicalComponentRepository.save(rfs, 2);
                        }
                    }

                    logicalDeviceRepository.save(ont, 2);
                    logicalComponentRepository.save(subscription, 2);
                    success = true;
                } catch (Exception e) {
                    throw new ModificationNotAllowedException("Failed to modify VOIP number: "+e.getMessage());
                }
                }


            // 7. Modify Logic for ONT
            else if ("Modify_ONT".equalsIgnoreCase(request.getModifyType()) || "ONT".equalsIgnoreCase(request.getModifyType())) {
                Optional<LogicalDevice> optOnt = logicalDeviceRepository.uivFindByGdn(ontName);
                if (!optOnt.isPresent()) {
                    throw new BadRequestException("ONT not found");
                }
                LogicalDevice ont = optOnt.get();

                List<LogicalComponent> allComponents = (List<LogicalComponent>) logicalComponentRepository.findAll();

                for (LogicalComponent sub : allComponents) {
                    Map<String, Object> subProps = sub.getProperties();

                    if (subProps != null && request.getSubscriberName().equals(subProps.get("simaCustomerId"))) {
                        subProps.put("serviceSN", request.getModifyParam1());
                        sub.setProperties(subProps);
                        logicalComponentRepository.save(sub, 2); // depth 2 for relationships
                    }
                }


                Map<String, Object> ontProps = ont.getProperties();
                ontProps.put("serialNo", request.getModifyParam1());
                ont.setLocalName("resourceName " + request.getModifyParam1());
                ont.setProperties(ontProps);

                List<LogicalInterface> allIfaces = (List<LogicalInterface>) logicalInterfaceRepository.findAll();

                for (LogicalInterface vlan : allIfaces) {
                    if (vlan.getLocalName() != null && vlan.getLocalName().contains(request.getOntSN())) {
                        String newVlanName = vlan.getLocalName().replace(request.getOntSN(), request.getModifyParam1());
                        vlan.setLocalName(newVlanName);
                        logicalInterfaceRepository.save(vlan, 2); // depth 2 to persist relationships
                    }
                    else{
                        System.out.println("ONT update failed");
                    }
                }


                String cpeDeviceName = request.getProductType() + "_" + request.getOntSN();
                Optional<LogicalDevice> optCpeDevice = logicalDeviceRepository.uivFindByGdn(cpeDeviceName);
                if (optCpeDevice.isPresent()) {
                    LogicalDevice cpeDevice = optCpeDevice.get();
                    Map<String, Object> cpeProps = cpeDevice.getProperties();
                    cpeProps.put("serialNo", request.getModifyParam1());
                    cpeDevice.setLocalName(request.getProductType() + "_" + request.getModifyParam1());
                    cpeDevice.setProperties(cpeProps);
                    logicalDeviceRepository.save(cpeDevice, 2);
                }

                logicalDeviceRepository.save(ont, 2);
                success = true;
            }

            // 8. Final Response
            if (success) {
                log.info(Constants.ACTION_COMPLETED);
                return new ModifySPRResponse("200", "UIV action ModifySPR executed successfully.", getCurrentTimestamp(),
                        ontName, subscriptionName);
            } else {
                throw new Exception("Modify operation failed");
            }

        } catch (BadRequestException bre) {
            // Code5: Missing mandatory parameter, Code6: ONT name too long, Code2: Not found
            log.error("Validation or not found error: {}", bre.getMessage(), bre);
            String msg = ERROR_PREFIX + bre.getMessage();
            return new ModifySPRResponse("400", msg, getCurrentTimestamp(), "", "");
        } catch (ModificationNotAllowedException ex) {
            // Code3: Persistence failure
            log.error("Persistence error: {}", ex.getMessage(), ex);
            String msg = ERROR_PREFIX + ex.getMessage();
            return new ModifySPRResponse("500", msg, getCurrentTimestamp(), "", "");
        } catch (Exception ex) {
            // Code1: Unhandled exception
            log.error("Unhandled exception during ModifySPR", ex);
            String msg = ERROR_PREFIX + "Internal server error occurred";
            return new ModifySPRResponse("500", msg, getCurrentTimestamp(), "", "");
        }
    }

    private String getCurrentTimestamp() {
        return java.time.Instant.now().toString();
    }
}
