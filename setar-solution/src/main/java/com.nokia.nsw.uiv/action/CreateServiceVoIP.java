package com.nokia.nsw.uiv.action;

import com.nokia.nsw.uiv.exception.AccessForbiddenException;
import com.nokia.nsw.uiv.exception.BadRequestException;
import com.nokia.nsw.uiv.framework.action.Action;
import com.nokia.nsw.uiv.framework.action.ActionContext;
import com.nokia.nsw.uiv.framework.action.HttpAction;
import com.nokia.nsw.uiv.model.resource.logical.LogicalDevice;
import com.nokia.nsw.uiv.model.service.Product;
import com.nokia.nsw.uiv.model.service.Service;
import com.nokia.nsw.uiv.repository.*;
import com.nokia.nsw.uiv.request.CreateServiceVoIPRequest;
import com.nokia.nsw.uiv.response.CreateServiceCBMResponse;
import com.nokia.nsw.uiv.response.CreateServiceCbmVoiceResponse;
import com.nokia.nsw.uiv.response.CreateServiceVoIPResponse;
import com.nokia.nsw.uiv.utils.Constants;
import com.nokia.nsw.uiv.utils.Validations;

import com.nokia.nsw.uiv.model.common.party.Customer;
import com.nokia.nsw.uiv.model.service.Subscription;
import com.nokia.nsw.uiv.model.service.SubscriptionRepository;
import com.nokia.nsw.uiv.model.common.party.CustomerRepository;
import com.nokia.nsw.uiv.model.resource.logical.LogicalDeviceRepository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@RestController
@Action
@Slf4j
public class CreateServiceVoIP implements HttpAction {
    protected static final String ACTION_LABEL = Constants.CREATE_SERVICE_VOIP;
    private static final String ERROR_PREFIX = "UIV action CreateServiceVoIP execution failed - ";

    @Autowired private CustomerCustomRepository customerRepo;
    @Autowired private SubscriptionCustomRepository subscriptionRepo;
    @Autowired private ProductCustomRepository productRepo;
    @Autowired private LogicalDeviceCustomRepository logicalDeviceRepo;
    @Autowired private ServiceCustomRepository serviceCustomRepository;

    @Override
    public Class<?> getActionClass() {
        return CreateServiceVoIPRequest.class;
    }

    @Override
    public Object doPost(ActionContext actionContext) {
        log.error("Executing CreateServiceVoIP action...");
        log.error(Constants.EXECUTING_ACTION, ACTION_LABEL);
        CreateServiceVoIPRequest req = (CreateServiceVoIPRequest) actionContext.getObject();

        try {
            // Step 1: Validate mandatory params
            try {
                log.error(Constants.MANDATORY_PARAMS_VALIDATION_STARTED);
                Validations.validateMandatoryParams(req.getSubscriberName(), "subscriberName");
                Validations.validateMandatoryParams(req.getProductType(), "productType");
                Validations.validateMandatoryParams(req.getProductSubtype(), "productSubtype");
                Validations.validateMandatoryParams(req.getOntSN(), "ontSN");
                Validations.validateMandatoryParams(req.getOntPort(), "ontPort");
                Validations.validateMandatoryParams(req.getOltName(), "oltName");
                Validations.validateMandatoryParams(req.getVoipServiceTemplate(), "voipServiceTemplate");
                Validations.validateMandatoryParams(req.getSimaCustID(), "simaCustID");
                Validations.validateMandatoryParams(req.getSimaSubsID(), "simaSubsID");
                Validations.validateMandatoryParams(req.getSimaEndpointID(), "simaEndpointID");
                Validations.validateMandatoryParams(req.getVoipNumber1(), "voipNumber1");
                Validations.validateMandatoryParams(req.getTemplateNameOnt(), "templateNameOnt");
                Validations.validateMandatoryParams(req.getTemplateNamePots1(), "templateNamePots1");
                Validations.validateMandatoryParams(req.getTemplateNamePots2(), "templateNamePots2");
                Validations.validateMandatoryParams(req.getHhid(), "hhid");
                Validations.validateMandatoryParams(req.getOntModel(), "ontModel");
                Validations.validateMandatoryParams(req.getServiceId(), "serviceId");
                Validations.validateMandatoryParams(req.getVoipServiceCode(), "voipServiceCode");
                Validations.validateMandatoryParams(req.getVoipPackage(), "voipPackage");
                log.error(Constants.MANDATORY_PARAMS_VALIDATION_COMPLETED);
            } catch (BadRequestException bre) {
                return new CreateServiceVoIPResponse(
                        "400",
                        ERROR_PREFIX + "Missing mandatory parameter: " + bre.getMessage(),
                        Instant.now().toString(),
                        null,
                        null
                );
            }
            AtomicBoolean isSubscriberExist = new AtomicBoolean(true);
            AtomicBoolean isSubscriptionExist = new AtomicBoolean(true);
            AtomicBoolean isProductExist = new AtomicBoolean(true);

            // Step 2 & 3: Subscriber
            String subscriberNameStr = req.getSubscriberName() + Constants.UNDER_SCORE  + req.getOntSN();
            if (subscriberNameStr.length() > 100) {
                return new CreateServiceVoIPResponse(
                        "400",
                        ERROR_PREFIX + "Subscriber name too long",
                        Instant.now().toString(),
                        null,
                        null
                );
            }
            Customer subscriber = customerRepo.findByDiscoveredName(subscriberNameStr)

                    .orElseGet(() -> {
                        Customer newSub = new Customer();
                        isSubscriberExist.set(false);
                        try {
                            newSub.setLocalName(Validations.encryptName(subscriberNameStr));
                            newSub.setDiscoveredName(subscriberNameStr);
                            newSub.setContext("Setar");
                            newSub.setKind("SetarSubscriber");
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                        Map<String, Object> subProps = new HashMap<>();
                        subProps.put("subscriptionStatus", "Active");
                        subProps.put("subscriberType", "Regular");
                        subProps.put("accountNumber", req.getSubscriberName());
                        subProps.put("householdId", req.getHhid());
                        newSub.setProperties(subProps);
                        return customerRepo.save(newSub);
                    });

            // Step 4: Subscription
            String subscriptionName = req.getSubscriberName() + Constants.UNDER_SCORE  + req.getServiceId() + Constants.UNDER_SCORE  + req.getOntSN();
            if (subscriptionName.length() > 100) {
                return new CreateServiceVoIPResponse(
                        "400",
                        ERROR_PREFIX + "Subscription name too long",
                        Instant.now().toString(),
                        null,
                        null
                );
            }
            Subscription subscription = subscriptionRepo.findByDiscoveredName(subscriptionName)
                    .orElseGet(() -> {
                        isSubscriptionExist.set(false);
                        Subscription subs = new Subscription();
                        try {
                            subs.setLocalName(Validations.encryptName(subscriptionName));
                            subs.setDiscoveredName(subscriptionName);
                            subs.setContext("Setar");
                            subs.setKind("SetarSubscription");
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                        Map<String, Object> subProps = new HashMap<>();
                        subProps.put("subscriptionStatus", "Active");
                        subProps.put("serviceSubtype", req.getProductSubtype());
                        subProps.put("serviceID", req.getServiceId());
                        subProps.put("oltPosition", req.getOltName());
                        subProps.put("householdId", req.getHhid());
                        subs.setProperties(subProps);
                        subs.setCustomer(subscriber);
                        return subscriptionRepo.save(subs);
                    });

            // Step 5: Update attributes
            Map<String, Object> subProps = subscriber.getProperties();
            if (req.getFirstName() != null) subProps.put("firstName", req.getFirstName());
            if (req.getLastName() != null) subProps.put("lastName", req.getLastName());
            if (req.getCompanyName() != null) subProps.put("companyName", req.getCompanyName());
            if (req.getContactPhone() != null) subProps.put("contactPhone", req.getContactPhone());
            if (req.getSubsAddress() != null) subProps.put("subsAddress", req.getSubsAddress());
            Object existingSimaCustId = subProps.get("simaCustId");

            if ((existingSimaCustId == null || existingSimaCustId.toString().isEmpty())
                    && req.getSimaCustID() != null && !req.getSimaCustID().isEmpty()) {
                subProps.put("simaCustId", req.getSimaCustID());
            }
            subscriber.setProperties(subProps);

            Map<String, Object> subsProps = subscription.getProperties();
            subsProps.put("voipNumber1", req.getVoipNumber1());
            subsProps.put("simaCustId", req.getSimaCustID());
            subsProps.put("simaSubsId", req.getSimaSubsID());
            subsProps.put("simaEndpointId", req.getSimaEndpointID());
            subsProps.put("voipPackage", req.getVoipPackage());
            subsProps.put("voipServiceCode", req.getVoipServiceCode());
            subsProps.put("serviceLink",(req.getOntSN()!=null && req.getOntSN().startsWith("ALCL"))?"ONT":"Cable_Modem");
            subscription.setProperties(subsProps);

            customerRepo.save(subscriber);
            subscriptionRepo.save(subscription);

            // Step 7: Product
            String productNameStr = req.getSubscriberName() +Constants.UNDER_SCORE + req.getProductSubtype() +Constants.UNDER_SCORE + req.getServiceId();
            if (productNameStr.length() > 100) {
                return new CreateServiceVoIPResponse(
                        "400",
                        ERROR_PREFIX + "Product name too long",
                        Instant.now().toString(),
                        null,
                        null
                );
            }
            Product product = productRepo.findByDiscoveredName(productNameStr)
                    .orElseGet(() -> {
                        Product prod = new Product();
                        isProductExist.set(false);
                        try {
                            prod.setLocalName(Validations.encryptName(productNameStr));
                            prod.setDiscoveredName(productNameStr);
                            prod.setContext("Setar");
                            prod.setKind("SetarProduct");
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                        Map<String, Object> prodProps = new HashMap<>();
                        prodProps.put("productStatus", "Active");
                        prodProps.put("productType", req.getProductType());
                        prod.setProperties(prodProps);
                        prod.setCustomer(subscriber);
                        return productRepo.save(prod);
                    });
            if(isSubscriptionExist.get()){
                subscription = subscriptionRepo.findByDiscoveredName(subscription.getDiscoveredName()).get();
                Set<Service> existingServices = subscription.getService();
                existingServices.add(product);
                subscription.setService(existingServices);
            }else{
                subscription.setService(new HashSet<>(List.of(product)));
            }
            subscriptionRepo.save(subscription,2);
            if(isSubscriberExist.get() && isSubscriptionExist.get() && isProductExist.get()){
                log.error("createServiceCbmVoice service already exist");
                return new CreateServiceVoIPResponse("409","Service already exist/Duplicate entry",Instant.now().toString(),subscriptionName,"ONT" + req.getOntSN());
            }
            // Step 8: CFS
            String cfsName = "CFS" + Constants.UNDER_SCORE + subscriptionName;
            Service cfs = serviceCustomRepository.findByDiscoveredName(cfsName)
                    .orElseGet(() -> {
                        Service newCfs = new Service();
                        try {
                            newCfs.setLocalName(Validations.encryptName(cfsName));
                            newCfs.setDiscoveredName(cfsName);
                            newCfs.setContext("Setar");
                            newCfs.setKind("SetarCFS");
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                        Map<String, Object> cfsProps = new HashMap<>();
                        cfsProps.put("cfsStatus", "Active");
                        cfsProps.put("cfsType", req.getProductType());
                        newCfs.setProperties(cfsProps);
                        newCfs.setUsingService(new HashSet<>(List.of(product)));
                        return serviceCustomRepository.save(newCfs);
                    });

            // Step 9: RFS
            String rfsName = "RFS" + Constants.UNDER_SCORE + subscriptionName;
            Service rfs = serviceCustomRepository.findByDiscoveredName(rfsName)
                    .orElseGet(() -> {
                        Service newRfs = new Service();
                        try {
                            newRfs.setLocalName(Validations.encryptName(rfsName));
                            newRfs.setDiscoveredName(rfsName);
                            newRfs.setContext("Setar");
                            newRfs.setKind("SetarRFS");
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                        Map<String, Object> rfsProps = new HashMap<>();
                        rfsProps.put("rfsStatus", "Active");
                        rfsProps.put("rfsType", req.getProductType());
                        newRfs.setProperties(rfsProps);
                        newRfs.setUsingService(new HashSet<>(List.of(cfs)));
                        return serviceCustomRepository.save(newRfs);
                    });

            // Step 10: ONT & OLT
            String ontName ="ONT" + req.getOntSN();
            if (ontName.length() > 100) {
                return new CreateServiceVoIPResponse(
                        "400",
                        ERROR_PREFIX + "ONT name too long",
                        Instant.now().toString(),
                        null,
                        null
                );
            }

            String oltName=req.getOltName();
            LogicalDevice olt = logicalDeviceRepo.findByDiscoveredName(oltName)
                    .orElseGet(() -> {
                        LogicalDevice dev = new LogicalDevice();
                        try {
                            dev.setLocalName(Validations.encryptName(req.getOltName()));
                            dev.setDiscoveredName(req.getOltName());
                            dev.setContext("Setar");
                            dev.setKind("OLTDevice");
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                        Map<String, Object> oltProps = new HashMap<>();
                        oltProps.put("deviceStatus", "Active");
                        oltProps.put("oltPosition", req.getOltName());
                        oltProps.put("ontTemplate", req.getTemplateNameOnt());
                        dev.setProperties(oltProps);
                        dev.setContainedservice(new HashSet<>(List.of(rfs)));
                        return logicalDeviceRepo.save(dev);
                    });


            LogicalDevice ont = logicalDeviceRepo.findByDiscoveredName(ontName)
                    .orElseGet(() -> {
                        LogicalDevice dev = new LogicalDevice();
                        try {
                            dev.setLocalName(Validations.encryptName(ontName));
                            dev.setDiscoveredName(ontName);
                            dev.setContext("Setar");
                            dev.setKind("ONTDevice");
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                        Map<String, Object> ontProps = new HashMap<>();
                        ontProps.put("deviceStatus", "Active");
                        ontProps.put("serialNo", req.getOntSN());
                        ontProps.put("deviceModel", req.getOntModel());
                        ontProps.put("oltPosition", req.getOltName());
                        ontProps.put("ontTemplate", req.getTemplateNameOnt());
                        dev.setProperties(ontProps);
                        dev.setUsedResource(new HashSet<>(List.of(olt)));
                        dev.addContainedservice(rfs);
                        return logicalDeviceRepo.save(dev);
                    });

            // Step 12: Configure VoIP ports
            Map<String, Object> ontProps = ont.getProperties();
            Map<String, Object> oltProps = olt.getProperties();

            if ("1".equals(req.getOntPort())) {
                ontProps.put("potsPort1Number", req.getVoipNumber1());
                oltProps.put("potsTemplate1", req.getTemplateNamePots1());
                ontProps.put("voipPort1",req.getVoipNumber1());
            } else {
                ontProps.put("potsPort2Number", req.getVoipNumber1());
                oltProps.put("potsTemplate2", req.getTemplateNamePots2());
                ontProps.put("voipPort2",req.getVoipNumber1());
            }
            oltProps.put("voipServiceTemplate", req.getVoipServiceTemplate());

            ont.setProperties(ontProps);
            olt.setProperties(oltProps);


            logicalDeviceRepo.save(ont);
            logicalDeviceRepo.save(olt);
            log.error(Constants.ACTION_COMPLETED);
            return new CreateServiceVoIPResponse(
                    "201",
                    "UIV action CreateServiceVoIP executed successfully.",
                    Instant.now().toString(),
                    subscriptionName,
                    ontName
            );

        } catch (Exception ex) {
            log.error("Exception in CreateServiceVoIP", ex);
            return new CreateServiceVoIPResponse(
                    "500",
                    ERROR_PREFIX + "Error occurred while creating service VOIP - " + ex.getMessage(),
                    Instant.now().toString(),
                    null,
                    null
            );
        }
    }
}
