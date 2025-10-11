package com.nokia.nsw.uiv.action;

import com.nokia.nsw.uiv.exception.BadRequestException;
import com.nokia.nsw.uiv.framework.action.Action;
import com.nokia.nsw.uiv.framework.action.ActionContext;
import com.nokia.nsw.uiv.framework.action.HttpAction;
import com.nokia.nsw.uiv.repository.*;
import com.nokia.nsw.uiv.request.QueryServiceRequest;
import com.nokia.nsw.uiv.response.QueryServiceResponse;
import com.nokia.nsw.uiv.utils.Validations;
import com.setar.uiv.model.product.*;
import com.nokia.nsw.uiv.model.common.party.Customer;
import com.nokia.nsw.uiv.model.service.Subscription;
import com.nokia.nsw.uiv.model.resource.logical.LogicalDevice;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;

@Component
@Action
@Slf4j
public class QueryService implements HttpAction {

    private static final String ACTION_LABEL = "QueryService";
    private static final String ERROR_PREFIX = "UIV action QueryService execution failed - ";

    @Autowired private CustomerCustomRepository customerRepository;
    @Autowired private SubscriptionCustomRepository subscriptionRepository;
    @Autowired private ProductCustomRepository productRepository;
    @Autowired private CustomerFacingServiceCustomRepository cfsRepository;
    @Autowired private ResourceFacingServiceCustomRepository rfsRepository;
    @Autowired private LogicalDeviceCustomRepository logicalDeviceRepository;

    @Override
    public Class<?> getActionClass() {
        return QueryServiceRequest.class;
    }

    @Override
    public Object doPost(ActionContext actionContext) throws Exception {
        log.info("Executing action {}", ACTION_LABEL);
        QueryServiceRequest request = (QueryServiceRequest) actionContext.getObject();
        Map<String, Object> iptvinfo = new HashMap<>();
        List<String> returnedParams = new ArrayList<>();

        try {
            // Step 1: Validate Mandatory Inputs
            try {
                Validations.validateMandatory(request.getServiceId(), "serviceId");
            } catch (BadRequestException bre) {
                return new QueryServiceResponse("400", ERROR_PREFIX + "Missing mandatory parameter : " + bre.getMessage(),
                        Instant.now().toString(), false, "Missing parameter");
            }

            String serviceId = request.getServiceId().trim();
            log.info("Processing QueryService for SERVICE_ID: {}", serviceId);

            // Step 2: Find Candidate CFS Names
            List<CustomerFacingService> cfsList = (List<CustomerFacingService>) cfsRepository.findAll();
            Set<String> cfsNameSet = new LinkedHashSet<>();

            for (CustomerFacingService cfs : cfsList) {
                if(cfs.getDiscoveredName().contains(serviceId)) {
                    String cfsName = cfs.getDiscoveredName();
                    if (cfsName == null) continue;
                    String[] parts = cfsName.split("_");
                    if (parts.length > 2 && parts[2].equalsIgnoreCase(serviceId)) {
                        cfsNameSet.add(cfsName);
                    }
                }
            }

            if (cfsNameSet.isEmpty()) {
                log.info("No matching CFS found for serviceId {}", serviceId);
                return new QueryServiceResponse("404", "No IPTV Service Details Found.",
                        Instant.now().toString(), false, "No CFS match found");
            }

            // Step 3: For each CFS, fetch linked data
            for (String cfsName : cfsNameSet) {
                Optional<CustomerFacingService> optCfs = cfsRepository.findByDiscoveredName(cfsName);
                if (!optCfs.isPresent()) continue;
                CustomerFacingService cfs = optCfs.get();

                String rfsName = cfsName.replace("CFS", "RFS");
                Optional<ResourceFacingService> optRfs = rfsRepository.findByDiscoveredName(rfsName);
                Optional<Product> optProd = Optional.ofNullable(cfs.getContainingProduct());
                Optional<Subscription> optSub = Optional.empty();
                Optional<Customer> optCust = Optional.empty();

                if (optProd.isPresent()) {
                    Product prod = optProd.get();
                    if (prod.getSubscription() != null)
                        optSub = Optional.ofNullable(prod.getSubscription());
                    if (prod.getSubscription() != null && prod.getSubscription().getCustomer() != null)
                        optCust = Optional.ofNullable(prod.getSubscription().getCustomer());
                }

                // Step 4: Populate Subscription details
                if (optSub.isPresent()) {
                    Subscription sub = optSub.get();
                    iptvinfo.put("CUSTOMER_GROUP_ID", sub.getProperties().get("customerGroupId"));
                    iptvinfo.put("CPE_MacAddr_1", sub.getProperties().get("serviceMAC"));
                    iptvinfo.put("Service_Link", sub.getProperties().get("serviceLink"));
                    iptvinfo.put("CPE_GW_MacAddr_1", sub.getProperties().get("gatewayMacAddress"));
                    iptvinfo.put("Service_Package_1", sub.getProperties().get("veipQosSessionProfile"));
                    iptvinfo.put("Service_Subscriber_1", sub.getProperties().get("veipQosSessionProfile"));
                    returnedParams.addAll(Arrays.asList("CUSTOMER_GROUP_ID","CPE_MacAddr_1","Service_Link",
                            "CPE_GW_MacAddr_1","Service_Package_1","Service_Subscriber_1"));
                }

                // Step 5: Populate Subscriber details
                if (optCust.isPresent()) {
                    Customer cust = optCust.get();
                    iptvinfo.put("Service_EmailId_1", cust.getProperties().get("email_username"));
                    iptvinfo.put("Service_EmailPw_1", cust.getProperties().get("email_pwd"));
                    iptvinfo.put("Service_Company_1", cust.getProperties().get("companyName"));
                    iptvinfo.put("Service_ContactPhone_1", cust.getProperties().get("contactPhoneNumber"));
                    iptvinfo.put("Service_Address_1", cust.getProperties().get("address"));
                    iptvinfo.put("Service_HHID_1", cust.getProperties().get("houseHoldId"));
                    iptvinfo.put("Service_FirstName_1", cust.getProperties().get("subscriberFirstName"));
                    iptvinfo.put("Service_LastName_1", cust.getProperties().get("subscriberLastName"));
                }

                // Step 6: Fetch Devices from RFS
                // Step 6: Fetch Devices from RFS
                if (optRfs.isPresent()) {
                    ResourceFacingService rfs = optRfs.get();

                    rfs.getUsedResource().forEach(res -> {
                        if (res instanceof LogicalDevice) {
                            LogicalDevice ont = (LogicalDevice) res;
                            String name = ont.getDiscoveredName();

                            if (name != null && name.contains("ONT")) {
                                iptvinfo.put("Service_Link", "ONT");
                                iptvinfo.put("CPE_Model_1", ont.getProperties().get("deviceModel"));
                                iptvinfo.put("CPE_Serial_Number_1", ont.getProperties().get("serialNo"));
                                iptvinfo.put("Menm_1", ont.getProperties().get("description"));
                            } else if (name != null && name.contains("CBM")) {
                                iptvinfo.put("Service_Link", "Cable_Modem");
                                iptvinfo.put("CBM_Device_MacAddr_1", ont.getProperties().get("macAddress"));
                                iptvinfo.put("CBM_Device_Model_1", ont.getProperties().get("deviceType"));
                            }
                            System.out.println("------------Test Trace #9--------------- ONT updated: " + ont.getLocalName());
                        }
                    });
                }

            }

            // Step 7: Build final success response
            return new QueryServiceResponse("200", "IPTV Service Details Found.",
                    Instant.now().toString(), true, iptvinfo);

        } catch (Exception ex) {
            log.error("Error executing QueryService", ex);
            return new QueryServiceResponse("500", ERROR_PREFIX + "Internal server error - " + ex.getMessage(),
                    Instant.now().toString(), false, null);
        }
    }
}
