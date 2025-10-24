package com.nokia.nsw.uiv.action;

import com.nokia.nsw.uiv.framework.action.Action;
import com.nokia.nsw.uiv.framework.action.ActionContext;
import com.nokia.nsw.uiv.framework.action.HttpAction;
import com.nokia.nsw.uiv.model.resource.logical.LogicalDevice;
import com.nokia.nsw.uiv.repository.*;
import com.nokia.nsw.uiv.request.ModifyServiceIdRequest;
import com.nokia.nsw.uiv.response.ModifyServiceIdResponse;
import com.nokia.nsw.uiv.utils.Validations;

import com.setar.uiv.model.product.CustomerFacingService;
import com.setar.uiv.model.product.CustomerFacingServiceRepository;
import com.setar.uiv.model.product.ResourceFacingService;
import com.setar.uiv.model.product.ResourceFacingServiceRepository;
import com.setar.uiv.model.product.Product;
import com.setar.uiv.model.product.ProductRepository;
import com.nokia.nsw.uiv.model.service.Subscription;
import com.nokia.nsw.uiv.model.service.SubscriptionRepository;
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
public class ModifyServiceId implements HttpAction {

    private static final String ERROR_PREFIX = "UIV action ModifyServiceId execution failed - ";

    @Autowired
    private CustomerFacingServiceCustomRepository cfsRepo;

    @Autowired
    private ResourceFacingServiceCustomRepository rfsRepo;

    @Autowired
    private ProductCustomRepository productRepo;

    @Autowired
    private SubscriptionCustomRepository subscriptionRepo;

    @Autowired
    private CustomerCustomRepository customerRepo;

    @Autowired
    private LogicalDeviceCustomRepository logicalDeviceRepository;

    @Override
    public Class<?> getActionClass() {
        return ModifyServiceIdRequest.class;
    }

    @Override
    public Object doPatch(ActionContext actionContext) {
        System.out.println("------------Test Trace # 1--------------- ModifyServiceId started");
        ModifyServiceIdRequest req = (ModifyServiceIdRequest) actionContext.getObject();

        try {
            // 1. Mandatory validation
            try {
                Validations.validateMandatory(req.getServiceId(), "serviceId");
                Validations.validateMandatory(req.getServiceIdNew(), "serviceIdNew");
            } catch (Exception bre) {
                System.out.println("------------Test Trace # 2--------------- Missing mandatory param: " + bre.getMessage());
                return new ModifyServiceIdResponse(
                        "400",
                        ERROR_PREFIX + "Missing mandatory parameter: " + bre.getMessage(),
                        Instant.now().toString()
                );
            }

            String oldServiceId = req.getServiceId();
            String newServiceId = req.getServiceIdNew();
            boolean updatesApplied = false;

            System.out.println("------------Test Trace # 3--------------- old=" + oldServiceId + ", new=" + newServiceId);

            // 2. Locate CFS candidates
            List<CustomerFacingService> cfsList1 = (List<CustomerFacingService>) cfsRepo.findAll();
            List<CustomerFacingService> cfsList = new ArrayList<>();
            for(CustomerFacingService cfs:cfsList1)
            {
                if(cfs.getDiscoveredName().contains(oldServiceId))
                {
                    cfsList.add(cfs);
                }
            }
            System.out.println("------------Test Trace # 4--------------- Found CFS candidates: " + cfsList.size());

            for (CustomerFacingService cfs : cfsList) {
                String cfsName = cfs.getDiscoveredName();

                // Matching Rule A/B
                boolean matches = false;
                if (oldServiceId.contains("_")) {
                    int first = cfsName.indexOf("_");
                    int last = cfsName.lastIndexOf("_");
                    if (first >= 0 && last > first) {
                        String between = cfsName.substring(first + 1, last);
                        if (between.equalsIgnoreCase(oldServiceId)) {
                            matches = true;
                        }
                    }
                } else {
                    String[] tokens = cfsName.split("_");
                    if (tokens.length >= 3 && tokens[2].equalsIgnoreCase(oldServiceId)) {
                        matches = true;
                    }
                }

                if (!matches) continue;
                System.out.println("------------Test Trace # 5--------------- Processing CFS: " + cfsName);

                // Locate RFS
                String rfsName = cfsName.replace("CFS", "RFS");
                Optional<ResourceFacingService> rfsOpt = rfsRepo.findByDiscoveredName(rfsName);

                // Retrieve product
                Optional<Product> productOpt =productRepo.findByDiscoveredName(cfs.getContainingProduct().getDiscoveredName()); // adjust association
                Optional<Subscription> subsOpt = Optional.empty();
                Optional<Customer> custOpt = Optional.empty();

                if (productOpt.isPresent()) {
                    Product prod = productOpt.get();
                    subsOpt = Optional.of(prod.getSubscription());
                    subsOpt = subscriptionRepo.findByDiscoveredName(subsOpt.get().getDiscoveredName());
                            // adjust association
                    custOpt = Optional.of(subsOpt.get().getCustomer()); // adjust association
                }

                // Update subscription
                if (subsOpt.isPresent()) {
                    Subscription subs = subscriptionRepo.findByDiscoveredName(subsOpt.get().getDiscoveredName()).get();
                    String newName = subs.getDiscoveredName().replace(oldServiceId, newServiceId);
                    subs.setDiscoveredName(newName);

                    Map<String, Object> props = subs.getProperties() == null ? new HashMap<>() : new HashMap<>(subs.getProperties());
                    props.put("serviceID", newServiceId);
                    subs.setProperties(props);
                    subscriptionRepo.save(subs);
                    updatesApplied = true;
                    System.out.println("------------Test Trace # 6--------------- Subscription updated: " + newName);
                }

                // Update product
                if (productOpt.isPresent()) {
                    Product prod = productRepo.findByDiscoveredName(productOpt.get().getDiscoveredName()).get();
                    String newName = prod.getDiscoveredName().replace(oldServiceId, newServiceId);
                    prod.setDiscoveredName(newName);
                    productRepo.save(prod);
                    updatesApplied = true;
                    System.out.println("------------Test Trace # 7--------------- Product updated: " + newName);
                }

                // Update subscriber
                if (custOpt.isPresent()) {
                    Customer cust = customerRepo.findByDiscoveredName(custOpt.get().getDiscoveredName()).get();
                    String newName = cust.getDiscoveredName().replace(oldServiceId, newServiceId);
                    cust.setDiscoveredName(newName);

                    Map<String, Object> custProps = cust.getProperties() == null ? new HashMap<>() : new HashMap<>(cust.getProperties());
                    custProps.put("accountNumber", newServiceId);
                    cust.setProperties(custProps);

                    customerRepo.save(cust);
                    updatesApplied = true;
                    System.out.println("------------Test Trace # 8--------------- Subscriber updated: " + newName);
                }

                // Update RFS + resources
                if (rfsOpt.isPresent()) {
                    ResourceFacingService rfs = rfsRepo.findByDiscoveredName(rfsOpt.get().getDiscoveredName()).get();

                    rfs.getUsedResource().forEach(res -> {
                        if (res instanceof LogicalDevice) {
                            LogicalDevice ont = (LogicalDevice) res;
                            Map<String,Object> props = ont.getProperties();
                            if (oldServiceId.equals(props.get("potsPort1Number")!=null?props.get("potsPort1Number").toString():null)) {
                                props.put("potsPort1Number",newServiceId);
                            }
                            if (oldServiceId.equals(props.get("potsPort1Number")!=null?props.get("potsPort1Number").toString():null)) {
                                props.put("potsPort1Number",newServiceId);
                            }
                            logicalDeviceRepository.save(ont);
                            System.out.println("------------Test Trace # 9--------------- ONT updated: " + ont.getDiscoveredName());
                        } else if (res instanceof LogicalDevice) {
                            LogicalDevice cbm = (LogicalDevice) res;
                            String newDevName = cbm.getDiscoveredName().replace(oldServiceId, newServiceId);
                            cbm.setDiscoveredName(newDevName);
                            System.out.println("------------Test Trace # 10--------------- CBM updated: " + cbm.getDiscoveredName());
                        }
                    });

                    String newRfsName = rfs.getDiscoveredName().replace(oldServiceId, newServiceId);
                    rfs.setDiscoveredName(newRfsName);
                    rfsRepo.save(rfs);
                    updatesApplied = true;
                    System.out.println("------------Test Trace # 11--------------- RFS updated: " + newRfsName);
                }

                // Update CFS
                cfs = cfsRepo.findByDiscoveredName(cfs.getDiscoveredName()).get();
                String newCfsName = cfs.getDiscoveredName().replace(oldServiceId, newServiceId);
                cfs.setDiscoveredName(newCfsName);
                cfsRepo.save(cfs);
                updatesApplied = true;
                System.out.println("------------Test Trace # 12--------------- CFS updated: " + newCfsName);
            }

            // 5. Generate response
            if (updatesApplied) {
                return new ModifyServiceIdResponse("200",
                        "ServiceID successfully updated",
                        Instant.now().toString());
            } else {
                return new ModifyServiceIdResponse("404",
                        ERROR_PREFIX + "Error, No Service found.",
                        Instant.now().toString());
            }

        } catch (Exception ex) {
            log.error("Unhandled exception in ModifyServiceId", ex);
            return new ModifyServiceIdResponse("500",
                    ERROR_PREFIX + "Internal server error occurred - " + ex.getMessage(),
                    Instant.now().toString());
        }
    }
}
