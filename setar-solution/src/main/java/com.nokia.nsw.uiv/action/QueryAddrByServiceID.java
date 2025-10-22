package com.nokia.nsw.uiv.action;

import com.nokia.nsw.uiv.exception.BadRequestException;
import com.nokia.nsw.uiv.framework.action.Action;
import com.nokia.nsw.uiv.framework.action.ActionContext;
import com.nokia.nsw.uiv.framework.action.HttpAction;
import com.nokia.nsw.uiv.model.common.party.Customer;
import com.nokia.nsw.uiv.model.service.Subscription;
import com.nokia.nsw.uiv.repository.*;
import com.nokia.nsw.uiv.request.QueryAddrByServiceIDRequest;
import com.nokia.nsw.uiv.response.QueryAddrByServiceIDResponse;
import com.nokia.nsw.uiv.utils.Validations;
import com.setar.uiv.model.product.CustomerFacingService;
import com.setar.uiv.model.product.Product;
import com.setar.uiv.model.product.ResourceFacingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@Action
@Slf4j
public class QueryAddrByServiceID implements HttpAction {

    private static final String ACTION_LABEL = "QueryAddrByServiceID";
    private static final String ERROR_PREFIX = "UIV action QueryAddrByServiceID execution failed - ";

    @Autowired
    private ResourceFacingServiceCustomRepository rfsRepository;

    @Autowired
    private CustomerFacingServiceCustomRepository cfsRepository;
    @Autowired
    private CustomerCustomRepository customerRepository;

    @Autowired
    private ProductCustomRepository productRepository;

    @Autowired
    private SubscriptionCustomRepository subscriptionRepository;

    @Override
    public Class<?> getActionClass() {
        return QueryAddrByServiceIDRequest.class;
    }

    @Override
    public Object doPost(ActionContext actionContext) throws Exception {
        log.info("Executing action {}", ACTION_LABEL);

        QueryAddrByServiceIDRequest request = (QueryAddrByServiceIDRequest) actionContext.getObject();
        try {
            // 1) Mandatory input validation
            try {
                Validations.validateMandatory(request.getServiceId(), "SERVICE_ID");
            } catch (BadRequestException bre) {
                // Code5 -> Missing mandatory parameter
                return createErrorResponse("400", ERROR_PREFIX + "Missing mandatory parameter: " + bre.getMessage());
            }

            String serviceId = request.getServiceId().trim();
            log.info("Looking up RFS entries containing service id '{}'", serviceId);

            // 2) Locate the target service (RFS) using SERVICE_ID
            List<ResourceFacingService> rfsListAll = (List<ResourceFacingService>) rfsRepository.findAll();
            List<ResourceFacingService> rfsList =new ArrayList<>();
            rfsListAll.forEach(rFS -> {
                if (rFS.getDiscoveredName().contains(serviceId)) {
                    rfsList.add(rFS);
                }});
            if (rfsList == null || rfsList.isEmpty()) {
                log.info("No RFS entries found containing '{}'", serviceId);
                return createErrorResponse("404", ERROR_PREFIX + "No Service Details Found");
            }

            ResourceFacingService matchedRfs = null;
            for (ResourceFacingService rfs : rfsList) {
                String name = rfs.getDiscoveredName() == null ? "" : rfs.getDiscoveredName();
                String[] tokens = name.split("_", -1);
                if (tokens.length >= 3 && serviceId.equals(tokens[2])) {
                    matchedRfs = rfsRepository.findByDiscoveredName(rfs.getDiscoveredName()).get();
                    log.info("Matched RFS by token check: {}", name);
                    break;
                }
            }

            if (matchedRfs == null) {
                log.info("No RFS found whose 3rd token equals '{}'", serviceId);
                return createErrorResponse("404", ERROR_PREFIX + "No Service Details Found");
            }

            // 3) Resolve related data: product <- cfs <- rfs ; subscription <- product ; subscriber <- subscription
            CustomerFacingService cfs = matchedRfs.getContainingCfs(); // assume this getter exists (as used in CreateServiceFibernet)
            if(cfs!=null){
                cfs = cfsRepository.findByDiscoveredName(cfs.getDiscoveredName()).get();  
            }
            if (cfs == null) {
                log.warn("Matched RFS does not reference a CustomerFacingService");
                return createErrorResponse("404", ERROR_PREFIX + "No Service Details Found");
            }

            // From CFS to Product
            Product product = cfs.getContainingProduct(); // assume this exists (CreateServiceFibernet set containingProduct)
            if(product!=null){
                product = productRepository.findByDiscoveredName(product.getDiscoveredName()).get();
            }
            if (product == null) {
                log.warn("CustomerFacingService does not reference Product");
                return createErrorResponse("404", ERROR_PREFIX + "No Service Details Found");
            }

            // Product name: Try typed access then fallback to properties map
            String productName = null;
            try {
                // Try product.getProductType().getName() if productType object exists
                if (product.getProperties().get("productType") != null) {
                    productName = product.getProperties().get("productType").toString();
                }
            } catch (Exception e) {
                log.debug("product.getProductType() access failed, will try properties map");
            }
            if (productName == null || productName.trim().isEmpty()) {
                Map<String, Object> prodProps = product.getProperties();
                if (prodProps != null && prodProps.get("productType") != null) {
                    productName = prodProps.get("productType").toString();
                }
            }

            // Subscription: try typed getter, fallback to product.properties
            Subscription subscription = product.getSubscription();
            if(subscription!=null){
                subscription = subscriptionRepository.findByDiscoveredName(subscription.getDiscoveredName()).get();
            }
            if (subscription == null) {
                // fallback: maybe product.properties contains subscription id or link - but per spec this is required
                log.warn("Product does not have Subscription reference");
                return createErrorResponse("404", ERROR_PREFIX + "No Service Details Found");
            }

            // Service link: prefer subscription.getServiceLink(); fallback to properties
            String serviceLink = null;
            try {
                if (subscription.getProperties().get("serviceLink") != null) serviceLink = subscription.getProperties().get("serviceLink").toString();
            } catch (Exception e) {
                log.debug("subscription.getServiceLink() access failed, will try properties map");
            }
            if ((serviceLink == null || serviceLink.trim().isEmpty()) && subscription.getProperties() != null) {
                Object sl = subscription.getProperties().get("serviceLink");
                if (sl != null) serviceLink = sl.toString();
            }

            // Subscriber -> address
            Customer subscriber = subscription.getCustomer(); // in CreateServiceFibernet subscription.setCustomer(subscriber)
            if(subscriber!=null){
                subscriber = customerRepository.findByDiscoveredName(subscriber.getDiscoveredName()).get();
            }
            String address = null;
            if (subscriber != null) {
                try {
                    if (subscriber.getProperties().get("address") != null) address = subscriber.getProperties().get("address").toString();
                } catch (Exception e) {
                    log.debug("subscriber.getAddress() failed, will try properties map");
                }
                if ((address == null || address.trim().isEmpty()) && subscriber.getProperties() != null) {
                    Object a = subscriber.getProperties().get("address");
                    if (a != null) address = a.toString();
                }
            } else {
                log.warn("Subscription does not reference a Subscriber");
            }

            // Validate final outputs - if any null/empty -> treat as not found per spec
            boolean missing = (productName == null || productName.trim().isEmpty())
                    || (address == null || address.trim().isEmpty())
                    || (serviceLink == null || serviceLink.trim().isEmpty());

            if (missing) {
                log.warn("One or more resolved values are missing: productName='{}', address='{}', serviceLink='{}'",
                        productName, address, serviceLink);
                return createErrorResponse("404", ERROR_PREFIX + "No Service Details Found");
            }

            // 4) Build success response
            QueryAddrByServiceIDResponse resp = new QueryAddrByServiceIDResponse();
            resp.setStatus("200");
            resp.setMessage("Service Details Found.");
            resp.setTimestamp(Instant.now().toString());
            resp.setProductName(productName == null ? "" : productName);
            resp.setAddress(address == null ? "" : address);
            resp.setServiceLink(serviceLink == null ? "" : serviceLink);
            return resp;

        } catch (Exception ex) {
            log.error("Unhandled error in {}", ACTION_LABEL, ex);
            return createErrorResponse("500", ERROR_PREFIX + ex.getMessage());
        }
    }

    private QueryAddrByServiceIDResponse createErrorResponse(String status, String message) {
        QueryAddrByServiceIDResponse resp = new QueryAddrByServiceIDResponse();
        resp.setStatus(status);
        resp.setMessage(message);
        resp.setTimestamp(Instant.now().toString());
        resp.setProductName("");
        resp.setAddress("");
        resp.setServiceLink("");
        return resp;
    }
}
