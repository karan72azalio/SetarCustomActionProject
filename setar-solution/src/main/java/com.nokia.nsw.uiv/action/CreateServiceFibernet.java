package com.nokia.nsw.uiv.action;

import com.nokia.nsw.uiv.exception.BadRequestException;
import com.nokia.nsw.uiv.framework.action.Action;
import com.nokia.nsw.uiv.framework.action.ActionContext;
import com.nokia.nsw.uiv.framework.action.HttpAction;
import com.nokia.nsw.uiv.model.common.party.Customer;
import com.nokia.nsw.uiv.model.common.party.CustomerRepository;
import com.nokia.nsw.uiv.model.resource.AdministrativeState;
import com.nokia.nsw.uiv.model.resource.logical.LogicalDevice;
import com.nokia.nsw.uiv.model.resource.logical.LogicalDeviceRepository;
import com.nokia.nsw.uiv.model.resource.logical.LogicalInterface;
import com.nokia.nsw.uiv.model.resource.logical.LogicalInterfaceRepository;
import com.nokia.nsw.uiv.model.service.Subscription;
import com.nokia.nsw.uiv.model.service.SubscriptionRepository;
import com.nokia.nsw.uiv.repository.*;
import com.nokia.nsw.uiv.request.CreateServiceFibernetRequest;
import com.nokia.nsw.uiv.response.CreateServiceFibernetResponse;
import com.nokia.nsw.uiv.utils.Constants;
import com.nokia.nsw.uiv.utils.Validations;
import com.setar.uiv.model.product.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
@Action
@Slf4j
public class CreateServiceFibernet implements HttpAction {

    private static final String ACTION_LABEL = "CreateServiceFibernet";
    private static final String ERROR_PREFIX = "UIV action CreateServiceFibernet execution failed - ";

    @Autowired
    private CustomerCustomRepository customerRepository;

    @Autowired
    private SubscriptionCustomRepository subscriptionRepository;

    @Autowired
    private ProductCustomRepository productRepository;

    @Autowired
    private CustomerFacingServiceCustomRepository cfsRepository;

    @Autowired
    private ResourceFacingServiceCustomRepository rfsRepository;

    @Autowired
    private LogicalDeviceCustomRepository logicalDeviceRepository;

    @Autowired
    private LogicalInterfaceCustomRepository logicalInterfaceRepository;

    @Override
    public Class<?> getActionClass() {
        return CreateServiceFibernetRequest.class;
    }



    @Override
    public Object doPost(ActionContext actionContext) throws Exception {
        log.info("Executing action {}", ACTION_LABEL);
        CreateServiceFibernetRequest request = (CreateServiceFibernetRequest) actionContext.getObject();

        try {
            // 1. Validate mandatory params
            try{
                Validations.validateMandatory(request.getSubscriberName(), "subscriberName");
                Validations.validateMandatory(request.getServiceID(), "serviceID");
                Validations.validateMandatory(request.getOntSN(), "ontSN");
                Validations.validateMandatory(request.getProductType(), "productType");
                Validations.validateMandatory(request.getProductSubtype(), "productSubtype");
            }catch (BadRequestException bre) {
                return new CreateServiceFibernetResponse("400", ERROR_PREFIX + "Missing mandatory parameter : " + bre.getMessage(),
                        java.time.Instant.now().toString(), "","");
            }
            // optional: template names etc.

            // Build canonical names
            String subscriberName = request.getSubscriberName() + "_" + request.getOntSN(); // subscriber with ONT suffix as per your conventions
            String subscriptionName = request.getSubscriberName() + "_" + request.getServiceID() + "_" + request.getOntSN();
            String productName = request.getSubscriberName() + "_" + request.getProductSubtype() + "_" + request.getServiceID();
            String cfsName = "CFS_" + subscriptionName;
            String rfsName = "RFS_" + subscriptionName;
            String ontName = "ONT" + request.getOntSN();





            // Length checks
            if (subscriberName.length() > 100) {
                return createErrorResponse("Subscriber name too long", "400", "", "");
            }
            if (subscriptionName.length() > 100) {
                return createErrorResponse("Subscription name too long", "400", "", "");
            }
            if (productName.length() > 100) {
                return createErrorResponse("Product name too long", "400", "", "");
            }
            if (ontName.length() > 100) {
                return createErrorResponse("ONT name too long", "400", "", "");
            }

            // 2. Subscriber: create or fetch
            Optional<Customer> optCustomer = customerRepository.findByDiscoveredName(subscriberName);
            Customer subscriber;
            if (optCustomer.isPresent()) {
                subscriber = optCustomer.get();
                log.info("Found existing subscriber: {}", subscriberName);
            } else {
                subscriber = new Customer();
                subscriber.setDiscoveredName(subscriberName);
                subscriber.setLocalName(Validations.encryptName(subscriberName));
                subscriber.setKind(Constants.SETAR_KIND_SETAR_SUBSCRIBER); // if you have a constant
                subscriber.setContext(Constants.SETAR);
                Map<String, Object> custProps = new HashMap<>();
                custProps.put("accountNumber", request.getSubscriberName());
                if (request.getFirstName() != null) custProps.put("subscriberFirstName", request.getFirstName());
                if (request.getLastName() != null) custProps.put("subscriberLastName", request.getLastName());
                if (request.getSubsAddress() != null) custProps.put("address", request.getSubsAddress());
                if (request.getCompanyName() != null) custProps.put("companyName", request.getCompanyName());
                if (request.getContactPhone() != null) custProps.put("contactPhoneNumber", request.getContactPhone());
                if (request.getHhid() != null) custProps.put("houseHoldId", request.getHhid());
                subscriber.setProperties(custProps);
                customerRepository.save(subscriber, 2);
                log.info("Created subscriber: {}", subscriberName);
            }

            // 3. Subscription: create or fetch (stored as LogicalComponent or service in your model — here we use Subscription entity)
            Optional<Subscription> optSubscription = subscriptionRepository.findByDiscoveredName(subscriptionName);
            Subscription subscription;
            if (optSubscription.isPresent()) {
                subscription = optSubscription.get();
                log.info("Found existing subscription: {}", subscriptionName);
            } else {
                subscription = new Subscription();
                subscription.setLocalName(Validations.encryptName(subscriptionName));
                subscription.setDiscoveredName(subscriptionName);
                subscription.setKind(Constants.SETAR_KIND_SETAR_SUBSCRIPTION);
                subscription.setContext(Constants.SETAR);
                Map<String, Object> subProps = new HashMap<>();
                subProps.put("serviceSubType", request.getProductSubtype());
                subProps.put("serviceLink", "ONT");
                subProps.put("serviceID", request.getServiceID());
                subProps.put("serviceSN", request.getOntSN());
                if (request.getQosProfile() != null) subProps.put("veipQosSessionProfile", request.getQosProfile());
                if (request.getKenanUidNo() != null) subProps.put("kenanSubscriberId", request.getKenanUidNo());
                subscription.setProperties(subProps);
                subscription.setCustomer(subscriber);
                subscriptionRepository.save(subscription, 2);
                log.info("Created subscription: {}", subscriptionName);
            }

            // 4. Product: create or fetch
            Optional<Product> optProduct = productRepository.findByDiscoveredName(productName);
            Product product;
            if (optProduct.isPresent()) {
                product = optProduct.get();
                log.info("Found existing product: {}", productName);
            } else {
                product = new Product();
                product.setLocalName(Validations.encryptName(productName));
                product.setDiscoveredName(productName);
                product.setKind(Constants.SETAR_KIND_SETAR_PRODUCT);
                product.setContext(Constants.SETAR);
                Map<String, Object> prodProps = new HashMap<>();
                prodProps.put("productType", request.getProductType());
                prodProps.put("productSubtype", request.getProductSubtype());
                product.setProperties(prodProps);
                product.setSubscription(subscription);
                productRepository.save(product, 2);
                log.info("Created product: {}", productName);
            }

            // 5. CFS: create or fetch
            Optional<CustomerFacingService> optCfs = cfsRepository.findByDiscoveredName(cfsName);
            CustomerFacingService cfs;
            if (optCfs.isPresent()) {
                cfs = optCfs.get();
                log.info("Found existing CFS: {}", cfsName);
            } else {
                cfs = new CustomerFacingService();
                cfs.setLocalName(Validations.encryptName(cfsName));
                cfs.setDiscoveredName(cfsName);
                cfs.setKind(Constants.SETAR_KIND_SETAR_CFS);
                cfs.setContext(Constants.SETAR);
                Map<String, Object> cfsProps = new HashMap<>();
                cfsProps.put("serviceStartDate", Instant.now().toString());
                if (request.getFxOrderID() != null) cfsProps.put("transactionId", request.getFxOrderID());
                cfs.setProperties(cfsProps);
                cfs.setContainingProduct(product);
                cfsRepository.save(cfs, 2);
                log.info("Created CFS: {}", cfsName);
            }

            // 6. RFS: create or fetch
            Optional<ResourceFacingService> optRfs = rfsRepository.findByDiscoveredName(rfsName);
            ResourceFacingService rfs;
            if (optRfs.isPresent()) {
                rfs = optRfs.get();
                log.info("Found existing RFS: {}", rfsName);
            } else {
                rfs = new ResourceFacingService();
                rfs.setLocalName(Validations.encryptName(rfsName));
                rfs.setDiscoveredName(rfsName);
                rfs.setKind(Constants.SETAR_KIND_SETAR_RFS);
                rfs.setContext(Constants.SETAR);
                Map<String, Object> rfsProps = new HashMap<>();
                rfsProps.put("status", "Active");
                if (request.getFxOrderID() != null) rfsProps.put("transactionId", request.getFxOrderID());
                rfs.setProperties(rfsProps);
                rfs.setContainingCfs(cfs);
                rfsRepository.save(rfs, 2);
                log.info("Created RFS: {}", rfsName);
            }
            // 7. OLT device: find or create as LogicalDevice with kind=OLT
            String oltName = request.getOltName() == null ? "" : request.getOltName();
            LogicalDevice oltDevice = null;
            if (!oltName.isEmpty()) {
                Optional<LogicalDevice> optOlt = logicalDeviceRepository.findByDiscoveredName(oltName);
                if (optOlt.isPresent()) {
                    oltDevice = optOlt.get();
                } else {
                    oltDevice = new LogicalDevice();
                    oltDevice.setLocalName(Validations.encryptName(oltName));
                    oltDevice.setDiscoveredName(oltName);
                    oltDevice.setKind(Constants.SETAR_KIND_OLT_DEVICE);
                    oltDevice.setContext(Constants.SETAR);
                    Map<String, Object> props = new HashMap<>();
                    props.put("localName", oltName);
                    if (request.getTemplateNameVEIP() != null) props.put("veipServiceTemplate", request.getTemplateNameVEIP());
                    if (request.getTemplateNameHSI() != null) props.put("veipHsiTemplate", request.getTemplateNameHSI());
                    oltDevice.setProperties(props);
                    oltDevice.addUsingService(rfs);
                    logicalDeviceRepository.save(oltDevice, 2);
                    log.info("Created OLT device: {}", oltName);
                }
            }


            // 8. ONT device: find or create as LogicalDevice with kind=ONT
            String ontContext = Constants.SETAR;
            Optional<LogicalDevice> optOnt = logicalDeviceRepository.findByDiscoveredName(ontName);
            LogicalDevice ontDevice;
            if (optOnt.isPresent()) {
                ontDevice = optOnt.get();
                log.info("Found existing ONT: {}", ontName);
            } else {
                ontDevice = new LogicalDevice();
                ontDevice.setLocalName(Validations.encryptName(ontName));
                ontDevice.setDiscoveredName(ontName);
                ontDevice.setKind(Constants.SETAR_KIND_ONT_DEVICE);
                ontDevice.setContext(ontContext);
                Map<String, Object> ontProps = new HashMap<>();
                ontProps.put("serialNo", request.getOntSN());
                if (request.getOntModel() != null) ontProps.put("deviceModel", request.getOntModel());
                if (request.getTemplateNameONT() != null) ontProps.put("ontTemplate", request.getTemplateNameONT());
                if (request.getMenm() != null) ontProps.put("description", request.getMenm());
                if (request.getVlanID() != null) ontProps.put("mgmtVlan", request.getVlanID());
                ontDevice.addUsingService(rfs);
                ontDevice.addManagingDevices(oltDevice);
                logicalDeviceRepository.save(ontDevice, 2);
                log.info("Created ONT device: {}", ontName);
            }

            // 8. ONT device: find or create as LogicalDevice with kind=ONT
            String stbDeviceName = "STB"+"_"+ request.getOntSN();
            Optional<LogicalDevice> stbOpt = logicalDeviceRepository.findByDiscoveredName(stbDeviceName);
            LogicalDevice stbDevice;
            if (stbOpt.isPresent()) {
                log.info("Found existing ONT: {}", stbDeviceName);
            } else {
                stbDevice = new LogicalDevice();
                stbDevice.setLocalName(Validations.encryptName(stbDeviceName));
                stbDevice.setDiscoveredName(stbDeviceName);
                stbDevice.setKind(Constants.SETAR_KIND_STB_AP_CM_DEVICE);
                stbDevice.setContext(ontContext);
                Map<String, Object> stbProps = new HashMap<>();
                stbProps.put("serialNo", request.getOntSN());
                if (request.getOntModel() != null) stbProps.put("deviceModel", request.getOntModel());
                if (request.getTemplateNameONT() != null) stbProps.put("ontTemplate", request.getTemplateNameONT());
                if (request.getMenm() != null) stbProps.put("description", request.getMenm());
                if (request.getVlanID() != null) stbProps.put("mgmtVlan", request.getVlanID());
                stbDevice.addUsingService(rfs);
                stbDevice.addManagingDevices(oltDevice);
                Map<String,Object> props = stbDevice.getProperties();
                props.put("administrativeState","Available");
                stbDevice.setProperties(props);
                logicalDeviceRepository.save(stbDevice, 2);
                log.info("Created ONT device: {}", stbDeviceName);
            }


            if(oltDevice!=null && ontDevice!=null){
                ontDevice.addContained(oltDevice);
            }


            // 9. VLAN interface (LogicalInterface) creation if needed
            if (request.getMenm() != null && request.getVlanID() != null) {
                String vlanName = request.getMenm() + "_" + request.getVlanID();
                String vlanContext=Constants.SETAR;
                Optional<LogicalInterface> optVlan = logicalInterfaceRepository.findByDiscoveredName(vlanName);
                if (!optVlan.isPresent()) {
                    LogicalInterface vlan = new LogicalInterface();
                    vlan.setLocalName(Validations.encryptName(vlanName));
                    vlan.setDiscoveredName(vlanName);
                    vlan.setKind(Constants.SETAR_KIND_VLAN_INTERFACE);
                    vlan.setContext(vlanContext);
                    Map<String, Object> vlanProps = new HashMap<>();
                    vlanProps.put("vlanId", request.getVlanID());
                    vlanProps.put("serviceId", request.getServiceID());
                    vlan.setProperties(vlanProps);
                    logicalInterfaceRepository.save(vlan, 2);
                    log.info("Created VLAN interface: {}", vlanName);
                }
            }

            // 10. Link RFS -> ONT or OLT (if model supports linking via properties)
            Map<String, Object> rfsProps = rfs.getProperties() == null ? new HashMap<>() : rfs.getProperties();
            rfsProps.put("serviceSN", request.getOntSN());
            if (oltDevice != null) rfsProps.put("oltPosition", oltDevice.getDiscoveredName());
            rfs.setProperties(rfsProps);
            rfsRepository.save(rfs, 2);

            // 11. Final response
            String ontNameResp = ontDevice != null ? ontDevice.getDiscoveredName() : "";
            CreateServiceFibernetResponse response = new CreateServiceFibernetResponse();
            response.setStatus("201");
            response.setMessage("Fibernet service created");
            response.setTimestamp(Instant.now().toString());
            response.setSubscriptionName(subscriptionName);
            response.setOntName(ontNameResp);
            return response;

        } catch (BadRequestException bre) {
            log.error("Validation error creating Fibernet", bre);
            return createErrorResponse(bre.getMessage(), "400", "", "");
        } catch (Exception ex) {
            log.error("Unhandled error in CreateServiceFibernet", ex);
            return createErrorResponse("Internal server error occurred - " + ex.getMessage(), "500", "", "");
        }
    }

    private CreateServiceFibernetResponse createErrorResponse(String message, String status, String subscriptionName, String ontName) {
        CreateServiceFibernetResponse resp = new CreateServiceFibernetResponse();
        resp.setStatus(status);
        resp.setMessage(ERROR_PREFIX + message);
        resp.setTimestamp(Instant.now().toString());
        resp.setSubscriptionName(subscriptionName == null ? "" : subscriptionName);
        resp.setOntName(ontName == null ? "" : ontName);
        return resp;
    }
}
