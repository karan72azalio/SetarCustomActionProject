package com.nokia.nsw.uiv.action;

import com.nokia.nsw.uiv.exception.BadRequestException;
import com.nokia.nsw.uiv.framework.action.Action;
import com.nokia.nsw.uiv.framework.action.ActionContext;
import com.nokia.nsw.uiv.framework.action.HttpAction;
import com.nokia.nsw.uiv.model.common.party.Customer;
import com.nokia.nsw.uiv.model.resource.Resource;
import com.nokia.nsw.uiv.model.resource.logical.LogicalDevice;
import com.nokia.nsw.uiv.model.resource.logical.LogicalInterface;
import com.nokia.nsw.uiv.model.service.Subscription;
import com.nokia.nsw.uiv.repository.*;
import com.nokia.nsw.uiv.request.QueryFlagsRequest;
import com.nokia.nsw.uiv.response.QueryFlagsResponse;
import com.nokia.nsw.uiv.utils.Constants;
import com.nokia.nsw.uiv.utils.Validations;
import com.setar.uiv.model.product.CustomerFacingService;
import com.setar.uiv.model.product.Product;
import com.setar.uiv.model.product.ResourceFacingService;
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
public class QueryFlags implements HttpAction {

    protected static final String ACTION_LABEL = Constants.QUERY_FLAGS;
    private static final String ERROR_PREFIX = "UIV action QueryFlags execution failed - ";

    @Autowired
    private LogicalDeviceCustomRepository deviceRepository;

    @Autowired
    private LogicalComponentCustomRepository componentRepository;

    @Autowired
    private LogicalInterfaceCustomRepository logicalInterfaceRepository;

    @Autowired
    private ResourceFacingServiceCustomRepository rfsRepository;

    @Autowired
    private SubscriptionCustomRepository subscriptionRepository;

    @Autowired
    private CustomerCustomRepository customerRepository;

    @Autowired
    private ProductCustomRepository productRepository;

    @Autowired
    private CustomerFacingServiceCustomRepository cfsRepository;

    @Override
    public Class getActionClass() {
        return QueryFlagsRequest.class;
    }

    @Override
    public Object doPost(ActionContext actionContext) throws Exception {
        log.error("------------Test Trace # 1---------------");
        log.error(Constants.EXECUTING_ACTION, ACTION_LABEL);

        QueryFlagsRequest request = (QueryFlagsRequest) actionContext.getObject();

        Map<String, String> flags = new HashMap<>();
        initializeFlags(flags);

        String subscriber = request.getSubscriberName();
        String productType = request.getProductType();
        String productSubType = request.getProductSubType();
        String actionType = request.getActionType();
        String ontSN = request.getOntSN();
        String ontPort = request.getOntPort();
        String serviceID = request.getServiceId();
        String subName =
                subscriber + Constants.UNDER_SCORE + serviceID;

        try {
            log.error("------------Test Trace # 2---------------");
            log.error("Validating mandatory parameters...");
            try {
                Validations.validateMandatory(subscriber, "subscriberName");
                Validations.validateMandatory(productType, "productType");
                Validations.validateMandatory(productSubType, "productSubType");
                Validations.validateMandatory(ontSN, "ontSN");
                Validations.validateMandatory(ontPort, "ontPort");
            } catch (BadRequestException bre) {
                String msg = ERROR_PREFIX + "Missing mandatory parameter : " + bre.getMessage();
                return new QueryFlagsResponse("400", msg, getCurrentTimestamp(), Collections.emptyMap());
            }
            log.error("Mandatory validation completed.");

            log.error("------------Test Trace # 3---------------");
            if (!equalsIgnoreCase(productType, "VOIP") && !equalsIgnoreCase(productType, "Voice")) {
                log.error("Trace: Non-voice product -> default VOIP ports to Available");
                flags.put("SERVICE_VOIP_NUMBER1", "Available");
                flags.put("SERVICE_VOIP_NUMBER2", "Available");
            }
            // ------------ ServiceID-scoped RFS check ----------------
            String serviceIdFlag = "New";

            if (serviceID != null && !serviceID.trim().isEmpty()) {

                List<ResourceFacingService> rfsList = new ArrayList<>();

                for (ResourceFacingService rfs : rfsRepository.findAll()) {
                    String rfsName = rfs.getDiscoveredName();

                    if (rfsName != null && rfsName.contains(serviceID)) {

                        String[] tokens = rfsName.split(Constants.UNDER_SCORE);

                        if (tokens.length > 2 && serviceID.equals(tokens[2])) {
                            rfsList.add(rfs);
                        }
                    }
                }

                serviceIdFlag = rfsList.isEmpty() ? "New" : "Exist";
            }

            flags.put("SERVICE_ID_FLAG", serviceIdFlag);
// --------------------------------------------------------

            ResourceFacingService rfs2=rfsRepository.findByDiscoveredName("RFS" + Constants.UNDER_SCORE + subscriber + Constants.UNDER_SCORE  + (serviceID == null ? "" : serviceID)).get();
            log.error("------------Test Trace # 4---------------");
            String serviceLink = "NA";
            if (ontSN != null) {
                if (ontSN.startsWith("ALC")) {
                    serviceLink = "ONT";
                    log.error("Trace: ontSN startsWith ALCL -> serviceLink=ONT");
                } else if (ontSN.startsWith("CW")) {
                    serviceLink = "SRX";
                    log.error("Trace: ontSN startsWith CW -> serviceLink=SRX");
                } else {
                    log.error("Trace: ontSN pattern not recognized -> serviceLink=NA");
                }
            }
            flags.put("SERVICE_LINK", serviceLink);

            log.error("------------Test Trace # 5---------------");
            boolean subtypeMatches = equalsAnyIgnoreCase(productSubType, "Broadband", "Voice", "Cloudstarter", "Bridged");
            if ((subtypeMatches || equalsIgnoreCase(productType, "ENTERPRISE"))
                    && !"Configure".equalsIgnoreCase(actionType)) {
                if (ontSN == null || ontSN.trim().isEmpty() || "NA".equalsIgnoreCase(ontSN)) {
                    try {
                        String rfsName = "RFS" + Constants.UNDER_SCORE + subscriber + Constants.UNDER_SCORE  + (serviceID == null ? "" : serviceID);
                        Iterable<ResourceFacingService> allRfs = rfsRepository.findAll();

                        for (ResourceFacingService rfs : allRfs) {
                            if (rfs.getDiscoveredName() != null &&
                                    rfs.getDiscoveredName().equalsIgnoreCase(rfsName)) {
                                ResourceFacingService rfs1=rfsRepository.findByDiscoveredName(rfs.getDiscoveredName()).get();
                                CustomerFacingService cfs=rfs1.getContainingCfs();
                                cfs = cfsRepository.findByDiscoveredName(cfs.getDiscoveredName()).get();
                                Product product = cfs.getContainingProduct();
                                product=productRepository.findByDiscoveredName(product.getDiscoveredName()).get();
                                Subscription subscription=product.getSubscription();

                                if (subscription!= null) {


                                    String subServiceId = (String) safeProps(subscription.getProperties())
                                            .getOrDefault("serviceID", "");

                                    if (serviceID != null && serviceID.equals(subServiceId)) {
                                        Set<Resource> used = rfs.getUsedResource();
                                        if (used != null) {
                                            for (Resource res : used) {
                                                if (res.getDiscoveredName() != null &&
                                                        res.getDiscoveredName().contains("ONT")) {
                                                    Object serial = safeProps(res.getProperties())
                                                            .get("serialNo");
                                                    if (serial != null) {
                                                        ontSN = serial.toString();
                                                        flags.put("ONT",ontSN);
                                                        flags.put("SERVICE_SN", ontSN);
                                                        flags.put("SERVICE_LINK", "ONT");
                                                    }
                                                } else if (res.getDiscoveredName() != null &&
                                                        res.getDiscoveredName().contains("CBM")) {
                                                    flags.put("SERVICE_LINK", "Cable_Modem");
                                                }
                                            }
                                        }

                                        String bridgeService =
                                                deriveBridgeServiceForSubscriberRfs(allRfs, ontSN, subscriber);
                                        flags.put("BRIDGE_SERVICE",
                                                bridgeService == null ? "NA" : bridgeService);
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        log.error("RFS discovery best-effort failed: {}", e.getMessage());
                    }
                }
            }

            log.error("------------Test Trace # 6---------------");
            if (equalsIgnoreCase(productType, "VOIP") && equalsIgnoreCase(actionType, "Configure") && serviceID != null) {
                log.error("Trace: VOIP Configure flow - checking voip device mapping");
                String voipDeviceName = subscriber + Constants.UNDER_SCORE  + serviceID;
                Optional<LogicalDevice> optVoip = deviceRepository.findByDiscoveredName(voipDeviceName);

                if (optVoip.isPresent()) {
                    Map<String, Object> p = safeProps(optVoip.get().getProperties());
                    String pots1 = (String) p.getOrDefault("potsPort1Number", "");
                    String pots2 = (String) p.getOrDefault("potsPort2Number", "");
                    if (serviceID.equals(pots1)) {
                        flags.put("VOICE_POTS_PORT", "1");
                        log.error("Trace: VOIP pots mapped on port1");
                    } else if (serviceID.equals(pots2)) {
                        flags.put("VOICE_POTS_PORT", "2");
                        log.error("Trace: VOIP pots mapped on port2");
                    }
                } else {
                    log.error("Trace: VOIP device not found by GDN - scanning all devices (best-effort)");
                    for (LogicalDevice d : deviceRepository.findAll()) {
                        Map<String, Object> p = safeProps(d.getProperties());
                        if (serviceID.equals(p.getOrDefault("potsPort1Number", ""))) {
                            flags.put("VOICE_POTS_PORT", "1");
                            break;
                        } else if (serviceID.equals(p.getOrDefault("potsPort2Number", ""))) {
                            flags.put("VOICE_POTS_PORT", "2");
                            break;
                        }
                    }
                }
            }
            // --------------------Logic 12--------------
            // ------------ Additional MAC Assignment (Modify) ----------------
            if ("Modify".equalsIgnoreCase(actionType)
                    && equalsAnyIgnoreCase(productType,
                    "MOCA", "BridgeMode", "APMNT", "WIFION")
                    && serviceID != null
                    && subscriber != null) {



                log.error("Trace: Modify MAC assignment, searching subscription {}", subName);

                subscriptionRepository.findByDiscoveredName(subName)
                        .ifPresent(sub -> {
                            Map<String, Object> p = safeProps(sub.getProperties());
                            Object mac = p.get("macAddress");

                            if (mac != null && !mac.toString().isEmpty()) {
                                flags.put("CBM_MAC", mac.toString());
                                log.error("Trace: CBM_MAC set from subscription: {}", mac);
                            }
                        });
            }
// ----------------------------------------------------------------

// ================= Step 6 integration =================
            if (ontSN != null && serviceID != null && subscriber != null) {

                log.error("Trace: Step-6 flags routine invoked");

                Map<String, String> step6Result =
                        executeStep6Flags(
                                ontSN,
                                serviceID,
                                subscriber,
                                actionType,
                                productSubType
                        );

                if (!step6Result.isEmpty()) {
                    flags.put("ACCOUNT_EXIST",
                            step6Result.getOrDefault("ACCOUNT_EXIST", flags.get("ACCOUNT_EXIST")));

                    flags.put("SERVICE_FLAG",
                            step6Result.getOrDefault("SERVICE_FLAG", flags.get("SERVICE_FLAG")));

                    flags.put("CBM_ACCOUNT_EXIST",
                            step6Result.getOrDefault("CBM_ACCOUNT_EXIST", flags.get("CBM_ACCOUNT_EXIST")));

                    if (step6Result.containsKey("SIMA_CUST_ID")) {
                        flags.put("SIMA_CUST_ID", step6Result.get("SIMA_CUST_ID"));
                    }
                }
            } else {
                log.error("Trace: Step-6 skipped (ontSN/serviceID/subscriber missing)");
            }
// =====================================================

            log.error("------------Test Trace # 7---------------");

            List<Subscription> subsForCustomer = new ArrayList<>();
            for (Subscription s : subscriptionRepository.findAll()) {
                if (s.getDiscoveredName() != null &&
                        s.getDiscoveredName().contains(subscriber)) {
                    subsForCustomer.add(s);
                }
            }
            // ================= MULTIPLE MATCHING SUBSCRIBERS LOGIC =================
            List<Customer> matchingSubscribers = new ArrayList<>();

            for (Customer cust : customerRepository.findAll()) {
                if (cust.getDiscoveredName() != null
                        && cust.getDiscoveredName().contains(subscriber)) {
                    matchingSubscribers.add(cust);
                }
            }

            if (matchingSubscribers.size() > 1) {

                log.error("Trace: Multiple matching subscribers found for {}", subscriber);

                // Initial values as per spec
                flags.put("SERVICE_FLAG", "Exist");
                flags.put("ACCOUNT_EXIST", "New");
                flags.put("CBM_ACCOUNT_EXIST", "New");

                boolean cbmFound = false;

                // List subscriptions whose name contains subscriber
                for (Subscription s : subscriptionRepository.findAll()) {

                    if (s.getDiscoveredName() == null
                            || !s.getDiscoveredName().contains(subscriber)) {
                        continue;
                    }

                    Map<String, Object> sp = safeProps(s.getProperties());
                    Object link = sp.get("serviceLink");

                    if ("Cable_Modem".equalsIgnoreCase(String.valueOf(link))) {
                        cbmFound = true;
                        break;
                    }
                }

                if (cbmFound) {
                    flags.put("CBM_ACCOUNT_EXIST", "Exist");
                    log.error("Trace: Cable_Modem subscription found → CBM_ACCOUNT_EXIST=Exist");
                } else {
                    flags.put("ACCOUNT_EXIST", "Exist");
                    log.error("Trace: No Cable_Modem subscription → ACCOUNT_EXIST=Exist");
                }
            }


            if (Arrays.asList("Unconfigure", "MoveOut",
                            "ChangeTechnology", "AccountTransfer")
                    .contains(actionType)) {

                if (subsForCustomer.isEmpty()) {
                    flags.put("SERVICE_FLAG", "New");
                    flags.put("ACCOUNT_EXIST", "New");
                    flags.put("CBM_ACCOUNT_EXIST", "New");

                } else if (subsForCustomer.size() == 1) {
                    flags.put("SERVICE_FLAG", "New");
                    flags.put("ACCOUNT_EXIST", "New");

                    Object link =
                            safeProps(subsForCustomer.get(0).getProperties())
                                    .get("serviceLink");

                    if ("Cable_Modem".equalsIgnoreCase(String.valueOf(link))) {
                        flags.put("CBM_ACCOUNT_EXIST", "New");
                    }

                } else {
                    flags.put("SERVICE_FLAG", "Exist");
                    flags.put("ACCOUNT_EXIST", "Exist");

                    int cbmCount = 0;
                    Set<String> macSet = new HashSet<>();

                    for (Subscription s : subsForCustomer) {
                        Map<String, Object> p = safeProps(s.getProperties());
                        if ("Cable_Modem".equalsIgnoreCase(
                                (String) p.get("serviceLink"))) {

                            cbmCount++;
                            Object mac = p.get("macAddress");
                            if (mac != null) macSet.add(mac.toString());
                        }
                    }

                    if (Arrays.asList("ChangeTechnology", "AccountTransfer")
                            .contains(actionType)) {
                        flags.put("CBM_ACCOUNT_EXIST",
                                macSet.size() > 1 ? "Exist" : "New");
                    } else {
                        flags.put("CBM_ACCOUNT_EXIST",
                                cbmCount > 1 ? "Exist" : "New");
                    }
                }

            } else if ("Configure".equalsIgnoreCase(actionType)
                    && ontSN != null && ontSN.contains("ALCL")) {

                String subscriberWithOnt =
                        subscriber + Constants.UNDER_SCORE + ontSN;

                boolean exists =
                        customerRepository
                                .findByDiscoveredName(subscriberWithOnt)
                                .isPresent();

                flags.put("SERVICE_FLAG", exists ? "Exist" : "New");
                flags.put("ACCOUNT_EXIST", exists ? "Exist" : "New");

            } else if ("Migrate".equalsIgnoreCase(actionType)
                    && ontSN != null && ontSN.contains("ALCL")) {

                flags.put("SERVICE_FLAG", "New");
                flags.put("ACCOUNT_EXIST", "New");

                for (Subscription s : subsForCustomer) {
                    if (s.getDiscoveredName() != null &&
                            s.getDiscoveredName().contains(ontSN)) {

                        flags.put("SERVICE_FLAG", "Exist");

                        if (subName.equalsIgnoreCase(
                                s.getDiscoveredName())) {
                            flags.put("ACCOUNT_EXIST", "Exist");
                        }

                        Object sima =
                                safeProps(s.getProperties())
                                        .get("subscriberIDForCableModem");
                        if (sima != null && !sima.toString().isEmpty()) {
                            flags.put("SIMA_CUST_ID", sima.toString());
                        }
                    }
                }

            } else {
                boolean subscriberExists =
                        customerRepository
                                .findByDiscoveredName(subscriber)
                                .isPresent();

                flags.put("SERVICE_FLAG",
                        subscriberExists ? "Exist" : "New");
                flags.put("ACCOUNT_EXIST",
                        subscriberExists ? "Exist" : "New");

                boolean anyCbm = subsForCustomer.stream().anyMatch(s -> {
                    Object l =
                            safeProps(s.getProperties()).get("serviceLink");
                    return l != null &&
                            l.toString().contains("Cable_Modem");
                });

                flags.put("CBM_ACCOUNT_EXIST",
                        anyCbm ? "Exist" : "New");
            }


            log.error("------------Test Trace # 8---------------");
            if (ontSN != null && !"".equals(ontSN) && equalsIgnoreCase(productSubType, "IPTV") && equalsIgnoreCase(actionType, "Unconfigure")) {
                log.error("Trace: IPTV Unconfigure path - searching subscription");
                String subGdn = subscriber + Constants.UNDER_SCORE  + (serviceID == null ? "" : serviceID);
                Optional<Subscription> optSub = subscriptionRepository.findByDiscoveredName(subGdn);
                String ontSNO = "NA";
                if (optSub.isPresent()) {
                    Subscription s = optSub.get();
                    Map<String, Object> p = safeProps(s.getProperties());
                    Object sSN = p.get("serviceSN");
                    if (sSN != null) {
                        if ("ONT".equalsIgnoreCase(serviceLink) || "SRX".equalsIgnoreCase(serviceLink)) {
                            ontSNO = sSN.toString();
                        } else if ("Cable_Modem".equalsIgnoreCase(serviceLink)) {
                            ontSNO = p.getOrDefault("macAddress", "").toString();
                        }
                    } else {
                        ontSNO = ontSN;
                    }
                } else {
                    ontSNO = ontSN;
                }
                if (!"NA".equalsIgnoreCase(ontSNO) && ontSNO != null && !"".equals(ontSNO)) {
                    int iptvCount = 0;
                    for (Subscription s : subscriptionRepository.findAll()) {
                        Map<String, Object> p = safeProps(s.getProperties());
                        if ("IPTV".equalsIgnoreCase((String) p.getOrDefault("serviceSubType", ""))) {
                            if (ontSNO.equals(p.getOrDefault("serviceSN", "")) || ontSNO.equals(p.getOrDefault("macAddress", ""))) {
                                iptvCount++;
                            }
                        }
                    }
                    flags.put("IPTV_COUNT", String.valueOf(iptvCount));
                    log.error("Trace: IPTV count for ontSNO=" + ontSNO + " is " + iptvCount);
                }
            }

            log.error("------------Test Trace # 9---------------");

            List<String> subscount = new ArrayList<>();

            boolean eligible =
                    equalsAnyIgnoreCase(productSubType,
                            "Fibernet", "Broadband", "Voice", "Bridged")
                            || (equalsIgnoreCase(productType, "Broadband")
                            && equalsIgnoreCase(productSubType, "Bridged"));

            if (eligible) {

                boolean ontBasedSearch =
                        ontSN != null && ontSN.contains("ALCL");

                // 🔁 Re-derive Bridge Service ID on these records
                String bridgeService = "NA";
                if (ontBasedSearch) {
                    bridgeService =
                            deriveBridgeServiceForSubscriberRfs(
                                    rfsRepository.findAll(),
                                    ontSN,
                                    subscriber
                            );
                }

                for (Subscription s : subscriptionRepository.findAll()) {

                    String name = s.getDiscoveredName();
                    if (name == null) continue;

                    boolean match;

                    if (ontBasedSearch) {
                        match = name.contains(ontSN);
                    } else {
                        match = name.contains(subscriber);
                    }

                    if (!match) continue;

                    Map<String, Object> sp = safeProps(s.getProperties());

                    // ✅ Add to subscount only for Fibernet / Broadband
                    if (name.endsWith(ontSN)
                            && equalsAnyIgnoreCase(productType,
                            "Fibernet", "Broadband")) {
                        subscount.add(name);
                    }

                    // ✅ QoS logic ONLY for Modify_CPE + valid bridgeService
                    if (ontBasedSearch
                            && bridgeService != null
                            && !"NA".equalsIgnoreCase(bridgeService)
                            && actionType != null
                            && actionType.contains("Modify_CPE")) {

                        if ("Bridged".equalsIgnoreCase(
                                (String) sp.getOrDefault("serviceSubType", ""))
                                && name.contains(ontSN)) {

                            String qos =
                                    (String) sp.getOrDefault(
                                            "evpnQosSessionProfile", "");

                            if (qos != null && !qos.isEmpty()) {
                                flags.put("QOS_PROFILE_BRIDGE", qos);
                                log.error("Trace: QOS_PROFILE_BRIDGE set to {}", qos);
                            }
                        }
                    }
                }
            }

            flags.put("FIBERNET_COUNT",
                    subscount.isEmpty()
                            ? "0"
                            : String.valueOf(subscount.size()));

            log.error("Trace: Fibernet count = {}",
                    flags.get("FIBERNET_COUNT"));

            log.error("Trace: Fibernet count = " + flags.get("FIBERNET_COUNT"));

            log.error("------------Test Trace # 10---------------");

            if (equalsIgnoreCase(productSubType, "IPTV")
                    && !equalsIgnoreCase(actionType, "Configure")
                    && subscriber != null
                    && serviceID != null) {

                String subscriptionToSearch =
                        subscriber + Constants.UNDER_SCORE + serviceID;

                log.error("Trace: IPTV subscription lookup: {}", subscriptionToSearch);

                Optional<Subscription> optFound =
                        subscriptionRepository.findByDiscoveredName(subscriptionToSearch);

                if (optFound.isPresent()) {

                    Subscription found = optFound.get();
                    Map<String, Object> p = safeProps(found.getProperties());

                    Object link  = p.get("serviceLink");
                    Object sSN   = p.get("serviceSN");
                    Object sMAC  = p.get("macAddress");
                    Object qos   = p.get("veipQosSessionProfile");
                    Object kenan = p.get("kenanSubscriberId");

                    if (link != null) {
                        flags.put("SERVICE_LINK", link.toString());
                    }

                    if (sSN != null) {
                        flags.put("SERVICE_SN", sSN.toString());
                    }

                    if (sMAC != null) {
                        flags.put("SERVICE_MAC", sMAC.toString());
                        flags.put("CBM_MAC", sMAC.toString()); // ✅ REQUIRED
                    }

                    if (qos != null) {
                        flags.put("QOS_PROFILE", qos.toString());
                    }

                    if (kenan != null) {
                        flags.put("KENAN_UIDNO", kenan.toString());
                    }

                    // ✅ ontSN fallback logic (CRITICAL)
                    if ("NA".equalsIgnoreCase(ontSN)
                            && sSN != null
                            && !sSN.toString().isEmpty()) {

                        ontSN = sSN.toString();
                        log.error("Trace: ontSN derived from serviceSN = {}", ontSN);
                    }
                }
            }

            else if (!equalsIgnoreCase(actionType, "Configure")) {
                String subscriptionToSearch;
                if (ontSN != null && ontSN.contains("ALCL")) {
                    subscriptionToSearch = subscriber + Constants.UNDER_SCORE  + (serviceID == null ? "" : serviceID) + Constants.UNDER_SCORE  + ontSN;
                } else {
                    subscriptionToSearch = subscriber + Constants.UNDER_SCORE  + (serviceID == null ? "" : serviceID);
                }
                log.error("Trace: Searching subscription by DN: " + subscriptionToSearch);
                Optional<Subscription> optFound = subscriptionRepository.findByDiscoveredName(subscriptionToSearch);
                if (optFound.isPresent()) {
                    Subscription found = optFound.get();
                    Map<String, Object> p = safeProps(found.getProperties());
                    Object link = p.get("serviceLink");
                    Object sSN = p.get("serviceSN");
                    Object sMAC = p.get("macAddress");
                    Object qos = p.get("veipQosSessionProfile");
                    Object kenan = p.get("kenanSubscriberId");

                    if (link != null) flags.put("SERVICE_LINK", link.toString());
                    if (sSN != null) flags.put("SERVICE_SN", sSN.toString());
                    if (sMAC != null) flags.put("SERVICE_MAC", sMAC.toString());
                    if (qos != null) flags.put("QOS_PROFILE", qos.toString());
                    if (kenan != null) flags.put("KENAN_UIDNO", kenan.toString());

                    if ("Cable_Modem".equalsIgnoreCase(String.valueOf(link))) {
                        String cbmName = "CBM" + Constants.UNDER_SCORE +(sMAC == null ? "" : sMAC.toString());
                        Optional<LogicalDevice> optCbm = deviceRepository.findByDiscoveredName(cbmName);
                        if (optCbm.isPresent()) {
                            LogicalDevice cbm = optCbm.get();
                            Map<String, Object> cbmProps = safeProps(cbm.getProperties());
                            flags.put("CBM_MAC", (String) cbmProps.getOrDefault("macAddress", ""));
                            String n1 = (String) cbmProps.getOrDefault("voipPort1", "Available");
                            String n2 = (String) cbmProps.getOrDefault("voipPort2", "Available");
                            flags.put("SERVICE_VOIP_NUMBER1", n1 == null ? "" : n1);
                            flags.put("SERVICE_VOIP_NUMBER2", n2 == null ? "" : n2);
                            flags.put("ONT_MODEL", (String) cbmProps.getOrDefault("deviceModel", ""));
                            if (!"Available".equalsIgnoreCase(n1) || !"Available".equalsIgnoreCase(n2)) {
                                flags.put("SERVICE_TEMPLATE_VOIP", "Exist");
                            } else {
                                flags.put("SERVICE_TEMPLATE_VOIP", "New");
                            }
                            log.error("Trace: CBM inspected: mac=" + flags.get("CBM_MAC") + " voip1=" + flags.get("SERVICE_VOIP_NUMBER1"));
                        } else {
                            String alt = "CBM" +(serviceID == null ? "" : serviceID);
                            deviceRepository.findByDiscoveredName(alt).ifPresent(dev -> {
                                Map<String, Object> dp = safeProps(dev.getProperties());
                                flags.put("ONT_MODEL", (String) dp.getOrDefault("deviceModel", ""));
                            });
                        }
                    }
                }
            }

            log.error("------------Test Trace # 11---------------");
            customerRepository.findByDiscoveredName(subscriber).ifPresent(cust -> {
                Map<String, Object> cp = safeProps(cust.getProperties());
                flags.put("FIRST_NAME", (String) cp.getOrDefault("firstName", ""));
                flags.put("LAST_NAME", (String) cp.getOrDefault("lastName", ""));
                log.error("Trace: Subscriber info: " + flags.get("FIRST_NAME") + " " + flags.get("LAST_NAME"));
            });

            log.error("------------Test Trace # 12---------------");
            if ("ONT".equalsIgnoreCase(serviceLink) || "SRX".equalsIgnoreCase(serviceLink) || (ontSN != null && ontSN.contains("ALCL"))) {
                String ontGdn = ontSN == null ? "" :"ONT" + ontSN;
                if (ontGdn.length() > 100) {
                    return new QueryFlagsResponse("400", ERROR_PREFIX + "ONT name too long", getCurrentTimestamp(), Collections.emptyMap());
                }
                Optional<LogicalDevice> optOntDev = deviceRepository.findByDiscoveredName(ontGdn);
                if (optOntDev.isPresent()) {
                    LogicalDevice ontDev = optOntDev.get();
                    Map<String, Object> ontProps = safeProps(ontDev.getProperties());
                    // ================= ENTERPRISE: derive ontPort from RFS =================
                    if (equalsIgnoreCase(productType, "ENTERPRISE")
                            && serviceID != null
                            && ontSN != null) {

                        log.error("Trace: ENTERPRISE flow - deriving ontPort from RFS");

                        for (ResourceFacingService rfs : rfsRepository.findAll()) {

                            if (rfs.getDiscoveredName() == null
                                    || !rfs.getDiscoveredName().contains(serviceID)) {
                                continue;
                            }

                            try {
                                ResourceFacingService rfs1 =
                                        rfsRepository.findByDiscoveredName(
                                                rfs.getDiscoveredName()).orElse(null);

                                if (rfs1 == null || rfs1.getContainingCfs() == null) continue;

                                CustomerFacingService cfs =
                                        cfsRepository.findByDiscoveredName(
                                                rfs1.getContainingCfs().getDiscoveredName()).orElse(null);

                                if (cfs == null || cfs.getContainingProduct() == null) continue;

                                Product product =
                                        productRepository.findByDiscoveredName(
                                                cfs.getContainingProduct().getDiscoveredName()).orElse(null);

                                if (product == null || product.getSubscription() == null) continue;

                                Subscription sub = product.getSubscription();
                                Map<String, Object> sp = safeProps(sub.getProperties());

                                Object port = sp.get("ontPort");
                                if (port != null) {
                                    ontPort = port.toString();
                                    flags.put("SERVICE_ONT_PORT", ontPort);
                                    log.error("Trace: ontPort derived from ENTERPRISE RFS = {}", ontPort);
                                    break;
                                }

                            } catch (Exception e) {
                                log.error("Trace: ENTERPRISE ontPort derivation failed", e);
                            }
                        }
                    }

                    flags.put("ONT_MODEL", (String) ontProps.getOrDefault("deviceModel", ""));
                    flags.put("SERVICE_SN", (String) ontProps.getOrDefault("serialNo", ""));
                    flags.put("SERVICE_MAC", (String) ontProps.getOrDefault("macAddress", ""));
                    log.error("Trace: ONT found: model=" + flags.get("ONT_MODEL") + " sn=" + flags.get("SERVICE_SN"));

                    Object parentOltObj = ontProps.get("parentOlt");
                    if (parentOltObj != null) {
                        String oltGdn = parentOltObj.toString();
                        deviceRepository.findByDiscoveredName(oltGdn).ifPresent(olt -> {
                            Map<String, Object> oltProps = safeProps(olt.getProperties());
                            flags.put("OLT_POSITION", (String) oltProps.getOrDefault("position", ""));
                            flags.put("SERVICE_TEMPLATE_ONT", existsString(oltProps.get("ontTemplate")));
                            flags.put("SERVICE_TEMPLATE_VEIP", existsString(oltProps.get("veipServiceTemplate")));
                            flags.put("SERVICE_TEMPLATE_HSI", existsString(oltProps.get("veipHsiTemplate")));
                            flags.put("SERVICE_TEMPLATE_VOIP", existsString(oltProps.get("voipServiceTemplate")));
                            flags.put("SERVICE_TEMPLATE_POTS1", existsString(oltProps.get("voipPots1Template")));
                            flags.put("SERVICE_TEMPLATE_POTS2", existsString(oltProps.get("voipPots2Template")));
                            log.error("Trace: OLT templates checked for OLT=" + oltGdn);
                        });
                    }

                    Set<String> vlanSet = new HashSet<>();
                    for (LogicalInterface vif : logicalInterfaceRepository.findAll()) {
                        String lname = vif.getDiscoveredName();
                        if (lname != null && lname.contains(ontSN) && ontPort != null && lname.contains("P" + ontPort)) {
                            vlanSet.add(lname);
                        }
                    }
                    for (String vl : vlanSet) {
                        logicalInterfaceRepository.findByDiscoveredName(vl).ifPresent(vif -> {
                            Map<String, Object> vp = safeProps(vif.getProperties());
                            Object tmpl = vp.get("template");
                            if ("4.3B EVPN SINGLETAGGED VLAN v2".equalsIgnoreCase(String.valueOf(tmpl))) {
                                flags.put("SERVICE_TEMPLATE_MGMT", "4.3B EVPN SINGLETAGGED VLAN v2");
                                log.error("Trace: VLAN " + vl + " uses management template " + vp.get("template"));
                            }
                        });
                    }
                } else {
                    log.error("Trace: ONT device not found by GDN: " + ontGdn);
                }
            } else if ("Cable_Modem".equalsIgnoreCase(serviceLink) || equalsIgnoreCase(productType, "CBM")) {
                boolean iptvExists = false, veipExists = false;
                for (Subscription s : subscriptionRepository.findAll()) {
                    Map<String, Object> p = safeProps(s.getProperties());
                    if ("IPTV".equalsIgnoreCase((String) p.getOrDefault("serviceSubType", ""))) iptvExists = true;
                    if ("Broadband".equalsIgnoreCase((String) p.getOrDefault("serviceSubType", ""))) veipExists = true;
                }
                flags.put("SERVICE_TEMPLATE_IPTV", iptvExists ? "Exist" : "New");
                flags.put("SERVICE_TEMPLATE_VEIP", veipExists ? "Exist" : "New");
                log.error("Trace: CBM path: IPTV exist=" + flags.get("SERVICE_TEMPLATE_IPTV") + " VEIP exist=" + flags.get("SERVICE_TEMPLATE_VEIP"));
            }

            log.error("------------Test Trace # 13---------------");
            if (Arrays.asList("AccountTransfer", "MoveOut", "ChangeTechnology", "Unconfigure").contains(actionType) || (actionType != null && actionType.contains("Modify_CPE"))) {
                log.error("Trace: IPTV service ID list discovery path");
                List<String> iptvIds = new ArrayList<>();
                if ("ONT".equalsIgnoreCase(serviceLink)) {
                    for (Subscription s : subscriptionRepository.findAll()) {
                        Map<String, Object> p = safeProps(s.getProperties());
                        if ("IPTV".equalsIgnoreCase((String) p.getOrDefault("serviceSubType", ""))) {
                            Object sid = p.get("serviceID");
                            if (sid != null) iptvIds.add(sid.toString());
                        }
                    }
                } else {
                    String cbmMac = flags.getOrDefault("CBM_MAC", "");
                    if (cbmMac != null && !cbmMac.isEmpty()) {
                        for (Subscription s : subscriptionRepository.findAll()) {
                            Map<String, Object> p = safeProps(s.getProperties());
                            if ("IPTV".equalsIgnoreCase((String) p.getOrDefault("serviceSubType", "")) && cbmMac.equals(p.getOrDefault("macAddress", ""))) {
                                Object sid = p.get("serviceID");
                                if (sid != null) iptvIds.add(sid.toString());
                            }
                        }
                        String cbmGdn = "CBM" + Constants.UNDER_SCORE +cbmMac;
                        deviceRepository.findByDiscoveredName(cbmGdn).ifPresent(cpe -> {
                            Map<String, Object> cp = safeProps(cpe.getProperties());
                            flags.put("RESOURCE_MAC_MTA_OLD", (String) cp.getOrDefault("macAddressMta", ""));
                            flags.put("RESOURCE_MODEL_MTA_OLD", (String) cp.getOrDefault("deviceModelMta", ""));
                        });
                    }
                }
                flags.put("IPTV_COUNT", String.valueOf(iptvIds.size()));
                log.error("Trace: IPTV IDs discovered count = " + iptvIds.size());
            }

            log.error("------------Test Trace # 14---------------");
            if ((equalsAnyIgnoreCase(productType, "EVPN", "ENTERPRISE") || equalsAnyIgnoreCase(productSubType, "Cloudstarter", "Bridged"))
                    && (equalsAnyIgnoreCase(actionType, "Configure", "Migrate"))) {
                log.error("Trace: Evaluating template requirements for EVPN/Enterprise/Cloudstarter/Bridged");
                String oltPos = flags.getOrDefault("OLT_POSITION", "");
                if (!oltPos.isEmpty()) {
                    String oltPosGdn = Validations.getGlobalName(oltPos);
                    deviceRepository.findByDiscoveredName(oltPos).ifPresent(olt -> {
                        Map<String, Object> oltProps = safeProps(olt.getProperties());
                        flags.put("SERVICE_PORT2_EXIST", existsString(oltProps.get("port2Template")));
                        flags.put("SERVICE_PORT3_EXIST", existsString(oltProps.get("port3Template")));
                        flags.put("SERVICE_PORT4_EXIST", existsString(oltProps.get("port4Template")));
                        flags.put("SERVICE_PORT5_EXIST", existsString(oltProps.get("port5Template")));
                        log.error("Trace: OLT port templates checked for OLT=" + oltPos);
                    });
                }
                if (ontPort != null) {
                    if ("3".equals(ontPort) || "4".equals(ontPort) || "5".equals(ontPort)) {
                        flags.put("SERVICE_TEMPLATE_PORT", "Exist");
                    } else if ("2".equals(ontPort)) {
                        flags.put("SERVICE_TEMPLATE_PORT", "New");
                    }
                }
            }

            log.error("------------Test Trace # 15---------------");
            flags.putIfAbsent("SERVICE_EXIST", "Exist");
            flags.putIfAbsent("SERVICE_IPTV_EXIST", flags.getOrDefault("SERVICE_TEMPLATE_IPTV", "New"));
            flags.putIfAbsent("QOS_PROFILE", flags.getOrDefault("QOS_PROFILE", ""));

            log.error("------------Test Trace # 16---------------");
            log.error(Constants.ACTION_COMPLETED);
            log.error("Trace: QueryFlags completed - returning flags map with " + flags.size() + " entries");
            return new QueryFlagsResponse("200", "UIV action QueryFlags executed successfully.", getCurrentTimestamp(), flags);

        } catch (Exception ex) {
            log.error("Unhandled exception during QueryFlags", ex);
            String msg = ERROR_PREFIX + "Internal server error occurred";
            return new QueryFlagsResponse("500", msg + " - " + ex.getMessage(), String.valueOf(System.currentTimeMillis()), Collections.emptyMap());
        }
    }


    private void initializeFlags(Map<String, String> flags) {
        String[] keys = {
                "SERVICE_EXIST","SERVICE_EVPN_EXIST","SERVICE_PORT_EXIST","SERVICE_VEIP_EXIST","SERVICE_VOIP_EXIST",
                "SERVICE_VOIP_NUMBER1","SERVICE_VOIP_NUMBER2","SERVICE_PORT2_EXIST","SERVICE_PORT3_EXIST","SERVICE_PORT4_EXIST",
                "SERVICE_POTS1_EXIST","SERVICE_POTS2_EXIST","SERVICE_TEMPLATE_VLAN","SERVICE_VLAN_ID","SERVICE_TEMPLATE_CREATE",
                "SERVICE_TEMPLATE_CARD","SERVICE_TEMPLATE_PORT","SERVICE_TEMPLATE_MGMT","SERVICE_TEMPLATE_MGMT_CREATE",
                "SERVICE_TEMPLATE_VEIP","SERVICE_TEMPLATE_HSI","SERVICE_TEMPLATE_ONT","SERVICE_TEMPLATE_VOIP","SERVICE_TEMPLATE_POTS1",
                "SERVICE_TEMPLATE_POTS2","SERVICE_HSI_EXIST","SERVICE_LINK","SERVICE_SN","SERVICE_MAC","ONT_MODEL","SERVICE_PORT5_EXIST",
                "SERVICE_TEMPLATE_VPLS","SERVICE_TEMPLATE_IPTV","SERVICE_IPTV_EXIST","SERVICE_ID","SERVICE_EVPN_WIFIM_FIRST",
                "OLT_POSITION","ONT_TEMPLATE","SERVICE_OLT_POSITION","CBM_MAC","IPTV_COUNT","FIBERNET_COUNT","QOS_PROFILE",
                "FIRST_NAME","LAST_NAME","ACCOUNT_EXIST","SERVICE_FLAG","SERVICE_ONT_PORT","KENAN_UIDNO","SIMA_CUST_ID",
                "CBM_ACCOUNT_EXIST","VOICE_POTS_PORT","RESOURCE_MAC_MTA_OLD","RESOURCE_MODEL_MTA_OLD","BRIDGE_SERVICE",
                "QOS_PROFILE_BRIDGE"
        };
        for (String k : keys) flags.put(k, "");
    }

    private Map<String, Object> safeProps(Map<String, Object> p) {
        return p == null ? new HashMap<>() : p;
    }

    private boolean equalsIgnoreCase(String a, String b) {
        return a != null && b != null && a.equalsIgnoreCase(b);
    }

    private boolean equalsAnyIgnoreCase(String val, String... tokens) {
        if (val == null) return false;
        for (String t : tokens) if (t != null && val.equalsIgnoreCase(t)) return true;
        return false;
    }

    private boolean containsIgnoreCase(String val, String token) {
        if (val == null || token == null) return false;
        return val.toLowerCase().contains(token.toLowerCase());
    }

    private String existsString(Object o) {
        if (o == null) return "New";
        String s = String.valueOf(o);
        if (s.trim().isEmpty()) return "New";
        return "Exist";
    }

    private String deriveBridgeServiceForSubscriberRfs(
            Iterable<ResourceFacingService> allRfs,
            String ontSN,
            String subscriber) {

        String bridgeService = "NA";

        if (allRfs == null || ontSN == null) {
            return bridgeService;
        }

        for (ResourceFacingService rfs : allRfs) {
            try {
                ResourceFacingService rfs1=rfsRepository.findByDiscoveredName(rfs.getDiscoveredName()).get();
                CustomerFacingService cfs=rfs1.getContainingCfs();
                cfs = cfsRepository.findByDiscoveredName(cfs.getDiscoveredName()).get();
                Product product = cfs.getContainingProduct();
                product=productRepository.findByDiscoveredName(product.getDiscoveredName()).get();
                Subscription sub=product.getSubscription();

                if (sub == null || sub.getDiscoveredName() == null) {
                    continue;
                }

                Map<String, Object> sp = safeProps(sub.getProperties());

                if ("Bridged".equalsIgnoreCase(
                        (String) sp.getOrDefault("serviceSubType", "")) &&
                        sub.getDiscoveredName().contains(ontSN)) {

                    // 🔥 DO NOT RETURN – last match wins
                    bridgeService =
                            (String) sp.getOrDefault("serviceID", "NA");
                }
            } catch (Exception ignore) {
            }
        }
        return bridgeService;
    }

    private Map<String, String> executeStep6Flags(
            String ontSN,
            String serviceID,
            String subscriber,
            String actionType,
            String productSubType) {

        Map<String, String> result = new HashMap<>();

        result.put("ACCOUNT_EXIST", "New");
        result.put("SERVICE_FLAG", "New");
        result.put("CBM_ACCOUNT_EXIST", "New");

        List<Subscription> subsForCustomer = new ArrayList<>();

        for (Subscription s : subscriptionRepository.findAll()) {
            if (s.getDiscoveredName() != null &&
                    s.getDiscoveredName().contains(subscriber)) {
                subsForCustomer.add(s);
            }
        }

        if (Arrays.asList("Unconfigure", "MoveOut",
                        "ChangeTechnology", "AccountTransfer")
                .contains(actionType)) {

            int subCount = subsForCustomer.size();

            if (subCount == 0) {
                // No subscriptions → New account
                result.put("ACCOUNT_EXIST", "New");
                result.put("SERVICE_FLAG", "New");
                result.put("CBM_ACCOUNT_EXIST", "New");

            } else if (subCount == 1) {
                // Single subscription → still New
                result.put("ACCOUNT_EXIST", "New");
                result.put("SERVICE_FLAG", "New");

                Map<String, Object> p =
                        safeProps(subsForCustomer.get(0).getProperties());

                if ("Cable_Modem".equalsIgnoreCase(
                        (String) p.get("serviceLink"))) {
                    result.put("CBM_ACCOUNT_EXIST", "New");
                }

                Object sima = p.get("simaCustomerId");
                if (sima != null && !sima.toString().isEmpty()) {
                    result.put("SIMA_CUST_ID", sima.toString());
                }

            } else {
                // More than one subscription → Existing account
                result.put("ACCOUNT_EXIST", "Exist");
                result.put("SERVICE_FLAG", "Exist");

                Set<String> macSet = new HashSet<>();

                for (Subscription s : subsForCustomer) {
                    Map<String, Object> p = safeProps(s.getProperties());

                    if ("Cable_Modem".equalsIgnoreCase(
                            (String) p.get("serviceLink"))) {

                        Object mac = p.get("macAddress");
                        if (mac != null) macSet.add(mac.toString());
                    }

                    Object sima = p.get("simaCustomerId");
                    if (sima != null && !sima.toString().isEmpty()) {
                        result.put("SIMA_CUST_ID", sima.toString());
                    }
                }

                result.put("CBM_ACCOUNT_EXIST",
                        macSet.size() > 1 ? "Exist" : "New");
            }
        }


        return result;
    }

    private String getCurrentTimestamp() {
        return Instant.now().toString();
    }
}
