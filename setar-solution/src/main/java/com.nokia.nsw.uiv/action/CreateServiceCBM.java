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
import com.nokia.nsw.uiv.request.CreateServiceCBMRequest;
import com.nokia.nsw.uiv.response.CreateServiceCBMResponse;
import com.nokia.nsw.uiv.response.CreateServiceFibernetResponse;
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

import java.util.*;

@Component
@Action
@Slf4j
public class CreateServiceCBM implements HttpAction {

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
        return CreateServiceCBMRequest.class;
    }

    @Override
    public Object doPost(ActionContext actionContext) throws Exception {
        CreateServiceCBMRequest request = (CreateServiceCBMRequest) actionContext.getObject();
        // 1. Validate mandatory params
        try{
            Validations.validateMandatoryParams(request.getSubscriberName(), "subscriberName");
            Validations.validateMandatoryParams(request.getProductType(), "productType");
        }catch (BadRequestException bre) {
            return new CreateServiceCBMResponse("400", Constants.ERROR_PREFIX + "Missing mandatory parameter : " + bre.getMessage(),
                    java.time.Instant.now().toString(), "","");
        }



        // --- 2. Subscriber Logic ---
        String subscriberName;
        if ("Broadband".equalsIgnoreCase(request.getProductSubtype())
                || "Cloudstarter".equalsIgnoreCase(request.getProductSubtype())
                || "Bridged".equalsIgnoreCase(request.getProductSubtype())) {

            // Update CPE if exists
            String cpeName = "CBM" + Constants.UNDER_SCORE + request.getCbmMac();
            Optional<LogicalDevice> cpeOpt = cpeDeviceRepository.findByDiscoveredName(cpeName);
            if (cpeOpt.isPresent()) {
                LogicalDevice cpe = cpeOpt.get();
                cpe.setAdministrativeState(AdministrativeState.valueOf("Available"));
                if ("Broadband".equalsIgnoreCase(request.getProductSubtype())) {
                    cpe.setDescription("Internet");
                }
                cpeDeviceRepository.save(cpe, 2);
            }

            subscriberName = request.getSubscriberName() + Constants.UNDER_SCORE + request.getCbmMac().replace(":", "");
        } else {
            subscriberName = request.getSubscriberName();
        }

        if (subscriberName.length() > 100) {
            return createErrorResponse("Subscriber name too long", 400);
        }

        Customer subscriber = subscriberRepository.findByDiscoveredName(subscriberName)
                .orElseGet(() -> {
                    Customer s = new Customer();
                    try {
                        s.setName(subscriberName);
                        s.setLocalName(Validations.encryptName(subscriberName));
                        s.setDiscoveredName(subscriberName);
                    } catch (AccessForbiddenException e) {
                        throw new RuntimeException(e);
                    } catch (BadRequestException e) {
                        throw new RuntimeException(e);
                    }
                    try {
                        s.setKind(Constants.SETAR_KIND_SETAR_SUBSCRIBER);
                    } catch (ModificationNotAllowedException e) {
                        throw new RuntimeException(e);
                    }
                    try {
                        s.setContext(Constants.SETAR);
                    } catch (BadRequestException e) {
                        throw new RuntimeException(e);
                    }
                    s.setType("Regular");

                    Map<String, Object> prop = new HashMap<>();
                    prop.put("accountNumber", request.getSubscriberName());
                    prop.put("status", "Active");
                    prop.put("subscriberUserName", request.getUserName());
                    s.setProperties(prop);

                    subscriberRepository.save(s, 2);
                    return s;
                });

// Optional subscriber info
        if (request.getFirstName() != null) subscriber.getProperties().put("firstName", request.getFirstName());
        if (request.getLastName() != null) subscriber.getProperties().put("lastName", request.getLastName());
        if (request.getCompanyName() != null) subscriber.getProperties().put("companyName", request.getCompanyName());
        if (request.getContactPhone() != null) subscriber.getProperties().put("contactPhone", request.getContactPhone());
        if (request.getSubsAddress() != null) subscriber.getProperties().put("subsAddress", request.getSubsAddress());

        subscriberRepository.save(subscriber, 2);


        // Optional subscriber info
        if (request.getFirstName() != null) subscriber.getProperties().get(request.getFirstName());
        if (request.getLastName() != null) subscriber.getProperties().get(request.getLastName());
        if (request.getCompanyName() != null) subscriber.getProperties().get(request.getCompanyName());
        if (request.getContactPhone() != null) subscriber.getProperties().get(request.getContactPhone());
        if (request.getSubsAddress() != null) subscriber.getProperties().get(request.getSubsAddress());
        subscriberRepository.save(subscriber, 2);

        // --- 3. Subscription Logic ---
        String subscriptionName = request.getSubscriberName() + Constants.UNDER_SCORE + request.getServiceId();
        if (subscriptionName.length() > 100) {
            return createErrorResponse("Subscription name too long", 400);
        }
        Subscription subscription = subscriptionRepository.findByDiscoveredName(subscriptionName)
                .orElseGet(() -> {
                    Subscription sub = new Subscription();
                    try {
                        sub.setLocalName(Validations.encryptName(subscriptionName));
                        sub.setDiscoveredName(subscriptionName);
                    } catch (AccessForbiddenException e) {
                        throw new RuntimeException(e);
                    } catch (BadRequestException e) {
                        throw new RuntimeException(e);
                    }
                    try {
                        sub.setKind(Constants.SETAR_KIND_SETAR_SUBSCRIPTION);
                    } catch (ModificationNotAllowedException e) {
                        throw new RuntimeException(e);
                    }
                    try {
                        sub.setContext(Constants.SETAR);
                    } catch (BadRequestException e) {
                        throw new RuntimeException(e);
                    }
                    Map<String, Object> prop = new HashMap<>();

                    prop.put("subscriptionStatus", "Active");
                    prop.put("serviceSubType", request.getProductSubtype());
                    prop.put("serviceLink", "Cable_Modem");
                    prop.put("serviceSN", request.getCbmSN());
                    prop.put("macAddress", request.getCbmMac());
                    prop.put("serviceID", request.getServiceId());
                    prop.put("veipQosSessionProfile", request.getQosProfile());
//                    prop.put("houseHoldId", request.getHhid());
                    prop.put("customerGroupId", request.getCustomerGroupId());
                    prop.put("subscriberID_CableModem", request.getSubscriberId());
                    prop.put("servicePackage", request.getServicePackage());
                    prop.put("kenanSubscriberId", request.getKenanUidNo());
                    prop.put("hhid",request.getHhid());
                    sub.setCustomer(subscriber);
                    sub.setProperties(prop);
                    subscriptionRepository.save(sub, 2);
                    return sub;
                });

        // --- 4. Product Logic ---
        String productName = request.getSubscriberName() +Constants.UNDER_SCORE+ request.getProductSubtype() +Constants.UNDER_SCORE+ request.getServiceId();
        if (productName.length() > 100) {
            return createErrorResponse("Product name too long", 400);
        }
        Product product = productRepository.findByDiscoveredName(productName)
                .orElseGet(() -> {
                    Product p = new Product();
                    try {
                        p.setName(productName);
                        p.setLocalName(productName);
                        p.setDiscoveredName(productName);
                    } catch (AccessForbiddenException e) {
                        throw new RuntimeException(e);
                    } catch (BadRequestException e) {
                        throw new RuntimeException(e);
                    }
                    try {
                        p.setKind(Constants.SETAR_KIND_SETAR_PRODUCT);
                    } catch (ModificationNotAllowedException e) {
                        throw new RuntimeException(e);
                    }
                    try {
                        p.setContext(Constants.SETAR);
                    } catch (BadRequestException e) {
                        throw new RuntimeException(e);
                    }
                    p.setType(request.getProductType());
                    Map<String, Object> prop = new HashMap<>();
                    prop.put("status", "Active");
                    p.setProperties(prop);
                    p.setCustomer(subscriber);
                    p.setSubscription(subscription);
                    productRepository.save(p, 2);
                    return p;
                });

        // --- 5. CFS Logic ---
        String cfsName = "CFS" +Constants.UNDER_SCORE + subscriptionName;
        CustomerFacingService cfs = cfsRepository.findByDiscoveredName(cfsName)
                .orElseGet(() -> {
                    CustomerFacingService c = new CustomerFacingService();
                    try {
                        c.setName(cfsName);
                        c.setLocalName(cfsName);
                        c.setDiscoveredName(cfsName);
                    } catch (AccessForbiddenException e) {
                        throw new RuntimeException(e);
                    } catch (BadRequestException e) {
                        throw new RuntimeException(e);
                    }
                    try {
                        c.setKind(Constants.SETAR_KIND_SETAR_CFS);
                    } catch (ModificationNotAllowedException e) {
                        throw new RuntimeException(e);
                    }
                    try {
                        c.setContext(Constants.SETAR);
                    } catch (BadRequestException e) {
                        throw new RuntimeException(e);
                    }
                    c.setType(request.getProductType());
                    Map<String, Object> prop = new HashMap<>();
                    prop.put("status", "Active");
                    c.setProperties(prop);
                    c.setStartDate(new Date());
                    c.setTransactionId(request.getFxOrderID());
                    c.setContainingProduct(product);
                    cfsRepository.save(c, 2);
                    return c;
                });
        // --- 7. CBM Device Logic ---
        String cbmName = "CBM" + request.getCbmSN();
        if (cbmName.length() > 100) {
            return createErrorResponse("CBM name too long", 400);
        }
        // --- 6. RFS Logic ---
        String rfsName = "RFS" +Constants.UNDER_SCORE + subscriptionName;
        ResourceFacingService rfs = rfsRepository.findByDiscoveredName(rfsName)
                .orElseGet(() -> {
                    ResourceFacingService r = new ResourceFacingService();
                    try {
                        r.setName(rfsName);
                        r.setLocalName(rfsName);
                        r.setDiscoveredName(rfsName);
                    } catch (AccessForbiddenException e) {
                        throw new RuntimeException(e);
                    } catch (BadRequestException e) {
                        throw new RuntimeException(e);
                    }
                    try {
                        r.setKind(Constants.SETAR_KIND_SETAR_RFS);
                    } catch (ModificationNotAllowedException e) {
                        throw new RuntimeException(e);
                    }
                    try {
                        r.setContext(Constants.SETAR);
                    } catch (BadRequestException e) {
                        throw new RuntimeException(e);
                    }
                    r.setType(request.getProductType());
                    Map<String, Object> prop = new HashMap<>();
                    prop.put("status", "Active");
                    r.setProperties(prop);
                    r.setContainingCfs(cfs);
                    rfsRepository.save(r, 2);
                    return r;
                });

        LogicalDevice cbmDevice = cbmDeviceRepository.findByDiscoveredName(cbmName)
                .orElseGet(() -> {
                    LogicalDevice d = new LogicalDevice();
                    try {
                        d.setName(cbmName);
                        d.setLocalName(cbmName);
                        d.setDiscoveredName(cbmName);
                    } catch (AccessForbiddenException e) {
                        throw new RuntimeException(e);
                    } catch (BadRequestException e) {
                        throw new RuntimeException(e);
                    }
                    try {
                        d.setKind(Constants.SETAR_KIND_STB_AP_CM_DEVICE);
                    } catch (ModificationNotAllowedException e) {
                        throw new RuntimeException(e);
                    }
                    try {
                        d.setContext(Constants.SETAR);
                    } catch (BadRequestException e) {
                        throw new RuntimeException(e);
                    }
                    Map<String, Object> deviceProps = new HashMap<>();
                    deviceProps.put("serialNo", request.getCbmSN());
                    deviceProps.put("macAddress", request.getCbmMac());
                    deviceProps.put("gatewayMacAddress", request.getCbmGatewayMac());
                    deviceProps.put("deviceType", request.getCbmType());
                    deviceProps.put("manufacturer", request.getCbmManufacturer());
                    deviceProps.put("deviceModel", request.getCbmModel());
                    deviceProps.put("OperationalState", "Active");
                    d.setProperties(deviceProps);
                    d.addUsingService(rfs);
                    cbmDeviceRepository.save(d, 2);
                    return d;
                });



        // --- 8. Final Response ---
        CreateServiceCBMResponse response = new CreateServiceCBMResponse();
        response.setStatus("201");
        response.setMessage("UIV action CreateServiceCBM executed successfully");
        response.setTimestamp(new Date().toString());
        response.setSubscriptionName(subscriptionName);
        response.setCbmName(cbmName);

        return response;
    }

    private CreateServiceCBMResponse createErrorResponse(String message, int status) {
        CreateServiceCBMResponse response = new CreateServiceCBMResponse();
        response.setStatus(String.valueOf(status));
        response.setMessage("UIV action CreateServiceCBM execution failed - " + message);
        response.setTimestamp(new Date().toString());
        return response;
    }
}
