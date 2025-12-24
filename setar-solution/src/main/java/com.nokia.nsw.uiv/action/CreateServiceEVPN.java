package com.nokia.nsw.uiv.action;

import com.nokia.nsw.uiv.exception.ModificationNotAllowedException;
import com.nokia.nsw.uiv.framework.action.Action;
import com.nokia.nsw.uiv.framework.action.ActionContext;
import com.nokia.nsw.uiv.framework.action.HttpAction;
import com.nokia.nsw.uiv.model.resource.logical.LogicalInterface;
import com.nokia.nsw.uiv.model.resource.logical.LogicalInterfaceRepository;
import com.nokia.nsw.uiv.model.service.Product;
import com.nokia.nsw.uiv.model.service.Service;
import com.nokia.nsw.uiv.repository.*;
import com.nokia.nsw.uiv.request.CreateServiceEVPNRequest;
import com.nokia.nsw.uiv.response.CreateServiceCBMResponse;
import com.nokia.nsw.uiv.response.CreateServiceCbmVoiceResponse;
import com.nokia.nsw.uiv.response.CreateServiceEVPNResponse;
import com.nokia.nsw.uiv.utils.Constants;
import com.nokia.nsw.uiv.utils.Validations;

import com.nokia.nsw.uiv.model.common.party.Customer;
import com.nokia.nsw.uiv.model.common.party.CustomerRepository;
import com.nokia.nsw.uiv.model.service.Subscription;
import com.nokia.nsw.uiv.model.service.SubscriptionRepository;
import com.nokia.nsw.uiv.model.resource.logical.LogicalDevice;
import com.nokia.nsw.uiv.model.resource.logical.LogicalDeviceRepository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;


@Component
@RestController
@Action
@Slf4j
public class CreateServiceEVPN implements HttpAction {

    private static final String ERROR_PREFIX = "UIV action CreateServiceEVPN execution failed - ";
    protected static final String ACTION_LABEL = Constants.CREATE_SERVICE_EVPN;

    @Autowired
    private CustomerCustomRepository customerRepo;

    @Autowired
    private SubscriptionCustomRepository subscriptionRepo;

    @Autowired
    private ProductCustomRepository productRepo;

    @Autowired
    private LogicalDeviceCustomRepository logicalDeviceRepo;

    @Autowired
    private LogicalInterfaceCustomRepository vlanRepo;

    @Autowired
    private ServiceCustomRepository serviceCustomRepository;


    @Override
    public Class<?> getActionClass() {
        return CreateServiceEVPNRequest.class;
    }

    @Override
    public Object doPost(ActionContext actionContext) {
        log.error(Constants.EXECUTING_ACTION, ACTION_LABEL);
        log.error("------------Trace # 1--------------- CreateServiceEVPN started");
        CreateServiceEVPNRequest req = (CreateServiceEVPNRequest) actionContext.getObject();

        try {
            // 1) Validate mandatory parameters (runtime)
            try {
                log.error(Constants.MANDATORY_PARAMS_VALIDATION_STARTED);
                Validations.validateMandatoryParams(req.getSubscriberName(), "subscriberName");
                Validations.validateMandatoryParams(req.getProductType(), "productType");
                Validations.validateMandatoryParams(req.getProductSubtype(), "productSubtype");
                Validations.validateMandatoryParams(req.getOntSN(), "ontSN");
                Validations.validateMandatoryParams(req.getOltName(), "oltName");
                Validations.validateMandatoryParams(req.getMgmntVlanId(), "mgmntVlanId");
                Validations.validateMandatoryParams(req.getServiceId(), "serviceId");
                Validations.validateMandatoryParams(req.getMenm(), "menm");
                Validations.validateMandatoryParams(req.getHhid(), "hhid");
                Validations.validateMandatoryParams(req.getOntModel(), "ontModel");
                log.error(Constants.MANDATORY_PARAMS_VALIDATION_COMPLETED);
            } catch (Exception bre) {
                log.error("------------Trace # 2--------------- Missing mandatory param: " + bre.getMessage());
                return new CreateServiceEVPNResponse(
                        "400",
                        ERROR_PREFIX + "Missing mandatory parameter: " + bre.getMessage(),
                        Instant.now().toString(),
                        null,
                        null
                );
            }
            AtomicBoolean isSubscriberExist = new AtomicBoolean(true);
            AtomicBoolean isSubscriptionExist = new AtomicBoolean(true);
            AtomicBoolean isProductExist = new AtomicBoolean(true);
            // 2) Prepare names
            String subscriberNameStr = req.getSubscriberName() + Constants.UNDER_SCORE  + req.getOntSN();
            if (subscriberNameStr.length() > 100) {
                log.error("------------Trace # 3--------------- Subscriber name too long");
                return new CreateServiceEVPNResponse(
                        "400",
                        ERROR_PREFIX + "Subscriber name too long",
                        Instant.now().toString(),
                        null,
                        null
                );
            }

            String subscriptionName = req.getSubscriberName() + Constants.UNDER_SCORE  + req.getServiceId() + Constants.UNDER_SCORE  + req.getOntSN();
            if (subscriptionName.length() > 100) {
                log.error("------------Trace # 4--------------- Subscription name too long");
                return new CreateServiceEVPNResponse(
                        "400",
                        ERROR_PREFIX + "Subscription name too long",
                        Instant.now().toString(),
                        null,
                        null
                );
            }

            String productNameStr = req.getSubscriberName() + Constants.UNDER_SCORE  + req.getProductSubtype() + Constants.UNDER_SCORE  + req.getServiceId();
            if (productNameStr.length() > 100) {
                log.error("------------Trace # 5--------------- Product name too long");
                return new CreateServiceEVPNResponse(
                        "400",
                        ERROR_PREFIX + "Product name too long",
                        Instant.now().toString(),
                        null,
                        null
                );
            }

            String cfsName = "CFS" + Constants.UNDER_SCORE + subscriptionName;
            String rfsName = "RFS" + Constants.UNDER_SCORE + subscriptionName;
            String ontName ="ONT" + req.getOntSN();
            if (ontName.length() > 100) {
                log.error("------------Trace # 6--------------- ONT name too long");
                return new CreateServiceEVPNResponse(
                        "400",
                        ERROR_PREFIX + "ONT name too long",
                        Instant.now().toString(),
                        null,
                        null
                );
            }

            String vlanName = req.getMenm() + Constants.UNDER_SCORE  + (req.getVlanId() == null ? "" : req.getVlanId());
            String mgmtVlanName = req.getMenm() + Constants.UNDER_SCORE  + req.getMgmntVlanId();

            log.error("------------Trace # 7--------------- Names prepared: subscriber=" + subscriberNameStr
                    + ", subscription=" + subscriptionName + ", product=" + productNameStr
                    + ", ont=" + ontName + ", vlan=" + vlanName + ", mgmtVlan=" + mgmtVlanName);

            // 3) Subscriber: find or create (properties map)
            Customer subscriber = customerRepo.findByDiscoveredName(subscriberNameStr)
                    .orElseGet(() -> {
                        isSubscriberExist.set(false);
                        log.error("------------Trace # 8--------------- Creating subscriber: " + subscriberNameStr);
                        Customer newSub = new Customer();
                        try {
                            newSub.setLocalName(Validations.encryptName(subscriberNameStr));
                            newSub.setDiscoveredName(subscriberNameStr);
                            newSub.setContext("Setar");
                            newSub.setKind("SetarSubscriber");
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                        Map<String,Object> subProps = new HashMap<>();
                        subProps.put("subscriberStatus", "Active");
                        subProps.put("subscriberType", "Regular");
                        subProps.put("accountNumber", req.getSubscriberName());
                        subProps.put("householdId", req.getHhid());
                        if (req.getFirstName() != null) subProps.put("firstName", req.getFirstName());
                        if (req.getLastName() != null) subProps.put("lastName", req.getLastName());
                        if (req.getCompanyName() != null) subProps.put("companyName", req.getCompanyName());
                        if (req.getContactPhone() != null) subProps.put("contactPhone", req.getContactPhone());
                        if (req.getSubsAddress() != null) subProps.put("subsAddress", req.getSubsAddress());
                        newSub.setProperties(subProps);
                        return customerRepo.save(newSub);
                    });
            // 4) Subscription: find or create (properties map)
            Subscription subscription = subscriptionRepo.findByDiscoveredName(subscriptionName)
                    .orElseGet(() -> {
                        isSubscriptionExist.set(true);
                        log.error("------------Trace # 9--------------- Creating subscription: " + subscriptionName);
                        Subscription subs = new Subscription();
                        try {
                            subs.setLocalName(Validations.encryptName(subscriptionName));
                            subs.setDiscoveredName(subscriptionName);
                            subs.setContext("Setar");
                            subs.setKind("SetarSubscription");
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                        Map<String,Object> subsProps = new HashMap<>();
                        subsProps.put("subscriptionStatus", "Active");
                        subsProps.put("serviceSubtype", req.getProductSubtype());
                        if (req.getQosProfile() != null) subsProps.put("evpnQosProfile", req.getQosProfile());
                        if (req.getOntPort() != null) subsProps.put("evpnPort", req.getOntPort());
                        if (req.getTemplateNameVlanCreate() != null) subsProps.put("evpnTemplateCreate", req.getTemplateNameVlanCreate());
                        if (req.getTemplateNameVlan() != null) subsProps.put("evpnTemplateVLAN", req.getTemplateNameVlan());
                        if (req.getTemplateNameVpls() != null) subsProps.put("evpnTemplateVPLS", req.getTemplateNameVpls());
                        if (req.getVlanId() != null) subsProps.put("evpnVLAN", req.getVlanId());
                        subsProps.put("serviceId", req.getServiceId());
                        subsProps.put("householdId", req.getHhid());
                        subsProps.put("serviceLink", ("IPBH".equalsIgnoreCase(req.getProductSubtype()) ? "SRX" : "ONT"));
                        // compute OLT position string per spec
                        String oltPos;
                        String p = req.getOntPort();
                        if ("5".equals(p)) {
                            oltPos = req.getOltName() + "-10-5";
                        } else {
                            // default fallback
                            String portVal = (p == null ? "1" : p);
                            oltPos = req.getOltName() + "-1-" + portVal;
                        }
                        subsProps.put("oltPosition", oltPos);
                        subsProps.put("kenanSubscriberId", req.getKenanUidNo());
                        subsProps.put("subscriberIdCbm", req.getSubscriberId());
                        subs.setProperties(subsProps);
                        subs.setCustomer(subscriber);
                        // association to subscriber (store link name so external process can link)
                        subsProps.put("linkedSubscriber", subscriber.getLocalName());
                        return subscriptionRepo.save(subs,2);
                    });

            // ensure we keep subscriber-subscription link when we didn't create subs
            if (!subscription.getProperties().containsKey("linkedSubscriber")) {
                subscription.getProperties().put("linkedSubscriber", subscriber.getLocalName());
                subscriptionRepo.save(subscription);
            }

            // 5) Product: find or create (properties map)
            Product product = productRepo.findByDiscoveredName(productNameStr)
                    .orElseGet(() -> {
                        isProductExist.set(true);
                        log.error("------------Trace # 10--------------- Creating product: " + productNameStr);
                        Product prod = new Product();
                        try {
                            prod.setLocalName(Validations.encryptName(productNameStr));
                            prod.setDiscoveredName(productNameStr);
                            prod.setContext("Setar");
                            prod.setKind("SetarProduct");
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                        Map<String,Object> prodProps = new HashMap<>();
                        prodProps.put("productType", req.getProductType());
                        prodProps.put("productStatus", "Active");
                        prodProps.put("linkedSubscriber", subscriber.getLocalName());
                        prodProps.put("linkedSubscription", subscription.getLocalName());
                        prod.setProperties(prodProps);
                        prod.setCustomer(subscriber);
                        return productRepo.save(prod,2);
                    });
            subscription.addService(product);
            subscriptionRepo.save(subscription);
            if(isSubscriberExist.get() && isSubscriptionExist.get() && isProductExist.get()){
                log.error("createServiceEVPN service already exist");
                return new CreateServiceEVPNResponse("409","Service already exist/Duplicate entry",Instant.now().toString(),subscriberNameStr,ontName);
            }

            // 6) CFS: find or create (properties map)
            Service cfs = serviceCustomRepository.findByDiscoveredName(cfsName)
                    .orElseGet(() -> {
                        log.error("------------Trace # 11--------------- Creating CFS: " + cfsName);
                        Service newCfs = new Service();
                        try {
                            newCfs.setLocalName(Validations.encryptName(cfsName));
                            newCfs.setDiscoveredName(cfsName);
                            newCfs.setContext("Setar");
                            newCfs.setKind("SetarCFS");
                        }catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                        Map<String,Object> cfsProps = new HashMap<>();
                        cfsProps.put("serviceStatus", "Active");
                        cfsProps.put("serviceType", req.getProductType());
                        cfsProps.put("cfsType",req.getProductSubtype());
                        cfsProps.put("serviceStartDate", Instant.now().toString());
                        if (req.getFxOrderID() != null) cfsProps.put("transactionId", req.getFxOrderID());
                        cfsProps.put("linkedProduct", product.getLocalName());
                        newCfs.setProperties(cfsProps);
                        newCfs.addUsingService(product);
                        return serviceCustomRepository.save(newCfs,2);
                    });

            // 7) RFS: find or create (properties map)
            Service rfs = serviceCustomRepository.findByDiscoveredName(rfsName)
                    .orElseGet(() -> {
                        log.error("------------Trace # 12--------------- Creating RFS: " + rfsName);
                        Service newRfs = new Service();
                        try {
                            newRfs.setLocalName(Validations.encryptName(rfsName));
                            newRfs.setDiscoveredName(rfsName);
                            newRfs.setContext("Setar");
                            newRfs.setKind("SetarRFS");
                        }catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                        Map<String,Object> rfsProps = new HashMap<>();
                        rfsProps.put("rfsStatus", "Active");
                        rfsProps.put("rfsType", req.getProductType());
                        rfsProps.put("linkedCFS", cfs.getLocalName());
                        newRfs.addUsedService(cfs);
                        return serviceCustomRepository.save(newRfs);
                    });

            // 8) OLT: find or create
            LogicalDevice olt = logicalDeviceRepo.findByDiscoveredName(req.getOltName())
                    .orElseGet(() -> {
                        log.error("------------Trace # 13--------------- Creating OLT: " + req.getOltName());
                        LogicalDevice dev = new LogicalDevice();
                        try {
                            dev.setLocalName(Validations.encryptName(req.getOltName()));
                            dev.setDiscoveredName(req.getOltName());
                            dev.setContext("Setar");
                            dev.setKind("OLTDevice");
                        }catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                        Map<String,Object> oltProps = new HashMap<>();
                        oltProps.put("operationalState", "Active");
                        oltProps.put("oltPosition", req.getOltName());
                        if (req.getTemplateNameOnt() != null) oltProps.put("ontTemplate", req.getTemplateNameOnt());
                        dev.setProperties(oltProps);
                        // link RFS reference if exists
                        dev.getProperties().put("linkedRFS", rfs.getLocalName());
                        dev.addContainedservice(rfs);
                        return logicalDeviceRepo.save(dev,2);
                    });

            // 9) ONT: find or create, manage EVPN counters
            LogicalDevice ont = null;
            Optional<LogicalDevice> ontOpt = logicalDeviceRepo.findByDiscoveredName(ontName);
            if(ontOpt.isPresent()){
                ont = ontOpt.get();
            }else{
                log.error("------------Trace # 14--------------- Creating ONT: " + ontName);
                LogicalDevice dev = new LogicalDevice();
                try {
                    dev.setLocalName(ontName);
                    dev.setDiscoveredName(ontName);
                    dev.setContext("Setar");
                    dev.setKind("ONTDevice");
                }catch (Exception e) {
                    throw new RuntimeException(e);
                }
                Map<String,Object> ontProps = new HashMap<>();
                ontProps.put("serialNumber", req.getOntSN());
                ontProps.put("deviceModel", req.getOntModel());
                ontProps.put("OperationalState", "Active");
                if (req.getTemplateNameOnt() != null) ontProps.put("ontTemplate", req.getTemplateNameOnt());
                ontProps.put("oltPosition", req.getOltName());
                // initialize counters
                ontProps.put("port3Counter", "0");
                ontProps.put("port4Counter", "0");
                ontProps.put("port5Counter", "0");
                // management fields
                if (req.getTemplateNameVlanMgmnt() != null) ontProps.put("mgmtTemplate", req.getTemplateNameVlanMgmnt());
                if (req.getMgmntVlanId() != null) ontProps.put("mgmtVlan", req.getMgmntVlanId());
                // link RFS
                ontProps.put("linkedRFS", rfs.getLocalName());
                dev.setProperties(ontProps);
                dev.getProperties().put("containingDevice", olt.getLocalName());
                dev.addUsedResource(olt);
                dev.addContainedservice(rfs);
                logicalDeviceRepo.save(dev);
                ont = dev;
            }

            // If ONT existed, ensure it is linked and counters initialized properly
            if (!ont.getProperties().containsKey("port3Counter")) {
                ont.getProperties().put("port3Counter", "0");
            }
            if (!ont.getProperties().containsKey("port4Counter")) {
                ont.getProperties().put("port4Counter", "0");
            }
            if (!ont.getProperties().containsKey("port5Counter")) {
                ont.getProperties().put("port5Counter", "0");
            }

            // 10) Management VLAN (for non-IPBH)
            if (!"IPBH".equalsIgnoreCase(req.getProductSubtype())) {
                LogicalInterface mgmtVlan = null;
                Optional<LogicalInterface> mgmtVlanOpt = vlanRepo.findByDiscoveredName(mgmtVlanName);
                if(mgmtVlanOpt.isPresent()){
                    mgmtVlan = mgmtVlanOpt.get();
                }else{
                    log.error("------------Trace # 15--------------- Creating mgmt VLAN: " + mgmtVlanName);
                    LogicalInterface v = new LogicalInterface();
                    try {
                        v.setLocalName(Validations.encryptName(mgmtVlanName));
                        v.setDiscoveredName(mgmtVlanName);
                        v.setContext("Setar");
                        v.setKind("VlanInterface");
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    Map<String,Object> vProps = new HashMap<>();
                    vProps.put("vlanId", req.getMgmntVlanId());
                    if (req.getTemplateNameVlanMgmnt() != null) vProps.put("mgmtTemplate", req.getTemplateNameVlanMgmnt());
                    vProps.put("OperationalState", "Active");
                    v.setProperties(vProps);
                    vlanRepo.save(v);
                    ont = logicalDeviceRepo.findByDiscoveredName(ont.getDiscoveredName()).get();
                    ont.addContainedinterface(v);
                    logicalDeviceRepo.save(ont);
                }
                // no direct association required here beyond existence
            }

            // 11) Service VLAN (per subtype rules)
            boolean usedStandardEvpn;
            if (req.getProductSubtype() != null) {
                String sub = req.getProductSubtype();
                // condition to treat as "generic branch" per spec
                if (sub.contains("BAAS") || sub.contains("PBX") || sub.contains("SIP")
                        || sub.contains("Cloudstarter") || sub.contains("Bridged")) {
                    usedStandardEvpn = false;
                } else {
                    usedStandardEvpn = true;
                }
            } else {
                usedStandardEvpn = true;
            }
            LogicalInterface serviceVlan = null;
            if (req.getVlanId() != null) {
                Optional<LogicalInterface> serviceVlanOpt = vlanRepo.findByDiscoveredName(vlanName);
                if(serviceVlanOpt.isPresent()){
                    serviceVlan=serviceVlanOpt.get();
                }else{
                    log.error("------------Trace # 16--------------- Creating service VLAN: " + vlanName);
                    LogicalInterface v = new LogicalInterface();
                    try {
                        v.setLocalName(Validations.encryptName(vlanName));
                        v.setDiscoveredName(vlanName);
                        v.setContext("Setar");
                        v.setKind("VlanInterface");
                    }catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    Map<String,Object> vProps = new HashMap<>();
                    vProps.put("vlanId", req.getVlanId());
                    vProps.put("OperationalState", "Active");
                    if (usedStandardEvpn) {
                        vProps.put("mgmtTemplate", req.getTemplateNameVlanMgmnt());
                        vProps.put("configuredOntSN", req.getOntSN());
                        vProps.put("configuredPort", req.getOntPort());
                        vProps.put("vlanTemplate", req.getTemplateNameVlan());
                        vProps.put("serviceId", req.getServiceId());
                        vProps.put("vlanCreateTemplate", req.getTemplateNameVlanCreate());
                        vProps.put("vplsTemplate", req.getTemplateNameVpls());
                        // associate with ONT
                        vProps.put("linkedOnt", ont.getLocalName());
                    } else {
                        // generic (no ONT association)
                        vProps.put("vlanTemplate", req.getTemplateNameVlan());
                    }
                    v.setProperties(vProps);
                    vlanRepo.save(v,2);
                    ont = logicalDeviceRepo.findByDiscoveredName(ont.getDiscoveredName()).get();
                    ont.addContainedinterface(v);
                    logicalDeviceRepo.save(ont);
                    serviceVlan = v;
                }
                // if created and usedStandardEvpn -> ensure association entry on ONT exists
                if (usedStandardEvpn) {
                    ont.getProperties().put("lastServiceVlan", serviceVlan.getLocalName());
                }
            }

            // 12) EVPN port & counters update
            String selectedPort = req.getOntPort() == null ? "1" : req.getOntPort();
            // determine current counter
            int currentCounter = 0;
            try {
                if ("4".equals(selectedPort)) {
                    currentCounter = Integer.parseInt((String) ont.getProperties().getOrDefault("port4Counter", "0"));
                } else if ("5".equals(selectedPort)) {
                    currentCounter = Integer.parseInt((String) ont.getProperties().getOrDefault("port5Counter", "0"));
                } else {
                    // ports 1..3 default to port3Counter usage
                    currentCounter = Integer.parseInt((String) ont.getProperties().getOrDefault("port3Counter", "0"));
                }
            } catch (Exception e) {
                currentCounter = 0;
            }
            int newCounter = currentCounter + 1;
            String servCounter = String.valueOf(newCounter);

            // apply per-port updates to OLT and ONT properties
            Map<String,Object> oltProps = olt.getProperties();
            Map<String,Object> ontProps = ont.getProperties();

            if ("4".equals(selectedPort)) {
                oltProps.put("port4ServiceTemplate", req.getTemplateNamePort());
                ontProps.put("port4Counter", servCounter);
            } else if ("5".equals(selectedPort)) {
                oltProps.put("port5ServiceTemplate", req.getTemplateNamePort());
                ontProps.put("port5Counter", servCounter);
            } else if ("3".equals(selectedPort)) {
                oltProps.put("port3ServiceTemplate", req.getTemplateNamePort());
                ontProps.put("port3Counter", servCounter);
            } else if ("2".equals(selectedPort)) {
                // spec asked: port2 uses original pre-increment value
                ontProps.put("port2Counter", String.valueOf(currentCounter));
                oltProps.put("port2ServiceTemplate", req.getTemplateNamePort());
            } else { // port 1
                // port 1: set ONT port-1 EVPN VLAN template
                ontProps.put("port1EvpnVlanTemplate", req.getTemplateNameVlan());
                // no olt update required
            }

            // 13) ONT description / mgmt template population
            if (req.getMenm() != null && !req.getMenm().isEmpty()) {
                ontProps.put("description", req.getMenm());
            }

            // ONT create template logic
            boolean isStandardEvpn = usedStandardEvpn;
            if (isStandardEvpn) {
                if (!ontProps.containsKey("createTemplate") || "NA".equals(ontProps.get("createTemplate"))) {
                    if (req.getTemplateNameCreate() != null && !"NA".equals(req.getTemplateNameCreate())) {
                        ontProps.put("createTemplate", req.getTemplateNameCreate());
                    }
                }
            }

            // Management template update
            if (req.getTemplateNameVlanMgmnt() != null && !"NA".equals(req.getTemplateNameVlanMgmnt())) {
                log.error("------------Trace # 17--------------- enter for tempMGMNTVlan " + ontProps.get("mgmtTemplate"));
                ontProps.put("mgmtTemplate", req.getTemplateNameVlanMgmnt());
                log.error("------------Trace # 18--------------- enter for tempMGMNTVlan 1 " + ontProps.get("mgmtTemplate"));
            }
            // Management VLAN set if blank
            String currentMgmtVlan = (String) ontProps.getOrDefault("mgmtVlan", "");
            if ((currentMgmtVlan == null || currentMgmtVlan.isEmpty() || "NA".equals(currentMgmtVlan))
                    && req.getMgmntVlanId() != null && !"NA".equals(req.getMgmntVlanId())) {
                ontProps.put("mgmtVlan", req.getMgmntVlanId());
            }

            // 14) OLT card template decision per port (if port 5 -> card-5)
            if ("5".equals(selectedPort)) {
                String currentCard5 = (String) oltProps.getOrDefault("card5ServiceTemplate", "");
                if ((currentCard5 == null || currentCard5.isEmpty()) && req.getTemplateNameCard() != null) {
                    oltProps.put("card5ServiceTemplate", req.getTemplateNameCard());
                }
            } else {
                String currentCard = (String) oltProps.getOrDefault("generalCardServiceTemplate", "");
                if ((currentCard == null || currentCard.isEmpty()) && req.getTemplateNameCard() != null) {
                    oltProps.put("generalCardServiceTemplate", req.getTemplateNameCard());
                }
            }

            // persist updates to ONT and OLT
            logicalDeviceRepo.save(ont);
            olt = logicalDeviceRepo.findByDiscoveredName(olt.getDiscoveredName()).get();
            logicalDeviceRepo.save(olt);

            // 15) Single-tagged VLAN interface creation logic (spec) - simplified: create one matching
            if ( (req.getProductType() != null && (req.getProductType().contains("EVPN") || req.getProductType().contains("ENTERPRISE")))
                    || (req.getProductSubtype() != null && req.getProductSubtype().contains("Cloudstarter")) ) {
                // pick index 2..8 -> naive approach: choose 2
                for (int idx = 2; idx <= 8; idx++) {
                    String singleName = req.getOntSN() + "_P" + selectedPort + "_SINGLETAGGED_" + idx;
                    if (!vlanRepo.findByDiscoveredName(singleName).isPresent()) {
                        LogicalInterface singleVlan = new LogicalInterface();
                        singleVlan.setLocalName(singleName);
                        singleVlan.setDiscoveredName(singleName);
                        singleVlan.setContext("Setar");
                        Map<String,Object> svProps = new HashMap<>();
                        svProps.put("kind", "VlanInterface");
                        svProps.put("vlanId", req.getVlanId());
                        svProps.put("mgmtTemplate", req.getTemplateNameVlanMgmnt());
                        svProps.put("configuredOntSN", req.getOntSN());
                        svProps.put("configuredPort", selectedPort);
                        svProps.put("vlanTemplate", req.getTemplateNameVlan());
                        svProps.put("serviceId", req.getServiceId());
                        svProps.put("vlanCreateTemplate", req.getTemplateNameVlanCreate());
                        svProps.put("OperationalState", "Active");
                        svProps.put("linkedOnt", ont.getLocalName());
                        singleVlan.setProperties(svProps);
                        vlanRepo.save(singleVlan);
                        ont = logicalDeviceRepo.findByDiscoveredName(ont.getDiscoveredName()).get();
                        ont.addContainedinterface(singleVlan);
                        logicalDeviceRepo.save(ont);
                        break;
                    }
                }
            }
            // 16) Ensure associations & final persist for RFS/ONT/OLT
            // - add linked RFS on ONT and OLT properties
            ont = logicalDeviceRepo.findByDiscoveredName(ont.getDiscoveredName()).get();
            ont.getProperties().put("linkedRFS", rfs.getLocalName());
            // persist again
            logicalDeviceRepo.save(ont);
            olt = logicalDeviceRepo.findByDiscoveredName(olt.getDiscoveredName()).get();
            olt.getProperties().put("linkedRFS", rfs.getLocalName());
            logicalDeviceRepo.save(olt);

            // final response
            log.error(Constants.ACTION_COMPLETED);
            log.error("------------Trace # 19--------------- CreateServiceEVPN completed successfully");
            return new CreateServiceEVPNResponse(
                    "201",
                    "UIV action CreateServiceEVPN executed successfully.",
                    Instant.now().toString(),
                    subscription.getLocalName(),
                    ont.getLocalName()
            );

        } catch (Exception ex) {
            log.error("Exception in CreateServiceEVPN", ex);
            return new CreateServiceEVPNResponse(
                    "500",
                    ERROR_PREFIX + "Error occurred while creating service EVPN - " + ex.getMessage(),
                    Instant.now().toString(),
                    null,
                    null
            );
        }
    }
}
