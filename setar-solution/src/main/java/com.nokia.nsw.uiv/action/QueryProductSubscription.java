package com.nokia.nsw.uiv.action;

import com.nokia.nsw.uiv.exception.AccessForbiddenException;
import com.nokia.nsw.uiv.exception.BadRequestException;
import com.nokia.nsw.uiv.exception.ModificationNotAllowedException;
import com.nokia.nsw.uiv.framework.action.Action;
import com.nokia.nsw.uiv.framework.action.ActionContext;
import com.nokia.nsw.uiv.framework.action.HttpAction;
import com.nokia.nsw.uiv.repository.ProductCustomRepository;
import com.nokia.nsw.uiv.request.QueryProductSubscriptionRequest;
import com.nokia.nsw.uiv.response.CreateServiceCBMResponse;
import com.nokia.nsw.uiv.response.QueryProductSubscriptionResponse;
import com.nokia.nsw.uiv.utils.Constants;
import com.nokia.nsw.uiv.utils.Validations;
import com.setar.uiv.model.product.Product;
import com.setar.uiv.model.product.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@Component
@RestController
@Action
@Slf4j
public class QueryProductSubscription implements HttpAction {

    protected static final String ACTION_LABEL = Constants.QUERY_PRODUCT_SUBSCRIPTION;

    @Autowired
    private ProductCustomRepository productRepository;

    @Override
    public Class<?> getActionClass() {
        return QueryProductSubscriptionRequest.class;
    }

    @Override
    public Object doPost(ActionContext actionContext) throws Exception {
        log.error("Executing action: {}", ACTION_LABEL);

        QueryProductSubscriptionRequest request = (QueryProductSubscriptionRequest) actionContext.getObject();

        try {
            log.error("Mandatory parameter validation started...");
            try{
                Validations.validateMandatoryParams(request.getSubscriberName(), "subscriberName");
                Validations.validateMandatoryParams(request.getServiceID(), "serviceID");
                Validations.validateMandatoryParams(request.getProductType(), "productType");
                Validations.validateMandatoryParams(request.getComponentName(), "componentName");
            }catch (BadRequestException bre) {
                return new QueryProductSubscriptionResponse("400", Constants.ERROR_PREFIX + "Missing mandatory parameter : " + bre.getMessage(),
                        java.time.Instant.now().toString(), "","");
            }


            log.error("Mandatory parameter validation completed");

            // ================== Construct Product Name ==================
            String subscriberName = request.getSubscriberName();
            String subscriptionName = subscriberName + Constants.UNDER_SCORE  + request.getServiceID();
            String productName = request.getServiceID() + Constants.UNDER_SCORE  + request.getComponentName();
            if (productName.length() > 100) {
                throw new BadRequestException("Product Name String exceeds 100 characters");
            }

            // ================== Lookup Product ==================

            Optional<Product> optProduct = productRepository.findByDiscoveredName(productName);

            if (optProduct.isPresent()) {
                Product product = optProduct.get();
                String productId = (String) product.getProperties().getOrDefault("productId", "");

                log.error("Product Subscription found: {} with ID {}", productName, productId);

                return new QueryProductSubscriptionResponse(
                        "200",
                        "UIV action QueryProductSubscription executed successfully.",
                        java.time.Instant.now().toString(),
                        productName,
                        productId
                );
            } else {
                log.error("Product Subscription not found: {}", productName);
                String msg = "UIV action QueryProductSubscription execution failed - " +
                        "Error, Product Subscription with name " + productName + " not found.";
                return new QueryProductSubscriptionResponse("404", msg,
                        java.time.Instant.now().toString(), "", "");
            }

        } catch (BadRequestException bre) {
            String msg = "UIV action QueryProductSubscription execution failed - Missing mandatory parameter : " + bre.getMessage();
            return new QueryProductSubscriptionResponse("400", msg,
                    java.time.Instant.now().toString(), "", "");
        } catch (Exception ex) {
            log.error("Unhandled exception during QueryProductSubscription", ex);
            String msg = "UIV action QueryProductSubscription execution failed - Internal server error occurred";
            return new QueryProductSubscriptionResponse("500", msg + " - " + ex.getMessage(),
                    java.time.Instant.now().toString(), "", "");
        }
    }
}
