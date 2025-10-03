package com.nokia.nsw.uiv.action;

import com.nokia.nsw.uiv.exception.BadRequestException;
import com.nokia.nsw.uiv.framework.action.Action;
import com.nokia.nsw.uiv.framework.action.ActionContext;
import com.nokia.nsw.uiv.framework.action.HttpAction;
import com.nokia.nsw.uiv.framework.rda.Associations;
import com.nokia.nsw.uiv.model.resource.Resource;
import com.nokia.nsw.uiv.model.resource.logical.LogicalDevice;
import com.nokia.nsw.uiv.model.resource.logical.LogicalDeviceRepository;
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

import com.tailf.jnc.Device;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.kernel.api.query.SchemaIndexUsage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.Flow;

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
    @Autowired private LogicalDeviceRepository cbmDeviceRepository;

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
            Product childProd = null;
            Subscription childSubs = null;
            CustomerFacingService childCfs = null;
            ResourceFacingService childRfs = null;
            LogicalDevice childDevice = null;

            // Step 3: Iterate matching CFS
            for (CustomerFacingService cfs : cfsList) {
                String cfsName = cfs.getName();
                String rfsName = cfsName.replace("CFS", "RFS");
                String rfsGdn = Validations.getGlobalName(rfsName);
                Optional<ResourceFacingService> rfsOpt = rfsRepo.uivFindByGdn(rfsGdn,1);
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
                    newCust = new Customer();
                    newCust.setName(req.getSubscriberName());
                    newCust.setLocalName(req.getSubscriberName());
                    newCust.setContext(Constants.SETAR);
                    Map<String,Object>prop= oldCust.getPropertiesMap();
                    prop.put("AccountNumber",req.getSubscriberName());
                    newCust.setProperties(prop);
                    newCust.setSubscription(oldCust.getSubscription());
                    newCust.setProduct(oldCust.getProduct());
                    Set<Subscription> subs1 = oldCust.getSubscription();
                    Set<Product> prodSet = oldCust.getProduct();
                    for(Product p : prodSet){
                        if(p.getLocalName().contains(req.getServiceId())){
                            childProd = p;
                            childProd = prodRepo.uivFindByGdn(childProd.getGlobalName()).get();
                            childSubs = childProd.getSubscription();
                            Set<CustomerFacingService> cfsSet = childProd.getContainedCfs();
                            for(CustomerFacingService cf:cfsSet){
                                if(cf.getLocalName().contains(req.getServiceId())){
                                    childCfs = cfs;
                                    childCfs = cfsRepo.uivFindByGdn(childCfs.getGlobalName()).get();
                                    childRfs = rfsRepo.uivFindByGdn(childCfs.getGlobalName().replace("CFS","RFS")).get();
                                    break;
                                }
                            }
                            if(childRfs!=null){
                                break;
                            }
                        }
                    }
                    Set<Resource> usedResource = childRfs.getUsedResource();
                    for (Resource r : usedResource) {
                        if (r instanceof LogicalDevice) {
                            childDevice = (LogicalDevice)r;
                            cbmDeviceRepository.delete((LogicalDevice) r);
                        }
                    }
                    rfsRepo.delete(childRfs);
                    cfsRepo.delete(childCfs);
                    custRepo.delete(oldCust);
                }
                Customer saveCustomer = new Customer();
                saveCustomer.setLocalName(childSubs.getLocalName().replace(oldSubscriberName,subscriberName));
                saveCustomer.setContext(Constants.SETAR);
                saveCustomer.setProperties(childSubs.getProperties());
                custRepo.save(saveCustomer);
                // Step 5: Update Subscription
                Subscription saveSubs = new Subscription();
                saveSubs.setLocalName(childSubs.getLocalName());
                saveSubs.setName(childSubs.getName());
                saveSubs.setContext(Constants.SETAR);
                saveSubs.setKind(Constants.SETAR_KIND_SETAR_SUBSCRIPTION);
                saveSubs.setProperties(childSubs.getProperties());
                saveSubs.setCustomer(saveCustomer);
                String subtype = saveSubs.getProperties().get("serviceSubType").toString();
                if ("Broadband".equalsIgnoreCase(subtype) && req.getKenanUidNo() != null) {
                    saveSubs.getProperties().put("kenanUidNo", req.getKenanUidNo());
                }
                if ("IPTV".equalsIgnoreCase(subtype)) {
                    saveSubs.getProperties().put("cbmSubscriberId", req.getSubscriberName());
                }
                subsRepo.save(saveSubs);
                System.out.println("------Trace #4: Subscription updated");

                // Step 6: Update Product
                Product prodSave = new Product();
                prodSave.setKind(Constants.SETAR_KIND_SETAR_PRODUCT);
                prodSave.setName(childProd.getName().replace(req.getSubscriberNameOld(), req.getSubscriberName()));
                prodSave.setLocalName(childProd.getLocalName().replace(req.getSubscriberNameOld(), req.getSubscriberName()));
                prodSave.setSubscription(saveSubs);
                prodSave.setCustomer(saveCustomer);
                prodRepo.save(prodSave);
                System.out.println("------Trace #5: Product updated");

                // Step 7: Update Subscriber if fallback used
//                if (newCust == oldCust) {
//                    custRepo.save(newCust);
//                    System.out.println("------Trace #6: Subscriber renamed");
//                }

                // Step 8: Update CFS and RFS
                CustomerFacingService saveCfs = new CustomerFacingService();
                saveCfs.setLocalName(childCfs.getLocalName().replace(req.getSubscriberNameOld(), req.getSubscriberName()));
                saveCfs.setName(childCfs.getName().replace(req.getSubscriberNameOld(), req.getSubscriberName()));
                saveCfs.setContext(Constants.SETAR);
                saveCfs.setKind(Constants.SETAR_KIND_SETAR_CFS);
                saveCfs.setCustomer(saveCustomer);
                saveCfs.setContainingProduct(prodSave);
                cfsRepo.save(saveCfs);
                System.out.println("------Trace #7: CFS updated");

                if (rfsOpt.isPresent()) {
                    ResourceFacingService saveRfs = new ResourceFacingService();
                    saveRfs.setLocalName(childRfs.getLocalName().replace(req.getSubscriberNameOld(), req.getSubscriberName()));
                    saveRfs.setName(childRfs.getName().replace(req.getSubscriberNameOld(), req.getSubscriberName()));
                    saveRfs.setContext(Constants.SETAR);
                    saveRfs.setKind(Constants.SETAR_KIND_SETAR_RFS);
                    saveRfs.setContainingCfs(saveCfs);
                    rfsRepo.save(saveRfs);
                    System.out.println("------Trace #8: RFS updated");
                    LogicalDevice saveDevice = new LogicalDevice();
                    saveDevice.setLocalName(childDevice.getLocalName());
                    saveDevice.setName(childDevice.getName());
                    saveDevice.setContext(Constants.SETAR);
                    saveDevice.setKind(Constants.SETAR_KIND_STB_AP_CM_DEVICE);
                    saveDevice.addUsingService(saveRfs);
                    cbmDeviceRepository.save(saveDevice);
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
