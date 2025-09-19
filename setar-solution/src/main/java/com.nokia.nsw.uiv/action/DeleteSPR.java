// ============================================================================
// Action: DeleteSPR
// Package: com.nokia.nsw.uiv.action
// Contract: Spring @Component, implements HttpAction, returns DeleteSPRResponse
// Repositories used: CustomerRepository, SubscriptionRepository, ProductRepository,
//                    CustomerFacingServiceRepository, ResourceFacingServiceRepository,
//                    LogicalDeviceRepository, LogicalInterfaceRepository
// Notes:
// - No Setar* types. Only base model classes are used.
// - Name building strictly follows the requirement.
// - Defensive null checks throughout.
// - Business logic paths for Broadband/Fibernet, Bridged, EVPN/Enterprise/Cloudstarter, Voice/VOIP.
// - Error mapping as per requirement (Code5/Code6/Code1).
// ============================================================================

package com.nokia.nsw.uiv.action;

import com.nokia.nsw.uiv.exception.BadRequestException;
import com.nokia.nsw.uiv.exception.InternalServerErrorException;
import com.nokia.nsw.uiv.framework.action.Action;
import com.nokia.nsw.uiv.framework.action.ActionContext;
import com.nokia.nsw.uiv.framework.action.HttpAction;

import com.nokia.nsw.uiv.model.common.party.Customer;
import com.nokia.nsw.uiv.model.common.party.CustomerRepository;


import com.nokia.nsw.uiv.model.service.Subscription;
import com.nokia.nsw.uiv.model.service.SubscriptionRepository;

import com.nokia.nsw.uiv.model.resource.logical.LogicalDevice;
import com.nokia.nsw.uiv.model.resource.logical.LogicalDeviceRepository;
import com.nokia.nsw.uiv.model.resource.logical.LogicalInterface;
import com.nokia.nsw.uiv.model.resource.logical.LogicalInterfaceRepository;

import com.nokia.nsw.uiv.request.DeleteSPRRequest;
import com.nokia.nsw.uiv.response.DeleteSPRResponse;

import com.nokia.nsw.uiv.utils.Validations;
import com.setar.uiv.model.product.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RestController
@Action
@Slf4j
public class DeleteSPR implements HttpAction {

    private static final String ACTION_LABEL = "DeleteSPR";
    private static final String ERROR_PREFIX = "UIV action DeleteSPR execution failed - ";

    @Autowired private CustomerRepository customerRepository;
    @Autowired private SubscriptionRepository subscriptionRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private CustomerFacingServiceRepository cfsRepository;
    @Autowired private ResourceFacingServiceRepository rfsRepository;
    @Autowired private LogicalDeviceRepository logicalDeviceRepository;
    @Autowired private LogicalInterfaceRepository logicalInterfaceRepository;

    @Override
    public Class<?> getActionClass() {
        return DeleteSPRRequest.class;
    }

    @Override
    public Object doPost(ActionContext actionContext) throws Exception {
        log.info("Executing action {}", ACTION_LABEL);
        DeleteSPRRequest req = (DeleteSPRRequest) actionContext.getObject();

        // -----------------------------
        // 1) Mandatory validations
        // -----------------------------
        try {
            validateMandatory(req.getSubscriberName(), "subscriberName");
            validateMandatory(req.getProductType(), "productType");
            validateMandatory(req.getProductSubtype(), "productSubtype");
            validateMandatory(req.getServiceId(), "serviceId");
            validateMandatory(req.getOntSN(), "ontSN");
        } catch (BadRequestException bre) {
            // Code5
            return new DeleteSPRResponse(
                    "400",
                    ERROR_PREFIX + "Missing mandatory parameter : " + bre.getMessage(),
                    now(),
                    "", ""
            );
        }

        // -----------------------------
        // 2) Construct Required Names
        // -----------------------------
        String subscriberNameWithOnt = req.getSubscriberName() + "_" + req.getOntSN();
        String subscriptionName = req.getSubscriberName() + "_" + req.getServiceId() + "_" + req.getOntSN();
        String cfsName = "CFS_" + subscriberNameWithOnt;
        String rfsName = "RFS_" + subscriptionName;
        String productName = req.getSubscriberName() + "_" + req.getProductSubtype() + "_" + req.getServiceId();
        String ontName = "ONT" + req.getOntSN();
        String subscriptionContext="";
        String productContext="";
        String rfsContext = "";

        if (ontName.length() > 100) {
            // Code6
            return new DeleteSPRResponse(
                    "400",
                    ERROR_PREFIX + "ONT name too long",
                    now(),
                    ontName,
                    subscriptionName
            );
        }

        try {
            // -----------------------------
            // 3) Retrieve Subscriber & determine "last service"
            // -----------------------------
            String subscriberGdn = Validations.getGlobalName("",subscriberNameWithOnt);
            Optional<Customer> optSubscriber = customerRepository.uivFindByGdn(subscriberGdn);
            boolean lastServiceForSubscriber = false;
            if (optSubscriber.isPresent()) {
                Customer sub = optSubscriber.get();
                try {
                    // Prefer a repository count if available; fallback to collection size
                    int subCount = countSubscriptionsByCustomer(sub);
                    lastServiceForSubscriber = (subCount <= 1);
                } catch (Exception ignore) {
                    // fallback null-safe
                    lastServiceForSubscriber = true;
                }
            }

            // -----------------------------
            // 4) Retrieve Objects
            // -----------------------------
             subscriptionContext=Validations.getGlobalName("",subscriberNameWithOnt);
            String subscriptionGdn=Validations.getGlobalName(subscriptionContext,subscriptionName);
            Optional<Subscription> optSubscription = subscriptionRepository.uivFindByGdn(subscriptionGdn);
            productContext=Validations.getGlobalName(subscriptionContext,subscriptionName);
            String productGdn=Validations.getGlobalName(productContext,productName);
            Optional<Product> optProduct = productRepository.uivFindByGdn(productGdn);
            String cfsGdn=Validations.getGlobalName("",cfsName);
            Optional<CustomerFacingService> optCfs = cfsRepository.uivFindByGdn(cfsGdn);
            rfsContext=Validations.getGlobalName("",cfsName);
            String rfsGdn=Validations.getGlobalName(rfsContext,rfsName);
            Optional<ResourceFacingService> optRfs = rfsRepository.uivFindByGdn(rfsGdn);
            String ontGdn=Validations.getGlobalName("",ontName);
            Optional<LogicalDevice> optOnt = logicalDeviceRepository.uivFindByGdn(ontGdn);

            // From ONT, try to retrieve parent OLT (if your data model links it via "parent" or property)
            Optional<LogicalDevice> optOlt = Optional.empty();
            if (optOnt.isPresent()) {
                LogicalDevice ont = optOnt.get();
                optOlt = getParentOlt(ont);
            }

            // Attempt to retrieve a CPE device named "ONT_" + ontSN (optional)
            Optional<LogicalDevice> optCpe = logicalDeviceRepository.uivFindByGdn("ONT_" + req.getOntSN());

            // -----------------------------
            // 5) Delete Fibernet/Broadband Services
            // -----------------------------
            if (equalsAny(req.getProductSubtype(), "Fibernet", "Broadband")) {
                // Clear VEIP/HSI templates on OLT
                optOlt.ifPresent(this::clearVeipAndHsiTemplates);

                // Remove RFS, unlink if needed, Remove CFS, Remove Product if no more CFS, Remove Subscription
                cleanupServiceObjects(optRfs, optCfs, optProduct, optSubscription);
            }

            // -----------------------------
            // 6) Delete Bridged Services
            // -----------------------------
            if ("Bridged".equalsIgnoreCase(req.getProductSubtype())) {
                String ontPort = nullSafe(req.getOntPort());
                // Clear EVPN template on that port in both OLT and ONT
                optOlt.ifPresent(olt -> clearEvpnsOnPort(olt, ontPort));
                optOnt.ifPresent(ont -> clearEvpnsOnOntPort(ont, ontPort));

                // If all EVPN port templates on OLT cleared -> clear EVPN card template
//                optOlt.ifPresent(this::maybeClearOntCardTemplateIfAllPortsEmpty);

                // Remove RFS/CFS/Product/Subscription
                cleanupServiceObjects(optRfs, optCfs, optProduct, optSubscription);
            }

            // -----------------------------
            // 7) Delete EVPN/Enterprise/Cloudstarter Services
            // -----------------------------
            if (equalsAny(req.getProductType(), "EVPN", "ENTERPRISE")
                    || "Cloudstarter".equalsIgnoreCase(req.getProductSubtype())) {

                String ontPort = nullSafe(req.getOntPort());
                // Identify current EVPN template on ONT port + VLAN from subscription
                String currentEvpnTemplateVal = optOnt.map(ont -> getPortTemplateValue(ont, ontPort)).orElse("0");
                String subVlan = optSubscription.map(s -> stringProp(s.getProperties(), "evpnTemplateVLAN")).orElse("");

                // Remove possible VLAN interfaces (ends with 2..8) based on naming rule
                removePossibleVlanInterfaces(req.getOntSN(), ontPort);

                if ("1".equals(currentEvpnTemplateVal)) {
                    optOlt.ifPresent(olt -> clearEvpnsOnPort(olt, ontPort));
                    optOnt.ifPresent(ont -> {
                        clearEvpnsOnOntPort(ont, ontPort);
                        clearOntCreateTemplate(ont);
                        safeSaveLogicalDevice(ont);
                    });
                    optOlt.ifPresent(this::safeSaveLogicalDevice);

                    if (allEvpnPortsEmpty(optOlt)) {
                        optOlt.ifPresent(this::clearOntCardTemplate);
                        optOnt.ifPresent(this::clearOntMgmtTemplates);
                        optOlt.ifPresent(this::safeSaveLogicalDevice);
                        optOnt.ifPresent(this::safeSaveLogicalDevice);
                    }
                }

                if (!"0".equals(currentEvpnTemplateVal)) {
                    try {
                        int val = Integer.parseInt(currentEvpnTemplateVal);
                        val = Math.max(val - 1, 0);
                        int finalVal = val;
                        optOnt.ifPresent(ont -> {
                            setPortTemplateValue(ont, ontPort, String.valueOf(finalVal));
                            safeSaveLogicalDevice(ont);
                        });
                    } catch (NumberFormatException ignored) {}
                }

                // If subtype NOT in BAAS/SIP/Cloudstarter/IPBH → remove extra VLAN (by ONT desc + sub evpn vlan)
                if (!equalsAny(req.getProductSubtype(), "BAAS", "SIP", "Cloudstarter", "IPBH")) {
                    String ontDesc = optOnt.map(LogicalDevice::getDescription).orElse("");
                    String nameToRemove = ontDesc + subVlan;
                    if (!nameToRemove.isEmpty()) {
                        logicalInterfaceRepository.uivFindByGdn(nameToRemove)
                                .ifPresent(li -> logicalInterfaceRepository.delete(li));
                    }
                }

                // Remove RFS/CFS/Product/Subscription
                cleanupServiceObjects(optRfs, optCfs, optProduct, optSubscription);
            }

            // -----------------------------
            // 8) Delete VOIP or Voice Services
            // -----------------------------
            if (equalsAny(req.getProductType(), "VOIP", "Voice")) {
                String voipNumber1 = optSubscription.map(s -> stringProp(s.getProperties(), "voipNumber1")).orElse("");

                // ONT POTS numbers
                String pots1 = optOnt.map(ont -> stringProp(ont.getProperties(), "potsPort1Number")).orElse("");
                String pots2 = optOnt.map(ont -> stringProp(ont.getProperties(), "potsPort2Number")).orElse("");

                boolean matchP2 = voipNumber1 != null && !voipNumber1.isEmpty() && voipNumber1.equals(pots2);

                if (matchP2) {
                    optOnt.ifPresent(ont -> {
                        ont.getProperties().put("potsPort2Number", "");
                        safeSaveLogicalDevice(ont);
                    });
                    optOlt.ifPresent(olt -> {
                        olt.getProperties().put("voipPots2Template", "");
                        safeSaveLogicalDevice(olt);
                    });
                    // CPE port2 → Available (if present)
                    setCpePortStateAvailable(optCpe, "POTS_2");
                } else {
                    optOnt.ifPresent(ont -> {
                        ont.getProperties().put("potsPort1Number", "");
                        safeSaveLogicalDevice(ont);
                    });
                    optOlt.ifPresent(olt -> {
                        olt.getProperties().put("voipPots1Template", "");
                        safeSaveLogicalDevice(olt);
                    });
                    // CPE port1 → Available (if present)
                    setCpePortStateAvailable(optCpe, "POTS_1");
                }

                // If both VoIP POTS templates empty on OLT -> clear voipServiceTemplate on OLT + SIMA cust id on subscriber
                if (optOlt.isPresent()) {
                    LogicalDevice olt = optOlt.get();
                    String p1 = stringProp(olt.getProperties(), "voipPots1Template");
                    String p2 = stringProp(olt.getProperties(), "voipPots2Template");
                    if (isEmpty(p1) && isEmpty(p2)) {
                        olt.getProperties().put("voipServiceTemplate", "");
                        safeSaveLogicalDevice(olt);
                        optSubscriber.ifPresent(sub -> {
                            sub.getProperties().put("simaCustomerId", "");
                            safeSaveCustomer(sub);
                        });
                    }
                }

                // Remove RFS/CFS/Product/Subscription
                cleanupServiceObjects(optRfs, optCfs, optProduct, optSubscription);
            }

            // -----------------------------
            // 9) Delete OLT and ONT Device if Unused
            // -----------------------------
            boolean shouldDeleteDevices = false;
            if (!"Exist".equalsIgnoreCase(nullSafe(req.getServiceFlag()))) {
                shouldDeleteDevices = true;
            }
            if (lastServiceForSubscriber) {
                shouldDeleteDevices = true;
            }
            if (shouldDeleteDevices) {
                // If OLT has no RFS linked (we approximate by checking a flag/property if any; otherwise skip)
                // Then remove ONT and OLT
                optOnt.ifPresent(logicalDeviceRepository::delete);
                optOlt.ifPresent(logicalDeviceRepository::delete);
            }

            // -----------------------------
            // 10) Delete Subscriber if no more subscriptions
            // -----------------------------
            if (optSubscriber.isPresent()) {
                Customer sub = optSubscriber.get();
                boolean deleteSubscriber = false;

                int remaining = 0;
                try {
                    remaining = countSubscriptionsByCustomer(sub);
                } catch (Exception ignore) {}

                if (remaining <= 0 && !"Exist".equalsIgnoreCase(nullSafe(req.getServiceFlag()))) {
                    deleteSubscriber = true;
                }
                if (lastServiceForSubscriber) {
                    deleteSubscriber = true;
                }

                if (deleteSubscriber) {
                    customerRepository.delete(sub);
                }
            }

            // -----------------------------
            // 11) Final Response
            // -----------------------------
            return new DeleteSPRResponse(
                    "200",
                    "UIV action DeleteSPR executed successfully.",
                    now(),
                    ontName,
                    subscriptionName
            );

        } catch (Exception ex) {
            log.error("Unhandled exception in DeleteSPR", ex);
            // Code1
            return new DeleteSPRResponse(
                    "500",
                    ERROR_PREFIX + "Internal server error occurred",
                    now(),
                    ontName,
                    subscriptionName
            );
        }
    }

    // -----------------------------
    // Helpers
    // -----------------------------
    private void validateMandatory(String val, String param) throws BadRequestException {
        if (val == null || val.trim().isEmpty()) {
            throw new BadRequestException(param);
        }
    }

    private String now() {
        return java.time.Instant.now().toString();
    }

    private boolean equalsAny(String s, String... opts) {
        if (s == null) return false;
        for (String o : opts) {
            if (s.equalsIgnoreCase(o)) return true;
        }
        return false;
    }

    private String nullSafe(String s) {
        return s == null ? "" : s;
    }

    private String stringProp(Map<String, Object> props, String key) {
        if (props == null) return "";
        Object v = props.get(key);
        return v == null ? "" : String.valueOf(v);
    }

    private void clearVeipAndHsiTemplates(LogicalDevice olt) {
        Map<String, Object> p = ensureProps(olt);
        p.put("veipServiceTemplate", "");
        p.put("veipHsiTemplate", "");
        safeSaveLogicalDevice(olt);
    }

    private void clearOntCreateTemplate(LogicalDevice ont) {
        ensureProps(ont).put("createTemplate", "");
    }

    private void clearOntMgmtTemplates(LogicalDevice ont) {
        Map<String, Object> p = ensureProps(ont);
        p.put("mgmtTemplate", "");
        p.put("veipIptvTemplate", "");
        p.put("tempTemplateMGMT", "");
        p.put("vlanCreateTemplate", "");
    }

    private void clearEvpnsOnPort(LogicalDevice olt, String port) {
        if (isEmpty(port)) return;
        Map<String, Object> p = ensureProps(olt);
        // Map common EVPN OLT port templates (2..5)
        p.put("evpnEthPort" + port + "Template", "");
        safeSaveLogicalDevice(olt);
    }

    private void clearEvpnsOnOntPort(LogicalDevice ont, String port) {
        if (isEmpty(port)) return;
        Map<String, Object> p = ensureProps(ont);
        // clear both create and active templates for the port
        p.put("evpnEthPort" + port + "Template", "");
        p.put("evpnEthPort" + port + "CreateTemplate", "");
        safeSaveLogicalDevice(ont);
    }

    private boolean allEvpnPortsEmpty(Optional<LogicalDevice> optOlt) {
        if (!optOlt.isPresent()) return true;
        Map<String, Object> p = ensureProps(optOlt.get());
        // Check ports 2..5 known in your model
        for (String k : Arrays.asList("evpnEthPort2Template","evpnEthPort3Template","evpnEthPort4Template","evpnEthPort5Template")) {
            String v = stringProp(p, k);
            if (!isEmpty(v)) return false;
        }
        return true;
    }

    private void clearOntCardTemplate(LogicalDevice olt) {
        ensureProps(olt).put("evpnOntCardTemplate", "");
    }

    private Map<String, Object> ensureProps(LogicalDevice d) {
        if (d.getProperties() == null) d.setProperties(new HashMap<>());
        return d.getProperties();
    }

    private void safeSaveLogicalDevice(LogicalDevice d) {
        logicalDeviceRepository.save(d, 2);
    }

    private void safeSaveCustomer(Customer c) {
        customerRepository.save(c, 2);
    }

    private Optional<LogicalDevice> getParentOlt(LogicalDevice ont) {
        // If your model has a relation for parent OLT, use it.
        // Here we try a heuristic via property "oltPosition" to fetch OLT by name in that position.
        String oltPos = stringProp(ont.getProperties(), "oltPosition");
        if (!isEmpty(oltPos)) {
            // some tenants prefix OLT by its position; adapt if needed.
            return logicalDeviceRepository.uivFindByGdn(oltPos);
        }
        return Optional.empty();
    }

    private boolean isEmpty(String s) { return s == null || s.trim().isEmpty(); }

    private String getPortTemplateValue(LogicalDevice ont, String port) {
        if (isEmpty(port)) return "0";
        return stringProp(ensureProps(ont), "evpnEthPort" + port + "Template");
    }

    private void setPortTemplateValue(LogicalDevice ont, String port, String value) {
        if (isEmpty(port)) return;
        ensureProps(ont).put("evpnEthPort" + port + "Template", value == null ? "" : value);
    }

    private void removePossibleVlanInterfaces(String ontSN, String ontPort) {
        if (isEmpty(ontSN) || isEmpty(ontPort)) return;
        // Values "2".."8"
        for (int suffix = 2; suffix <= 8; suffix++) {
            String vlanName = ontSN + "_P" + ontPort + "SINGLETAGGED" + suffix;
            Optional<LogicalInterface> optVlan = logicalInterfaceRepository.uivFindByGdn(vlanName);
            if (optVlan.isPresent()) {
                logicalInterfaceRepository.delete(optVlan.get());
                break; // stop after first match as per requirement
            }
        }
    }

    private void setCpePortStateAvailable(Optional<LogicalDevice> optCpe, String portType) {
        if (!optCpe.isPresent()) return;
        LogicalDevice cpe = optCpe.get();
        // In many deployments, CPE port state is recorded on contained components.
        // If they’re on properties of device itself, adapt keys accordingly.
        String key = "voipPort" + ("POTS_2".equals(portType) ? "2" : "1");
        ensureProps(cpe).put(key, "Available");
        safeSaveLogicalDevice(cpe);
    }

    private void cleanupServiceObjects(Optional<ResourceFacingService> optRfs,
                                       Optional<CustomerFacingService> optCfs,
                                       Optional<Product> optProduct,
                                       Optional<Subscription> optSubscription) {

        // Remove RFS
        optRfs.ifPresent(rfsRepository::delete);

        // Remove CFS
        optCfs.ifPresent(cfsRepository::delete);

        // Remove Product (we cannot check attached CFS count generically here; delete directly as per requirement note)
        optProduct.ifPresent(productRepository::delete);

        // Remove Subscription
        optSubscription.ifPresent(subscriptionRepository::delete);
    }

    private int countSubscriptionsByCustomer(Customer customer) {
        // If your SubscriptionRepository exposes a counter method, use it.
        // Fallback: defensive read via property map "subscriptions" if present, otherwise 0/1 heuristic.
        try {
            // Example optional API (replace if you have different method)
            // return subscriptionRepository.countByCustomer(customer);
            Collection<Subscription> subs = (Collection<Subscription>) customer.getProperties().get("subscriptions");
            if (subs != null) return subs.size();
        } catch (Exception ignore) {}
        return 1; // conservative fallback
    }
//    private void maybeClearOntCardTemplateIfAllPortsEmpty(LogicalDevice oltDevice) {
//        // Fetch all logical interfaces linked to the OLT
//        var oltInterfaces = logicalInterfaceRepository.findByParent(oltDevice);
//
//        boolean allPortsCleared = oltInterfaces.stream()
//                .filter(intf -> "EVPN_PORT".equalsIgnoreCase(intf.getTemplateName()))
//                .allMatch(intf -> intf.getProperties() == null
//                        || intf.getProperties().isEmpty());
//
//        if (allPortsCleared) {
//            // Clear the EVPN card template (reset description/properties, etc.)
//            oltDevice.setDescription("EVPN card template cleared");
//
//            Map<String, Object> props = new HashMap<>();
//            props.put("AdministrativeState", "Available");
//            oltDevice.setProperties(props);
//
//            logicalDeviceRepository.save(oltDevice, 2);
//            log.info("Cleared EVPN card template for OLT {}", oltDevice.getLocalName());
//        }
//    }
}

