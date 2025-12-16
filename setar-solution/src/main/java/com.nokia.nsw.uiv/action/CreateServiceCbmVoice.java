package com.nokia.nsw.uiv.action;
import com.nokia.nsw.uiv.exception.AccessForbiddenException;
import com.nokia.nsw.uiv.exception.BadRequestException;
import com.nokia.nsw.uiv.exception.ModificationNotAllowedException;
import com.nokia.nsw.uiv.framework.action.Action;
import com.nokia.nsw.uiv.framework.action.ActionContext;
import com.nokia.nsw.uiv.framework.action.HttpAction;
import com.nokia.nsw.uiv.model.common.party.Customer;
import com.nokia.nsw.uiv.model.common.party.CustomerRepository;
import com.nokia.nsw.uiv.model.resource.AdministrativeState;
import com.nokia.nsw.uiv.model.resource.logical.LogicalDevice;
import com.nokia.nsw.uiv.model.resource.logical.LogicalDeviceRepository;
import com.nokia.nsw.uiv.model.service.Subscription;
import com.nokia.nsw.uiv.model.service.SubscriptionRepository;
import com.nokia.nsw.uiv.repository.*;
import com.nokia.nsw.uiv.request.CreateServiceCbmVoiceRequest;
import com.nokia.nsw.uiv.response.CreateServiceCBMResponse;
import com.nokia.nsw.uiv.response.CreateServiceCbmVoiceResponse;
import com.nokia.nsw.uiv.utils.Constants;
import com.nokia.nsw.uiv.utils.Validations;
import com.setar.uiv.model.product.CustomerFacingService;
import com.setar.uiv.model.product.CustomerFacingServiceRepository;
import com.setar.uiv.model.product.Product;
import com.setar.uiv.model.product.ProductRepository;
import com.setar.uiv.model.product.ResourceFacingService;
import com.setar.uiv.model.product.ResourceFacingServiceRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@Action
@Slf4j
public class CreateServiceCbmVoice implements HttpAction {
    protected static final String ACTION_LABEL = Constants.CREATE_SERVICE_CBM_VOICE;
    // Error code mappings (adjust if you use different codes)
    private static final String CODE_SUCCESS = "201";
    private static final String CODE_MISSING_PARAMS = "400"; // $code5
    private static final String CODE_ALREADY_EXISTS = "409"; // $code2
    private static final String CODE_PERSISTENCE_ERROR = "500"; // $code3
    private static final String CODE_CPE_NOT_FOUND = "404"; // $code4
    private static final String CODE_NAME_TOO_LONG = "400"; // $code6
    private static final String CODE_EXCEPTION = "500"; // $code1

    @Autowired
    private CustomerCustomRepository subscriberRepository;

    @Autowired
    private SubscriptionCustomRepository subscriptionRepository;

    @Autowired
    private ProductCustomRepository productRepository;

    @Autowired
    private CustomerFacingServiceCustomRepository cfsRepository;

    @Autowired
    private ResourceFacingServiceCustomRepository rfsRepository;

    @Autowired
    private LogicalDeviceCustomRepository cbmDeviceRepository;

    @Autowired
    private LogicalDeviceCustomRepository cpeDeviceRepository;


    @Override
    public Class<?> getActionClass() {
        return CreateServiceCbmVoiceRequest.class;
    }

    @Override
    public Object doPost(ActionContext actionContext) throws Exception {
        log.error(Constants.EXECUTING_ACTION, ACTION_LABEL);
        CreateServiceCbmVoiceRequest request = (CreateServiceCbmVoiceRequest) actionContext.getObject();

        // 1. Mandatory validations
        try {
            log.error(Constants.MANDATORY_PARAMS_VALIDATION_STARTED);
            Validations.validateMandatoryParams(request.getSubscriberName(), "subscriberName");
            Validations.validateMandatoryParams(request.getProductType(), "productType");
            Validations.validateMandatoryParams(request.getCbmSN(), "cbmSN");
            Validations.validateMandatoryParams(request.getCbmMac(), "cbmMac");
            Validations.validateMandatoryParams(request.getCbmManufacturer(), "cbmManufacturer");
            Validations.validateMandatoryParams(request.getCbmType(), "cbmType");
            Validations.validateMandatoryParams(request.getCbmModel(), "cbmModel");
            Validations.validateMandatoryParams(request.getHhid(), "hhid");
            Validations.validateMandatoryParams(request.getServiceId(), "serviceId");
            Validations.validateMandatoryParams(request.getQosProfile(), "qosProfile");
            Validations.validateMandatoryParams(request.getVoipNumber1(), "voipNumber1");
            Validations.validateMandatoryParams(request.getSimaCustId(), "simaCustId");
            Validations.validateMandatoryParams(request.getSimaSubsId(), "simaSubsId");
            Validations.validateMandatoryParams(request.getSimaEndpointId(), "simaEndpointId");
            log.error(Constants.MANDATORY_PARAMS_VALIDATION_COMPLETED);
        } catch (BadRequestException bre) {
            return createErrorResponse(CODE_MISSING_PARAMS,
                    "Missing mandatory parameter(s): " + bre.getMessage());
        }
        AtomicBoolean isSubscriberExist = new AtomicBoolean(true);
        AtomicBoolean isSubscriptionExist = new AtomicBoolean(true);
        AtomicBoolean isProductExist = new AtomicBoolean(true);

        // 2. Construct names
        String subscriberNameString;
        String productSubtype = request.getProductSubtype();
        if ("Broadband".equalsIgnoreCase(productSubtype) || "Voice".equalsIgnoreCase(productSubtype)) {
            // remove colons if present
            String macClean = request.getCbmMac() == null ? "" : request.getCbmMac().replace(":", "");
            subscriberNameString = request.getSubscriberName() + Constants.UNDER_SCORE  + macClean;
        } else {
            subscriberNameString = request.getSubscriberName();
        }

        String subscriptionName = request.getSubscriberName() + Constants.UNDER_SCORE + request.getServiceId();
        String cfsName = "CFS" + Constants.UNDER_SCORE + subscriptionName;
        String rfsName = "RFS" + Constants.UNDER_SCORE + subscriptionName;
        String cbmName = "CBM" +request.getServiceId();

        // name length checks
        if (subscriberNameString.length() > 100 ||
                subscriptionName.length() > 100 ||
                cfsName.length() > 100 ||
                rfsName.length() > 100 ||
                cbmName.length() > 100) {
            return createErrorResponse(CODE_NAME_TOO_LONG, "Name value too long");
        }

        try {
            // 3. Subscriber logic
            Customer subscriber = subscriberRepository.findByDiscoveredName(subscriberNameString)
                    .orElseGet(() -> {
                        Customer s = new Customer();
                        isSubscriberExist.set(false);
                        try {
                            s.setLocalName(Validations.encryptName(subscriberNameString));
                            s.setDiscoveredName(subscriberNameString);
                            s.setContext(Constants.SETAR);
                            s.setKind(Constants.SETAR_KIND_SETAR_SUBSCRIBER);
                            s.setDiscoveredName(subscriberNameString);
                        } catch (AccessForbiddenException | BadRequestException | ModificationNotAllowedException e) {
                            throw new RuntimeException(e);
                        }
                        Map<String, Object> subProps = new HashMap<>();
                        subProps.put("subscriberStatus", "Active");
                        subProps.put("subscriberType", "Regular");
                        subProps.put("accountNumber", request.getSubscriberName());
                        subProps.put("householdId", request.getHhid());
                        if (request.getUserName() != null && !request.getUserName().trim().isEmpty()) {
                            subProps.put("userName", request.getUserName());
                        }
                        s.setProperties(subProps);
                        // save with depth 2 as in your codebase
                        subscriberRepository.save(s, 2);
                        return s;
                    });

            // update optional subscriber fields properly
            try {
                Map<String, Object> sp = subscriber.getProperties() == null ? new HashMap<>() : subscriber.getProperties();
                if (request.getFirstName() != null && !request.getFirstName().trim().isEmpty()) sp.put("firstName", request.getFirstName());
                if (request.getLastName() != null && !request.getLastName().trim().isEmpty()) sp.put("lastName", request.getLastName());
                if (request.getCompanyName() != null && !request.getCompanyName().trim().isEmpty()) sp.put("companyName", request.getCompanyName());
                if (request.getContactPhone() != null && !request.getContactPhone().trim().isEmpty()) sp.put("contactPhone", request.getContactPhone());
                if (request.getSubsAddress() != null && !request.getSubsAddress().trim().isEmpty()) sp.put("subsAddress", request.getSubsAddress());
                subscriber.setProperties(sp);
                subscriberRepository.save(subscriber, 2);
            } catch (Exception e) {
                log.error("Persistence error updating subscriber properties", e);
                return createErrorResponse(CODE_PERSISTENCE_ERROR, "Persistence error while updating subscriber: " + e.getMessage());
            }

            // 4. Subscription logic
            Subscription subscription = subscriptionRepository.findByDiscoveredName(subscriptionName)
                    .orElseGet(() -> {
                        isSubscriptionExist.set(false);
                        Subscription sub = new Subscription();
                        try {
                            sub.setLocalName(Validations.encryptName(subscriptionName));
                            sub.setDiscoveredName(subscriptionName);
                            sub.setContext(Constants.SETAR);
                            sub.setKind(Constants.SETAR_KIND_SETAR_SUBSCRIPTION);
                        } catch (AccessForbiddenException | BadRequestException | ModificationNotAllowedException e) {
                            throw new RuntimeException(e);
                        }

                        Map<String, Object> props = new HashMap<>();
                        props.put("subscriptionStatus", "Active");
                        props.put("serviceSubType", request.getProductSubtype());
                        props.put("serviceLink", "Cable_Modem");
                        props.put("serviceSN", request.getCbmSN());
                        props.put("macAddress", request.getCbmMac());
                        props.put("serviceID", request.getServiceId());
                        if (request.getQosProfile() != null) props.put("qosProfile", request.getQosProfile());
                        props.put("householdId", request.getHhid());
                        if (request.getCustomerGroupId() != null && !"NA".equalsIgnoreCase(request.getCustomerGroupId()))
                            props.put("customerGroupId", request.getCustomerGroupId());
                        if (request.getSubscriberId() != null) props.put("subscriberId", request.getSubscriberId());
                        if (request.getServicePackage() != null) props.put("servicePackage", request.getServicePackage());
                        if (request.getKenanUidNo() != null) props.put("billingId", request.getKenanUidNo());
                        props.put("simaCustId", request.getSimaCustId());
                        props.put("voipNumber1", request.getVoipNumber1());
                        props.put("simaSubsId", request.getSimaSubsId());
                        props.put("simaEndpointId", request.getSimaEndpointId());

                        // if both servicePackage and voipServiceCode provided, store them
                        if (request.getServicePackage() != null && request.getServicePackage().trim().length() > 0
                                && request.getCpeMacAddressMTA() != null) {
                            // nothing special, already stored servicePackage above; VOIP service code handled below if present in request
                        }
                        sub.setProperties(props);
                        sub.setCustomer(subscriber);
                        subscriptionRepository.save(sub, 2);
                        return sub;
                    });

            // If voip package/code present, store them in subscription
            try {
                Map<String, Object> subsProps = subscription.getProperties() == null ? new HashMap<>() : subscription.getProperties();
                if (request.getServicePackage() != null && request.getServicePackage().trim().length() > 0)
                    subsProps.put("voipPackagePrimary", request.getServicePackage());
                // voipServiceCode might be named differently in your request; if present add accordingly
                // if request object has getVoipServiceCode() then store, else ignore
                // subsProps.put("voipServiceCodePrimary", request.getVoipServiceCode());
                subscription.setProperties(subsProps);
                subscriptionRepository.save(subscription, 2);
            } catch (Exception e) {
                log.error("Persistence error updating subscription", e);
                return createErrorResponse(CODE_PERSISTENCE_ERROR, "Persistence error while updating subscription: " + e.getMessage());
            }

            // 5. Product logic
            String productNameStr = request.getSubscriberName() +Constants.UNDER_SCORE+ request.getProductSubtype() +Constants.UNDER_SCORE+ request.getServiceId();
            if (productNameStr.length() > 100) {
                return createErrorResponse(CODE_NAME_TOO_LONG, "Identifier exceeds allowed character length");
            }
            Product product = productRepository.findByDiscoveredName(productNameStr)
                    .orElseGet(() -> {
                        isProductExist.set(false);
                        Product p = new Product();
                        try {
                            p.setLocalName(Validations.encryptName(productNameStr));
                            p.setDiscoveredName(productNameStr);
                            p.setContext(Constants.SETAR);
                            p.setKind(Constants.SETAR_KIND_SETAR_PRODUCT);
                        } catch (AccessForbiddenException | BadRequestException | ModificationNotAllowedException e) {
                            throw new RuntimeException(e);
                        }
                        Map<String, Object> prodProps = new HashMap<>();
                        prodProps.put("productStatus", "Active");
                        prodProps.put("productType",request.getProductType());
                        p.setProperties(prodProps);
                        p.setCustomer(subscriber);
                        p.setSubscription(subscription);
                        productRepository.save(p, 2);
                        return p;
                    });
            if(isSubscriberExist.get() && isSubscriptionExist.get() && isProductExist.get()){
                log.error("createServiceCbmVoice service already exist");
                return new CreateServiceCbmVoiceResponse("409","Service already exist/Duplicate entry",Instant.now().toString(),subscriptionName,cbmName);
            }

            // 6. CFS logic
            CustomerFacingService cfs = cfsRepository.findByDiscoveredName(cfsName)
                    .orElseGet(() -> {
                        CustomerFacingService c = new CustomerFacingService();
                        try {
                            c.setLocalName(cfsName);
                            c.setDiscoveredName(cfsName);
                            c.setContext(Constants.SETAR);
                            c.setKind(Constants.SETAR_KIND_SETAR_CFS);
                            c.setCustomer(subscriber);
                        } catch (AccessForbiddenException | BadRequestException | ModificationNotAllowedException e) {
                            throw new RuntimeException(e);
                        }
                        c.setContainingProduct(product);
                        Map<String, Object> cfsProps = new HashMap<>();
                        cfsProps.put("serviceStatus", "Active");
                        cfsProps.put("serviceType", request.getProductType());
                        cfsProps.put("cfsType",request.getProductSubtype());
                        cfsProps.put("serviceStartDate", Instant.now().toString());
                        if (request.getFxOrderID() != null) cfsProps.put("transactionId", request.getFxOrderID());
                        c.setProperties(cfsProps);
                        cfsRepository.save(c, 2);
                        return c;
                    });

            // 7. RFS logic
            ResourceFacingService rfs = rfsRepository.findByDiscoveredName(rfsName)
                    .orElseGet(() -> {
                        ResourceFacingService r = new ResourceFacingService();
                        try {
                            r.setLocalName(Validations.encryptName(rfsName));
                            r.setDiscoveredName(rfsName);
                            r.setContext(Constants.SETAR);
                            r.setKind(Constants.SETAR_KIND_SETAR_RFS);
                        } catch (AccessForbiddenException | BadRequestException | ModificationNotAllowedException e) {
                            throw new RuntimeException(e);
                        }
                        r.setContainingCfs(cfs);
                        Map<String, Object> rfsProps = new HashMap<>();
                        rfsProps.put("serviceStatus", "Active");
                        rfsProps.put("serviceType", request.getProductType());
                        r.setProperties(rfsProps);
                        r.setContainingCfs(cfs);
                        rfsRepository.save(r, 2);
                        return r;
                    });

            // 8. CBM device logic
            LogicalDevice cbmDevice = cbmDeviceRepository.findByDiscoveredName(cbmName)
                    .orElseGet(() -> {
                        LogicalDevice d = new LogicalDevice();
                        try {
                            d.setLocalName(Validations.encryptName(cbmName));
                            d.setDiscoveredName(cbmName);
                            d.setContext(Constants.SETAR);
                            d.setKind(Constants.SETAR_KIND_STB_AP_CM_DEVICE);
                        } catch (AccessForbiddenException | BadRequestException | ModificationNotAllowedException e) {
                            throw new RuntimeException(e);
                        }
                        Map<String, Object> deviceProps = new HashMap<>();
                        deviceProps.put("serialNo", request.getCbmSN());
                        deviceProps.put("macAddress", request.getCbmMac());
                        if (request.getCbmGatewayMac() != null) deviceProps.put("gatewayMacAddress", request.getCbmGatewayMac());
                        if (request.getCbmType() != null) deviceProps.put("deviceType", request.getCbmType());
                        if (request.getCbmManufacturer() != null) deviceProps.put("manufacturer", request.getCbmManufacturer());
                        if (request.getCbmModel() != null) deviceProps.put("deviceModel", request.getCbmModel());
                        deviceProps.put("operationalState", "Active");
                        d.setProperties(deviceProps);
                        d.addUsingService(rfs);
                        cbmDeviceRepository.save(d, 2);
                        return d;
                    });

            // 9. CPE Voice Port Update (only when productSubtype == "Voice")
            if ("Voice".equalsIgnoreCase(request.getProductSubtype())) {
                String cpeDeviceName = "CBM" + Constants.UNDER_SCORE +request.getCbmMac();
                Optional<LogicalDevice> cpeOpt = cpeDeviceRepository.findByDiscoveredName(cpeDeviceName);
                if (!cpeOpt.isPresent()) {
                    return createErrorResponse(CODE_CPE_NOT_FOUND, "CPE device not found");
                }

                try {
                    LogicalDevice cpe = cpeOpt.get();
                    Map<String, Object> props = cpe.getProperties() == null ? new HashMap<>() : cpe.getProperties();

                    if (request.getCpeMacAddressMTA() != null && !request.getCpeMacAddressMTA().trim().isEmpty()) {
                        props.put("mtaMacAddress", request.getCpeMacAddressMTA());
                    }

                    if (Integer.valueOf(1).equals(request.getVoipPort())) {
                        props.put("voipPort1", request.getVoipNumber1());
                    } else if (Integer.valueOf(2).equals(request.getVoipPort())) {
                        props.put("voipPort2", request.getVoipNumber1());
                    }


                    cpe.setProperties(props);
                    cpeDeviceRepository.save(cpe, 2);
                } catch (Exception e) {
                    log.error("Persistence error updating CPE device", e);
                    return createErrorResponse(CODE_PERSISTENCE_ERROR, "Persistence error while updating CPE device: " + e.getMessage());
                }
            }
            log.error(Constants.ACTION_COMPLETED);
            // 10. Final success response
            CreateServiceCbmVoiceResponse response = new CreateServiceCbmVoiceResponse();
            response.setStatus(CODE_SUCCESS);
            response.setMessage("UIV action CreateServiceCbmVoice executed successfully");
            response.setTimestamp(new Date().toString());
            response.setSubscriptionName(subscriptionName);
            response.setCbmName(cbmName);
            return response;

        } catch (RuntimeException rte) {
            // handle thrown runtime exceptions (wrapped checked exceptions)
            log.error("Runtime exception", rte);
            // If cause indicates duplicate key, return already exists
            Throwable cause = rte.getCause();
            if (cause != null && cause.getMessage() != null && cause.getMessage().toLowerCase().contains("duplicate")) {
                return createErrorResponse(CODE_ALREADY_EXISTS, "Entity already exists: " + cause.getMessage());
            }
            return createErrorResponse(CODE_EXCEPTION, "Exception - " + rte.getMessage());
        } catch (Exception ex) {
            log.error("Unexpected exception", ex);
            return createErrorResponse(CODE_EXCEPTION, "Exception - " + ex.getMessage());
        }
    }

    private CreateServiceCbmVoiceResponse createErrorResponse(String code, String message) {
        CreateServiceCbmVoiceResponse resp = new CreateServiceCbmVoiceResponse();
        resp.setStatus(code);
        resp.setMessage("UIV action CreateServiceCbmVoice execution failed - " + message);
        resp.setTimestamp(new Date().toString());
        return resp;
    }
}
