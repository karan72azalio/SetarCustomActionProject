package com.nokia.nsw.uiv.action;

import com.nokia.nsw.uiv.exception.BadRequestException;
import com.nokia.nsw.uiv.framework.action.Action;
import com.nokia.nsw.uiv.framework.action.ActionContext;
import com.nokia.nsw.uiv.framework.action.HttpAction;
import com.nokia.nsw.uiv.model.common.party.Customer;
import com.nokia.nsw.uiv.model.common.party.CustomerRepository;
import com.nokia.nsw.uiv.model.service.Subscription;
import com.nokia.nsw.uiv.model.service.SubscriptionRepository;
import com.nokia.nsw.uiv.model.resource.logical.LogicalDevice;
import com.nokia.nsw.uiv.model.resource.logical.LogicalDeviceRepository;
import com.setar.uiv.model.product.CustomerFacingService;
import com.setar.uiv.model.product.CustomerFacingServiceRepository;
import com.setar.uiv.model.product.Product;
import com.setar.uiv.model.product.ProductRepository;
import com.setar.uiv.model.product.ResourceFacingService;
import com.setar.uiv.model.product.ResourceFacingServiceRepository;
import com.nokia.nsw.uiv.request.DeleteIPTVRequest;
import com.nokia.nsw.uiv.response.DeleteIPTVResponse;
import com.nokia.nsw.uiv.utils.Validations;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.*;

@Component
@RestController
@Action
@Slf4j
public class DeleteIPTV implements HttpAction {

    private static final String ERROR_PREFIX = "UIV action DeleteIPTV execution failed - ";

    @Autowired private CustomerRepository customerRepository;
    @Autowired private SubscriptionRepository subscriptionRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private CustomerFacingServiceRepository cfsRepository;
    @Autowired private ResourceFacingServiceRepository rfsRepository;
    @Autowired private LogicalDeviceRepository deviceRepository;

    @Override
    public Class getActionClass() {
        return DeleteIPTVRequest.class;
    }

    @Override
    public Object doPost(ActionContext actionContext) throws Exception {
        DeleteIPTVRequest request = (DeleteIPTVRequest) actionContext.getObject();
        String subscriberName = request.getSubscriberName();
        String productType = request.getProductType();
        String productSubType = request.getProductSubType();
        String serviceId = request.getServiceId();
        String serviceFlag = request.getServiceFlag();
        String ontSN = request.getOntSN();

        try {
            // Step 1: Validate mandatory parameters
            Validations.validateMandatoryParams(subscriberName, "subscriberName");
            Validations.validateMandatoryParams(productType, "productType");
            Validations.validateMandatoryParams(productSubType, "productSubType");
            Validations.validateMandatoryParams(serviceId, "serviceId");
            Validations.validateMandatoryParams(ontSN, "ontSN");

            // Step 2: Prepare entity names
            String subscriptionName = subscriberName + "_" + serviceId;
            String productName = subscriberName + productSubType + serviceId;
            String cfsName = "CFS_" + subscriptionName;
            String rfsName = "RFS_" + subscriptionName;
            String ontName = "ONT_" + ontSN;

            if (ontName.length() > 100) {
                return errorResponse("400", ERROR_PREFIX + "ONT name too long");
            }

            // Step 3: Retrieve entities
            Optional<Customer> optCust = customerRepository.uivFindByGdn(subscriberName);
            Optional<Subscription> optSub = subscriptionRepository.uivFindByGdn(subscriptionName);
            Optional<Product> optProd = productRepository.uivFindByGdn(productName);
            Optional<CustomerFacingService> optCfs = cfsRepository.uivFindByGdn(cfsName);
            Optional<ResourceFacingService> optRfs = rfsRepository.uivFindByGdn(rfsName);
            Optional<LogicalDevice> optOnt = deviceRepository.uivFindByGdn(ontName);

            if (optCust.isEmpty() || optSub.isEmpty()) {
                return successResponse(subscriptionName, ontName, "No entry found for Delete.");
            }

            LogicalDevice olt = null;
            if (optOnt.isPresent()) {
                LogicalDevice ont = optOnt.get();
                olt = (LogicalDevice) ont.getContained();
            }

            // Step 4: Update OLT template values
            if (olt != null) {
                Map<String, Object> props = olt.getProperties();
                props.put("hsiTemplate", "");
                props.put("iptvTemplate", "");
                props.put("igmpTemplate", "");
                deviceRepository.save(olt);
            }

            // Step 5: Reset STB/AP devices
            if (optRfs.isPresent()) {
                ResourceFacingService rfs = optRfs.get();
                rfs.getUsedResource().forEach(res -> {
                    if (res.getLocalName().startsWith("STB") || res.getLocalName().startsWith("AP")) {
                        try {
                            deviceRepository.uivFindByGdn(res.getLocalName()).ifPresent(dev -> {
                                dev.getProperties().put("deviceGroupId", "");
                                dev.getProperties().put("administrativeState", "Available");
                                deviceRepository.save(dev);
                            });
                        } catch (Exception ignored) {}
                    }
                });
            }

            // Step 6: Delete RFS, CFS, Product
            optRfs.ifPresent(rfsRepository::delete);
            optCfs.ifPresent(cfsRepository::delete);
            optProd.ifPresent(productRepository::delete);

            // Step 7: Conditional deletion of devices
            if (!"Exist".equalsIgnoreCase(serviceFlag) && optOnt.isPresent()) {
                deviceRepository.delete(optOnt.get());
                if (olt != null && olt.getContained().isEmpty()) {
                    deviceRepository.delete(olt);
                }
            }

            // Step 8: Conditional deletion of Subscriber & Subscription
            if (optCust.isPresent() && optSub.isPresent()) {
                Customer cust = optCust.get();
                List<Subscription> subs = new ArrayList<>();
                subscriptionRepository.findAll().forEach(subs::add);
                if (subs.size() <= 1) {
                    subscriptionRepository.delete(optSub.get());
                    customerRepository.delete(cust);
                } else {
                    subscriptionRepository.delete(optSub.get());
                }
            }

            // Step 9: Return success
            return successResponse(subscriptionName, ontName, "UIV action DeleteIPTV executed successfully.");

        } catch (BadRequestException bre) {
            return errorResponse("400", ERROR_PREFIX + "Missing mandatory parameter : " + bre.getMessage());
        } catch (Exception ex) {
            return errorResponse("500", ERROR_PREFIX + "Internal server error occurred - " + ex.getMessage());
        }
    }

    private DeleteIPTVResponse errorResponse(String code, String message) {
        return new DeleteIPTVResponse(code, message, Instant.now().toString());
    }

    private DeleteIPTVResponse successResponse(String subscriptionId, String ontName, String message) {
        return new DeleteIPTVResponse("200", message, Instant.now().toString(), subscriptionId,ontName);
    }
}
