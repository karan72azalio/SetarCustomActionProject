package com.nokia.nsw.uiv.action;

import com.nokia.nsw.uiv.exception.BadRequestException;
import com.nokia.nsw.uiv.framework.action.Action;
import com.nokia.nsw.uiv.framework.action.ActionContext;
import com.nokia.nsw.uiv.framework.action.HttpAction;
import com.nokia.nsw.uiv.request.QueryAllEquipmentRequest;
import com.nokia.nsw.uiv.response.QueryAllEquipmentResponse;
import com.nokia.nsw.uiv.utils.Constants;
import com.nokia.nsw.uiv.utils.Validations;
import com.setar.uiv.model.product.ResourceFacingService;
import com.setar.uiv.model.product.ResourceFacingServiceRepository;
import com.nokia.nsw.uiv.model.resource.logical.LogicalDevice;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

@Component
@RestController
@Action
@Slf4j
public class QueryAllEquipment implements HttpAction {

    private static final String ERROR_PREFIX = "UIV action QueryAllEquipment execution failed - ";

    @Autowired
    private ResourceFacingServiceRepository rfsRepository;

    @Override
    public Class<?> getActionClass() {
        return QueryAllEquipmentRequest.class;
    }

    @Override
    public Object doPost(ActionContext actionContext) {
        log.info("Executing QueryAllEquipment action...");
        QueryAllEquipmentRequest request = (QueryAllEquipmentRequest) actionContext.getObject();

        try {
            // Step 1: Validate mandatory params
            try {
                Validations.validateMandatoryParams(request.getSubscriberName(), "subscriberName");
                Validations.validateMandatoryParams(request.getServiceId(), "serviceId");
            } catch (BadRequestException bre) {
                return new QueryAllEquipmentResponse(
                        "400",
                        ERROR_PREFIX + "Missing mandatory parameter: " + bre.getMessage(),
                        Instant.now().toString(),
                        null, null, null, null, null,
                        null, null, null, null, null,
                        null, null, null, null, null,
                        null, null, null, null,null,null,null,null
                );
            }

            // Step 2: Build RFS Name
            String rfsName = "RFS_" + request.getSubscriberName() + "_" + request.getServiceId();
            String rfsGdn=Validations.getGlobalName(rfsName);

            // Step 4: Fetch RFS
            Optional<ResourceFacingService> optRfs = rfsRepository.uivFindByGdn(rfsGdn);
            if (!optRfs.isPresent()) {
                return new QueryAllEquipmentResponse(
                        "404",
                        ERROR_PREFIX + "No entry found for delete",
                        Instant.now().toString(),
                        null, null, null, null, null,
                        null, null, null, null, null,
                        null, null, null, null, null,
                        null, null, null, null,null,null,null,null
                );
            }
            ResourceFacingService rfs = optRfs.get();

            boolean successFlag = false;
            int stbCounter = 1;
            int apCounter = 1;

            // Step 5: Process linked resources
            Set<com.nokia.nsw.uiv.model.resource.Resource> resources = rfs.getUsedResource();
            QueryAllEquipmentResponse response = new QueryAllEquipmentResponse();
            response.setStatus("200");
            response.setMessage("UIV action QueryAllEquipment executed successfully.");
            response.setTimestamp(Instant.now().toString());

            for (com.nokia.nsw.uiv.model.resource.Resource res : resources) {
                if (res instanceof LogicalDevice) {
                    LogicalDevice dev = (LogicalDevice) res;
                    String name = dev.getLocalName();

                    if (name.startsWith("STB")) {
                        successFlag = true;
                        switch (stbCounter) {
                            case 1:
                                response.setStbSerialNo1((String) dev.getProperties().getOrDefault("serialNo", ""));
                                response.setStbModel1((String) dev.getProperties().getOrDefault("deviceModel", ""));
                                response.setStbMacAddr1((String) dev.getProperties().getOrDefault("macAddress", ""));
                                response.setStbPreSharedKey1((String) dev.getProperties().getOrDefault("preSharedKey", ""));
                                response.setStbCustomerGroupID1((String) dev.getProperties().getOrDefault("customerGroupId", ""));
                                break;
                            case 2:
                                response.setStbSerialNo2((String) dev.getProperties().getOrDefault("serialNo", ""));
                                response.setStbModel2((String) dev.getProperties().getOrDefault("deviceModel", ""));
                                response.setStbMacAddr2((String) dev.getProperties().getOrDefault("macAddress", ""));
                                response.setStbPreSharedKey2((String) dev.getProperties().getOrDefault("preSharedKey", ""));
                                response.setStbCustomerGroupID2((String) dev.getProperties().getOrDefault("customerGroupId", ""));
                                break;
                            case 3:
                                response.setStbSerialNo3((String) dev.getProperties().getOrDefault("serialNo", ""));
                                response.setStbModel3((String) dev.getProperties().getOrDefault("deviceModel", ""));
                                response.setStbMacAddr3((String) dev.getProperties().getOrDefault("macAddress", ""));
                                response.setStbPreSharedKey3((String) dev.getProperties().getOrDefault("preSharedKey", ""));
                                response.setStbCustomerGroupID3((String) dev.getProperties().getOrDefault("customerGroupId", ""));
                                break;
                        }
                        stbCounter++;
                    } else if (name.startsWith("AP")) {
                        successFlag = true;
                        switch (apCounter) {
                            case 1:
                                response.setApSerialNo1((String) dev.getProperties().getOrDefault("serialNo", ""));
                                response.setApModel1((String) dev.getProperties().getOrDefault("deviceModel", ""));
                                response.setApMacAddr1((String) dev.getProperties().getOrDefault("macAddress", ""));
                                response.setApPreShareKey1((String) dev.getProperties().getOrDefault("preSharedKey", ""));
                                break;
                            case 2:
                                response.setApSerialNo2((String) dev.getProperties().getOrDefault("serialNo", ""));
                                response.setApModel2((String) dev.getProperties().getOrDefault("deviceModel", ""));
                                response.setApMacAddr2((String) dev.getProperties().getOrDefault("macAddress", ""));
                                response.setApPreShareKey2((String) dev.getProperties().getOrDefault("preSharedKey", ""));
                                break;
                        }
                        apCounter++;
                    }
                }
            }

            if (!successFlag) {
                return new QueryAllEquipmentResponse(
                        "500",
                        ERROR_PREFIX + "Error, Equipment Not Queried",
                        Instant.now().toString(),
                        null, null, null, null, null,
                        null, null, null, null, null,
                        null, null, null, null, null,
                        null, null, null, null,null,null,null,null
                );
            }

            return response;

        } catch (Exception ex) {
            log.error("Unhandled exception in QueryAllEquipment", ex);
            return new QueryAllEquipmentResponse(
                    "500",
                    ERROR_PREFIX + "Internal server error occurred - " + ex.getMessage(),
                    Instant.now().toString(),
                    null, null, null, null, null,
                    null, null, null, null, null,
                    null, null, null, null, null,
                    null, null, null, null,null,null,null,null
            );
        }
    }
}
