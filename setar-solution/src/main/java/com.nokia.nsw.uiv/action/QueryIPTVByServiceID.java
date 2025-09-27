package com.nokia.nsw.uiv.action;

import com.nokia.nsw.uiv.framework.action.Action;
import com.nokia.nsw.uiv.framework.action.ActionContext;
import com.nokia.nsw.uiv.framework.action.HttpAction;
import com.nokia.nsw.uiv.model.common.party.Customer;
import com.nokia.nsw.uiv.model.common.party.CustomerRepository;
import com.nokia.nsw.uiv.model.resource.logical.LogicalDevice;
import com.nokia.nsw.uiv.model.resource.logical.LogicalDeviceRepository;
import com.nokia.nsw.uiv.model.service.Subscription;
import com.nokia.nsw.uiv.model.service.SubscriptionRepository;
import com.setar.uiv.model.product.CustomerFacingService;
import com.setar.uiv.model.product.CustomerFacingServiceRepository;
import com.setar.uiv.model.product.Product;
import com.setar.uiv.model.product.ProductRepository;
import com.setar.uiv.model.product.ResourceFacingService;
import com.setar.uiv.model.product.ResourceFacingServiceRepository;
import com.nokia.nsw.uiv.request.QueryIPTVByServiceIDRequest;
import com.nokia.nsw.uiv.response.QueryIPTVByServiceIDResponse;
import com.nokia.nsw.uiv.utils.Validations;
import com.nokia.nsw.uiv.utils.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@Action
@Slf4j
public class QueryIPTVByServiceID implements HttpAction {

    private static final String CODE_SUCCESS = "200";
    private static final String CODE_NO_ENTRY = "404";
    private static final String CODE_MISSING_PARAMS = "400";
    private static final String CODE_EXCEPTION = "500";

    @Autowired
    private CustomerFacingServiceRepository cfsRepository;

    @Autowired
    private ResourceFacingServiceRepository rfsRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private LogicalDeviceRepository deviceRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Override
    public Class<?> getActionClass() {
        return QueryIPTVByServiceIDRequest.class;
    }

    @Override
    public Object doPost(ActionContext actionContext) {
        QueryIPTVByServiceIDRequest request = (QueryIPTVByServiceIDRequest) actionContext.getObject();

        // 1. Mandatory validation
        if (request.getServiceID() == null || request.getServiceID().trim().isEmpty()) {
            return createErrorResponse(CODE_MISSING_PARAMS, "Missing mandatory parameter: SERVICE_ID");
        }

        try {
            String serviceId = request.getServiceID().trim();
            Iterable<CustomerFacingService> allCfs = cfsRepository.findAll();
            List<CustomerFacingService> matchingCfs = new ArrayList<>();

            // 2. Identify target CFS
            for (CustomerFacingService cfs : allCfs) {
                String cfsName = cfs.getName();
                String[] parts = cfsName.split("_");
                boolean matches = (cfsName.contains(serviceId)) || (parts.length >= 3 && parts[2].equals(serviceId));
                if (matches) matchingCfs.add(cfs);
            }

            if (matchingCfs.isEmpty()) {
                return createErrorResponse(CODE_NO_ENTRY, "No entry found for SERVICE_ID: " + serviceId);
            }

            boolean successFlag = false;
            QueryIPTVByServiceIDResponse response = new QueryIPTVByServiceIDResponse();
            response.setTimestamp(new Date().toString());
            response.setServiceComponents(new ArrayList<>());
            response.setApSerialNos(new ArrayList<>());
            response.setApMacAddrs(new ArrayList<>());
            response.setApPreShareKeys(new ArrayList<>());
            response.setApStatuses(new ArrayList<>());
            response.setApModels(new ArrayList<>());
            response.setStbSerialNos(new ArrayList<>());
            response.setStbMacAddrs(new ArrayList<>());
            response.setStbPreShareKeys(new ArrayList<>());
            response.setStbCustomerGroupIds(new ArrayList<>());
            response.setStbStatuses(new ArrayList<>());
            response.setStbModels(new ArrayList<>());

            int apCounter = 1, stbCounter = 1, serviceComponentCounter = 1;

            for (CustomerFacingService cfs : matchingCfs) {
                String rfsName = cfs.getName().replace("CFS", "RFS");
                Optional<ResourceFacingService> rfsOpt = rfsRepository.uivFindByGdn(rfsName);
                ResourceFacingService rfs = rfsOpt.orElse(null);

                Product product = cfs.getContainingProduct();
                Subscription subscription = product != null ? product.getSubscription() : null;
                Customer subscriber = subscription != null ? subscription.getCustomer() : null;

                // 5. Capture subscription details
                if (subscription != null) {
                    Map<String, Object> subProps = subscription.getProperties();
                    if (subProps != null) {
                        response.setCustomerGroupId((String) subProps.get("customerGroupId"));
                        response.setCpeMacAddr1((String) subProps.get("serviceMAC"));
                        response.setServiceLink((String) subProps.get("serviceLink"));
                        response.setCpeGwMacAddr1((String) subProps.get("serviceGW"));
                        if ("Cable_Modem".equals(subProps.get("serviceLink"))) {
                            response.setCustomerGroupId((String) subProps.get("serviceSN")); // CBM subscriber ID
                        }
                        successFlag = true;
                    }
                }

                // 6. Capture service components
                if (product != null) {
                    List<Product> products = productRepository.findByCustomer(subscriber);
                    for (Product p : products) {
                        if (p.getName().startsWith(serviceId)) {
                            String componentLabel = p.getName().replace(serviceId, "").replace("_", "");
                            response.getServiceComponents().add(componentLabel);
                            serviceComponentCounter++;
                            successFlag = true;
                        }
                    }
                }

                // 7. Capture RFS resource details
                if (rfs != null && rfs.getContainedResources() != null) {
                    for (LogicalDevice device : rfs.getContainedResources()) {
                        Map<String, Object> devProps = device.getProperties();
                        String kind = device.getKind();

                        if ("ONTDevice".equals(kind)) {
                            response.setServiceLink("ONT");
                            response.setCpeModel1((String) devProps.get("deviceModel"));
                            response.setCpeSerialNumber1((String) devProps.get("serialNo"));
                            response.setOntObjectId((String) devProps.get("objectId"));
                            response.setTemplateNameONT((String) devProps.get("templateONT"));
                            response.setTemplateNameIPTV((String) devProps.get("templateIPTV"));
                            response.setTemplateNameIGMP((String) devProps.get("templateIGMP"));
                            response.setTemplateNameVEIP((String) devProps.get("templateVEIP"));
                            successFlag = true;
                        } else if ("CBMDevice".equals(kind)) {
                            response.setServiceLink("Cable_Modem");
                            response.setCpeMacAddr1((String) devProps.get("macAddress"));
                            response.setCpeModel1((String) devProps.get("deviceModel"));
                            successFlag = true;
                        } else if ("APDevice".equals(kind)) {
                            response.getApSerialNos().add((String) devProps.get("serialNo"));
                            response.getApMacAddrs().add((String) devProps.get("macAddress"));
                            response.getApPreShareKeys().add((String) devProps.get("preShareKey"));
                            response.getApStatuses().add((String) devProps.get("status"));
                            response.getApModels().add((String) devProps.get("deviceModel"));
                            apCounter++;
                            successFlag = true;
                        } else if ("STBDevice".equals(kind)) {
                            response.getStbSerialNos().add((String) devProps.get("serialNo"));
                            response.getStbMacAddrs().add((String) devProps.get("macAddress"));
                            response.getStbPreShareKeys().add((String) devProps.get("preShareKey"));
                            response.getStbCustomerGroupIds().add((String) devProps.get("customerGroupId"));
                            response.getStbStatuses().add((String) devProps.get("status"));
                            response.getStbModels().add((String) devProps.get("deviceModel"));
                            stbCounter++;
                            successFlag = true;
                        }
                    }
                }
            }

            if (successFlag) {
                response.setStatus(CODE_SUCCESS);
                response.setMessage("IPTV Service Details Found");
            } else {
                response.setStatus(CODE_NO_ENTRY);
                response.setMessage("No IPTV Service Details Found");
            }

            return response;

        } catch (Exception ex) {
            log.error("Exception retrieving IPTV details", ex);
            return createErrorResponse(CODE_EXCEPTION, "Exception - " + ex.getMessage());
        }
    }

    private QueryIPTVByServiceIDResponse createErrorResponse(String code, String message) {
        QueryIPTVByServiceIDResponse resp = new QueryIPTVByServiceIDResponse();
        resp.setStatus(code);
        resp.setMessage("UIV action QueryIPTVByServiceID execution failed - " + message);
        resp.setTimestamp(new Date().toString());
        return resp;
    }
}
