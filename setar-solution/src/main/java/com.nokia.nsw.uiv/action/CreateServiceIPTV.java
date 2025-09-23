package com.nokia.nsw.uiv.action;

import com.nokia.nsw.uiv.exception.AccessForbiddenException;
import com.nokia.nsw.uiv.exception.BadRequestException;
import com.nokia.nsw.uiv.exception.ModificationNotAllowedException;
import com.nokia.nsw.uiv.framework.action.Action;
import com.nokia.nsw.uiv.framework.action.ActionContext;
import com.nokia.nsw.uiv.framework.action.HttpAction;
import com.nokia.nsw.uiv.model.common.party.Customer;
import com.nokia.nsw.uiv.model.common.party.CustomerRepository;
import com.nokia.nsw.uiv.model.service.Subscription;
import com.nokia.nsw.uiv.model.service.SubscriptionRepository;
import com.setar.uiv.model.product.CustomerFacingService;
import com.setar.uiv.model.product.CustomerFacingServiceRepository;
import com.setar.uiv.model.product.Product;
import com.setar.uiv.model.product.ProductRepository;
import com.setar.uiv.model.product.ResourceFacingService;
import com.setar.uiv.model.product.ResourceFacingServiceRepository;
import com.nokia.nsw.uiv.model.resource.logical.LogicalDevice;
import com.nokia.nsw.uiv.model.resource.logical.LogicalDeviceRepository;
import com.nokia.nsw.uiv.model.resource.logical.LogicalInterface;
import com.nokia.nsw.uiv.model.resource.logical.LogicalInterfaceRepository;
import com.nokia.nsw.uiv.request.CreateServiceIPTVRequest;
import com.nokia.nsw.uiv.response.CreateServiceIPTVResponse;
import com.nokia.nsw.uiv.utils.Constants;
import com.nokia.nsw.uiv.utils.Validations;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
@RestController
@Action
@Slf4j
public class CreateServiceIPTV implements HttpAction {

    protected static final String ACTION_LABEL = Constants.CREATE_SERVICE_IPTV;
    private static final String ERROR_PREFIX = "UIV action CreateServiceIPTV execution failed - ";

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
    private LogicalInterfaceRepository vlanRepository;

    @Override
    public Class getActionClass() {
        return CreateServiceIPTVRequest.class;
    }

    @Override
    public Object doPost(ActionContext actionContext) throws Exception {
        log.warn(Constants.EXECUTING_ACTION, ACTION_LABEL);

        CreateServiceIPTVRequest request = (CreateServiceIPTVRequest) actionContext.getObject();

        try {
            // Validate mandatory parameters
            log.info(Constants.MANDATORY_PARAMS_VALIDATION_STARTED);
            Validations.validateMandatoryParams(request.getSubscriberName(), "subscriberName");
            Validations.validateMandatoryParams(request.getProductType(), "productType");
            Validations.validateMandatoryParams(request.getProductSubtype(), "productSubtype");
            Validations.validateMandatoryParams(request.getOntSN(), "ontSN");
            Validations.validateMandatoryParams(request.getOltName(), "oltName");
            Validations.validateMandatoryParams(request.getQosProfile(), "qosProfile");
            Validations.validateMandatoryParams(request.getVlanID(), "vlanID");
            Validations.validateMandatoryParams(request.getHhid(), "hhid");
            Validations.validateMandatoryParams(request.getServiceID(), "serviceID");
            Validations.validateMandatoryParams(request.getCustomerGroupID(), "customerGroupID");
            log.info(Constants.MANDATORY_PARAMS_VALIDATION_COMPLETED);

            // Construct entity names
            String subscriberName = request.getSubscriberName();
            String subscriptionName = subscriberName + "_" + request.getServiceID();
            String productName = subscriberName + Constants.UNDER_SCORE + request.getProductSubtype() + Constants.UNDER_SCORE+ request.getServiceID();
            String cfsName = "CFS_" + subscriptionName;
            String rfsName = "RFS_" + subscriptionName;
            String ontName = "ONT_" + request.getOntSN();
            String mgmtVlanName = request.getMenm() + "_" + request.getVlanID();


            // ------------------- Subscriber -------------------
            String subscriberGdn = Validations.getGlobalName(subscriberName);
            Optional<Customer> optSubscriber = customerRepository.uivFindByGdn(subscriberGdn);
            Customer subscriber;
            if (optSubscriber.isPresent()) {
                subscriber = optSubscriber.get();
                log.info("Subscriber already exists: {}", subscriberName);
            } else {
                subscriber = new Customer();
                subscriber.setLocalName(subscriberName);
                subscriber.setKind("SetarSubscriber");
                subscriber.setContext(Constants.SETAR);

                Map<String, Object> subscriberProps = new HashMap<>();
                subscriberProps.put("accountNumber", subscriberName);
                subscriberProps.put("houseHoldId", request.getHhid());
                subscriberProps.put("subscriberFirstName", request.getFirstName());
                subscriberProps.put("subscriberLastName", request.getLastName());
                subscriberProps.put("companyName", request.getCompanyName());
                subscriberProps.put("contactPhoneNumber", request.getContactPhone());

                subscriber.setProperties(subscriberProps);
                customerRepository.save(subscriber, 2);
                log.info("Created Subscriber: {}", subscriberName);
            }

            // ------------------- Subscription -------------------
            String subscriptionGdn = Validations.getGlobalName(subscriptionName);
            Optional<Subscription> optSubscription = subscriptionRepository.uivFindByGdn(subscriptionGdn);
            Subscription subscription;
            if (optSubscription.isPresent()) {
                subscription = optSubscription.get();
                log.info("Subscription already exists: {}", subscriptionName);
            } else {
                subscription = new Subscription();
                subscription.setLocalName(subscriptionName);
                subscription.setKind("SetarSubscription");
                subscription.setContext(Constants.SETAR);

                Map<String, Object> subscriptionProps = new HashMap<>();
                subscriptionProps.put("serviceID", request.getServiceID());
                subscriptionProps.put("serviceSubType", request.getProductSubtype());
                subscriptionProps.put("serviceSN", request.getOntSN());
                subscriptionProps.put("serviceMAC", request.getOntMacAddr());
                subscriptionProps.put("iptvQosSessionProfile", request.getQosProfile());
                subscriptionProps.put("customerGroupID", request.getCustomerGroupID());
                subscriptionProps.put("householdID", request.getHhid());
                subscriptionProps.put("servicePackage", request.getServicePackage());
                subscriptionProps.put("kenanSubscriberID", request.getKenanUidNo());
                subscriptionProps.put("gatewayMacAddress", request.getGatewayMac());

                subscription.setProperties(subscriptionProps);
                subscription.setCustomer(subscriber); // association
                subscriptionRepository.save(subscription, 2);
                log.info("Created Subscription: {}", subscriptionName);
            }

            // ------------------- Product -------------------
            String productGdn = Validations.getGlobalName(productName);
            Optional<Product> optProduct = productRepository.uivFindByGdn(productGdn);
            Product product;
            if (optProduct.isPresent()) {
                product = optProduct.get();
                log.info("Product already exists: {}", productName);
            } else {
                product = new Product();
                product.setLocalName(productName);
                product.setKind("SetarProduct");
                product.setContext(Constants.SETAR);

                Map<String, Object> productProps = new HashMap<>();
                productProps.put("productType", request.getProductType());
                productProps.put("productStatus", "ACTIVE");

                product.setProperties(productProps);
                product.setCustomer(subscriber);
                product.setSubscription(subscription);
                productRepository.save(product, 2);
                log.info("Created Product: {}", productName);
            }

            // ------------------- Customer Facing Service (CFS) -------------------
            String cfsGdn = Validations.getGlobalName(cfsName);
            Optional<CustomerFacingService> optCFS = cfsRepository.uivFindByGdn(cfsGdn);
            CustomerFacingService cfs;
            if (optCFS.isPresent()) {
                cfs = optCFS.get();
                log.info("CFS already exists: {}", cfsName);
            } else {
                cfs = new CustomerFacingService();
                cfs.setLocalName(cfsName);
                cfs.setKind("SetarCFS");
                cfs.setContext(Constants.SETAR);

                Map<String, Object> cfsProps = new HashMap<>();
                cfsProps.put("serviceStartDate", java.time.Instant.now().toString());
                cfsProps.put("transactionID", request.getFxOrderID());
                cfsProps.put("serviceStatus", "ACTIVE");
                cfsProps.put("serviceType", request.getProductType());

                cfs.setProperties(cfsProps);
                cfs.setContainingProduct(product);
                cfsRepository.save(cfs, 2);
                log.info("Created CFS: {}", cfsName);
            }

            // ------------------- Resource Facing Service (RFS) -------------------
            String rfsGdn = Validations.getGlobalName(rfsName);
            Optional<ResourceFacingService> optRFS = rfsRepository.uivFindByGdn(rfsGdn);
            ResourceFacingService rfs;
            if (optRFS.isPresent()) {
                rfs = optRFS.get();
                log.info("RFS already exists: {}", rfsName);
            } else {
                rfs = new ResourceFacingService();
                rfs.setLocalName(rfsName);
                rfs.setKind("SetarRFS");
                rfs.setContext(Constants.SETAR);

                Map<String, Object> rfsProps = new HashMap<>();
                rfsProps.put("serviceStatus", "ACTIVE");
                rfsProps.put("serviceType", request.getProductType());

                rfs.setProperties(rfsProps);
                rfs.addContained(cfs);
                rfsRepository.save(rfs, 2);
                log.info("Created RFS: {}", rfsName);
            }

            String oltName=request.getOltName()==null?"":request.getOltName();

            // ------------------- Logical Devices -------------------
            // OLT Device
            String oltGdn = Validations.getGlobalName(oltName);
            Optional<LogicalDevice> optOlt = logicalDeviceRepository.uivFindByGdn(oltGdn);
            LogicalDevice oltDevice;
            if (optOlt.isPresent()) {
                oltDevice = optOlt.get();
                log.info("OLT already exists: {}", request.getOltName());
            } else {
                oltDevice = new LogicalDevice();
                oltDevice.setLocalName(request.getOltName());
                oltDevice.setKind("OLTDevice");
                oltDevice.setContext("");

                Map<String, Object> oltProps = new HashMap<>();
                oltProps.put("oltPosition", request.getOltName());
                oltProps.put("operationalState", "ACTIVE");
                oltProps.put("ontTemplate", request.getTemplateNameONT());
                oltProps.put("veipServiceTemplate", request.getTemplateNameVEIP());
                oltProps.put("veipIptvTemplate", request.getTemplateNameIPTV());
                oltProps.put("igmpTemplate", request.getTemplateNameIGMP());

                oltDevice.setProperties(oltProps);
                oltDevice.addUsingService(rfs);
                logicalDeviceRepository.save(oltDevice, 2);
                log.info("Created OLT Device: {}", request.getOltName());
            }

            // ONT Device
            String ontGdn = Validations.getGlobalName(ontName);
            Optional<LogicalDevice> optOnt = logicalDeviceRepository.uivFindByGdn(ontGdn);
            LogicalDevice ontDevice;
            if (optOnt.isPresent()) {
                ontDevice = optOnt.get();
                log.info("ONT already exists: {}", ontName);
            } else {
                ontDevice = new LogicalDevice();
                ontDevice.setLocalName(ontName);
                ontDevice.setKind("ONTDevice");
                ontDevice.setContext(Constants.SETAR);

                Map<String, Object> ontProps = new HashMap<>();
                ontProps.put("serialNo", request.getOntSN());
                ontProps.put("deviceModel", request.getOntModel());
                ontProps.put("operationalState", "ACTIVE");
                ontProps.put("iptvVlan", request.getVlanID());
                ontDevice.setProperties(ontProps);
                ontDevice.addUsingService(rfs);
                ontDevice.addManagingDevices(oltDevice);
                logicalDeviceRepository.save(ontDevice, 2);
                log.info("Created ONT Device: {}", ontName);
            }

            // VLAN Interface
            String vlanGdn = Validations.getGlobalName(mgmtVlanName);
            Optional<LogicalInterface> optVlan = vlanRepository.uivFindByGdn(vlanGdn);
            LogicalInterface vlanInterface;
            if (optVlan.isPresent()) {
                vlanInterface = optVlan.get();
                log.info("VLAN Interface already exists: {}", mgmtVlanName);
            } else {
                vlanInterface = new LogicalInterface();
                vlanInterface.setLocalName(mgmtVlanName);
                vlanInterface.setKind("VLANInterface");
                vlanInterface.setContext(Constants.SETAR);

                Map<String, Object> vlanProps = new HashMap<>();
                vlanProps.put("vlanId", request.getVlanID());
                vlanProps.put("operationalState", "ACTIVE");
                vlanInterface.setProperties(vlanProps);

                vlanRepository.save(vlanInterface, 2);
                log.info("Created VLAN Interface: {}", mgmtVlanName);
            }

            log.info(Constants.ACTION_COMPLETED);

            return new CreateServiceIPTVResponse(
                    "201",
                    "IPTV service created",
                    java.time.Instant.now().toString(),
                    subscriptionName,
                    ontName
            );

        } catch (BadRequestException bre) {
            log.error("Validation error: {}", bre.getMessage(), bre);
            return new CreateServiceIPTVResponse(
                    "400",
                    ERROR_PREFIX + "Missing mandatory parameter : " + bre.getMessage(),
                    String.valueOf(System.currentTimeMillis()),
                    "",
                    ""
            );
        } catch (AccessForbiddenException | ModificationNotAllowedException ex) {
            log.error("Access or modification error: {}", ex.getMessage(), ex);
            return new CreateServiceIPTVResponse(
                    "403",
                    ERROR_PREFIX + ex.getMessage(),
                    String.valueOf(System.currentTimeMillis()),
                    "",
                    ""
            );
        } catch (Exception ex) {
            log.error("Unhandled exception during CreateServiceIPTV", ex);
            return new CreateServiceIPTVResponse(
                    "500",
                    ERROR_PREFIX + "Internal server error occurred - " + ex.getMessage(),
                    String.valueOf(System.currentTimeMillis()),
                    "",
                    ""
            );
        }
    }
}
