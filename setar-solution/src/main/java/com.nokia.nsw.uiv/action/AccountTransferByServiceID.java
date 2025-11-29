package com.nokia.nsw.uiv.action;

import com.nokia.nsw.uiv.framework.action.Action;
import com.nokia.nsw.uiv.framework.action.ActionContext;
import com.nokia.nsw.uiv.framework.action.HttpAction;
import com.nokia.nsw.uiv.model.resource.Resource;
import com.nokia.nsw.uiv.model.resource.logical.LogicalDevice;
import com.nokia.nsw.uiv.model.resource.logical.LogicalDeviceRepository;
import com.nokia.nsw.uiv.repository.*;
import com.nokia.nsw.uiv.request.AccountTransferByServiceIDRequest;
import com.nokia.nsw.uiv.response.AccountTransferByServiceIDResponse;

import com.nokia.nsw.uiv.utils.Constants;
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
    protected static final String ACTION_LABEL = Constants.ACCOUNT_TRANSFER_BY_SERVICE_ID;
    private static final String ERROR_PREFIX = "UIV action AccountTransferByServiceID execution failed - ";

    @Autowired private CustomerFacingServiceCustomRepository cfsRepo;
    @Autowired private ResourceFacingServiceCustomRepository rfsRepo;
    @Autowired private SubscriptionCustomRepository subsRepo;
    @Autowired private ProductCustomRepository prodRepo;
    @Autowired private CustomerCustomRepository custRepo;
    @Autowired private LogicalDeviceCustomRepository cbmDeviceRepository;

    @Override
    public Class<?> getActionClass() {
        return AccountTransferByServiceIDRequest.class;
    }

    @Override
    public Object doPost(ActionContext actionContext) {
        log.warn(Constants.EXECUTING_ACTION, ACTION_LABEL);
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
                p = prodRepo.findByDiscoveredName(p.getDiscoveredName()).get();
                if(p.getCustomer().getDiscoveredName().contains(oldSubscriberName))
                {
                    log.error("Cfs found containing oldSuscriberName: "+cfs.getDiscoveredName());
                    cfsList.add(cfs);
                }
            }

            cfsList.removeIf(cfs -> !cfs.getDiscoveredName().contains(req.getServiceId()));
            if (cfsList.isEmpty()) {
                return errorResponse("404", "No entry found for update");
            }
            System.out.println("------Trace #3: Matching CFS found count=" + cfsList.size());

            boolean updated = false;
            Product childProd = null;
            Subscription childSubs = null;
            CustomerFacingService childCfs = null;
            ResourceFacingService childRfs = null;
            LogicalDevice childDevice = null;

            // Step 3: Iterate matching CFS
            for (CustomerFacingService cfs : cfsList) {
                String cfsName = cfs.getDiscoveredName();
                String rfsName = cfsName.replace("CFS", "RFS");
                Optional<ResourceFacingService> rfsOpt = rfsRepo.findByDiscoveredName(rfsName);
                ResourceFacingService rfs = rfsOpt.get();
                Product prod = cfs.getContainingProduct();
                prod = prodRepo.findByDiscoveredName(prod.getDiscoveredName()).get();
                Subscription subs = prod.getSubscription();
                Customer oldCust = custRepo.findByDiscoveredName(oldSubscriberName).orElse(null);
//                Customer oldCust1 = customerCustomRepository.findByDiscoveredName(oldSubscriberName);

                if (subs == null || prod == null || oldCust == null) {
                    continue;
                }
                try{
                    if(subs!=null){
                        subs.setDiscoveredName(oldCust.getDiscoveredName().replace(oldSubscriberName,subscriberName));
                        Map<String, Object> subsProps = subs.getProperties();
                        String serviceSubType = subsProps.get("serviceSubType")!=null?subsProps.get("serviceSubType").toString():"";
                        if(serviceSubType!=null && serviceSubType.equalsIgnoreCase("Broadband")){
                            subsProps.put("kenanSubscriberId",req.getKenanUidNo());
                        }else if(serviceSubType!=null && serviceSubType.equalsIgnoreCase("IPTV")){
                            subsProps.put("subscriberId_cableModem",subscriberName);
                        }
                        subs.setProperties(subsProps);
                        log.error("Subscription updated: "+subs.getDiscoveredName());
                        subsRepo.save(subs,2);
                    }
                    if(prod!=null){
                        prod = prodRepo.findByDiscoveredName(prod.getDiscoveredName()).get();
                        prod.setDiscoveredName(prod.getDiscoveredName().replace(oldSubscriberName,subscriberName));
                        log.error("Product updated: "+prod.getDiscoveredName());
                        prodRepo.save(prod,2);
                    }
                    if(oldCust!=null){
                        oldCust = custRepo.findByDiscoveredName(oldCust.getDiscoveredName()).get();
                        oldCust.setDiscoveredName(oldCust.getDiscoveredName().replace(oldSubscriberName,subscriberName));
                        log.error("Subscriber updated: "+oldCust.getDiscoveredName());
                        custRepo.save(oldCust,2);
                    }

                }catch (Exception e){
                    if(subs!=null){
                        subs.setDiscoveredName(oldCust.getDiscoveredName().replace(oldSubscriberName,subscriberName));
                        Map<String, Object> subsProps = subs.getProperties();
                        String serviceSubType = subsProps.get("serviceSubType")!=null?subsProps.get("serviceSubType").toString():"";
                        if(serviceSubType!=null && serviceSubType.equalsIgnoreCase("Broadband")){
                            subsProps.put("kenanSubscriberId",req.getKenanUidNo());
                        }else if(serviceSubType!=null && serviceSubType.equalsIgnoreCase("IPTV")){
                            subsProps.put("subscriberId_cableModem",subscriberName);
                        }
                        log.error("Subscription updated: "+subs.getDiscoveredName());
                        subs.setProperties(subsProps);
                        subsRepo.save(subs,2);
                    }
                    if(oldCust != null){
                        if(oldCust.getDiscoveredName().contains(Constants.UNDER_SCORE)){

                            String[] subscriberNameArray = oldCust.getDiscoveredName().split(Constants.UNDER_SCORE);
                            if(subscriberNameArray.length>1){
                                String	ontSerial=subscriberNameArray[1];
                                oldCust.setDiscoveredName(subscriberName+Constants.UNDER_SCORE + ontSerial);
                            }
                        }else{
                            oldCust.setDiscoveredName(oldCust.getDiscoveredName().replace(oldSubscriberName, subscriberName));
                        }
                        Map<String,Object> oldCustProps = oldCust.getProperties();
                        oldCustProps.put("accountNumber",subscriberName);
                        log.error("Subscriber updated: "+oldCust.getDiscoveredName());
                        oldCust.setProperties(oldCustProps);
                        custRepo.save(oldCust,2);
                    }

                    if(prod != null){
                        prod = prodRepo.findByDiscoveredName(prod.getDiscoveredName()).get();
                        prod.setDiscoveredName(prod.getDiscoveredName().replace(oldSubscriberName,subscriberName));
                        log.error("Product updated: "+prod.getDiscoveredName());
                        prodRepo.save(prod,2);
                    }
                }
                if(cfs!=null){
                    cfs = cfsRepo.findByDiscoveredName(cfs.getDiscoveredName()).get();
                    cfs.setDiscoveredName(cfs.getDiscoveredName().replace(oldSubscriberName,subscriberName));
                    log.error("CFS updated: "+cfs.getDiscoveredName());
                    cfsRepo.save(cfs,2);
                }
                if(rfs!=null){
                    rfs = rfsRepo.findByDiscoveredName(rfs.getDiscoveredName()).get();
                    rfs.setDiscoveredName(rfs.getDiscoveredName().replace(oldSubscriberName,subscriberName));
                    log.error("RFS updated: "+rfs.getDiscoveredName());
                    rfsRepo.save(rfs,2);
                }
                updated = true;
            }

            if (!updated) {
                return errorResponse("404", "Error, No Account found");
            }
            log.info(Constants.ACTION_COMPLETED);
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
