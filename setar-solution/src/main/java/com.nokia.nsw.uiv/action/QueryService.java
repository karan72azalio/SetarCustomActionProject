package com.nokia.nsw.uiv.action;

import com.nokia.nsw.uiv.framework.action.Action;
import com.nokia.nsw.uiv.framework.action.ActionContext;
import com.nokia.nsw.uiv.framework.action.HttpAction;
import com.nokia.nsw.uiv.request.QueryServiceRequest;
import com.nokia.nsw.uiv.response.QueryServiceResponse;
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
import com.nokia.nsw.uiv.model.resource.logical.LogicalDevice;
import com.nokia.nsw.uiv.model.resource.logical.LogicalDeviceRepository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.*;

/**
 * QueryService aggregates IPTV details from ServiceId, CFS, RFS, Subscription, Subscriber, and devices.
 */
@Component
@RestController
@Action
@Slf4j
public class QueryService implements HttpAction {

    private static final String ERROR_PREFIX = "UIV action QueryService execution failed - ";

    @Autowired private CustomerFacingServiceRepository cfsRepo;
    @Autowired private ResourceFacingServiceRepository rfsRepo;
    @Autowired private ProductRepository productRepo;
    @Autowired private SubscriptionRepository subscriptionRepo;
    @Autowired private CustomerRepository customerRepo;
    @Autowired private LogicalDeviceRepository logicalDeviceRepo;

    @Override
    public Class<?> getActionClass() {
        return QueryServiceRequest.class;
    }

    @Override
    public Object doPost(ActionContext actionContext) {
        System.out.println("------------Trace # 1--------------- QueryService started");
        QueryServiceRequest req = (QueryServiceRequest) actionContext.getObject();

        Map<String,String> iptvinfo = new LinkedHashMap<>();

        try {
            // Step 1: Validate mandatory param
            try {
                Validations.validateMandatoryParams(req.getServiceId(), "serviceId");
            } catch (Exception bre) {
                return new QueryServiceResponse(
                        "400",
                        ERROR_PREFIX + "Missing mandatory parameter: " + bre.getMessage(),
                        Instant.now().toString(),
                        null
                );
            }

            String serviceId = req.getServiceId();
            Set<String> cfsNameSet = new LinkedHashSet<>();

            // Step 3: Build candidate CFS set
            List<CustomerFacingService> cfsSearchResults = cfsRepo.findByServiceId(serviceId);
            if (cfsSearchResults != null) {
                for (CustomerFacingService cfs : cfsSearchResults) {
                    String cfsName = cfs.getName();
                    if (cfsName == null) continue;

                    // Rule A
                    if (serviceId.contains("_")) {
                        int first = cfsName.indexOf("_");
                        int last = cfsName.lastIndexOf("_");
                        if (first >= 0 && last > first) {
                            String between = cfsName.substring(first+1, last);
                            if (between.equalsIgnoreCase(serviceId)) {
                                cfsNameSet.add(cfsName);
                            }
                        }
                    }

                    // Rule B
                    String[] parts = cfsName.split("_");
                    if (parts.length >= 3 && parts[2].equals(serviceId)) {
                        cfsNameSet.add(cfsName);
                    }
                }
            }

            if (cfsNameSet.isEmpty()) {
                return new QueryServiceResponse(
                        "404",
                        "No IPTV Service Details Found.",
                        Instant.now().toString(),
                        null
                );
            }

            // Step 4+: For each candidate
            for (String cfsName : cfsNameSet) {
                CustomerFacingService cfs = cfsRepo.uivFindByGdn(cfsName).orElse(null);
                if (cfs == null) {
                    return new QueryServiceResponse(
                            "500",
                            ERROR_PREFIX + "No entry found to update.",
                            Instant.now().toString(),
                            null
                    );
                }
                String rfsName = cfsName.replace("CFS", "RFS");
                ResourceFacingService rfs = rfsRepo.uivFindByGdn(rfsName).orElse(null);
                List<Product> allProductsDetails = (List<Product>) productRepo.findAll();
                Product prod = null;
                for(Product pro:allProductsDetails)
                {
                    CustomerFacingService containedcfs= (CustomerFacingService) pro.getContainedCfs();
                    if(containedcfs.getLocalName().equalsIgnoreCase(cfsName)){
                        prod=pro;
                    }
                }

                if (prod == null) {
                    return new QueryServiceResponse(
                            "500",
                            ERROR_PREFIX + "No entry found to update.",
                            Instant.now().toString(),
                            null
                    );
                }

                // Subscription + Subscriber
                Subscription subs = null;
                Customer subscriber = null;
                if (prod.getProperties().containsKey("linkedSubscription")) {
                    String subsName = (String) prod.getProperties().get("linkedSubscription");
                    subs = subscriptionRepo.uivFindByGdn(subsName).orElse(null);
                }
                if (prod.getProperties().containsKey("linkedSubscriber")) {
                    String subName = (String) prod.getProperties().get("linkedSubscriber");
                    subscriber = customerRepo.uivFindByGdn(subName).orElse(null);
                }

                // Step 5: Collect subscription details
                if (subs != null) {
                    Map<String,Object> subsProps = subs.getProperties();
                    iptvinfo.put("CUSTOMER_GROUP_ID", safeStr(subsProps.get("customerGroupId")));
                    iptvinfo.put("CPE_MacAddr_1", safeStr(subsProps.get("serviceMAC")));
                    iptvinfo.put("Service_Link", safeStr(subsProps.get("serviceLink")));
                    iptvinfo.put("CPE_GW_MacAddr_1", safeStr(subsProps.get("gatewayMacAddress")));
                    iptvinfo.put("Service_Package_1", safeStr(subsProps.get("veipQosSessionProfile")));
                    iptvinfo.put("Service_Subscriber_1", safeStr(subsProps.get("veipQosSessionProfile")));

                    if ("Cable_Modem".equalsIgnoreCase((String) subsProps.get("serviceLink"))) {
                        iptvinfo.put("CBM_Subscriber_ID_1", safeStr(subsProps.get("subscriberIdCbm")));
                    }

                    // Service components
                    List<Product> allProductswithoutFilter = (List<Product>) productRepo.findAll();
                    List<Product>allProducts=new ArrayList<>();
                    for(Product pro:allProductswithoutFilter)
                    {
                        Subscription subscription= pro.getSubscription();
                        if(subscription.getLocalName().equalsIgnoreCase(subs.getLocalName())){
                            allProducts.add(pro);
                        }
                    }


                    int idx = 1;
                    for (Product p : allProducts) {
                        if (p.getLocalName().startsWith(serviceId)) {
                            String val = p.getLocalName().replace(serviceId, "").replace("_", "");
                            iptvinfo.put("Service_Component_" + idx, val);
                            idx++;
                        }
                    }
                }

                // Step 6: Subscriber details
                if (subscriber != null) {
                    Map<String,Object> subProps = subscriber.getProperties();
                    iptvinfo.put("Service_EmailId_1", safeStr(subProps.get("emailUsername")));
                    iptvinfo.put("Service_EmailPw_1", safeStr(subProps.get("emailPwd")));
                    iptvinfo.put("Service_Company_1", safeStr(subProps.get("companyName")));
                    iptvinfo.put("Service_ContactPhone_1", safeStr(subProps.get("contactPhoneNumber")));
                    iptvinfo.put("Service_Address_1", safeStr(subProps.get("address")));
                    iptvinfo.put("Service_HHID_1", safeStr(subProps.get("houseHoldId")));
                    iptvinfo.put("Service_FirstName_1", safeStr(subProps.get("subscriberFirstName")));
                    iptvinfo.put("Service_LastName_1", safeStr(subProps.get("subscriberLastName")));
                }

                // Step 7: Device/template details from RFS resources
                if (rfs != null && rfs.getProperties().containsKey("resources")) {
                    List<String> resList = (List<String>) rfs.getProperties().get("resources");
                    int apCount = 1, stbCount = 1;
                    for (String resName : resList) {
                        Optional<LogicalDevice> resOpt = logicalDeviceRepo.uivFindByGdn(resName);
                        if (!resOpt.isPresent()) continue;
                        LogicalDevice dev = resOpt.get();
                        Map<String,Object> dProps = dev.getProperties();

                        if (resName.contains("ONT")) {
                            iptvinfo.put("Service_Link", "ONT");
                            iptvinfo.put("CPE_Model_1", safeStr(dProps.get("deviceModel")));
                            iptvinfo.put("CPE_Serial_Number_1", safeStr(dProps.get("serialNumber")));
                            iptvinfo.put("Menm_1", safeStr(dProps.get("description")));
                        } else if (resName.contains("CBM")) {
                            iptvinfo.put("Service_Link", "Cable_Modem");
                            iptvinfo.put("CBM_Device_MacAddr_1", safeStr(dProps.get("macAddress")));
                            iptvinfo.put("CBM_Device_Model_1", safeStr(dProps.get("deviceType")));
                        } else if (resName.contains(":")) { // OLT
                            iptvinfo.put("ONT_OBJECT_ID", safeStr(dev.getName()));
                            iptvinfo.put("TEMPLATE_NAME_ONT", safeStr(dProps.get("ontTemplate")));
                            iptvinfo.put("TEMPLATE_NAME_IPTV", safeStr(dProps.get("veipIptvTemplate")));
                            iptvinfo.put("TEMPLATE_NAME_IGMP", safeStr(dProps.get("igmpTemplate")));
                            iptvinfo.put("TEMPLATE_NAME_VEIP", safeStr(dProps.get("veipServiceTemplate")));
                            iptvinfo.put("TEMPLATE_NAME_HSI", safeStr(dProps.get("veipHsiTemplate")));
                        } else if (resName.startsWith("AP")) {
                            iptvinfo.put("AP_SerialNo_" + apCount, safeStr(dProps.get("serialNo")));
                            iptvinfo.put("AP_MacAddr_" + apCount, safeStr(dProps.get("macAddress")));
                            iptvinfo.put("AP_PreShareKey_" + apCount, safeStr(dProps.get("presharedKey")));
                            iptvinfo.put("AP_Status_" + apCount, safeStr(dProps.get("administrativeStateName")));
                            iptvinfo.put("AP_Model_" + apCount, safeStr(dProps.get("deviceModel")));
                            apCount++;
                        } else if (resName.startsWith("STB")) {
                            iptvinfo.put("STB_SerialNo_" + stbCount, safeStr(dProps.get("serialNo")));
                            iptvinfo.put("STB_MacAddr_" + stbCount, safeStr(dProps.get("macAddress")));
                            iptvinfo.put("STB_PreShareKey_" + stbCount, safeStr(dProps.get("presharedKey")));
                            iptvinfo.put("STB_CustomerGroupID_" + stbCount, safeStr(dProps.get("deviceGroupId")));
                            iptvinfo.put("STB_Status_" + stbCount, safeStr(dProps.get("administrativeStateName")));
                            iptvinfo.put("STB_Model_" + stbCount, safeStr(dProps.get("deviceModel")));
                            stbCount++;
                        }
                    }
                }
            }

            // Step 9: Final response
            if (iptvinfo.isEmpty()) {
                return new QueryServiceResponse(
                        "404",
                        "No IPTV Service Details Found.",
                        Instant.now().toString(),
                        null
                );
            } else {
                return new QueryServiceResponse(
                        "200",
                        "UIV action QueryService executed successfully.",
                        Instant.now().toString(),
                        iptvinfo
                );
            }

        } catch (Exception ex) {
            log.error("Exception in QueryService", ex);
            return new QueryServiceResponse(
                    "500",
                    ERROR_PREFIX + ex.getMessage(),
                    Instant.now().toString(),
                    null
            );
        }
    }

    private String safeStr(Object o) {
        return (o == null) ? "" : o.toString();
    }
}
