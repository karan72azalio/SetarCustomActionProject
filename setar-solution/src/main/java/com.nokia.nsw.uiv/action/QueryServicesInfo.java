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
import com.nokia.nsw.uiv.request.QueryServicesInfoRequest;
import com.nokia.nsw.uiv.response.QueryServicesInfoResponse;
import com.nokia.nsw.uiv.utils.Validations;
import com.setar.uiv.model.product.Product;
import com.setar.uiv.model.product.ResourceFacingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;

@Component
@Action
@Slf4j
public class QueryServicesInfo implements HttpAction {

    private static final String ACTION_LABEL = "QueryServicesInfo";
    private static final String ERROR_PREFIX = "UIV action QueryServicesInfo execution failed - ";

    @Autowired
    private CustomerCustomRepository customerRepository;

    @Autowired
    private SubscriptionCustomRepository subscriptionRepository;

    @Autowired
    private ProductCustomRepository productRepository;

    @Autowired
    private CustomerFacingServiceCustomRepository cfsRepository;

    @Autowired
    private ResourceFacingServiceCustomRepository rfsRepository;

    @Autowired
    private LogicalDeviceCustomRepository logicalDeviceRepository;

    @Autowired
    private LogicalInterfaceCustomRepository logicalInterfaceRepository;

    @Override
    public Class<?> getActionClass() {
        return QueryServicesInfoRequest.class;
    }

    @Override
    public Object doPost(ActionContext actionContext) throws Exception {
        log.info("Executing action {}", ACTION_LABEL);
        QueryServicesInfoRequest request = (QueryServicesInfoRequest) actionContext.getObject();

        try {
            // 1) Input validations (both optional but at least one required)
            String accno = request.getSubscriberName();
            String ontSN = request.getOntSn();
            if ((accno == null || accno.trim().isEmpty()) && (ontSN == null || ontSN.trim().isEmpty())) {
                return createErrorResponse("400", ERROR_PREFIX + "Missing mandatory parameter(s): SUBSCRIBER_NAME or ONT_SN");
            }

            log.info("QueryServicesInfo start: subscriberName='{}', ontSN='{}'", accno, ontSN);

            // 2) Find initial candidate services
            List<ResourceFacingService> setarsRFS = new ArrayList<>();
            boolean isAccno = false;
            ResourceFacingService iptvrfsname = null; // candidate to add later

            if (accno != null && !accno.trim().isEmpty()) {
                isAccno = true;
                log.debug("Searching RFS by subscriber/account number '{}'", accno);
                java.util.List<ResourceFacingService> resourceFacingServices = (List<ResourceFacingService>) rfsRepository.findAll();
                if(!resourceFacingServices.isEmpty()){
                    for(ResourceFacingService rfs:resourceFacingServices)
                    {
                        if(rfs.getDiscoveredName().contains(accno))
                        {
                            setarsRFS= Collections.singletonList(rfs);
                        }
                    }
                }

            } else {
                // ontSN branch
                log.debug("Searching RFS by ontSN '{}'", ontSN);
                List<ResourceFacingService> rfsByOnt = new ArrayList<>();
                java.util.List<ResourceFacingService> resourceFacingServicesONT = (List<ResourceFacingService>) rfsRepository.findAll();
                if(!resourceFacingServicesONT.isEmpty()){
                    for(ResourceFacingService rfs:resourceFacingServicesONT)
                    {
                        if(rfs.getDiscoveredName().contains(ontSN))
                        {
                            rfsByOnt= Collections.singletonList(rfs);
                        }
                    }
                }

                if (rfsByOnt != null && !rfsByOnt.isEmpty()) {
                    setarsRFS.addAll(rfsByOnt);
                    if (rfsByOnt.size() >= 2) {
                        String rfsName2 = rfsByOnt.get(1).getDiscoveredName();
                        if (rfsName2 != null) {
                            String[] parts = rfsName2.split("_", -1);
                            if (parts.length >= 2) {
                                String anchorAccNo = parts[1];
                                List<ResourceFacingService> candidates = new ArrayList<>();
                                java.util.List<ResourceFacingService> resourceFacingServicesCand = (List<ResourceFacingService>) rfsRepository.findAll();
                                if(!resourceFacingServicesCand.isEmpty()){
                                    for(ResourceFacingService rfs:resourceFacingServicesCand)
                                    {
                                        if(rfs.getDiscoveredName().contains(anchorAccNo))
                                        {
                                            rfsByOnt= Collections.singletonList(rfs);
                                        }
                                    }
                                }
                                if (candidates != null && !candidates.isEmpty()) {
                                    // iptvrfsname = last candidate whose Name does NOT contain ontSN
                                    for (int i = candidates.size() - 1; i >= 0; i--) {
                                        ResourceFacingService c = candidates.get(i);
                                        if (c.getDiscoveredName() == null || !c.getDiscoveredName().contains(ontSN)) {
                                            iptvrfsname = c;
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3) Decide which services to process
            List<ResourceFacingService> setarRFSs = new ArrayList<>();
            if (setarsRFS != null && !setarsRFS.isEmpty()) setarRFSs.addAll(setarsRFS);
            if (iptvrfsname != null) setarRFSs.add(iptvrfsname);

            if (setarRFSs.isEmpty()) {
                log.warn("No candidate RFS found for inputs subscriber='{}' ontSN='{}'", accno, ontSN);
                return createErrorResponse("404", ERROR_PREFIX + "Error, No Service Details Available.");
            }

            // 4) Prepare working space (collectors & flags)
            boolean success = true;
            int p = 1, q = 1, r = 1, s = 1, u = 1; // counters
            Map<String, Object> allvalues = new LinkedHashMap<>();

            // 5) For each service
            for (ResourceFacingService rfs : setarRFSs) {
                try {
                    String rfsnameget = rfs.getDiscoveredName() == null ? "" : rfs.getDiscoveredName();
                    String serviceType = rfs.getProperties().get("ServiceTypeName") == null ? "" : rfs.getProperties().get("ServiceTypeName").toString();
                    log.info("Processing RFS '{}' with serviceType='{}'", rfsnameget, serviceType);

                    // initialize per-service vars
                    String subscriber = "";
                    String serviceID = "";
                    String setarSubscribername = null;
                    String ontSno = "";
                    String cbmSubscriberID = "";
                    boolean isIptvCandidate = serviceType.equalsIgnoreCase("IPTV");

                    // resources and components collectors
                    List<String> resourcesAP = new ArrayList<>();
                    List<String> resourcesSTB = new ArrayList<>();
                    List<String> listOfComponents = new ArrayList<>();

                    // 5.1 Derive subscriber, serviceID, subscriptionName
                    String subscriptionName = "";
                    if (isIptvCandidate) {
                        String[] rfsnames = rfsnameget.split("_", -1);
                        if (rfsnames.length >= 3) {
                            subscriber = rfsnames[1];
                            serviceID = rfsnames[2];
                            subscriptionName = subscriber + "_" + serviceID;
                        } else {
                            log.debug("IPTV rfs name not parsable: {}", rfsnameget);
                        }
                    } else {
                        String fiberrfsname = rfsnameget;
                        if (fiberrfsname.contains("ALC")) {
                            // tokens: ... _ACC_NO_..._ONT_SNO
                            String[] tokens = fiberrfsname.split("_", -1);
                            if (tokens.length >= 3) {
                                String accNo = tokens[1];
                                String ontLast = tokens[tokens.length - 1];
                                // SID = middle tokens joined by "_"
                                StringBuilder sidBuilder = new StringBuilder();
                                for (int i = 2; i < tokens.length - 1; i++) {
                                    if (sidBuilder.length() > 0) sidBuilder.append("_");
                                    sidBuilder.append(tokens[i]);
                                }
                                String sid = sidBuilder.toString();
                                subscriber = accNo;
                                serviceID = sid;
                                ontSno = ontLast;
                                subscriptionName = subscriber + "_" + serviceID + "_" + ontSno;
                                setarSubscribername = subscriber + "_" + ontSno;
                            } else {
                                log.debug("ALC-format rfs name not parsable: {}", fiberrfsname);
                            }
                        } else {
                            String[] fibesubname = fiberrfsname.split("_", -1);
                            if (fibesubname.length >= 3) {
                                subscriber = fibesubname[1];
                                // serviceID is token[2] plus remaining tokens
                                StringBuilder sidBuilder = new StringBuilder();
                                sidBuilder.append(fibesubname[2]);
                                for (int i = 3; i < fibesubname.length; i++) {
                                    sidBuilder.append("_").append(fibesubname[i]);
                                }
                                serviceID = sidBuilder.toString();
                                subscriptionName = subscriber + "_" + serviceID;
                            } else {
                                log.debug("Generic format rfs name not parsable: {}", fiberrfsname);
                            }
                        }
                    }

                    log.debug("Derived subscriber='{}' serviceID='{}' subscriptionName='{}' ontSno='{}' setarSubscribername='{}'",
                            subscriber, serviceID, subscriptionName, ontSno, setarSubscribername);

                    // 5.2 Resolve subscription and linked entities
                    Subscription setarSubscription = null;
                    if (subscriptionName != null && !subscriptionName.isEmpty()) {
                        Optional<Subscription> optSub = subscriptionRepository.findByDiscoveredName(subscriptionName);
                        if (optSub.isPresent()) setarSubscription = optSub.get();
                    }

                    if (setarSubscription == null) {
                        // Try alternative: sometimes subscriptionName may be subscriber_serviceId only (no ONT)
                        String altSubName = subscriber + "_" + serviceID;
                        Optional<Subscription> optSubAlt = subscriptionRepository.findByDiscoveredName(altSubName);
                        if (optSubAlt.isPresent()) setarSubscription = optSubAlt.get();
                    }

                    if (setarSubscription != null) {
                        // if ServiceLink equals ONT (case-insensitive) and ServiceSN present then ontSno override
                        Object slObj = setarSubscription.getProperties() == null ? null : setarSubscription.getProperties().get("serviceLink");
                        String serviceLink = slObj == null ? (setarSubscription.getProperties().get("serviceLink") == null ? "" : setarSubscription.getProperties().get("serviceLink").toString()) : slObj.toString();
                        Object ssn = setarSubscription.getProperties() == null ? null : setarSubscription.getProperties().get("serviceSN");
                        if ((serviceLink != null && serviceLink.equalsIgnoreCase("ONT")) && (ssn != null && !ssn.toString().isEmpty())) {
                            ontSno = ssn.toString();
                        }

                        Object cbm = setarSubscription.getProperties() == null ? null : setarSubscription.getProperties().get("subscriberId_CableModem");
                        if (cbm != null && (serviceLink == null || !serviceLink.equalsIgnoreCase("ONT"))) {
                            cbmSubscriberID = cbm.toString();
                            // setarSubscribername then from subscription's subscriber
                            if (setarSubscription.getCustomer() != null) {
                                setarSubscribername = setarSubscription.getCustomer().getDiscoveredName();
                            }
                        }
                    } else {
                        log.debug("Subscription '{}' not found for RFS '{}'", subscriptionName, rfsnameget);
                    }

                    // Resolve setarSubscriber
                    Customer setarSubscriber = null;
                    if (serviceType != null && serviceType.toUpperCase().contains("IPTV") && setarSubscription != null) {
                        setarSubscriber = setarSubscription.getCustomer();
                    } else if (setarSubscribername != null) {
                        Optional<Customer> optCust = customerRepository.findByDiscoveredName(setarSubscribername);
                        if (optCust.isPresent()) setarSubscriber = optCust.get();
                    } else if (subscriber != null && !subscriber.isEmpty() && ontSno != null && !ontSno.isEmpty()) {
                        // fallback subscriber + ont
                        String name = subscriber + "_" + ontSno;
                        Optional<Customer> optCust = customerRepository.findByDiscoveredName(name);
                        if (optCust.isPresent()) setarSubscriber = optCust.get();
                    }

                    // productName and product lookup
                    String productName = "";
                    Product setarProduct = null;
                    if (serviceID != null && !serviceID.isEmpty() && setarSubscription != null) {
                        String serviceSubType = (setarSubscription.getProperties() == null) ? "" : String.valueOf(setarSubscription.getProperties().getOrDefault("serviceSubType", ""));
                        productName = subscriber + "_" + serviceSubType + "_" + serviceID;
                        if (productName != null && !productName.isEmpty()) {
                            Optional<Product> optProd = productRepository.findByDiscoveredName(productName);
                            if (optProd.isPresent()) setarProduct = optProd.get();
                        }
                    }

                    // If serviceLink is ONT: resolve ONT and OLT
                    LogicalDevice nameONT = null;
                    LogicalDevice oltDevice = null;
                    if (setarSubscription != null) {
                        String serviceLink = setarSubscription.getProperties().get("serviceLink").toString();
                        if (serviceLink != null && serviceLink.equalsIgnoreCase("ONT")) {
                            // ensure ontSno present
                            if (ontSno == null || ontSno.trim().isEmpty()) {
                                // attempt read from subscription properties
                                Object ssn = setarSubscription.getProperties() == null ? null : setarSubscription.getProperties().get("serviceSN");
                                if (ssn != null) ontSno = ssn.toString();
                            }
                            if (ontSno != null && !ontSno.isEmpty()) {
                                String ontName = "ONT" + ontSno;
                                Optional<LogicalDevice> optOnt = logicalDeviceRepository.findByDiscoveredName(ontName);
                                if (optOnt.isPresent()) {
                                    nameONT = optOnt.get();
                                    // try retrieving the OLT device via containing/managing relationships
                                    // assume nameONT.getManagingDevices() returns list or properties contain oltPosition
                                    Set<LogicalDevice> mng =  nameONT.getManagingDevices();
                                    if (mng != null && !mng.isEmpty()) {
                                        for(LogicalDevice device:mng){
                                            oltDevice = device;
                                            break;
                                        }
                                    } else {
                                        // fallback to property oltPosition on RFS or ONT
                                        Object oltPos = (rfs.getProperties() == null) ? null : rfs.getProperties().get("oltPosition");
                                        if (oltPos != null) {
                                            Optional<LogicalDevice> optOlt = logicalDeviceRepository.findByDiscoveredName(oltPos.toString());
                                            if (optOlt.isPresent()) oltDevice = optOlt.get();
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 5.3 Read resources attached to this RFS
                    Set<Resource> resources = rfs.getUsedResource();
                    if (resources != null && !resources.isEmpty()) {
                        for (Resource dev : resources) {
                            String resName = dev.getDiscoveredName() == null ? "" : dev.getDiscoveredName();
                            String serial = (dev.getProperties() == null) ? null : String.valueOf(dev.getProperties().get("serialNo"));
                            if (resName.startsWith("AP")) {
                                if (serial != null) resourcesAP.add(serial);
                            } else if (resName.startsWith("STB")) {
                                if (serial != null) resourcesSTB.add(serial);
                            }
                        }
                    }

                    // Components: products whose name begins with serviceID
                    List<Product> products = (List<Product>) productRepository.findAll();
                    List<Product> compos = new ArrayList<>();
                    if(!compos.isEmpty()){
                        for(Product pds:products)
                        {
                            if(pds.getDiscoveredName().startsWith(serviceID))
                            {
                                compos= Collections.singletonList(pds);
                            }
                        }
                    }
                    if (compos != null && !compos.isEmpty()) {
                        for (Product prod : compos) {
                            String pname = prod.getDiscoveredName();
                            if (pname != null) {
                                String[] parts = pname.split("_", -1);
                                if (parts.length >= 2) {
                                    listOfComponents.add(parts[1]);
                                }
                            }
                        }
                    }

                    // 5.4 Choose sname and ordinal t
                    String sname; int t;
                    String serviceSubType = (setarSubscription == null) ? "" : String.valueOf(setarSubscription.getProperties() == null ? "" : setarSubscription.getProperties().getOrDefault("serviceSubType", ""));
                    String serviceSubTypeLower = serviceSubType == null ? "" : serviceSubType.toLowerCase();
                    String serviceTypeLower = serviceType == null ? "" : serviceType.toLowerCase();

                    if (serviceType.equalsIgnoreCase("IPTV")) { sname = "IPTV"; t = p; }
                    else if (serviceSubTypeLower.contains("fibernet")) { sname = "FIBERNET"; t = q; }
                    else if (serviceSubTypeLower.contains("broadband")) { sname = "BROADBAND"; t = q; }
                    else if (serviceTypeLower.contains("voip")) { sname = "VOIP"; t = r; }
                    else if (serviceType.equalsIgnoreCase("Voice")) { sname = "VOICE"; t = r; }
                    else if (serviceTypeLower.contains("evpn") || serviceTypeLower.contains("enterprise")) { sname = "EVPN"; t = s; }
                    else if (serviceSubTypeLower.contains("cloudstarter")) { sname = "CLOUDSTARTER"; t = u; }
                    else { sname = "SERVICE"; t = 1; }

                    String prefix = sname + "_" + t + "_";

                    // 5.5 If IPTV, collect device serials and components (we already did)
                    boolean applicableType = serviceTypeLower.contains("iptv") ||
                            serviceSubTypeLower.contains("broadband") ||
                            serviceSubTypeLower.contains("fibernet") ||
                            serviceTypeLower.contains("voip") ||
                            serviceTypeLower.contains("voice") ||
                            serviceTypeLower.contains("evpn") ||
                            serviceTypeLower.contains("enterprise");

                    if (!applicableType) {
                        log.debug("Service type/subtype not in output set; skipping detailed output for RFS '{}'", rfsnameget);
                        // advance counters where appropriate and continue
                        if (serviceType.equalsIgnoreCase("IPTV")) p++;
                        else if (serviceSubTypeLower.contains("fibernet") || serviceSubTypeLower.contains("broadband")) q++;
                        else if (serviceTypeLower.contains("voip") || serviceType.equalsIgnoreCase("Voice")) r++;
                        else if (serviceTypeLower.contains("evpn") || serviceTypeLower.contains("enterprise")) s++;
                        else if (serviceSubTypeLower.contains("cloudstarter")) u++;
                        continue;
                    }

                    // 6) Declare and fill values into allvalues (key -> value)
                    // AP serials
                    for (int j = 0; j < resourcesAP.size(); j++) {
                        String key = prefix + "AP_SerialNo_" + (j+1);
                        allvalues.put(key, resourcesAP.get(j));
                    }
                    // STB serials
                    for (int j = 0; j < resourcesSTB.size(); j++) {
                        String key = prefix + "STB_SerialNo_" + (j+1);
                        allvalues.put(key, resourcesSTB.get(j));
                    }
                    // Components
                    for (int kidx = 0; kidx < listOfComponents.size(); kidx++) {
                        String key = prefix + "COMPONENT_" + (kidx+1);
                        allvalues.put(key, listOfComponents.get(kidx));
                    }

                    // If nameONT exists
                    if (nameONT != null) {
                        allvalues.put(prefix + "ONT_NAME", nameONT.getDiscoveredName());
                    }

                    // ServiceLink handling
                    String setarServiceLink = setarSubscription == null ? "" : (setarSubscription.getProperties().get("serviceLink") == null ? "" : setarSubscription.getProperties().get("serviceLink").toString());
                    if (setarServiceLink != null && setarServiceLink.equalsIgnoreCase("ONT")) {
                        if (oltDevice != null) {
                            allvalues.put(prefix + "OLT_NAME", oltDevice.getDiscoveredName());
                        } else {
                            Object oltpos = (rfs.getProperties() == null) ? null : rfs.getProperties().get("oltPosition");
                            if (oltpos != null) allvalues.put(prefix + "OLT_NAME", String.valueOf(oltpos));
                        }
                        // TEMPLATE_NAME_ONT from oltDevice or nameONT properties
                        String ontTemplate = null;
                        if (oltDevice != null && oltDevice.getProperties() != null) {
                            ontTemplate = (String) oltDevice.getProperties().getOrDefault("ontTemplate", null);
                        }
                        if (ontTemplate == null && nameONT != null && nameONT.getProperties() != null) {
                            ontTemplate = (String) nameONT.getProperties().getOrDefault("ontTemplate", "");
                        }
                        allvalues.put(prefix + "TEMPLATE_NAME_ONT", ontTemplate == null ? "" : ontTemplate);
                    } else {
                        // not ONT -> use ServiceMAC from subscription (CBM)
                        Object mac = (setarSubscription == null) ? null : (setarSubscription.getProperties() == null ? null : setarSubscription.getProperties().get("serviceMAC"));
                        allvalues.put(prefix + "CBM_MAC", mac == null ? "" : String.valueOf(mac));
                    }

                    // IPTV specific values
                    if (serviceType.equalsIgnoreCase("IPTV")) {
                        if (setarServiceLink != null && setarServiceLink.equalsIgnoreCase("ONT")) {
                            String veipIptv = (oltDevice != null && oltDevice.getProperties() != null) ? (String) oltDevice.getProperties().getOrDefault("veipIptvTemplate", "") : "";
                            String igmp = (oltDevice != null && oltDevice.getProperties() != null) ? (String) oltDevice.getProperties().getOrDefault("igmpTemplate", "") : "";
                            allvalues.put(prefix + "TEMPLATE_NAME_IPTV", veipIptv == null ? "" : veipIptv);
                            allvalues.put(prefix + "TEMPLATE_NAME_IGMP", igmp == null ? "" : igmp);
                        }
                        Object custGrp = (setarSubscription == null) ? null : setarSubscription.getProperties().get("customerGroupId");
                        Object iptvQos = (setarSubscription == null) ? null : setarSubscription.getProperties().get("iptvQosSessionProfile");
                        allvalues.put(prefix + "CUSTOMER_GROUP_ID", custGrp == null ? "" : String.valueOf(custGrp));
                        allvalues.put(prefix + "QOS_SESSION_PROFILE", iptvQos == null ? "" : String.valueOf(iptvQos));
                        allvalues.put(prefix + "SERVICE_LINK", setarServiceLink == null ? "" : setarServiceLink);
                        allvalues.put(prefix + "SERVICE_ID", serviceID == null ? "" : serviceID);
                    }

                    // Fibernet / Broadband specific
                    if (serviceSubTypeLower.contains("fibernet") || serviceSubTypeLower.contains("broadband")) {
                        if (setarServiceLink != null && setarServiceLink.equalsIgnoreCase("ONT")) {
                            String hsi = (oltDevice != null && oltDevice.getProperties() != null) ? (String) oltDevice.getProperties().getOrDefault("veipHsiTemplate", "") : "";
                            String veip = (oltDevice != null && oltDevice.getProperties() != null) ? (String) oltDevice.getProperties().getOrDefault("veipServiceTemplate", "") : "";
                            allvalues.put(prefix + "TEMPLATE_NAME_HSI", hsi == null ? "" : hsi);
                            allvalues.put(prefix + "TEMPLATE_NAME_VEIP", veip == null ? "" : veip);
                        }
                        Object veipQos = (setarSubscription == null) ? null : setarSubscription.getProperties().get("veipQosSessionProfile");
                        allvalues.put(prefix + "QOS_SESSION_PROFILE", veipQos == null ? "" : String.valueOf(veipQos));
                        // subscriber names
                        if (setarSubscriber != null) {
                            Object fn = setarSubscriber.getProperties() == null ? null : setarSubscriber.getProperties().get("subscriberFirstName");
                            Object ln = setarSubscriber.getProperties() == null ? null : setarSubscriber.getProperties().get("subscriberLastName");
                            Object em = setarSubscriber.getProperties() == null ? null : setarSubscriber.getProperties().get("email_username");
                            allvalues.put(prefix + "FIRST_NAME", fn == null ? "" : String.valueOf(fn));
                            allvalues.put(prefix + "LAST_NAME", ln == null ? "" : String.valueOf(ln));
                            allvalues.put(prefix + "EMAIL", em == null ? "" : String.valueOf(em));
                        } else {
                            allvalues.put(prefix + "FIRST_NAME", "");
                            allvalues.put(prefix + "LAST_NAME", "");
                            allvalues.put(prefix + "EMAIL", "");
                        }
                        allvalues.put(prefix + "SERVICE_ID", serviceID == null ? "" : serviceID);
                    }

                    // VOIP / Voice
                    if (serviceTypeLower.contains("voip") || serviceType.equalsIgnoreCase("Voice")) {
                        if (setarServiceLink != null && setarServiceLink.equalsIgnoreCase("ONT")) {
                            String voipT = (oltDevice != null && oltDevice.getProperties() != null) ? (String) oltDevice.getProperties().getOrDefault("voipServiceTemplate", "") : "";
                            String pots1 = (oltDevice != null && oltDevice.getProperties() != null) ? (String) oltDevice.getProperties().getOrDefault("voipPots1Template", "") : "";
                            String pots2 = (oltDevice != null && oltDevice.getProperties() != null) ? (String) oltDevice.getProperties().getOrDefault("voipPots2Template", "") : "";
                            allvalues.put(prefix + "TEMPLATE_NAME_VOIP", voipT == null ? "" : voipT);
                            allvalues.put(prefix + "TEMPLATE_NAME_POTS1", pots1 == null ? "" : pots1);
                            allvalues.put(prefix + "TEMPLATE_NAME_POTS2", pots2 == null ? "" : pots2);
                        }
                        allvalues.put(prefix + "SERVICE_ID", serviceID == null ? "" : serviceID);
                        // SIMA and voice details from subscription if present
                        if (setarSubscription != null && setarSubscription.getProperties() != null) {
                            Map<String,Object> sprops = setarSubscription.getProperties();
                            allvalues.put(prefix + "SIMA_CUST_ID", sprops.getOrDefault("simaCustomerId",""));
                            allvalues.put(prefix + "SIMA_ENDPOINT_ID", sprops.getOrDefault("simaEndpointId1",""));
                            allvalues.put(prefix + "SIMA_ENDPOINT_ID2", sprops.getOrDefault("simaEndpointId2",""));
                            allvalues.put(prefix + "SIMA_SUBS_ID", sprops.getOrDefault("simaSubscriptionId1",""));
                            allvalues.put(prefix + "SIMA_SUBS_ID2", sprops.getOrDefault("simaSubscriptionId2",""));
                            allvalues.put(prefix + "VOIP_NUMBER_1", sprops.getOrDefault("voipNumber1",""));
                            allvalues.put(prefix + "VOIP_NUMBER_2", sprops.getOrDefault("voipNumber2",""));
                            allvalues.put(prefix + "VOIP_PACKAGE", sprops.getOrDefault("voipPackage1",""));
                            allvalues.put(prefix + "VOIP_PACKAGE2", sprops.getOrDefault("voipPackage2",""));
                            // VoIP service code logic: if code2 exists overwrite code1
                            Object svc1 = sprops.getOrDefault("voipServiceCode1", "");
                            Object svc2 = sprops.getOrDefault("voipServiceCode2", "");
                            if (svc2 != null && !String.valueOf(svc2).isEmpty()) allvalues.put(prefix + "VOIP_SERVICE_CODE", String.valueOf(svc2));
                            else allvalues.put(prefix + "VOIP_SERVICE_CODE", String.valueOf(svc1));
                        }
                    }

                    // EVPN / Enterprise / Cloudstarter
                    if (serviceTypeLower.contains("evpn") || serviceTypeLower.contains("enterprise") || serviceSubTypeLower.contains("cloudstarter")) {
                        if (setarServiceLink != null && setarServiceLink.equalsIgnoreCase("ONT")) {
                            // Evpn templates on oltDevice
                            if (oltDevice != null && oltDevice.getProperties() != null) {
                                Map<String,Object> opl = oltDevice.getProperties();
                                allvalues.put(prefix + "SERVICE_TEMPLATE_PORT2", opl.getOrDefault("evpnEthPort2Template",""));
                                allvalues.put(prefix + "SERVICE_TEMPLATE_PORT3", opl.getOrDefault("evpnEthPort3Template",""));
                                allvalues.put(prefix + "SERVICE_TEMPLATE_PORT4", opl.getOrDefault("evpnEthPort4Template",""));
                                allvalues.put(prefix + "SERVICE_TEMPLATE_PORT5", opl.getOrDefault("evpnEthPort5Template",""));
                                allvalues.put(prefix + "SERVICE_TEMPLATE_CARD", opl.getOrDefault("evpnOntCardTemplate",""));
                            }
                            // subscription-level evpn values
                            if (setarSubscription != null && setarSubscription.getProperties() != null) {
                                Map<String,Object> sprops = setarSubscription.getProperties();
                                allvalues.put(prefix + "VLAN", sprops.getOrDefault("evpnVLAN",""));
                                allvalues.put(prefix + "QOS_SESSION_PROFILE", sprops.getOrDefault("evpnQosSessionProfile",""));
                                allvalues.put(prefix + "EVPN_PORT", sprops.getOrDefault("evpnPort",""));
                                allvalues.put(prefix + "PRODUCT_SUB_TYPE", sprops.getOrDefault("serviceSubType",""));
                                allvalues.put(prefix + "SERVICE_ID", sprops.getOrDefault("serviceID",""));
                                allvalues.put(prefix + "SERVICE_TEMPLATE_VLAN", sprops.getOrDefault("evpnTemplateVLAN",""));
                                allvalues.put(prefix + "SERVICE_TEMPLATE_CREATE", sprops.getOrDefault("evpnTemplateCreateVLAN",""));
                            }
                            // Additional mgmt template from VLAN logical interface (prefix logic)
                            if (oltDevice != null && setarSubscription != null && setarSubscription.getProperties() != null) {
                                Object evpnVlan = setarSubscription.getProperties().get("evpnVLAN");
                                if (evpnVlan != null) {
                                    String oltPrefix = oltDevice.getDiscoveredName();
                                    String[] preparts = oltPrefix.split(":", -1);
                                    String prefixText = preparts.length > 0 ? preparts[0] : oltPrefix;
                                    String vlanIntName = prefixText + "_" + evpnVlan;
                                    Optional<LogicalInterface> optVlanInt = logicalInterfaceRepository.findByDiscoveredName(vlanIntName);
                                    if (optVlanInt.isPresent()) {
                                        Object mgmt = optVlanInt.get().getProperties() == null ? null : optVlanInt.get().getProperties().get("mgmtTemplate");
                                        allvalues.put(prefix + "SERVICE_TEMPLATE_MGMT", mgmt == null ? "" : String.valueOf(mgmt));
                                    } else {
                                        allvalues.put(prefix + "SERVICE_TEMPLATE_MGMT", "");
                                    }
                                } else {
                                    allvalues.put(prefix + "SERVICE_TEMPLATE_MGMT", "");
                                }
                            }
                        } else {
                            // not ONT: still declare product subtype and service id from subscription if available
                            if (setarSubscription != null && setarSubscription.getProperties() != null) {
                                Map<String,Object> sprops = setarSubscription.getProperties();
                                allvalues.put(prefix + "PRODUCT_SUB_TYPE", sprops.getOrDefault("serviceSubType",""));
                                allvalues.put(prefix + "SERVICE_ID", sprops.getOrDefault("serviceID",""));
                            } else {
                                allvalues.put(prefix + "PRODUCT_SUB_TYPE", "");
                                allvalues.put(prefix + "SERVICE_ID", "");
                            }
                        }
                    }

                    // Always record subscription status, HHID, address, account number, product name (if exists)
                    String subscriptionStatus = (setarSubscription == null) ? "" : (setarSubscription.getProperties() == null ? "" : String.valueOf(setarSubscription.getProperties().getOrDefault("subscriptionStatusName", setarSubscription.getProperties().getOrDefault("status", ""))));
                    String hhid = (setarSubscriber == null) ? "" : String.valueOf(setarSubscriber.getProperties() == null ? "" : setarSubscriber.getProperties().getOrDefault("houseHoldId", ""));
                    String address = (setarSubscriber == null) ? "" : String.valueOf(setarSubscriber.getProperties() == null ? "" : setarSubscriber.getProperties().getOrDefault("address", ""));
                    String acct = (setarSubscriber == null) ? "" : String.valueOf(setarSubscriber.getProperties() == null ? "" : setarSubscriber.getProperties().getOrDefault("accountNumber", ""));
                    allvalues.put(prefix + "SUBSCRIPTION_STAUS", subscriptionStatus == null ? "" : subscriptionStatus);
                    allvalues.put(prefix + "HHID", hhid == null ? "" : hhid);
                    allvalues.put(prefix + "ADDRESS", address == null ? "" : address);
                    allvalues.put(prefix + "ACCOUNT_NUMBER", acct == null ? "" : acct);
                    if (setarProduct != null) allvalues.put(prefix + "PRODUCT", setarProduct.getDiscoveredName());
                    else allvalues.put(prefix + "PRODUCT", "");

                    // Advance counters
                    if (serviceType.equalsIgnoreCase("IPTV")) p++;
                    else if (serviceSubTypeLower.contains("fibernet") || serviceSubTypeLower.contains("broadband")) q++;
                    else if (serviceTypeLower.contains("voip") || serviceType.equalsIgnoreCase("Voice")) r++;
                    else if (serviceTypeLower.contains("evpn") || serviceTypeLower.contains("enterprise")) s++;
                    else if (serviceSubTypeLower.contains("cloudstarter")) u++;

                } catch (Exception svcEx) {
                    log.error("Error processing RFS {}", rfs.getDiscoveredName(), svcEx);
                    success = false;
                }
            } // end for each RFS

            // 9) Package the result
            if (!success || allvalues.isEmpty()) {
                log.warn("No service details could be collected (success={}, keys={})", success, allvalues.size());
                return createErrorResponse("404", ERROR_PREFIX + "Error, No Service Details Available.");
            }

            QueryServicesInfoResponse resp = new QueryServicesInfoResponse();
            resp.setStatus("200");
            resp.setMessage("Service Details Found.");
            resp.setTimestamp(Instant.now().toString());
            resp.setStructuredObject(allvalues);
            return resp;

        } catch (Exception ex) {
            log.error("Unhandled error in QueryServicesInfo", ex);
            return createErrorResponse("500", ERROR_PREFIX + ex.getMessage());
        }
    }

    private QueryServicesInfoResponse createErrorResponse(String status, String message) {
        QueryServicesInfoResponse resp = new QueryServicesInfoResponse();
        resp.setStatus(status);
        resp.setMessage(message);
        resp.setTimestamp(Instant.now().toString());
        resp.setStructuredObject(Collections.emptyMap());
        return resp;
    }
}
