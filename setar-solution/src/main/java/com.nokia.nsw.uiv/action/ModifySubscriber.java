package com.nokia.nsw.uiv.action;

import com.nokia.nsw.uiv.framework.action.Action;
import com.nokia.nsw.uiv.framework.action.ActionContext;
import com.nokia.nsw.uiv.framework.action.HttpAction;
import com.nokia.nsw.uiv.repository.*;
import com.nokia.nsw.uiv.request.ModifySubscriberRequest;
import com.nokia.nsw.uiv.response.ModifySubscriberResponse;
import com.nokia.nsw.uiv.utils.Validations;

import com.nokia.nsw.uiv.model.service.Subscription;
import com.nokia.nsw.uiv.model.service.SubscriptionRepository;
import com.setar.uiv.model.product.Product;
import com.setar.uiv.model.product.ProductRepository;
import com.setar.uiv.model.product.CustomerFacingService;
import com.setar.uiv.model.product.CustomerFacingServiceRepository;
import com.setar.uiv.model.product.ResourceFacingService;
import com.setar.uiv.model.product.ResourceFacingServiceRepository;
import com.nokia.nsw.uiv.model.common.party.Customer;
import com.nokia.nsw.uiv.model.common.party.CustomerRepository;

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
public class ModifySubscriber implements HttpAction {

    private static final String ERROR_PREFIX = "UIV action ModifySubscriber execution failed - ";

    @Autowired
    private SubscriptionCustomRepository subscriptionCustomRepo;

    @Autowired
    private ProductCustomRepository productCustomRepo;

    @Autowired
    private CustomerFacingServiceCustomRepository cfsRepo;

    @Autowired
    private ResourceFacingServiceCustomRepository rfsRepo;

    @Autowired
    private CustomerCustomRepository customerRepo;

    @Override
    public Class<?> getActionClass() {
        return ModifySubscriberRequest.class;
    }

    @Override
    public Object doPost(ActionContext actionContext) {
        System.out.println("------------Test Trace # 1--------------- ModifySubscriber started");
        ModifySubscriberRequest req = (ModifySubscriberRequest) actionContext.getObject();

        try {
            // 1. Validate mandatory params
            try {
                Validations.validateMandatory(req.getSubscriberName(), "subscriberName");
                Validations.validateMandatory(req.getSubscriberNameOld(), "subscriberNameOld");
            } catch (Exception bre) {
                System.out.println("------------Test Trace # 2--------------- Missing mandatory param: " + bre.getMessage());
                return new ModifySubscriberResponse(
                        "400",
                        ERROR_PREFIX + "Missing mandatory parameter: " + bre.getMessage(),
                        Instant.now().toString()
                );
            }

            String oldSubscriberName = req.getSubscriberNameOld();
            String newSubscriberName = req.getSubscriberName();

            System.out.println("------------Test Trace # 3--------------- old=" + oldSubscriberName + ", new=" + newSubscriberName);

            boolean updatesApplied = false;

            // 3. Locate CFS containing old subscriber
            List<CustomerFacingService> cfsList1 = (List<CustomerFacingService>) cfsRepo.findAll();
            List<CustomerFacingService> cfsList = new ArrayList<>();
            for(CustomerFacingService cfs:cfsList1)
            {
                if(cfs.getProperties().get("SubscriberName").toString().contains(oldSubscriberName))
                {
                    cfsList.add(cfs);
                }
            }
            System.out.println("------------Test Trace # 4--------------- CFS candidates found: " + cfsList.size());

            for (CustomerFacingService cfs : cfsList) {
                String cfsName = cfs.getLocalName();
                if (!cfsName.contains(oldSubscriberName)) continue;

                System.out.println("------------Test Trace # 5--------------- Processing CFS: " + cfsName);

                // Derive RFS name
                String rfsName = cfsName.replace("CFS", "RFS");
                Optional<ResourceFacingService> rfsOpt = rfsRepo.findByDiscoveredName(rfsName);

                // Retrieve product linked (via properties or association)
                Optional<Product> productOpt = Optional.of(cfs.getContainingProduct()); // adjust to actual association
                Optional<Subscription> subsOpt = Optional.empty();
                Optional<Customer> oldCustOpt = Optional.empty();

                if (productOpt.isPresent()) {
                    Product prod = productOpt.get();
                    subsOpt = Optional.of(prod.getSubscription()); // adjust to actual association
                    oldCustOpt = Optional.of(subsOpt.get().getCustomer()); // adjust to actual association
                }

                // Try to find new subscriber
                Optional<Customer> newCustOpt = customerRepo.findByDiscoveredName(newSubscriberName);

                if (newCustOpt.isPresent()) {
                    Customer newCust = newCustOpt.get();
                    System.out.println("------------Test Trace # 6--------------- New subscriber found: " + newCust.getLocalName());

                    // Update subscription
                    if (subsOpt.isPresent()) {
                        Subscription subs = subsOpt.get();
                        String newSubName = subs.getLocalName().replace(oldSubscriberName, newSubscriberName);
                        subs.setDiscoveredName(newSubName);
                        subs.setCustomer(newCust);
                        subscriptionCustomRepo.save(subs);
                        updatesApplied = true;
                        System.out.println("------------Test Trace # 7--------------- Subscription updated: " + newSubName);
                    }

                    // Update product
                    if (productOpt.isPresent()) {
                        Product prod = productOpt.get();
                        String newProdName = prod.getLocalName().replace(oldSubscriberName, newSubscriberName);
                        prod.setLocalName(newProdName);
                        prod.setName(newProdName);
                        prod.setCustomer(newCust);
                        productCustomRepo.save(prod);
                        updatesApplied = true;
                        System.out.println("------------Test Trace # 8--------------- Product updated: " + newProdName);
                    }

                } else {
                    System.out.println("------------Test Trace # 9--------------- New subscriber not found → fallback mode");

                    // Fallback: update subscription, subscriber, product with renaming
                    if (subsOpt.isPresent()) {
                        Subscription subs = subsOpt.get();
                        String newSubName = subs.getLocalName().replace(oldSubscriberName, newSubscriberName);
                        subs.setLocalName(newSubName);
                        subs.setName(newSubName);
                        subscriptionCustomRepo.save(subs);
                        updatesApplied = true;
                        System.out.println("------------Test Trace # 10--------------- Subscription renamed (fallback): " + newSubName);
                    }

                    if (oldCustOpt.isPresent()) {
                        Customer oldCust = oldCustOpt.get();
                        String newName;
                        if (oldCust.getLocalName().contains("_")) {
                            String[] parts = oldCust.getLocalName().split("_");
                            newName = newSubscriberName + "_" + parts[1];
                        } else {
                            newName = oldCust.getLocalName().replace(oldSubscriberName, newSubscriberName);
                        }
                        oldCust.setLocalName(newName);
                        oldCust.setName(newName);

                        Map<String, Object> custProps = oldCust.getProperties() == null ? new HashMap<>() : new HashMap<>(oldCust.getProperties());
                        custProps.put("accountNumber", newSubscriberName);
                        oldCust.setProperties(custProps);

                        customerRepo.save(oldCust);
                        updatesApplied = true;
                        System.out.println("------------Test Trace # 11--------------- Subscriber updated (fallback): " + newName);
                    }

                    if (productOpt.isPresent()) {
                        Product prod = productOpt.get();
                        String newProdName = prod.getLocalName().replace(oldSubscriberName, newSubscriberName);
                        prod.setDiscoveredName(newProdName);
                        productCustomRepo.save(prod);
                        updatesApplied = true;
                        System.out.println("------------Test Trace # 12--------------- Product updated (fallback): " + newProdName);
                    }
                }

                // Update RFS
                if (rfsOpt.isPresent()) {
                    ResourceFacingService rfs = rfsOpt.get();
                    String newRfsName = rfs.getLocalName().replace(oldSubscriberName, newSubscriberName);
                    rfs.setDiscoveredName(newRfsName);
                    rfsRepo.save(rfs);
                    updatesApplied = true;
                    System.out.println("------------Test Trace # 13--------------- RFS updated: " + newRfsName);
                }

                // Update CFS
                String newCfsName = cfs.getLocalName().replace(oldSubscriberName, newSubscriberName);
                cfs.setDiscoveredName(newCfsName);
                cfsRepo.save(cfs);
                updatesApplied = true;
                System.out.println("------------Test Trace # 14--------------- CFS updated: " + newCfsName);
            }

            // 5. Generate response
            if (updatesApplied) {
                return new ModifySubscriberResponse("200",
                        "AccountNumber successfully updated",
                        Instant.now().toString());
            } else {
                return new ModifySubscriberResponse("404",
                        ERROR_PREFIX + "Error, No Account found.",
                        Instant.now().toString());
            }

        } catch (Exception ex) {
            log.error("Unhandled exception in ModifySubscriber", ex);
            return new ModifySubscriberResponse("500",
                    ERROR_PREFIX + "Internal server error occurred - " + ex.getMessage(),
                    Instant.now().toString());
        }
    }
}
