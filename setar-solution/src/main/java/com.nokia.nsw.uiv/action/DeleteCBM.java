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
import com.nokia.nsw.uiv.model.service.Subscription;
import com.nokia.nsw.uiv.model.service.SubscriptionRepository;
import com.nokia.nsw.uiv.request.DeleteCBMRequest;
import com.nokia.nsw.uiv.response.CreateServiceFibernetResponse;
import com.nokia.nsw.uiv.response.DeleteCBMResponse;
import com.nokia.nsw.uiv.utils.Constants;
import com.nokia.nsw.uiv.utils.Validations;
import com.setar.uiv.model.product.Product;
import com.setar.uiv.model.product.ProductRepository;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RestController;

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
    private LogicalComponentRepository rfsRepository;

    @Autowired
    private CustomerRepository subscriberRepository;

    @Override
    public Class getActionClass() {
        return DeleteCBMRequest.class;
    }

    @Override
    public Object doPost(ActionContext actionContext) throws Exception {
        log.info("Executing action {}", ACTION_LABEL);

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

        String subscriptionName = request.getSubscriberName() + request.getServiceId();
        String cfsName = "CFS" + subscriptionName;
        String rfsName = "RFS_" + subscriptionName;
        String productName = request.getSubscriberName() + request.getProductSubtype() + request.getServiceId();
        String cbmName = "CBM" + request.getCbmSN();
        String subscriberName = request.getSubscriberName();

        try {
            // 2. Fetch CBM device
            Optional<LogicalDevice> optCbmDevice = cbmDeviceRepository.uivFindByGdn(cbmName);
            if (!optCbmDevice.isPresent()) {
                log.warn("CBM device {} not found", cbmName);
            }

            Optional<Subscription> optSubscription = subscriptionRepository.uivFindByGdn(subscriptionName);

            int subscriptionCount = 0;
            if (optSubscription.isPresent()) {
                Subscription subscription = optSubscription.get();
                Customer customer = subscription.getCustomer();
                if (customer != null && customer.getSubscription() != null) {
                    subscriptionCount = customer.getSubscription().size();
                }
            }


            // 4. Fetch Product
            Optional<Product> optProduct = productRepository.uivFindByGdn(productName);

            // 5. Fetch RFS
            Optional<LogicalComponent> optRfs = rfsRepository.uivFindByGdn(rfsName);

            // 6. CPE Device logic (Voice/Broadband)
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

            // 7. Validate CBM name length
            if (cbmName.length() > 100) {
                throw new BadRequestException("CBM name too long");
            }

            // 8. Delete CBM device
            optCbmDevice.ifPresent(cbmDeviceRepository::delete);

            // 9. Reset RFS linked resources
            if (optRfs.isPresent()) {
                LogicalComponent rfs = optRfs.get();

                rfs.getContained().forEach(resource -> {
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
                        rfsRepository.save(resource, 2);
                    }
                });

                // Delete RFS after processing its contained resources
                rfsRepository.delete(rfs);
            }


            // 10. Delete Product & Subscription
            optProduct.ifPresent(productRepository::delete);
            optSubscription.ifPresent(subscriptionRepository::delete);


            if (subscriptionCount == 1) {
                subscriberRepository.findById(subscriberName).ifPresent(subscriberRepository::delete);
            }

            return new DeleteCBMResponse("200", "SPR objects Deleted", java.time.Instant.now().toString(),
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
