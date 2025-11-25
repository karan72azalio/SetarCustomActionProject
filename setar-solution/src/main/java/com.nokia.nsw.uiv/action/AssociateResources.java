package com.nokia.nsw.uiv.action;

import com.nokia.nsw.uiv.exception.BadRequestException;
import com.nokia.nsw.uiv.framework.action.Action;
import com.nokia.nsw.uiv.framework.action.ActionContext;
import com.nokia.nsw.uiv.framework.action.HttpAction;
import com.nokia.nsw.uiv.model.resource.Resource;
import com.nokia.nsw.uiv.repository.LogicalDeviceCustomRepository;
import com.nokia.nsw.uiv.repository.ResourceFacingServiceCustomRepository;
import com.nokia.nsw.uiv.request.AssociateResourcesRequest;
import com.nokia.nsw.uiv.response.AssociateResourcesResponse;
import com.nokia.nsw.uiv.utils.Constants;
import com.nokia.nsw.uiv.utils.Validations;
import com.setar.uiv.model.product.ResourceFacingService;
import com.setar.uiv.model.product.ResourceFacingServiceRepository;
import com.nokia.nsw.uiv.model.resource.logical.LogicalDevice;
import com.nokia.nsw.uiv.model.resource.logical.LogicalDeviceRepository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
@RestController
@Action
@Slf4j
public class AssociateResources implements HttpAction {

    private static final String ERROR_PREFIX = "UIV action AssociateResources execution failed - ";

    @Autowired
    private ResourceFacingServiceCustomRepository rfsRepository;

    @Autowired
    private LogicalDeviceCustomRepository deviceRepository;


    @Override
    public Class<?> getActionClass() {
        return AssociateResourcesRequest.class;
    }

    @Override
    public Object doPost(ActionContext actionContext) {
        log.info("Executing AssociateResources action...");
        System.out.println("----Trace #1: Entered AssociateResources Action ----");

        AssociateResourcesRequest request = (AssociateResourcesRequest) actionContext.getObject();

        try {
            // Step 1: Mandatory validations
            System.out.println("----Trace #2: Validating mandatory params ----");
            try {
                Validations.validateMandatoryParams(request.getSubscriberName(), "subscriberName");
                Validations.validateMandatoryParams(request.getServiceId(), "serviceId");
                Validations.validateMandatoryParams(request.getProductSubType(), "productSubType");
            } catch (BadRequestException bre) {
                return new AssociateResourcesResponse(
                        "400",
                        ERROR_PREFIX + "Missing mandatory parameter: " + bre.getMessage(),
                        Instant.now().toString(),
                        ""
                );
            }

            // Step 2: Prepare entity names
            String subscriberName = request.getSubscriberName();
            String subscriptionName;
            String rfsName;
            System.out.println("----Trace #3: Preparing entity names ----");
            if ("IPTV".equalsIgnoreCase(request.getProductSubType())) {
                subscriptionName = subscriberName + "_" + request.getServiceId();
                rfsName = "RFS_" + subscriptionName;
            } else if (request.getOntSN() != null && !"NA".equalsIgnoreCase(request.getOntSN())) {
                subscriptionName = subscriberName + request.getServiceId() + request.getOntSN();
                rfsName = "RFS_" + subscriptionName;
            } else if (request.getCbmSN() != null && !"NA".equalsIgnoreCase(request.getCbmSN())) {
                subscriptionName = subscriberName + request.getServiceId() + request.getCbmSN();
                rfsName = "RFS_" + subscriptionName;
            } else {
                return new AssociateResourcesResponse(
                        "400",
                        ERROR_PREFIX + "Invalid combination of identifiers",
                        Instant.now().toString(),
                        ""
                );
            }

            // Step 3: Retrieve RFS and Admin State
            System.out.println("----Trace #4: Retrieving RFS and AdminState ----");
            Optional<ResourceFacingService> optRfs = rfsRepository.findByDiscoveredName(rfsName);
            if (!optRfs.isPresent()) {
                return new AssociateResourcesResponse(
                        "404",
                        ERROR_PREFIX + "Required entity not found: " + rfsName,
                        Instant.now().toString(),
                        ""
                );
            }
            ResourceFacingService rfs = optRfs.get();
            // Step 4: IPTV logic
            boolean deviceUpdated = false;
            if ("IPTV".equalsIgnoreCase(request.getProductSubType())) {
                System.out.println("----Trace #5: Executing IPTV device association ----");

                // STBs
                String[] stbSerials = {
                        request.getStbSN1(), request.getStbSN2(), request.getStbSN3(),
                        request.getStbSN4(), request.getStbSN5(), request.getStbSN6(),
                        request.getStbSN7(), request.getStbSN8(), request.getStbSN9(),
                        request.getStbSN10(), request.getStbSN11(), request.getStbSN12(),
                        request.getStbSN13(), request.getStbSN14(), request.getStbSN15(),
                        request.getStbSN16(), request.getStbSN17(), request.getStbSN18(),
                        request.getStbSN19(), request.getStbSN20()
                };

                String[] apSerials = {
                        request.getApSN1(), request.getApSN2(), request.getApSN3(),
                        request.getApSN4(), request.getApSN5(), request.getApSN6(),
                        request.getApSN7(), request.getApSN8(), request.getApSN9(),
                        request.getApSN10(), request.getApSN11(), request.getApSN12(),
                        request.getApSN13(), request.getApSN14(), request.getApSN15(),
                        request.getApSN16(), request.getApSN17(), request.getApSN18(),
                        request.getApSN19(), request.getApSN20()
                };

                for (int i = 0; i < stbSerials.length; i++) {
                    String sn = stbSerials[i];
                    if (sn != null && !"NA".equalsIgnoreCase(sn) && !sn.isEmpty()) {
                        String devName = "STB_" + sn;
                        System.out.println("----Trace #6: Processing STB device: " + devName + " ----");
                        Optional<LogicalDevice> optDev = deviceRepository.findByDiscoveredName(devName);
                        if (!optDev.isPresent()) {
                            return new AssociateResourcesResponse(
                                    "404",
                                    ERROR_PREFIX + "Device not found: " + devName,
                                    Instant.now().toString(),
                                    ""
                            );
                        }
                        LogicalDevice device = optDev.get();
                        device.getProperties().put("deviceGroupId", "GROUP" + (i + 1));
                        Map<String,Object>props=new HashMap<>();
                        props.put("administrativeState","Allocated");
                        if (request.getOntSN() != null && !"NA".equalsIgnoreCase(request.getOntSN())) {
                            device.setDescription(request.getServiceId() + "_" + request.getOntSN().replace("ONT", "_"));
                        } else {
                            device.setDescription(request.getServiceId());
                        }
                        device.addUsingService(rfs);
                        deviceRepository.save(device);
                        deviceUpdated = true;
                    }
                }

                for (String sn : apSerials) {
                    if (sn != null && !"NA".equalsIgnoreCase(sn) && !sn.isEmpty()) {
                        String devName = "AP_" + sn;
                        System.out.println("----Trace #7: Processing AP device: " + devName + " ----");
                        Optional<LogicalDevice> optDev = deviceRepository.findByDiscoveredName(devName);
                        if (!optDev.isPresent()) {
                            return new AssociateResourcesResponse(
                                    "404",
                                    ERROR_PREFIX + "Device not found: " + devName,
                                    Instant.now().toString(),
                                    ""
                            );
                        }
                        LogicalDevice device = optDev.get();
                        Map<String,Object>props=new HashMap<>();
                        props.put("administrativeState","Allocated");
                        device.setDescription(request.getServiceId());
                        device.addUsingService(rfs);
                        deviceRepository.save(device);
                        deviceUpdated = true;
                    }
                }
            } else {
                // Step 6: Non-IPTV (ONT/CBM)
                System.out.println("----Trace #8: Executing Non-IPTV device association ----");
                String devName = null;
                if (request.getOntSN() != null && !"NA".equalsIgnoreCase(request.getOntSN())) {
                    devName = "ONT" + request.getOntSN();
                } else if (request.getCbmSN() != null && !"NA".equalsIgnoreCase(request.getCbmSN())) {
                    devName = "CBM" + request.getCbmSN();
                }

                if (devName != null) {
                    Optional<LogicalDevice> optDev = deviceRepository.findByDiscoveredName(devName);
                    if (!optDev.isPresent()) {
                        return new AssociateResourcesResponse(
                                "404",
                                ERROR_PREFIX + "Device not found: " + devName,
                                Instant.now().toString(),
                                ""
                        );
                    }
                    LogicalDevice device = optDev.get();
                    Map<String,Object>props=new HashMap<>();
                    props.put("administrativeState","Allocated");
                    device.setProperties(props);
                    device.setDescription(request.getServiceId());
                    device.addUsingService(rfs);
                    deviceRepository.save(device);
                    deviceUpdated = true;
                }
            }

            // Step 7: Persist RFS changes
            if (deviceUpdated) {
                rfs = rfsRepository.findByDiscoveredName(rfs.getDiscoveredName()).get();
                Map<String,Object> rfsProps = rfs.getProperties();
                rfsProps.put("transactionId",request.getFxOrderID());
                rfsRepository.save(rfs,2);
                System.out.println("----Trace #9: Saving RFS changes ----");
                return new AssociateResourcesResponse(
                        "200",
                        "UIV action AssociateResources executed successfully.",
                        Instant.now().toString(),
                        subscriptionName
                );
            } else {
                return new AssociateResourcesResponse(
                        "400",
                        ERROR_PREFIX + "No valid devices provided",
                        Instant.now().toString(),
                        ""
                );
            }

        } catch (Exception ex) {
            log.error("Unhandled exception during AssociateResources", ex);
            return new AssociateResourcesResponse(
                    "500",
                    ERROR_PREFIX + "Internal server error occurred - " + ex.getMessage(),
                    Instant.now().toString(),
                    ""
            );
        }
    }
}
