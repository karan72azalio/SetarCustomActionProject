package com.nokia.nsw.uiv.action;

import com.nokia.nsw.uiv.exception.BadRequestException;
import com.nokia.nsw.uiv.framework.action.Action;
import com.nokia.nsw.uiv.framework.action.ActionContext;
import com.nokia.nsw.uiv.framework.action.HttpAction;
import com.nokia.nsw.uiv.model.common.party.Customer;
import com.nokia.nsw.uiv.model.common.party.CustomerRepository;
import com.nokia.nsw.uiv.model.resource.logical.LogicalDevice;
import com.nokia.nsw.uiv.model.resource.logical.LogicalDeviceRepository;
import com.nokia.nsw.uiv.model.resource.logical.LogicalInterface;
import com.nokia.nsw.uiv.model.resource.logical.LogicalInterfaceRepository;
import com.nokia.nsw.uiv.model.service.Subscription;
import com.nokia.nsw.uiv.model.service.SubscriptionRepository;
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
    private LogicalInterfaceRepository logicalInterfaceRepository;

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
            Validations.validateMandatoryParams(request.getSubscriberName(), "subscriberName");
            Validations.validateMandatoryParams(request.getServiceID(), "serviceID");
            Validations.validateMandatoryParams(request.getOntSN(), "ontSN");
            Validations.validateMandatoryParams(request.getProductType(), "productType");
            Validations.validateMandatoryParams(request.getProductSubtype(), "productSubtype");
            // optional: template names etc.

            // Build canonical names
            String subscriberGdn = request.getSubscriberName() + "_" + request.getOntSN(); // subscriber with ONT suffix as per your conventions
            String subscriptionGdn = request.getSubscriberName() + "_" + request.getServiceID() + "_" + request.getOntSN();
            String productGdn = request.getSubscriberName() + "_" + request.getProductSubtype() + "_" + request.getServiceID();
            String cfsGdn = "CFS_" + subscriptionGdn;
            String rfsGdn = "RFS_" + subscriptionGdn;
            String ontGdn = "ONT" + request.getOntSN();

            // Length checks
            if (subscriberGdn.length() > 100) {
                return createErrorResponse("Subscriber name too long", "400", "", "");
            }
            if (subscriptionGdn.length() > 100) {
                return createErrorResponse("Subscription name too long", "400", "", "");
            }
            if (productGdn.length() > 100) {
                return createErrorResponse("Product name too long", "400", "", "");
            }
            if (ontGdn.length() > 100) {
                return createErrorResponse("ONT name too long", "400", "", "");
            }

            // 2. Subscriber: create or fetch
            Optional<Customer> optCustomer = customerRepository.uivFindByGdn(subscriberGdn);
            Customer subscriber;
            if (optCustomer.isPresent()) {
                subscriber = optCustomer.get();
                log.info("Found existing subscriber: {}", subscriberGdn);
            } else {
                subscriber = new Customer();
                subscriber.setLocalName(subscriberGdn);
                subscriber.setKind(Constants.SETAR_KIND_SETAR_SUBSCRIBER); // if you have a constant
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
                log.info("Created subscriber: {}", subscriberGdn);
            }

            // 3. Subscription: create or fetch (stored as LogicalComponent or service in your model — here we use Subscription entity)
            Optional<Subscription> optSubscription = subscriptionRepository.uivFindByGdn(subscriptionGdn);
            Subscription subscription;
            if (optSubscription.isPresent()) {
                subscription = optSubscription.get();
                log.info("Found existing subscription: {}", subscriptionGdn);
            } else {
                subscription = new Subscription();
                subscription.setLocalName(subscriptionGdn);
                subscription.setKind(Constants.SETAR_KIND_SETAR_SUBSCRIPTION);
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
                log.info("Created subscription: {}", subscriptionGdn);
            }

            // 4. Product: create or fetch
            Optional<Product> optProduct = productRepository.uivFindByGdn(productGdn);
            Product product;
            if (optProduct.isPresent()) {
                product = optProduct.get();
                log.info("Found existing product: {}", productGdn);
            } else {
                product = new Product();
                product.setLocalName(productGdn);
                product.setKind(Constants.SETAR_KIND_SETAR_PRODUCT);
                Map<String, Object> prodProps = new HashMap<>();
                prodProps.put("productType", request.getProductType());
                prodProps.put("productSubtype", request.getProductSubtype());
                product.setProperties(prodProps);
                productRepository.save(product, 2);
                log.info("Created product: {}", productGdn);
            }

            // 5. CFS: create or fetch
            Optional<CustomerFacingService> optCfs = cfsRepository.uivFindByGdn(cfsGdn);
            CustomerFacingService cfs;
            if (optCfs.isPresent()) {
                cfs = optCfs.get();
                log.info("Found existing CFS: {}", cfsGdn);
            } else {
                cfs = new CustomerFacingService();
                cfs.setLocalName(cfsGdn);
                cfs.setKind(Constants.SETAR_KIND_SETAR_CFS);
                Map<String, Object> cfsProps = new HashMap<>();
                cfsProps.put("serviceStartDate", Instant.now().toString());
                if (request.getFxOrderID() != null) cfsProps.put("transactionId", request.getFxOrderID());
                cfs.setProperties(cfsProps);
                cfs.setContainingProduct(product);
                cfsRepository.save(cfs, 2);
                log.info("Created CFS: {}", cfsGdn);
            }

            // 6. RFS: create or fetch
            Optional<ResourceFacingService> optRfs = rfsRepository.uivFindByGdn(rfsGdn);
            ResourceFacingService rfs;
            if (optRfs.isPresent()) {
                rfs = optRfs.get();
                log.info("Found existing RFS: {}", rfsGdn);
            } else {
                rfs = new ResourceFacingService();
                rfs.setLocalName(rfsGdn);
                rfs.setKind(Constants.SETAR_KIND_SETAR_RFS);
                Map<String, Object> rfsProps = new HashMap<>();
                rfsProps.put("status", "Active");
                if (request.getFxOrderID() != null) rfsProps.put("transactionId", request.getFxOrderID());
                rfs.setProperties(rfsProps);
                rfs.setContainingCfs(cfs);
                rfsRepository.save(rfs, 2);
                log.info("Created RFS: {}", rfsGdn);
            }

            // 7. OLT device: find or create as LogicalDevice with kind=OLT
            String oltGdn = request.getOltName() == null ? "" : request.getOltName();
            LogicalDevice oltDevice = null;
            if (!oltGdn.isEmpty()) {
                Optional<LogicalDevice> optOlt = logicalDeviceRepository.uivFindByGdn(oltGdn);
                if (optOlt.isPresent()) {
                    oltDevice = optOlt.get();
                } else {
                    oltDevice = new LogicalDevice();
                    oltDevice.setLocalName(oltGdn);
                    oltDevice.setKind(Constants.SETAR_KIND_OLT_DEVICE);
                    Map<String, Object> props = new HashMap<>();
                    props.put("localName", oltGdn);
                    if (request.getTemplateNameVEIP() != null) props.put("veipServiceTemplate", request.getTemplateNameVEIP());
                    if (request.getTemplateNameHSI() != null) props.put("veipHsiTemplate", request.getTemplateNameHSI());
                    oltDevice.setProperties(props);
                    logicalDeviceRepository.save(oltDevice, 2);
                    log.info("Created OLT device: {}", oltGdn);
                }
            }

            // 8. ONT device: find or create as LogicalDevice with kind=ONT
            Optional<LogicalDevice> optOnt = logicalDeviceRepository.uivFindByGdn(ontGdn);
            LogicalDevice ontDevice;
            if (optOnt.isPresent()) {
                ontDevice = optOnt.get();
                log.info("Found existing ONT: {}", ontGdn);
            } else {
                ontDevice = new LogicalDevice();
                ontDevice.setLocalName(ontGdn);
                ontDevice.setKind(Constants.SETAR_KIND_ONT_DEVICE);
                Map<String, Object> ontProps = new HashMap<>();
                ontProps.put("serialNo", request.getOntSN());
                if (request.getOntModel() != null) ontProps.put("deviceModel", request.getOntModel());
                if (request.getTemplateNameONT() != null) ontProps.put("ontTemplate", request.getTemplateNameONT());
                if (request.getMenm() != null) ontProps.put("description", request.getMenm());
                if (request.getVlanID() != null) ontProps.put("mgmtVlan", request.getVlanID());
                ontDevice.setProperties(ontProps);
                logicalDeviceRepository.save(ontDevice, 2);
                log.info("Created ONT device: {}", ontGdn);
            }

            // 9. VLAN interface (LogicalInterface) creation if needed
            if (request.getMenm() != null && request.getVlanID() != null) {
                String vlanGdn = request.getMenm() + "_" + request.getVlanID();
                Optional<LogicalInterface> optVlan = logicalInterfaceRepository.uivFindByGdn(vlanGdn);
                if (!optVlan.isPresent()) {
                    LogicalInterface vlan = new LogicalInterface();
                    vlan.setLocalName(vlanGdn);
                    vlan.setKind(Constants.SETAR_KIND_VLAN_INTERFACE);
                    Map<String, Object> vlanProps = new HashMap<>();
                    vlanProps.put("vlanId", request.getVlanID());
                    vlanProps.put("serviceId", request.getServiceID());
                    vlan.setProperties(vlanProps);
                    logicalInterfaceRepository.save(vlan, 2);
                    log.info("Created VLAN interface: {}", vlanGdn);
                }
            }

            // 10. Link RFS -> ONT or OLT (if model supports linking via properties)
            Map<String, Object> rfsProps = rfs.getProperties() == null ? new HashMap<>() : rfs.getProperties();
            rfsProps.put("serviceSN", request.getOntSN());
            if (oltDevice != null) rfsProps.put("oltPosition", oltDevice.getLocalName());
            rfs.setProperties(rfsProps);
            rfsRepository.save(rfs, 2);

            // 11. Final response
            String ontNameResp = ontDevice != null ? ontDevice.getLocalName() : "";
            CreateServiceFibernetResponse response = new CreateServiceFibernetResponse();
            response.setStatus("201");
            response.setMessage("Fibernet service created");
            response.setTimestamp(Instant.now().toString());
            response.setSubscriptionName(subscriptionGdn);
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
