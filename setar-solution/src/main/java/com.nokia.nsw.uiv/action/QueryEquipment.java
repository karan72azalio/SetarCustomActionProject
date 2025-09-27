package com.nokia.nsw.uiv.action;

import com.nokia.nsw.uiv.exception.BadRequestException;
import com.nokia.nsw.uiv.framework.action.Action;
import com.nokia.nsw.uiv.framework.action.ActionContext;
import com.nokia.nsw.uiv.framework.action.HttpAction;
import com.nokia.nsw.uiv.model.common.party.CustomerRepository;
import com.nokia.nsw.uiv.model.resource.logical.LogicalDevice;
import com.nokia.nsw.uiv.model.resource.logical.LogicalDeviceRepository;
import com.nokia.nsw.uiv.model.service.SubscriptionRepository;
import com.nokia.nsw.uiv.request.QueryEquipmentRequest;
import com.nokia.nsw.uiv.response.QueryEquipmentResponse;
import com.nokia.nsw.uiv.utils.Validations;
import com.setar.uiv.model.product.CustomerFacingServiceRepository;
import com.setar.uiv.model.product.ResourceFacingService;
import com.setar.uiv.model.product.ResourceFacingServiceRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@Action
@Slf4j
public class QueryEquipment implements HttpAction {

    private static final String CODE_SUCCESS = "200";
    private static final String CODE_MISSING_PARAMS = "400";
    private static final String CODE_NO_ENTRY = "404";
    private static final String CODE_EQUIP_NOT_FOUND = "500";
    private static final String CODE_EXCEPTION = "500";

    @Autowired
    private CustomerRepository subscriberRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private CustomerFacingServiceRepository cfsRepository;

    @Autowired
    private ResourceFacingServiceRepository rfsRepository;

    @Autowired
    private LogicalDeviceRepository logicalDeviceRepository;

    @Override
    public Class<?> getActionClass() {
        return QueryEquipmentRequest.class;
    }

    @Override
    public Object doPost(ActionContext actionContext) {
        QueryEquipmentRequest request = (QueryEquipmentRequest) actionContext.getObject();

        // 1. Mandatory validations
        try {
            Validations.validateMandatoryParams(request.getSubscriberName(), "subscriberName");
            Validations.validateMandatoryParams(request.getServiceId(), "serviceId");
            Validations.validateMandatoryParams(request.getResourceSn(), "resourceSn");
            Validations.validateMandatoryParams(request.getProductType(), "productType");
            Validations.validateMandatoryParams(request.getProductSubType(), "productSubType");
        } catch (BadRequestException bre) {
            return createErrorResponse(CODE_MISSING_PARAMS,
                    "Missing mandatory parameter(s): " + bre.getMessage());
        }

        // 2. Construct names
        String subscriptionName = request.getSubscriberName() + "_" + request.getServiceId();
        String cfsName = "CFS_" + subscriptionName;
        String rfsName = "RFS_" + subscriptionName;
        String productName = request.getSubscriberName() + "_" + request.getProductSubType() + "_" + request.getServiceId();

        boolean successFlag = false;
        int apCounter = 1;
        int stbCounter = 1;

        try {
            // 3. Locate entities
            ResourceFacingService rfs = rfsRepository.uivFindByGdn(Validations.getGlobalName(rfsName)).orElse(null);
            if (rfs == null) {
                return createErrorResponse(CODE_NO_ENTRY, "No RFS entry found for subscriber/service");
            }

            List<String> apSns = new ArrayList<>();
            List<String> stbSns = new ArrayList<>();

            // 4. Process linked devices
            Object linkedDevicesObj = null;
            if (rfs.getProperties() != null) {
                linkedDevicesObj = rfs.getProperties().get("linkedDevices");
            }

            if (linkedDevicesObj != null && linkedDevicesObj instanceof List) {
                List<?> linkedDevicesList = (List<?>) linkedDevicesObj;

                for (Object gdnObj : linkedDevicesList) {
                    if (!(gdnObj instanceof String)) continue;
                    String gdn = (String) gdnObj;

                    LogicalDevice device = logicalDeviceRepository.uivFindByGdn(gdn).orElse(null);
                    if (device == null) continue;

                    String devName = device.getName();
                    if (devName == null) continue;

                    if (devName.startsWith("AP") && apCounter <= 5) {
                        Object serial = device.getProperties() != null ? device.getProperties().get("serialNo") : null;
                        apSns.add(serial != null ? serial.toString() : "");
                        apCounter++;
                        successFlag = true;
                    } else if (devName.startsWith("STB") && stbCounter <= 5) {
                        Object serial = device.getProperties() != null ? device.getProperties().get("serialNo") : null;
                        stbSns.add(serial != null ? serial.toString() : "");
                        stbCounter++;
                        successFlag = true;
                    }
                }
            }


            // 5. Prepare response
            QueryEquipmentResponse response = new QueryEquipmentResponse();
            response.setSubscriptionId(subscriptionName);
            response.setTimestamp(new Date().toString());

            // Map APs
            if (!apSns.isEmpty()) {
                if (apSns.size() > 0) response.setApSn1(apSns.get(0));
                if (apSns.size() > 1) response.setApSn2(apSns.get(1));
                if (apSns.size() > 2) response.setApSn3(apSns.get(2));
                if (apSns.size() > 3) response.setApSn4(apSns.get(3));
                if (apSns.size() > 4) response.setApSn5(apSns.get(4));
            }

            // Map STBs
            if (!stbSns.isEmpty()) {
                if (stbSns.size() > 0) response.setStbSn1(stbSns.get(0));
                if (stbSns.size() > 1) response.setStbSn2(stbSns.get(1));
                if (stbSns.size() > 2) response.setStbSn3(stbSns.get(2));
                if (stbSns.size() > 3) response.setStbSn4(stbSns.get(3));
                if (stbSns.size() > 4) response.setStbSn5(stbSns.get(4));
            }

            if (successFlag) {
                response.setStatus(CODE_SUCCESS);
                response.setMessage("Equipment Queried.");
            } else {
                response.setStatus(CODE_EQUIP_NOT_FOUND);
                response.setMessage("Error, Equipment Not Queried.");
            }

            return response;

        } catch (Exception e) {
            log.error("Exception querying equipment", e);
            return createErrorResponse(CODE_EXCEPTION, "Exception: " + e.getMessage());
        }
    }

    private QueryEquipmentResponse createErrorResponse(String code, String message) {
        QueryEquipmentResponse resp = new QueryEquipmentResponse();
        resp.setStatus(code);
        resp.setMessage("QueryEquipment execution failed - " + message);
        resp.setTimestamp(new Date().toString());
        resp.setSubscriptionId(null);
        resp.setApSn1(null); resp.setApSn2(null); resp.setApSn3(null); resp.setApSn4(null); resp.setApSn5(null);
        resp.setStbSn1(null); resp.setStbSn2(null); resp.setStbSn3(null); resp.setStbSn4(null); resp.setStbSn5(null);
        return resp;
    }
}
