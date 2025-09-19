package com.nokia.nsw.uiv.action;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.nokia.nsw.uiv.exception.BadRequestException;
import com.nokia.nsw.uiv.exception.InternalServerErrorException;
import com.nokia.nsw.uiv.framework.action.Action;
import com.nokia.nsw.uiv.framework.action.ActionContext;
import com.nokia.nsw.uiv.framework.action.HttpAction;
import com.nokia.nsw.uiv.model.common.party.Customer;
import com.nokia.nsw.uiv.model.common.party.CustomerRepository;
import com.nokia.nsw.uiv.model.resource.logical.LogicalDevice;
import com.nokia.nsw.uiv.model.resource.logical.LogicalDeviceRepository;
import com.nokia.nsw.uiv.model.resource.logical.LogicalComponent;
import com.nokia.nsw.uiv.model.resource.logical.LogicalComponentRepository;
import com.nokia.nsw.uiv.model.service.ServiceRepository;
import com.nokia.nsw.uiv.model.service.Subscription;
import com.nokia.nsw.uiv.model.service.SubscriptionRepository;
import com.nokia.nsw.uiv.request.DeleteCBMRequest;
import com.nokia.nsw.uiv.response.CreateServiceFibernetResponse;
import com.nokia.nsw.uiv.response.DeleteCBMResponse;
import com.nokia.nsw.uiv.utils.Constants;
import com.nokia.nsw.uiv.utils.Validations;
import com.setar.uiv.model.product.*;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RestController;

import javax.swing.text.html.Option;

@Component
@RestController
@Action
@Slf4j
public class DeleteCBM implements HttpAction {

    private static final String ACTION_LABEL = "DeleteCBM";

    @Autowired
    private LogicalDeviceRepository cbmDeviceRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ResourceFacingServiceRepository rfsRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private CustomerRepository subscriberRepository;

    @Autowired
    private CustomerFacingServiceRepository cfsRepository;

    @Override
    public Class getActionClass() {
        return DeleteCBMRequest.class;
    }

    @Override
    public Object doPost(ActionContext actionContext) throws Exception {
        log.info("Executing action {}", ACTION_LABEL);
        String context = "";

        DeleteCBMRequest request = (DeleteCBMRequest) actionContext.getObject();

        // 1. Validate mandatory params
        try{
            Validations.validateMandatoryParams(request.getSubscriberName(), "subscriberName");
            Validations.validateMandatoryParams(request.getProductType(), "productType");
            Validations.validateMandatoryParams(request.getProductSubtype(), "productSubtype");
            Validations.validateMandatoryParams(request.getServiceId(), "serviceId");
            Validations.validateMandatoryParams(request.getServiceFlag(), "serviceFlag");
        }catch (BadRequestException bre) {
            return new DeleteCBMResponse("400", Constants.ERROR_PREFIX + "Missing mandatory parameter : " + bre.getMessage(),
                    java.time.Instant.now().toString(), "","");
        }

        String subscriptionName = request.getSubscriberName()+Constants.UNDER_SCORE + request.getServiceId();
        String cfsName = "CFS"+Constants.UNDER_SCORE + subscriptionName;
        String rfsName = "RFS"+Constants.UNDER_SCORE + subscriptionName;
        String productName = request.getSubscriberName()+Constants.UNDER_SCORE + request.getProductSubtype() +Constants.UNDER_SCORE+ request.getServiceId();
        String cbmName = "CBM"+Constants.UNDER_SCORE + request.getCbmSN();
        String subscriberName = request.getSubscriberName();
        String subscriptionContext="";
        String productContext="";
        String rfsContext = "";
        String cfsContext = "";

        try {
            ResourceFacingService setarRFS = null;
            Optional<Product> optProduct = Optional.empty();
            Optional<Subscription> optSubscription = Optional.empty();
            Optional<LogicalDevice> optCbmDevice = Optional.empty();
            Optional<CustomerFacingService> setarCFS = Optional.empty();
            int subscriptionCount = 0;
            try{
                // --- 3. Fetch CBM Device ---
                optCbmDevice = cbmDeviceRepository.uivFindByGdn(cbmName);
                if (!optCbmDevice.isPresent()) {
                    log.warn("CBM device {} not found", cbmName);
                }
                LogicalDevice cbm = optCbmDevice.get();
                System.out.println(cbm.getProperties().size());
                System.out.println(cbm.getPropertiesMap().size());

                Customer setarSubscriber;

                if ("IPTV".equalsIgnoreCase(request.getProductType())) {
                    // Direct subscriber lookup
                    setarSubscriber = subscriberRepository.uivFindByGdn(subscriberName).get();
                    subscriptionCount = setarSubscriber.getSubscription().size();
                } else {
                    // Use MAC address to build subscriberName
                    String cbmMacAddr = cbm.getProperties().get("macAddress").toString();
                    String macWithoutColons = cbmMacAddr.replaceAll(":", "");
                    String newSubscriberName = subscriberName + Constants.UNDER_SCORE + macWithoutColons;
                    subscriptionContext = Validations.getGlobalName("",newSubscriberName);
                    String subscriberGdn = Validations.getGlobalName("",newSubscriberName);
                    Optional<Customer> subscriber = subscriberRepository.uivFindByGdn(subscriberGdn);
                    subscriptionCount = subscriber.get().getSubscription().size();
                }

                // --- 4. Retrieve Associated Entities ---
                String subscriptionGdn = Validations.getGlobalName(subscriptionContext,subscriptionName);
                optSubscription = subscriptionRepository.uivFindByGdn(subscriptionGdn);
                productContext = subscriptionGdn;
                String productGdn = Validations.getGlobalName(productContext,productName);
                optProduct = productRepository.uivFindByGdn(productGdn);
                cfsContext = productGdn;
                String cfsGdn = Validations.getGlobalName(cfsContext,cfsName);
                setarCFS = cfsRepository.uivFindByGdn(cfsGdn);

                rfsContext = cfsGdn;
                String rfsGdn = Validations.getGlobalName(rfsContext,rfsName);
                Optional<ResourceFacingService> RFSComponent = rfsRepository.uivFindByGdn(rfsGdn);
                if(RFSComponent.isPresent()){
                    setarRFS = RFSComponent.get();
                }
            } catch (Exception e) {

            }

            // 5. CPE Device logic (Voice/Broadband)
            if (optSubscription.isPresent() && optCbmDevice.isPresent()) {
                Subscription subscription = optSubscription.get();
                LogicalDevice cbmDevice = optCbmDevice.get();

                if ("Voice".equalsIgnoreCase(request.getProductSubtype())) {
                    // reset voip ports if matching
                    resetVoipPorts(subscription, cbmDevice);
                } else if ("Broadband".equalsIgnoreCase(request.getProductSubtype())) {
                    cbmDevice.setDescription("");
                    // logic to check voip ports availability if required
                    cbmDeviceRepository.save(cbmDevice, 2);
                }
            }

            // 6. Validate CBM name length
            if (cbmName.length() > 100) {
                throw new BadRequestException("CBM name too long");
            }

            // 7. Delete CBM device
            optCbmDevice.ifPresent(cbmDeviceRepository::delete);

            // 8. Reset RFS linked resources
            setarRFS.getUsedResource().forEach(resource -> {
                if (resource.getLocalName().startsWith("AP") || resource.getLocalName().startsWith("STB")) {

                    // Create a properties map
                    Map<String, Object> props = new HashMap<>();
                    props.put("AdministrativeState", "Available");

                    if (resource.getLocalName().startsWith("STB")) {
                        props.put("DeviceGroupId", null);
                    }

                    // Set the map into resource properties
                    resource.setProperties(props);

                    // Save the resource
                    cbmDeviceRepository.save((LogicalDevice) resource, 2);
                }
            });

            // Delete RFS after processing its contained resources
            rfsRepository.delete(setarRFS);


            // 9. Delete Product & Subscription
            optProduct.ifPresent(productRepository::delete);
            optSubscription.ifPresent(subscriptionRepository::delete);
            if (subscriptionCount == 1) {
                subscriberRepository.findById(subscriberName).ifPresent(subscriberRepository::delete);
            }

            return new DeleteCBMResponse("200", "CBM objects Deleted", java.time.Instant.now().toString(),
                    cbmName, subscriptionName);

        } catch (Exception e) {
            log.error("DeleteCBM action failed", e);
            throw new InternalServerErrorException("UIV action DeleteCBM execution failed - " + e.getMessage());
        }
    }

    private void resetVoipPorts(Subscription subscription, LogicalDevice cbmDevice) {
        if (subscription.getProperties().get("oipNumber1") != null) {
            if (subscription.getProperties().get("oipNumber1").equals(cbmDevice.getProperties().get("voipPort1"))) {
                cbmDevice.getProperties().put("voipPort1", "Available");
            }
            if (subscription.getProperties().get("oipNumber1").equals(cbmDevice.getProperties().get("voipPort2"))) {
                cbmDevice.getProperties().put("voipPort2", "Available");
            }
        }
        if (subscription.getProperties().get("oipNumber2") != null) {
            if (subscription.getProperties().get("oipNumber1").equals(cbmDevice.getProperties().get("voipPort1"))) {
                cbmDevice.getProperties().put("voipPort1", "Available");
            }
            if (subscription.getProperties().get("oipNumber1").equals(cbmDevice.getProperties().get("voipPort2"))) {
                cbmDevice.getProperties().put("voipPort2", "Available");
            }
        }
        cbmDeviceRepository.save(cbmDevice, 2);
    }

}
