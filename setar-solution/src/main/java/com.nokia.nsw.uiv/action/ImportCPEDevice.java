package com.nokia.nsw.uiv.action;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.nokia.nsw.uiv.exception.AccessForbiddenException;
import com.nokia.nsw.uiv.exception.BadRequestException;
import com.nokia.nsw.uiv.exception.ModificationNotAllowedException;
import com.nokia.nsw.uiv.framework.action.Action;
import com.nokia.nsw.uiv.framework.action.ActionContext;
import com.nokia.nsw.uiv.framework.action.HttpAction;
import com.nokia.nsw.uiv.model.resource.logical.LogicalComponent;
import com.nokia.nsw.uiv.model.resource.logical.LogicalComponentRepository;
import com.nokia.nsw.uiv.model.resource.logical.LogicalDevice;
import com.nokia.nsw.uiv.model.resource.logical.LogicalDeviceRepository;
import com.nokia.nsw.uiv.model.resource.logical.LogicalInterface;
import com.nokia.nsw.uiv.model.resource.logical.LogicalInterfaceRepository;
import com.nokia.nsw.uiv.request.ImportCPEDeviceRequest;
import com.nokia.nsw.uiv.response.CreateServiceFibernetResponse;
import com.nokia.nsw.uiv.response.ImportCPEDeviceResponse;
import com.nokia.nsw.uiv.utils.Constants;
import com.nokia.nsw.uiv.utils.Validations;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RestController;

@Component
@RestController
@Action
@Slf4j
public class ImportCPEDevice implements HttpAction {

    protected static final String ACTION_LABEL = Constants.IMPORT_CPE_DEVICE;
    private static final String ERROR_PREFIX = "UIV action CreateServiceFibernet execution failed - ";

    @Autowired
    private LogicalDeviceRepository cpeDeviceRepository;

    @Autowired
    private LogicalComponentRepository componentRepository;

    @Autowired
    private LogicalInterfaceRepository logicalInterfaceRepository;

    @Override
    public Class getActionClass() {
        return ImportCPEDeviceRequest.class;
    }

    @Override
    public Object doPost(ActionContext actionContext) throws Exception {
        log.warn(Constants.EXECUTING_ACTION, ACTION_LABEL);

        ImportCPEDeviceRequest request = (ImportCPEDeviceRequest) actionContext.getObject();

        try {
            log.info(Constants.MANDATORY_PARAMS_VALIDATION_STARTED);
            try{
                Validations.validateMandatoryParams(request.getCpeSerialNo(), "cpeSerialNo");
                Validations.validateMandatoryParams(request.getCpeModel(), "cpeModel");
                Validations.validateMandatoryParams(request.getCpeType(), "cpeType");
                Validations.validateMandatoryParams(request.getCpeMacAddress(), "cpeMacAddress");
                Validations.validateMandatoryParams(request.getCpeGwMacAddress(), "cpeGwMacAddress");
            }catch (BadRequestException bre) {
                return new ImportCPEDeviceResponse("400", ERROR_PREFIX + "Missing mandatory parameter : " + bre.getMessage(),
                        java.time.Instant.now().toString());
            }

            log.info(Constants.MANDATORY_PARAMS_VALIDATION_COMPLETED);

            String devName = request.getCpeType() + "_" + request.getCpeSerialNo();
            log.info("devName :: {}", devName);

            Optional<LogicalDevice> optDevice = cpeDeviceRepository.uivFindByGdn(devName);

            LogicalDevice cpeDevice;
            if (optDevice.isPresent()) {
                cpeDevice = optDevice.get();
                log.info("Found existing CPE device: {}", devName);
            } else {
                log.info("Creating new CPE device: {}", devName);
                cpeDevice = new LogicalDevice();
                cpeDevice.setLocalName(devName);
                cpeDevice.setKind(Constants.SETAR_KIND_CPE_DEVICE);
                cpeDevice.setContext("NA");
                Map<String, Object> properties = new HashMap<>();
                properties.put("name", devName);
                properties.put("serialNo", request.getCpeSerialNo());
                properties.put("deviceModel", request.getCpeModel());
                properties.put("deviceModelMta", request.getCpeModelMta());
                properties.put("deviceType", request.getCpeType());
                properties.put("gatewayMacAddress", request.getCpeGwMacAddress());
                properties.put("inventoryType", request.getCpeType());
                properties.put("macAddress", request.getCpeMacAddress());
                properties.put("macAddressMta", request.getCpeMacAddressMta());
                properties.put("manufacturer", request.getCpeManufacturer());
                properties.put("modelSubType", request.getCpeModelSubType());

                properties.put("operationalState", "Active");
                properties.put("administrativeState", "Available");

                cpeDevice.setProperties(properties);
                cpeDeviceRepository.save(cpeDevice, 2);
                log.info("Saved new CPE device: {}", devName);
            }

            // Create POTS ports
            log.info("-----------------Create POTS ports------------------");
            createPotsPort(request.getCpeSerialNo(), "POTS_1", cpeDevice);
            createPotsPort(request.getCpeSerialNo(), "POTS_2", cpeDevice);

            // Create Ethernet ports
            log.info("-----------------Create Ethernet ports------------------");
            int noOfPorts = determineNumberOfEthernetPorts(request.getCpeType(), request.getCpeModel());
            for (int i = 1; i <= noOfPorts; i++) {
                createEthernetPort(request.getCpeSerialNo(), "ETH_" + i, cpeDevice);
            }

            log.info(Constants.ACTION_COMPLETED);
            return new ImportCPEDeviceResponse("201", "CPE Details Found", getCurrentTimestamp());

        } catch (BadRequestException bre) {
            log.error("Validation error: {}", bre.getMessage(), bre);
            String msg = "UIV action ImportCPEDevice execution failed - Missing mandatory parameter : " + bre.getMessage();
            return new ImportCPEDeviceResponse("400", msg, String.valueOf(System.currentTimeMillis()));
        } catch (AccessForbiddenException | ModificationNotAllowedException ex) {
            log.error("Access or modification error: {}", ex.getMessage(), ex);
            String msg = "UIV action ImportCPEDevice execution failed - " + ex.getMessage();
            return new ImportCPEDeviceResponse("403", msg, String.valueOf(System.currentTimeMillis()));
        } catch (Exception ex) {
            log.error("Unhandled exception during ImportCPEDevice", ex);
            String msg = "UIV action ImportCPEDevice execution failed - Internal server error occurred";
            return new ImportCPEDeviceResponse("500", msg + " - " + ex.getMessage(), String.valueOf(System.currentTimeMillis()));
        }
    }

    private void createPotsPort(String serialNo, String portType, LogicalDevice cpeDevice)
            throws BadRequestException, AccessForbiddenException, ModificationNotAllowedException {
        log.info("-----------------Create POTS ports-Started------------------");
        String portName = serialNo + "_" + portType;
        Optional<LogicalComponent> optPort = componentRepository.uivFindByGdn(portName);

        if (!optPort.isPresent()) {
            log.info("Creating POTS port: {}", portName);
            LogicalComponent potsPort = new LogicalComponent();
            potsPort.setLocalName(portName);
            potsPort.setKind(Constants.SETAR_KIND_CPE_PORT);
            potsPort.setDescription("Voice Port");
potsPort.setContext("NA");
            Map<String, Object> properties = new HashMap<>();
            properties.put("portName", portName);
            properties.put("serialNumber", serialNo);
            properties.put("portStatus", "Available");
            properties.put("portType", portType);
            properties.put("serviceCount", "0");
            potsPort.setProperties(properties);

            componentRepository.save(potsPort, 2);
            cpeDevice.addContained(potsPort);
            cpeDeviceRepository.save(cpeDevice, 2);
            log.info("POTS port created and associated: {}", portName);
        } else {
            log.info("POTS port already exists: {}", portName);
        }
        log.info("-----------------Create POTS ports-Completed------------------");
    }

    private void createEthernetPort(String serialNo, String portType, LogicalDevice cpeDevice)
            throws BadRequestException, AccessForbiddenException, ModificationNotAllowedException {

        String portName = serialNo + "_" + portType;
        Optional<LogicalComponent> optPort = componentRepository.uivFindByGdn(portName);

        if (!optPort.isPresent()) {
            log.info("Creating Ethernet port: {}", portName);
            LogicalComponent ethPort = new LogicalComponent();
            ethPort.setLocalName(portName);
            ethPort.setKind(Constants.SETAR_KIND_CPE_PORT);
            ethPort.setDescription("Data Port");
ethPort.setContext("NA");
            Map<String, Object> properties = new HashMap<>();
            properties.put("portName", portName);
            properties.put("serialNumber", serialNo);
            properties.put("portStatus", "Available");
            properties.put("portType", portType);
            properties.put("serviceCount", "0");
            ethPort.setProperties(properties);

            componentRepository.save(ethPort, 2);
            cpeDevice.addContained(ethPort);
            cpeDeviceRepository.save(cpeDevice, 2);
            log.info("Ethernet port created and associated: {}", portName);

            // VLAN interfaces (LogicalInterface)
            for (int vlanIndex = 1; vlanIndex <= 7; vlanIndex++) {
                String vlanName = portName + "_" + vlanIndex;
                Optional<LogicalInterface> optVlan = logicalInterfaceRepository.uivFindByGdn(vlanName);

                if (!optVlan.isPresent()) {
                    log.info("Creating VLAN interface: {}", vlanName);
                    LogicalInterface vlan = new LogicalInterface();
                    vlan.setLocalName(vlanName);
                    vlan.setKind(Constants.SETAR_KIND_VLAN_INTERFACE);
                    vlan.setContext("NA");
                    vlan.setDescription("VLAN Interface for " + portName);

                    Map<String, Object> vlanProps = new HashMap<>();
                    vlanProps.put("name", vlanName);
                    vlanProps.put("linkedEthPort", portName);
                    vlanProps.put("serviceId", "");
                    vlanProps.put("serviceType", "");
                    vlanProps.put("vlanId", "");
                    vlanProps.put("vlanStatus", "Available");
                    vlan.setProperties(vlanProps);

                    logicalInterfaceRepository.save(vlan, 2);
                    ethPort.addContained(vlan);
                    componentRepository.save(ethPort, 2);
                    log.info("VLAN interface created and associated: {}", vlanName);
                } else {
                    log.info("VLAN interface already exists: {}", vlanName);
                }
            }

        } else {
            log.info("Ethernet port already exists: {}", portName);
        }
    }

    private int determineNumberOfEthernetPorts(String cpeType, String cpeModel) {
        if ("ONT".equalsIgnoreCase(cpeType)) {
            if ("XS-250WX-A".equalsIgnoreCase(cpeModel) || "XS-250X-A".equalsIgnoreCase(cpeModel)) {
                return 5;
            }
            return 4;
        }
        return 0;
    }

    private String getCurrentTimestamp() {
        return java.time.Instant.now().toString();
    }
}
