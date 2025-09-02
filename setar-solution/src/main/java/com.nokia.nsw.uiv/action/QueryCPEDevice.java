package com.nokia.nsw.uiv.action;

import com.nokia.nsw.uiv.framework.action.Action;
import com.nokia.nsw.uiv.framework.action.ActionContext;
import com.nokia.nsw.uiv.framework.action.HttpAction;
import com.nokia.nsw.uiv.model.resource.logical.LogicalDevice;
import com.nokia.nsw.uiv.model.resource.logical.LogicalDeviceRepository;
import com.nokia.nsw.uiv.request.QueryCPEDeviceRequest;
import com.nokia.nsw.uiv.response.QueryCPEDeviceResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Component
@Action
@Slf4j
public class QueryCPEDevice implements HttpAction {

    @Autowired
    private LogicalDeviceRepository cpeDeviceRepository;

    @Override
    public Class getActionClass() {
        return QueryCPEDeviceRequest.class;
    }

    @Override
    public Object doPost(ActionContext actionContext) throws Exception {
        QueryCPEDeviceRequest request = (QueryCPEDeviceRequest) actionContext.getObject();
        String resourceType = request.getResourceType();
        if ("Cable_Modem".equalsIgnoreCase(resourceType)) {
            resourceType = "CBM";
        }

        String devName = resourceType + "_" + request.getResourceSN();
        Optional<LogicalDevice> deviceOpt = cpeDeviceRepository.uivFindByGdn(devName);

        if (!deviceOpt.isPresent()) {
            return new QueryCPEDeviceResponse("404", "CPE Details Not Found", System.currentTimeMillis());
        }

        LogicalDevice device = deviceOpt.get();
        QueryCPEDeviceResponse response = new QueryCPEDeviceResponse();
        response.setStatus("200");
        response.setMessage("CPE Details Found.");
        response.setTimestamp(System.currentTimeMillis());

        response.setResourceModel((String) device.getProperties().get("deviceModel"));
        response.setResourceModelMTA((String) device.getProperties().get("deviceModelMta"));
        response.setResourceGWMac((String) device.getProperties().get("gatewayMacAddress"));
        response.setResourceInventoryType((String) device.getProperties().get("inventoryType"));
        response.setResourceMac((String) device.getProperties().get("macAddress"));
        response.setResourceMacMTA((String) device.getProperties().get("macAddressMta"));
        response.setResourceManufacturer((String) device.getProperties().get("manufacturer"));
        response.setResourceStatus((String) device.getProperties().get("operationalState"));
        response.setResourceDescription((String) device.getProperties().get("description"));

        // Map resourceSN from localName (remove prefix)
        response.setResourceSN(device.getLocalName().replaceFirst(resourceType + "_", ""));

        // Map voice ports
        response.setResourceVoicePort1((String) device.getProperties().get("voipPort1"));
        response.setResourceVoicePort2((String) device.getProperties().get("voipPort2"));

        // Device type specific adjustments
        if ("CBM".equalsIgnoreCase(resourceType)) {
            response.setResourceMacMTA((String) device.getProperties().get("macAddressMta"));
            response.setResourceModelMTA((String) device.getProperties().get("deviceModelMta"));
            response.setResourceModelSubtype("HFC");
        } else if ("ONT".equalsIgnoreCase(resourceType)) {
            response.setResourceModelSubtype("GPON");
            for (int portNumber = 1; portNumber <= 5; portNumber++) {
                String portName = request.getResourceSN() + "_P" + portNumber + "_SINGLETAGGED";
//                 int vlanCount = cpeDeviceRepository.countVlanInterfaces(portName, portNumber);
//                 String dataPortStatus = vlanCount <= 7 ? "Available" : "Allocated";
//                 response.setDataPortStatus(portNumber, dataPortStatus);
            }
        }

        return response;
    }
}
