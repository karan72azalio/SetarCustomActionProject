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
import com.nokia.nsw.uiv.repository.CustomerCustomRepository;
import com.nokia.nsw.uiv.repository.ProductCustomRepository;
import com.nokia.nsw.uiv.repository.SubscriptionCustomRepository;
import com.nokia.nsw.uiv.request.CreateProductSubscriptionRequest;
import com.nokia.nsw.uiv.response.CreateProductSubscriptionResponse;
import com.nokia.nsw.uiv.utils.Constants;
import com.nokia.nsw.uiv.utils.Validations;
import com.setar.uiv.model.product.Product;
import com.setar.uiv.model.product.ProductRepository;
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
public class CreateProductSubscription implements HttpAction {

    protected static final String ACTION_LABEL = "CreateProductSubscription";

    @Autowired
    private CustomerCustomRepository subscriberRepository;

    @Autowired
    private SubscriptionCustomRepository subscriptionRepository;

    @Autowired
    private ProductCustomRepository productRepository;

    @Override
    public Class<?> getActionClass() {
        return CreateProductSubscriptionRequest.class;
    }

    @Override
    public Object doPost(ActionContext actionContext) throws Exception {
        log.warn(Constants.EXECUTING_ACTION, ACTION_LABEL);

        CreateProductSubscriptionRequest request = (CreateProductSubscriptionRequest) actionContext.getObject();

        try {
            log.info("Mandatory parameter validation started...");
            try{
                Validations.validateMandatoryParams(request.getSubscriberName(), "subscriberName");
                Validations.validateMandatoryParams(request.getProductType(), "productType");
                Validations.validateMandatoryParams(request.getServiceID(), "serviceID");
                Validations.validateMandatoryParams(request.getComponentName(), "componentName");
                Validations.validateMandatoryParams(request.getProductVariant(), "productVariant");
                Validations.validateMandatoryParams(request.getProduct(), "product");
                Validations.validateMandatoryParams(request.getReferenceID(), "referenceID");
            }catch (BadRequestException bre) {
                return new CreateProductSubscriptionResponse("400", Constants.ERROR_PREFIX + "Missing mandatory parameter : " + bre.getMessage(),
                        java.time.Instant.now().toString(), "","");
            }

            log.info("Mandatory parameter validation completed");

            // ================== Subscriber ==================
            String subscriberName = request.getSubscriberName();
            if (subscriberName.length() > 100) {
                throw new BadRequestException("Subscriber name too long");
            }

            Optional<Customer> optSubscriber = subscriberRepository.findByDiscoveredName(subscriberName);
            Customer subscriber;
            if (optSubscriber.isPresent()) {
                subscriber = optSubscriber.get();
                log.info("Found existing subscriber: {}", subscriberName);
            } else {
                subscriber = new Customer();
                subscriber.setLocalName(Validations.encryptName(subscriberName));
                subscriber.setDiscoveredName(subscriberName);
                subscriber.setKind("SetarSubscriber");
                subscriber.setContext(Constants.SETAR);
                Map<String, Object> props = new HashMap<>();
                props.put("name", subscriberName);
                props.put("status", "Active");
                props.put("type", "Regular");
                subscriber.setProperties(props);
                subscriberRepository.save(subscriber, 2);
                log.info("Created new subscriber: {}", subscriberName);
            }

            // ================== Subscription ==================
            String subscriptionName = subscriberName + "_" + request.getServiceID();
            if (subscriptionName.length() > 100) {
                throw new BadRequestException("Subscription name too long");
            }

            Optional<Subscription> optSubscription = subscriptionRepository.findByDiscoveredName(subscriptionName);
            Subscription subscription;
            if (optSubscription.isPresent()) {
                subscription = optSubscription.get();
                log.info("Found existing subscription: {}", subscriptionName);
            } else {
                subscription = new Subscription();
                subscription.setLocalName(Validations.encryptName(subscriptionName));
                subscription.setDiscoveredName(subscriptionName);
                subscription.setKind("SetarSubscription");
                subscription.setContext(Constants.SETAR);
                Map<String, Object> props = new HashMap<>();
                props.put("name", subscriptionName);
                props.put("status", "Active");
                props.put("serviceID", request.getServiceID());
                subscription.setProperties(props);
                subscription.setCustomer(subscriber);
                subscriptionRepository.save(subscription, 2);
                log.info("Created new subscription: {}", subscriptionName);
            }

            // ================== Product ==================
            String productName = request.getServiceID() + "_" + request.getComponentName();
            if (productName.length() > 100) {
                throw new BadRequestException("Product name too long");
            }

            Optional<Product> optProduct = productRepository.findByDiscoveredName(productName);
            Product product;
            if (optProduct.isPresent()) {
                product = optProduct.get();
                log.info("Found existing product: {}", productName);
            } else {
                product = new Product();
                product.setLocalName(Validations.encryptName(productName));
                product.setDiscoveredName(productName);
                product.setKind("SetarProduct");
                product.setContext(Constants.SETAR);
                Map<String, Object> props = new HashMap<>();
                props.put("name", productName);
                props.put("status", "Active");
                props.put("type", request.getProductType());
                props.put("productId", request.getReferenceID());
                props.put("catalogItemName", request.getProduct());
                props.put("catalogItemVersion", request.getProductVariant());
                product.setProperties(props);
                product.setCustomer(subscriber);
                product.setSubscription(subscription);
                productRepository.save(product, 2);
                log.info("Created new product: {}", productName);
            }

            // ================== Success Response ==================
            return new CreateProductSubscriptionResponse(
                    "201",
                    "Fibernet service created",
                    java.time.Instant.now().toString(),
                    subscriptionName,
                    productName
            );

        } catch (BadRequestException bre) {
            String msg = "UIV action CreateProductSubscription execution failed - " + bre.getMessage();
            return new CreateProductSubscriptionResponse("400", msg, java.time.Instant.now().toString(), "", "");
        } catch (AccessForbiddenException | ModificationNotAllowedException ex) {
            String msg = "UIV action CreateProductSubscription execution failed - " + ex.getMessage();
            return new CreateProductSubscriptionResponse("403", msg, java.time.Instant.now().toString(), "", "");
        } catch (Exception ex) {
            String msg = "UIV action CreateProductSubscription execution failed - Internal server error occurred";
            return new CreateProductSubscriptionResponse("500", msg + " - " + ex.getMessage(),
                    java.time.Instant.now().toString(), "", "");
        }
    }
}
