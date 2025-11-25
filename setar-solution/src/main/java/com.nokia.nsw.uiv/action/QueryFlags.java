package com.nokia.nsw.uiv.action;

import com.nokia.nsw.uiv.exception.BadRequestException;
import com.nokia.nsw.uiv.framework.action.Action;
import com.nokia.nsw.uiv.framework.action.ActionContext;
import com.nokia.nsw.uiv.framework.action.HttpAction;
import com.nokia.nsw.uiv.model.resource.Resource;
import com.nokia.nsw.uiv.model.resource.logical.LogicalDevice;
import com.nokia.nsw.uiv.model.resource.logical.LogicalInterface;
import com.nokia.nsw.uiv.model.service.Subscription;
import com.nokia.nsw.uiv.repository.*;
import com.nokia.nsw.uiv.request.QueryFlagsRequest;
import com.nokia.nsw.uiv.response.QueryFlagsResponse;
import com.nokia.nsw.uiv.utils.Constants;
import com.nokia.nsw.uiv.utils.Validations;
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
        System.out.println("------------Test Trace # 1---------------");
        log.warn(Constants.EXECUTING_ACTION, ACTION_LABEL);

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

        try {
            log.info("------------Test Trace # 2---------------");
            log.info("Validating mandatory parameters...");
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
            log.info("Mandatory validation completed.");

            log.info("------------Test Trace # 3---------------");
            if (!equalsIgnoreCase(productType, "VOIP") && !equalsIgnoreCase(productType, "Voice")) {
                log.info("Trace: Non-voice product -> default VOIP ports to Available");
                flags.put("SERVICE_VOIP_NUMBER1", "Available");
                flags.put("SERVICE_VOIP_NUMBER2", "Available");
            }

            log.info("------------Test Trace # 4---------------");
            String serviceLink = "NA";
            if (ontSN != null) {
                if (ontSN.startsWith("ALCL")) {
                    serviceLink = "ONT";
                    log.info("Trace: ontSN startsWith ALCL -> serviceLink=ONT");
                } else if (ontSN.startsWith("CW")) {
                    serviceLink = "SRX";
                    log.info("Trace: ontSN startsWith CW -> serviceLink=SRX");
                } else {
                    log.info("Trace: ontSN pattern not recognized -> serviceLink=NA");
                }
            }
            flags.put("SERVICE_LINK", serviceLink);

            log.info("------------Test Trace # 5---------------");
            boolean subtypeMatches = equalsAnyIgnoreCase(productSubType, "Broadband", "Voice", "Cloudstarter", "Bridged");
            if ((subtypeMatches || equalsIgnoreCase(productType, "ENTERPRISE"))
                    && !"Configure".equalsIgnoreCase(actionType)) {
                if (ontSN == null || ontSN.trim().isEmpty() || "NA".equalsIgnoreCase(ontSN)) {
                    try {
                        String rfsLocalName = "RFS_" + subscriber + "_" + (serviceID == null ? "" : serviceID);
                        Iterable<ResourceFacingService> allRfs = rfsRepository.findAll();

                        for (ResourceFacingService rfs : allRfs) {
                            if (rfs.getDiscoveredName() != null &&
                                    rfs.getDiscoveredName().equalsIgnoreCase(rfsLocalName)) {

                                if (rfs.getContainingCfs() != null &&
                                        rfs.getContainingCfs().getContainingProduct() != null &&
                                        rfs.getContainingCfs().getContainingProduct().getSubscription() != null) {

                                    Subscription sub = rfs.getContainingCfs()
                                            .getContainingProduct()
                                            .getSubscription();
                                    String subServiceId = (String) safeProps(sub.getProperties())
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
                        log.warn("RFS discovery best-effort failed: {}", e.getMessage());
                    }
                }
            }

            log.info("------------Test Trace # 6---------------");
            if (equalsIgnoreCase(productType, "VOIP") && equalsIgnoreCase(actionType, "Configure") && serviceID != null) {
                log.info("Trace: VOIP Configure flow - checking voip device mapping");
                String voipDeviceName = subscriber + "_" + serviceID;
                Optional<LogicalDevice> optVoip = deviceRepository.findByDiscoveredName(voipDeviceName);

                if (optVoip.isPresent()) {
                    Map<String, Object> p = safeProps(optVoip.get().getProperties());
                    String pots1 = (String) p.getOrDefault("potsPort1Number", "");
                    String pots2 = (String) p.getOrDefault("potsPort2Number", "");
                    if (serviceID.equals(pots1)) {
                        flags.put("VOICE_POTS_PORT", "1");
                        log.info("Trace: VOIP pots mapped on port1");
                    } else if (serviceID.equals(pots2)) {
                        flags.put("VOICE_POTS_PORT", "2");
                        log.info("Trace: VOIP pots mapped on port2");
                    }
                } else {
                    log.info("Trace: VOIP device not found by GDN - scanning all devices (best-effort)");
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

            log.info("------------Test Trace # 7---------------");
            boolean subscriberExists = customerRepository.findByDiscoveredName(subscriber).isPresent();
            List<Subscription> subsForCustomer = new ArrayList<>();
            List<Subscription> subs = (List<Subscription>) subscriptionRepository.findAll();
            for (Subscription s:subs) {
                if (s.getDiscoveredName() != null && s.getDiscoveredName().startsWith(subscriber))
                    subsForCustomer.add(s);
            }
            if (Arrays.asList("Unconfigure", "MoveOut", "ChangeTechnology", "AccountTransfer").contains(actionType)) {
                log.info("Trace: Action in Unconfigure/MoveOut/ChangeTechnology/AccountTransfer");
                if (subsForCustomer.size() <= 1) {
                    flags.put("ACCOUNT_EXIST", "New");
                    flags.put("SERVICE_FLAG", "New");
                } else {
                    flags.put("ACCOUNT_EXIST", "Exist");
                    flags.put("SERVICE_FLAG", "Exist");
                }
                boolean anyCbm = subsForCustomer.stream().anyMatch(s -> {
                    Object l = safeProps(s.getProperties()).get("serviceLink");
                    return l != null && l.toString().contains("Cable_Modem");
                });
                flags.put("CBM_ACCOUNT_EXIST", anyCbm ? "Exist" : "New");
            } else if (!equalsIgnoreCase(actionType, "Configure") && ontSN != null && ontSN.contains("ALCL")) {
                log.info("Trace: Configure with ALCL ONT -> check subscriber_ONT existence");
                String subscriberWithOnt = subscriber + "_" + ontSN;
                boolean exists = customerRepository.findByDiscoveredName(subscriberWithOnt).isPresent();
                flags.put("ACCOUNT_EXIST", exists ? "Exist" : "New");
                flags.put("SERVICE_FLAG", exists ? "Exist" : "New");
            } else if (equalsIgnoreCase(actionType, "Migrate") && ontSN != null && ontSN.contains("ALCL")) {
                log.info("Trace: Migrate with ALCL ONT -> default flags New, check subscriptions for existence");
                flags.put("ACCOUNT_EXIST", "New");
                flags.put("SERVICE_FLAG", "New");
                String finalOntSN1 = ontSN;
                boolean found = subsForCustomer.stream().anyMatch(s -> s.getDiscoveredName() != null && s.getDiscoveredName().contains(finalOntSN1));
                if (found) {
                    flags.put("ACCOUNT_EXIST", "Exist");
                    flags.put("SERVICE_FLAG", "Exist");
                    String finalOntSN = ontSN;
                    subsForCustomer.stream().filter(s -> s.getDiscoveredName() != null && s.getDiscoveredName().contains(finalOntSN)).findFirst().ifPresent(s -> {
                        Object sima = safeProps(s.getProperties()).get("simaCustomerId");
                        flags.put("SIMA_CUST_ID", sima == null ? "" : sima.toString());
                    });
                }
            } else {
                log.info("Trace: Default account/service flag handling");
                flags.put("ACCOUNT_EXIST", subscriberExists ? "Exist" : "New");
                boolean anyCbm = subsForCustomer.stream().anyMatch(s -> {
                    Object l = safeProps(s.getProperties()).get("serviceLink");
                    return l != null && l.toString().contains("Cable_Modem");
                });
                flags.put("CBM_ACCOUNT_EXIST", anyCbm ? "Exist" : "New");
                flags.put("SERVICE_FLAG", subscriberExists ? "Exist" : "New");
            }

            log.info("------------Test Trace # 8---------------");
            if (ontSN != null && !"".equals(ontSN) && equalsIgnoreCase(productSubType, "IPTV") && equalsIgnoreCase(actionType, "Unconfigure")) {
                log.info("Trace: IPTV Unconfigure path - searching subscription");
                String subGdn = subscriber + "_" + (serviceID == null ? "" : serviceID);
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
                    log.info("Trace: IPTV count for ontSNO=" + ontSNO + " is " + iptvCount);
                }
            }

            log.info("------------Test Trace # 9---------------");
            List<String> subscount = new ArrayList<>();
            if (equalsAnyIgnoreCase(productSubType, "Fibernet", "Broadband", "Voice", "Bridged")
                    || (equalsIgnoreCase(productType, "Broadband") && equalsIgnoreCase(productSubType, "Bridged"))) {
                log.info("Trace: Searching subscriptions for fibernet/bridged related entries");
                for (Subscription s : subscriptionRepository.findAll()) {
                    String name = s.getDiscoveredName() == null ? "" : s.getDiscoveredName();
                    if (ontSN != null && ontSN.contains("ALCL")) {
                        if (name.endsWith(ontSN)) {
                            subscount.add(name);
                            Map<String, Object> sp = safeProps(s.getProperties());
                            if ("Bridged".equalsIgnoreCase((String) sp.getOrDefault("serviceSubType", "")) && name.contains(ontSN)) {
                                String qos = (String) sp.getOrDefault("evpnQosSessionProfile", "");
                                if (qos != null && !qos.isEmpty()) flags.put("QOS_PROFILE_BRIDGE", qos);
                                log.info("Trace: Found bridged subscription with QOS profile: " + qos);
                            }
                        }
                    } else {
                        if (name.startsWith(subscriber)) {
                            subscount.add(name);
                        }
                    }
                }
            }
            flags.put("FIBERNET_COUNT", subscount.isEmpty() ? "0" : String.valueOf(subscount.size()));
            log.info("Trace: Fibernet count = " + flags.get("FIBERNET_COUNT"));

            log.info("------------Test Trace # 10---------------");
            if (!equalsIgnoreCase(actionType, "Configure")) {
                String subscriptionToSearch;
                if (ontSN != null && ontSN.contains("ALCL")) {
                    subscriptionToSearch = subscriber + "_" + (serviceID == null ? "" : serviceID) + "_" + ontSN;
                } else {
                    subscriptionToSearch = subscriber + "_" + (serviceID == null ? "" : serviceID);
                }
                log.info("Trace: Searching subscription by DN: " + subscriptionToSearch);
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
                        String cbmName = "CBM_" + (sMAC == null ? "" : sMAC.toString());
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
                            log.info("Trace: CBM inspected: mac=" + flags.get("CBM_MAC") + " voip1=" + flags.get("SERVICE_VOIP_NUMBER1"));
                        } else {
                            String alt = "CBM" + (serviceID == null ? "" : serviceID);
                            deviceRepository.findByDiscoveredName(alt).ifPresent(dev -> {
                                Map<String, Object> dp = safeProps(dev.getProperties());
                                flags.put("ONT_MODEL", (String) dp.getOrDefault("deviceModel", ""));
                            });
                        }
                    }
                }
            }

            log.info("------------Test Trace # 11---------------");
            customerRepository.findByDiscoveredName(subscriber).ifPresent(cust -> {
                Map<String, Object> cp = safeProps(cust.getProperties());
                flags.put("FIRST_NAME", (String) cp.getOrDefault("subscriberFirstName", ""));
                flags.put("LAST_NAME", (String) cp.getOrDefault("subscriberLastName", ""));
                log.info("Trace: Subscriber info: " + flags.get("FIRST_NAME") + " " + flags.get("LAST_NAME"));
            });

            log.info("------------Test Trace # 12---------------");
            if ("ONT".equalsIgnoreCase(serviceLink) || "SRX".equalsIgnoreCase(serviceLink) || (ontSN != null && ontSN.contains("ALCL"))) {
                String ontGdn = ontSN == null ? "" : "ONT" + ontSN;
                if (ontGdn.length() > 100) {
                    return new QueryFlagsResponse("400", ERROR_PREFIX + "ONT name too long", getCurrentTimestamp(), Collections.emptyMap());
                }
                Optional<LogicalDevice> optOntDev = deviceRepository.findByDiscoveredName(ontGdn);
                if (optOntDev.isPresent()) {
                    LogicalDevice ontDev = optOntDev.get();
                    Map<String, Object> ontProps = safeProps(ontDev.getProperties());
                    flags.put("ONT_MODEL", (String) ontProps.getOrDefault("deviceModel", ""));
                    flags.put("SERVICE_SN", (String) ontProps.getOrDefault("serialNo", ""));
                    flags.put("SERVICE_MAC", (String) ontProps.getOrDefault("macAddress", ""));
                    log.info("Trace: ONT found: model=" + flags.get("ONT_MODEL") + " sn=" + flags.get("SERVICE_SN"));

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
                            log.info("Trace: OLT templates checked for OLT=" + oltGdn);
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
                                log.info("Trace: VLAN " + vl + " uses management template " + vp.get("template"));
                            }
                        });
                    }
                } else {
                    log.info("Trace: ONT device not found by GDN: " + ontGdn);
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
                log.info("Trace: CBM path: IPTV exist=" + flags.get("SERVICE_TEMPLATE_IPTV") + " VEIP exist=" + flags.get("SERVICE_TEMPLATE_VEIP"));
            }

            log.info("------------Test Trace # 13---------------");
            if (Arrays.asList("AccountTransfer", "MoveOut", "ChangeTechnology", "Unconfigure").contains(actionType) || (actionType != null && actionType.contains("Modify_CPE"))) {
                log.info("Trace: IPTV service ID list discovery path");
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
                        String cbmGdn = "CBM_" + cbmMac;
                        deviceRepository.findByDiscoveredName(cbmGdn).ifPresent(cpe -> {
                            Map<String, Object> cp = safeProps(cpe.getProperties());
                            flags.put("RESOURCE_MAC_MTA_OLD", (String) cp.getOrDefault("macAddressMta", ""));
                            flags.put("RESOURCE_MODEL_MTA_OLD", (String) cp.getOrDefault("deviceModelMta", ""));
                        });
                    }
                }
                flags.put("IPTV_COUNT", String.valueOf(iptvIds.size()));
                log.info("Trace: IPTV IDs discovered count = " + iptvIds.size());
            }

            log.info("------------Test Trace # 14---------------");
            if ((equalsAnyIgnoreCase(productType, "EVPN", "ENTERPRISE") || equalsAnyIgnoreCase(productSubType, "Cloudstarter", "Bridged"))
                    && (equalsAnyIgnoreCase(actionType, "Configure", "Migrate"))) {
                log.info("Trace: Evaluating template requirements for EVPN/Enterprise/Cloudstarter/Bridged");
                String oltPos = flags.getOrDefault("OLT_POSITION", "");
                if (!oltPos.isEmpty()) {
                    String oltPosGdn = Validations.getGlobalName(oltPos);
                    deviceRepository.findByDiscoveredName(oltPos).ifPresent(olt -> {
                        Map<String, Object> oltProps = safeProps(olt.getProperties());
                        flags.put("SERVICE_PORT2_EXIST", existsString(oltProps.get("port2Template")));
                        flags.put("SERVICE_PORT3_EXIST", existsString(oltProps.get("port3Template")));
                        flags.put("SERVICE_PORT4_EXIST", existsString(oltProps.get("port4Template")));
                        flags.put("SERVICE_PORT5_EXIST", existsString(oltProps.get("port5Template")));
                        log.info("Trace: OLT port templates checked for OLT=" + oltPos);
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

            log.info("------------Test Trace # 15---------------");
            flags.putIfAbsent("SERVICE_EXIST", "Exist");
            flags.putIfAbsent("SERVICE_IPTV_EXIST", flags.getOrDefault("SERVICE_TEMPLATE_IPTV", "New"));
            flags.putIfAbsent("QOS_PROFILE", flags.getOrDefault("QOS_PROFILE", ""));

            log.info("------------Test Trace # 16---------------");
            log.info(Constants.ACTION_COMPLETED);
            log.info("Trace: QueryFlags completed - returning flags map with " + flags.size() + " entries");
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

    private String deriveBridgeServiceForSubscriberRfs(Iterable<ResourceFacingService> allRfs, String ontSN, String subscriber) {
        if (allRfs == null) return "NA";
        for (ResourceFacingService rfs : allRfs) {
            try {
                if (rfs.getContainingCfs() == null || rfs.getContainingCfs().getContainingProduct() == null) continue;
                Subscription sub = rfs.getContainingCfs().getContainingProduct().getSubscription();
                if (sub == null) continue;
                Map<String, Object> sp = safeProps(sub.getProperties());
                if ("Bridged".equalsIgnoreCase((String) sp.getOrDefault("serviceSubType", "")) &&
                        sub.getDiscoveredName() != null && ontSN != null && sub.getDiscoveredName().contains(ontSN)) {
                    return (String) sp.getOrDefault("serviceID", "NA");
                }
            } catch (Exception ignore) {
            }
        }
        return "NA";
    }

    private String getCurrentTimestamp() {
        return Instant.now().toString();
    }
}
