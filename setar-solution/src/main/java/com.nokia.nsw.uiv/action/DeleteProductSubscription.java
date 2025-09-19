package com.nokia.nsw.uiv.action;

import com.nokia.nsw.uiv.exception.AccessForbiddenException;
import com.nokia.nsw.uiv.exception.BadRequestException;
import com.nokia.nsw.uiv.exception.ModificationNotAllowedException;
import com.nokia.nsw.uiv.framework.action.Action;
import com.nokia.nsw.uiv.framework.action.ActionContext;
import com.nokia.nsw.uiv.framework.action.HttpAction;
import com.nokia.nsw.uiv.request.DeleteProductSubscriptionRequest;
import com.nokia.nsw.uiv.response.DeleteProductSubscriptionResponse;
import com.nokia.nsw.uiv.utils.Validations;
import com.setar.uiv.model.product.Product;
import com.setar.uiv.model.product.ProductRepository;
import com.setar.uiv.model.product.ResourceFacingService;
import com.setar.uiv.model.product.ResourceFacingServiceRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Optional;

@Component
@RestController
@Action
@Slf4j
public class DeleteProductSubscription implements HttpAction {

    protected static final String ACTION_LABEL = "DeleteProductSubscription";

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ResourceFacingServiceRepository rfsRepository;

    @Override
    public Class<?> getActionClass() {
        return DeleteProductSubscriptionRequest.class;
    }

    @Override
    public Object doPost(ActionContext actionContext) throws Exception {
        log.warn("Executing action: {}", ACTION_LABEL);

        DeleteProductSubscriptionRequest request = (DeleteProductSubscriptionRequest) actionContext.getObject();

        try {
            log.info("Mandatory parameter validation started...");
            Validations.validateMandatoryParams(request.getSubscriberName(), "subscriberName");
            Validations.validateMandatoryParams(request.getServiceID(), "serviceID");
            Validations.validateMandatoryParams(request.getProductType(), "productType");
            Validations.validateMandatoryParams(request.getComponentName(), "componentName");
            log.info("Mandatory parameter validation completed");

            // ========== Construct Product Name ==========
            String productNameStr = request.getServiceID() + "_" + request.getComponentName();
            if (productNameStr.length() > 100) {
                throw new BadRequestException("Product Name String exceeds 100 characters");
            }

            // ========== RFS Update ==========
            if (request.getFxOrderID() != null && !request.getFxOrderID().isEmpty()) {
                String rfsName = "RFS_" + request.getSubscriberName() + "_" + request.getServiceID();
                Optional<ResourceFacingService> optRfs = rfsRepository.uivFindByGdn(rfsName);

                if (optRfs.isPresent()) {
                    ResourceFacingService rfs = optRfs.get();
                    rfs.getProperties().put("transactionType", "DeleteProductSubscription");
                    rfs.getProperties().put("transactionId", request.getFxOrderID());
                    rfsRepository.save(rfs);
                    log.info("RFS updated successfully for {}", rfsName);
                } else {
                    log.warn("No RFS found for name {}", rfsName);
                }
            }

            // ========== Delete Product ==========
            Optional<Product> optProduct = productRepository.uivFindByGdn(productNameStr);

            if (optProduct.isPresent()) {
                Product product = optProduct.get();
                productRepository.delete(product);
                log.info("Deleted Product Subscription {}", productNameStr);

                return new DeleteProductSubscriptionResponse(
                        "200",
                        "UIV action DeleteProductSubscription executed successfully. Product Subscription Deleted.",
                        Instant.now().toString(),
                        productNameStr
                );
            } else {
                String msg = "UIV action DeleteProductSubscription execution failed - " +
                        "Error, Product Subscription with name " + productNameStr + " not found.";
                return new DeleteProductSubscriptionResponse("404", msg,
                        Instant.now().toString(), "");
            }

        } catch (BadRequestException bre) {
            String msg = "UIV action DeleteProductSubscription execution failed - Missing mandatory parameter : " + bre.getMessage();
            return new DeleteProductSubscriptionResponse("400", msg,
                    Instant.now().toString(), "");
        } catch (Exception ex) {
            log.error("Unhandled exception during DeleteProductSubscription", ex);
            String msg = "UIV action DeleteProductSubscription execution failed - Error while deleting Product Subscription " + ex.getMessage();
            return new DeleteProductSubscriptionResponse("500", msg,
                    Instant.now().toString(), "");
        }
    }
}

