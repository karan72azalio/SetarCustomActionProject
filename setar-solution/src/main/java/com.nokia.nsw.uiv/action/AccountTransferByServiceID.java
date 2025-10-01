package com.nokia.nsw.uiv.action;

import com.nokia.nsw.uiv.exception.BadRequestException;
import com.nokia.nsw.uiv.framework.action.Action;
import com.nokia.nsw.uiv.framework.action.ActionContext;
import com.nokia.nsw.uiv.framework.action.HttpAction;
import com.nokia.nsw.uiv.request.AccountTransferByServiceIDRequest;
import com.nokia.nsw.uiv.response.AccountTransferByServiceIDResponse;

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
public class AccountTransferByServiceID implements HttpAction {

    private static final String ERROR_PREFIX = "UIV action AccountTransferByServiceID execution failed - ";

    @Autowired private CustomerFacingServiceRepository cfsRepo;
    @Autowired private ResourceFacingServiceRepository rfsRepo;
    @Autowired private SubscriptionRepository subsRepo;
    @Autowired private ProductRepository prodRepo;
    @Autowired private CustomerRepository custRepo;

    @Override
    public Class<?> getActionClass() {
        return AccountTransferByServiceIDRequest.class;
    }

    @Override
    public Object doPost(ActionContext actionContext) {
        AccountTransferByServiceIDRequest req = (AccountTransferByServiceIDRequest) actionContext.getObject();
        System.out.println("------Trace #1: Starting AccountTransferByServiceID");

        try {
            // Step 1: Mandatory validations
            if (req.getSubscriberNameOld() == null || req.getSubscriberNameOld().isEmpty()
                    || req.getSubscriberName() == null || req.getSubscriberName().isEmpty()
                    || req.getServiceId() == null || req.getServiceId().isEmpty()) {
                return errorResponse("400", "Missing mandatory parameter(s)");
            }
            System.out.println("------Trace #2: Validated mandatory params");

            String oldSubscriberName = req.getSubscriberNameOld();
            String subscriberName = req.getSubscriberName();
            // Step 2: Search for CFS containing old subscriber and service ID
            Iterable<CustomerFacingService> cfsList1 = cfsRepo.findAll();
            List<CustomerFacingService> cfsList = new ArrayList<>();
            for(CustomerFacingService cfs:cfsList1)
            {
                Product p = cfs.getContainingProduct();
                String productGdn = p.getGlobalName();
                p = prodRepo.uivFindByGdn(productGdn).get();
                if(p.getCustomer().getName().contains(oldSubscriberName))
                {
                    cfsList.add(cfs);
                }
            }

            cfsList.removeIf(cfs -> !cfs.getName().contains(req.getServiceId()));
            if (cfsList.isEmpty()) {
                return errorResponse("404", "No entry found for update");
            }
            System.out.println("------Trace #3: Matching CFS found count=" + cfsList.size());

            boolean updated = false;

            // Step 3: Iterate matching CFS
            for (CustomerFacingService cfs : cfsList) {
                String cfsName = cfs.getName();
                String rfsName = cfsName.replace("CFS", "RFS");
                String rfsGdn = Validations.getGlobalName(rfsName);
                Optional<ResourceFacingService> rfsOpt = rfsRepo.uivFindByGdn(rfsGdn);
                Product prod = cfs.getContainingProduct();
                String productGdn = prod.getGlobalName();
                prod = prodRepo.uivFindByGdn(productGdn).get();
                Subscription subs = prod.getSubscription();
                String oldSubscriberGdn = Validations.getGlobalName(oldSubscriberName);
                Customer oldCust = custRepo.uivFindByGdn(oldSubscriberGdn).orElse(null);

                if (subs == null || prod == null || oldCust == null) {
                    continue;
                }

                // Step 4: Find or fallback new subscriber
                Customer newCust = custRepo.uivFindByGdn(Validations.getGlobalName(req.getSubscriberName())).orElse(null);
                if (newCust == null) {
                    newCust = oldCust;
                    newCust.setName(req.getSubscriberName());
                    newCust.setLocalName(req.getSubscriberName());
                    Map<String,Object>prop=new HashMap<>();
                    prop.put("AccountNumber",req.getSubscriberName());
                }

                // Step 5: Update Subscription
                subs.setName(subs.getName().replace(req.getSubscriberNameOld(), req.getSubscriberName()));
                subs.setCustomer(newCust);

                String subtype = subs.getProperties().get("ServiceSubtype").toString();
                if ("Broadband".equalsIgnoreCase(subtype) && req.getKenanUidNo() != null) {
                    subs.getProperties().put("kenanUidNo", req.getKenanUidNo());
                }
                if ("IPTV".equalsIgnoreCase(subtype)) {
                    subs.getProperties().put("cbmSubscriberId", req.getSubscriberName());
                }
                subsRepo.save(subs);
                System.out.println("------Trace #4: Subscription updated");

                // Step 6: Update Product
                prod.setName(prod.getName().replace(req.getSubscriberNameOld(), req.getSubscriberName()));
                prod.setCustomer(newCust);
                prodRepo.save(prod);
                System.out.println("------Trace #5: Product updated");

                // Step 7: Update Subscriber if fallback used
                if (newCust == oldCust) {
                    custRepo.save(newCust);
                    System.out.println("------Trace #6: Subscriber renamed");
                }

                // Step 8: Update CFS and RFS
                cfs.setName(cfsName.replace(req.getSubscriberNameOld(), req.getSubscriberName()));
                cfsRepo.save(cfs);
                System.out.println("------Trace #7: CFS updated");

                if (rfsOpt.isPresent()) {
                    ResourceFacingService rfs = rfsOpt.get();
                    rfs.setName(rfsName.replace(req.getSubscriberNameOld(), req.getSubscriberName()));
                    rfsRepo.save(rfs);
                    System.out.println("------Trace #8: RFS updated");
                }

                updated = true;
            }

            if (!updated) {
                return errorResponse("404", "Error, No Account found");
            }

            AccountTransferByServiceIDResponse resp = new AccountTransferByServiceIDResponse();
            resp.setStatus("200");
            resp.setMessage("AccountNumber successfully updated executed successfully.");
            resp.setTimestamp(Instant.now().toString());
            return resp;

        } catch (Exception ex) {
            log.error("Exception in AccountTransferByServiceID", ex);
            return errorResponse("500", "Unexpected error - " + ex.getMessage());
        }
    }

    private AccountTransferByServiceIDResponse errorResponse(String status, String msg) {
        AccountTransferByServiceIDResponse resp = new AccountTransferByServiceIDResponse();
        resp.setStatus(status);
        resp.setMessage(ERROR_PREFIX + msg);
        resp.setTimestamp(Instant.now().toString());
        return resp;
    }
}
