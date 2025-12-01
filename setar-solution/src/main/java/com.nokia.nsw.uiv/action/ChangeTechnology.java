package com.nokia.nsw.uiv.action;

import com.nokia.nsw.uiv.exception.AccessForbiddenException;
import com.nokia.nsw.uiv.exception.BadRequestException;
import com.nokia.nsw.uiv.exception.ModificationNotAllowedException;
import com.nokia.nsw.uiv.framework.action.Action;
import com.nokia.nsw.uiv.framework.action.ActionContext;
import com.nokia.nsw.uiv.framework.action.HttpAction;
import com.nokia.nsw.uiv.model.resource.logical.LogicalInterface;
import com.nokia.nsw.uiv.model.resource.logical.LogicalInterfaceRepository;
import com.nokia.nsw.uiv.repository.*;
import com.nokia.nsw.uiv.request.AssociateResourcesRequest;
import com.nokia.nsw.uiv.request.ChangeTechnologyRequest;
import com.nokia.nsw.uiv.response.ChangeTechnologyResponse;
import com.nokia.nsw.uiv.utils.Constants;
import com.nokia.nsw.uiv.utils.Validations;

import com.nokia.nsw.uiv.model.common.party.Customer;
import com.nokia.nsw.uiv.model.service.Subscription;
import com.setar.uiv.model.product.Product;
import com.setar.uiv.model.product.CustomerFacingService;
import com.setar.uiv.model.product.ResourceFacingService;
import com.nokia.nsw.uiv.model.resource.logical.LogicalDevice;

import com.nokia.nsw.uiv.model.common.party.CustomerRepository;
import com.nokia.nsw.uiv.model.service.SubscriptionRepository;
import com.setar.uiv.model.product.CustomerFacingServiceRepository;
import com.setar.uiv.model.product.ResourceFacingServiceRepository;
import com.nokia.nsw.uiv.model.resource.logical.LogicalDeviceRepository;
import com.setar.uiv.model.product.ProductRepository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
@RestController
@Action
@Slf4j
public class ChangeTechnology implements HttpAction {
    protected static final String ACTION_LABEL = Constants.CHANGE_TECHNOLOGY;
    private static final String ERROR_PREFIX = "UIV action ChangeTechnology execution failed - ";

    @Autowired
    private CustomerCustomRepository customerRepo;

    @Autowired
    private SubscriptionCustomRepository subscriptionRepo;

    @Autowired
    private ProductCustomRepository productRepo;

    @Autowired
    private CustomerFacingServiceCustomRepository cfsRepo;

    @Autowired
    private ResourceFacingServiceCustomRepository rfsRepo;

    @Autowired
    private LogicalDeviceCustomRepository logicalDeviceRepo;

    @Autowired
    private LogicalDeviceCustomRepository cpeRepo;

    @Autowired
    private LogicalInterfaceCustomRepository vlanRepo;

    @Autowired
    private LogicalDeviceCustomRepository cbmRepo;


    @Override
    public Class<?> getActionClass() { return ChangeTechnologyRequest.class; }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Object doPost(ActionContext actionContext) {
        log.warn(Constants.EXECUTING_ACTION, ACTION_LABEL);
        log.info("Executing ChangeTechnology action...");
        ChangeTechnologyRequest req = (ChangeTechnologyRequest) actionContext.getObject();

        try {
            // Declare variables from req
            String subscriberName   = req.getSubscriberName();
            String productSubtype   = req.getProductSubtype();
            String serviceId        = req.getServiceId();
            String fxOrderId        = req.getFxOrderId();
            String ontSN            = req.getOntSN();
            String ontMacAddr       = req.getOntMacAddr();
            String cbmSn            = req.getCbmSn();
            String qosProfile       = req.getQosProfile();
            String oltName          = req.getOltName();
            String templateNameOnt  = req.getTemplateNameOnt();
            String templateNameVeip = req.getTemplateNameVeip();
            String templateNameHsi  = req.getTemplateNameHsi();
            String templateNameIptv = req.getTemplateNameIptv();
            String templateNameIgmp = req.getTemplateNameIgmp();
            String menm             = req.getMenm();
            String vlanId           = req.getVlanId();
            String ontModel         = req.getOntModel();
            String cbmMac           = req.getCbmMac();
            String hhid             = req.getHhid();

// Validate mandatory parameters
            try {
                log.info(Constants.MANDATORY_PARAMS_VALIDATION_STARTED);
                Validations.validateMandatoryParams(subscriberName, "subscriberName");
                Validations.validateMandatoryParams(productSubtype, "productSubtype");
                Validations.validateMandatoryParams(serviceId, "serviceId");
                Validations.validateMandatoryParams(ontSN, "ontSN");
                Validations.validateMandatoryParams(ontMacAddr, "ontMacAddr");
                Validations.validateMandatoryParams(cbmSn, "cbmSn");
                Validations.validateMandatoryParams(oltName, "oltName");
                Validations.validateMandatoryParams(menm, "menm");
                Validations.validateMandatoryParams(vlanId, "vlanId");
                Validations.validateMandatoryParams(ontModel, "ontModel");
                Validations.validateMandatoryParams(cbmMac, "cbmMac");
                log.info(Constants.MANDATORY_PARAMS_VALIDATION_COMPLETED);
            } catch (BadRequestException bre) {
                return new ChangeTechnologyResponse(
                        "400",
                        Constants.ERROR_PREFIX + "Missing mandatory parameter : " + bre.getMessage(),
                        Instant.now().toString(),
                        "",
                        ""
                );
            }



            // 2. Prepare states - in UIV we typically store as properties; if there are dedicated entities, repo lookups would be used.
            // Legacy ensures existence of SubscriberStatus/SubscriberType/Admin/Operational state entities.
            // In UIV mapped model we set properties accordingly (create missing objects handled by repositories when needed).

            // 3. Prepare names
            String subscriptionName = subscriberName + Constants.UNDER_SCORE  + serviceId;
            String cfsName = "CFS" + Constants.UNDER_SCORE + subscriptionName;
            String rfsName = "RFS" + Constants.UNDER_SCORE + subscriptionName;
            String cbmName = "CBM" +cbmSn;
            String mgmtVlanName = menm + Constants.UNDER_SCORE  + vlanId;
            String ontName ="ONT" + ontSN;
            String subscriberNameFibernet = subscriberName + Constants.UNDER_SCORE  + ontSN;
            String subscriberNameCbmKey = subscriberName + Constants.UNDER_SCORE  + cbmMac.replace(":", "");

            // 4. Update existing subscriber (only when productSubType == Fibernet)
            if ("Fibernet".equalsIgnoreCase(productSubtype)) {
                if (subscriberNameFibernet.length() > 100) {
                    return new ChangeTechnologyResponse("400", ERROR_PREFIX + "Subscriber name too long", Instant.now().toString(),"","");
                }
                // Try find CBM-keyed subscriber
                Optional<Customer> maybeCbmSubscriber = customerRepo.findByDiscoveredName(subscriberNameCbmKey);
                if (maybeCbmSubscriber.isPresent()) {
                    Customer cbmSubscriber = maybeCbmSubscriber.get();
                    cbmSubscriber.setDiscoveredName(subscriberNameFibernet);
                    cbmSubscriber.setContext("Setar");
                    cbmSubscriber.setKind("SetarSubscriber");
                    Map<String, Object> props = cbmSubscriber.getProperties() != null ? cbmSubscriber.getProperties() : new HashMap<>();
                    props.put("subscriberStatus", "Active");
                    props.put("subscriberType", "Regular");
                    props.put("accountNumber", subscriberName);
                    if (hhid != null) props.put("householdId", hhid);
                    cbmSubscriber.setProperties(props);
                    customerRepo.save(cbmSubscriber);
                }
            }

            // 5. Update existing subscription (if exists)
            Optional<Subscription> maybeSubscription = subscriptionRepo.findByDiscoveredName(subscriptionName);
            Subscription subscription = null;
            if (maybeSubscription.isPresent()) {
                subscription = maybeSubscription.get();
                Map<String, Object> subProps = subscription.getProperties() != null ? subscription.getProperties() : new HashMap<>();
                subProps.put("serviceLink", "ONT");
                subProps.put("macAddress", ontMacAddr);
                subProps.put("serviceSN", ontSN);
                subProps.put("serviceSubtype", "Broadband");
                if ("Fibernet".equalsIgnoreCase(productSubtype)) {
                    if (qosProfile != null) subProps.put("veipQosSessionProfile", qosProfile);
                    subscription.setDiscoveredName(subscriptionName + Constants.UNDER_SCORE  + ontSN);
                    // link to subscriber updated earlier if present
                    String gdnFibernet = Validations.getGlobalName(subscriberNameFibernet);
                    Optional<Customer> maybeSub = customerRepo.findByDiscoveredName(subscriberNameFibernet);
                    maybeSub.ifPresent(subscription::setCustomer);
                }
                subscription.setProperties(subProps);
                subscriptionRepo.save(subscription);
            }

            // 6. Update existing CFS (if exists)
            Optional<CustomerFacingService> maybeCfs = cfsRepo.findByDiscoveredName(cfsName);
            if (maybeCfs.isPresent() && "Fibernet".equalsIgnoreCase(productSubtype)) {
                CustomerFacingService cfs = maybeCfs.get();
                cfs.setDiscoveredName(cfs.getLocalName() + Constants.UNDER_SCORE  + ontSN);
                if (fxOrderId != null) {
                    Map<String, Object> p = cfs.getProperties() != null ? cfs.getProperties() : new HashMap<>();
                    p.put("transactionId", fxOrderId);
                    cfs.setProperties(p);
                }
                cfsRepo.save(cfs);
            }

            // 7. Update existing RFS (if exists)
            Optional<ResourceFacingService> maybeRfs = rfsRepo.findByDiscoveredName(rfsName);
            if (maybeRfs.isPresent() && "Fibernet".equalsIgnoreCase(productSubtype)) {
                ResourceFacingService rfs = maybeRfs.get();
                rfs.setDiscoveredName(rfs.getName() + Constants.UNDER_SCORE  + ontSN);
                rfsRepo.save(rfs);
            }

            // 8. Prepare OLT device (create if missing)
            LogicalDevice olt = logicalDeviceRepo.findByDiscoveredName(oltName)
                    .orElseGet(() -> {
                        LogicalDevice d = new LogicalDevice();
                        try {
                            d.setLocalName(Validations.encryptName(oltName));
                            d.setDiscoveredName(oltName);
                            d.setContext("Setar");
                            d.setKind("OLTDevice");
                        } catch (AccessForbiddenException e) {
                            throw new RuntimeException(e);
                        } catch (BadRequestException e) {
                            throw new RuntimeException(e);
                        } catch (ModificationNotAllowedException e) {
                            throw new RuntimeException(e);
                        }
                        Map<String, Object> p = new HashMap<>();
                        p.put("deviceStatus", "Active");
                        p.put("oltPosition", oltName);
                        if (templateNameOnt != null) p.put("ontTemplate", templateNameOnt);
                        if (templateNameVeip != null) p.put("veipServiceTemplate", templateNameVeip);
                        if (templateNameIptv != null) p.put("veipIptvTemplate", templateNameIptv);
                        if (templateNameHsi != null) p.put("veipHsiTemplate", templateNameHsi);
                        if (templateNameIgmp != null) p.put("igmpTemplate", templateNameIgmp);
                        d.setProperties(p);
                        // if RFS exists link to it
                        maybeRfs.ifPresent(rfs -> d.addUsingService(rfs));
                        return logicalDeviceRepo.save(d);
                    });

            // validate ont name length
            if (ontName.length() > 100) {
                return new ChangeTechnologyResponse("400", ERROR_PREFIX + "ONT name too long", Instant.now().toString(),"","");
            }

            // 9. Prepare ONT device (create if missing)
            LogicalDevice ont = logicalDeviceRepo.findByDiscoveredName(ontName)
                    .orElseGet(() -> {
                        LogicalDevice d = new LogicalDevice();
                        try {
                            d.setLocalName(Validations.encryptName(ontName));
                            d.setDiscoveredName(ontName);
                            d.setContext("Setar");
                            d.setKind("ONTDevice");
                        } catch (AccessForbiddenException e) {
                            throw new RuntimeException(e);
                        } catch (BadRequestException e) {
                            throw new RuntimeException(e);
                        } catch (ModificationNotAllowedException e) {
                            throw new RuntimeException(e);
                        }
                        Map<String, Object> p = new HashMap<>();
                        p.put("deviceStatus", "Active");
                        p.put("serialNo", ontSN);
                        p.put("deviceModel",ontModel );
                        p.put("description", menm);
                        p.put("veipVlan", vlanId);
                        p.put("iptvVlan", vlanId);
                        p.put("oltPosition", oltName);
                        if (templateNameOnt != null) p.put("ontTemplate", templateNameOnt);
                        d.setProperties(p);
                        // link containing logical device (OLT)
                        d.addManagingDevices(olt);
                        // link rfs if present
                        maybeRfs.ifPresent(rfs -> d.addUsingService(rfs));
                        return logicalDeviceRepo.save(d);
                    });

            // 10. Create or retrieve management VLAN interface
            vlanRepo.findByDiscoveredName(mgmtVlanName).orElseGet(() -> {
                LogicalInterface v = new LogicalInterface();
                try {
                    v.setLocalName(Validations.encryptName(mgmtVlanName));
                    v.setDiscoveredName(mgmtVlanName);
                    v.setContext("Setar");
                    v.setKind("VLANInterface");
                } catch (AccessForbiddenException e) {
                    throw new RuntimeException(e);
                } catch (BadRequestException e) {
                    throw new RuntimeException(e);
                } catch (ModificationNotAllowedException e) {
                    throw new RuntimeException(e);
                }

                Map<String, Object> p = new HashMap<>();
                p.put("vlanId", vlanId);
                p.put("vlanStatus", "Active");
                v.setProperties(p);
                return vlanRepo.save(v);
            });

            // 11. Persist CBM device if exists
            Optional<LogicalDevice> maybeCbm = cbmRepo.findByDiscoveredName(cbmName);
            if (maybeCbm.isPresent()) {
                LogicalDevice cbmDevice = maybeCbm.get();
                Map<String, Object> cbmProps = cbmDevice.getProperties() != null ? cbmDevice.getProperties() : new HashMap<>();
                cbmProps.put("administrativeState", "Available");
                cbmProps.put("operationalState", "Available");
                cbmDevice.setProperties(cbmProps);
                // remove from inventory
                cbmRepo.save(cbmDevice,2);
            }

            // 12. Reassign CPE devices
            String cpeDeviceName ="ONT" + ontSN;
            String cpeDeviceOldName = "CBM" + Constants.UNDER_SCORE +req.getCbmSn();

            Optional<LogicalDevice> maybeCpeNew = cpeRepo.findByDiscoveredName(cpeDeviceName);
            Optional<LogicalDevice> maybeCpeOld = cpeRepo.findByDiscoveredName(cpeDeviceOldName);

            if (maybeCpeNew.isPresent() && maybeCpeOld.isPresent()) {
                LogicalDevice cpeNew = maybeCpeNew.get();
                LogicalDevice cpeOld = maybeCpeOld.get();

                Map<String, Object> newProps = cpeNew.getProperties() != null ? cpeNew.getProperties() : new HashMap<>();
                newProps.put("description", "Internet");
                newProps.put("administrativeState", "Allocated");

                Map<String, Object> oldProps = cpeOld.getProperties() != null ? cpeOld.getProperties() : new HashMap<>();
                oldProps.put("description", null);
                oldProps.put("administrativeState", "Available");

                cpeNew.setProperties(newProps);
                cpeOld.setProperties(oldProps);

                cpeRepo.save(cpeOld);
                cpeRepo.save(cpeNew);
            } else {
                // legacy returns error if either missing
                return new ChangeTechnologyResponse("500", ERROR_PREFIX + "ONT name String: " + ontName + " is not found in CPEDevice", Instant.now().toString(),"","");
            }

            // 13. Final success response: include subscriptionName and ontName
            Map<String, String> out = new HashMap<>();
            out.put("subscriptionName", subscriptionName);
            out.put("ontName", ontName);
            log.info(Constants.ACTION_COMPLETED);
            return new ChangeTechnologyResponse("200", "ChangeTechnology executed successfully.", Instant.now().toString(), subscriptionName,ontName);

        } catch (Exception ex) {
            log.error("Exception in ChangeTechnology", ex);
            return new ChangeTechnologyResponse("500", ERROR_PREFIX + ex.getMessage(), Instant.now().toString(),"","");
        }
    }
}
