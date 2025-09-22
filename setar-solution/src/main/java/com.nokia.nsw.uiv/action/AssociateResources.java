package com.nokia.nsw.uiv.action;

import com.nokia.nsw.uiv.exception.BadRequestException;
import com.nokia.nsw.uiv.framework.action.Action;
import com.nokia.nsw.uiv.framework.action.ActionContext;
import com.nokia.nsw.uiv.framework.action.HttpAction;
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
import java.util.Optional;

@Component
@RestController
@Action
@Slf4j
public class AssociateResources implements HttpAction {

    private static final String ERROR_PREFIX = "UIV action AssociateResources execution failed - ";

    @Autowired
    private ResourceFacingServiceRepository rfsRepository;

    @Autowired
    private LogicalDeviceRepository deviceRepository;


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
            String subscriptionContext;
            String productContext;
            String cfsContext = "";
            String rfsContext = "";
            String ontContext = "";
            String productName = subscriberName + Constants.UNDER_SCORE+ request.getProductSubType() + Constants.UNDER_SCORE+ request.getServiceId();
            System.out.println("----Trace #3: Preparing entity names ----");
            if ("IPTV".equalsIgnoreCase(request.getProductSubType())) {
                subscriptionName = subscriberName + "_" + request.getServiceId();
                subscriptionContext = subscriberName;
                productContext = Validations.getGlobalName(subscriptionContext,subscriptionName);
                cfsContext = Validations.getGlobalName(productContext,productName);
                String cfsName = "CFS_"+subscriptionName;
                rfsContext = Validations.getGlobalName(cfsContext,cfsName);
                rfsName = "RFS_" + subscriptionName;
            } else if (request.getOntSN() != null && !"NA".equalsIgnoreCase(request.getOntSN())) {
                subscriptionName = subscriberName + request.getServiceId() + request.getOntSN();
                subscriberName = subscriberName + "_" + request.getOntSN();
                subscriptionContext = subscriberName;
                productContext = Validations.getGlobalName(subscriptionContext,subscriptionName);
                cfsContext = Validations.getGlobalName(productContext,productName);
                String cfsName = "CFS_"+subscriptionName;
                rfsContext = Validations.getGlobalName(cfsContext,cfsName);
                rfsName = "RFS_" + subscriptionName;
            } else if (request.getCbmSN() != null && !"NA".equalsIgnoreCase(request.getCbmSN())) {
                subscriptionName = subscriberName + request.getServiceId() + request.getCbmSN();
                subscriberName = subscriberName + "_" + request.getCbmSN();
                subscriptionContext = subscriberName;
                productContext = Validations.getGlobalName(subscriptionContext,subscriptionName);
                cfsContext = Validations.getGlobalName(productContext,productName);
                String cfsName = "CFS_"+subscriptionName;
                rfsContext = Validations.getGlobalName(cfsContext,cfsName);
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
            String rfsGdn = Validations.getGlobalName(rfsContext,rfsName);
            System.out.println("----Trace #4: Retrieving RFS and AdminState ----");
            Optional<ResourceFacingService> optRfs = rfsRepository.uivFindByGdn(rfsGdn);
            if (!optRfs.isPresent()) {
                return new AssociateResourcesResponse(
                        "404",
                        ERROR_PREFIX + "Required entity not found: " + rfsName,
                        Instant.now().toString(),
                        ""
                );
            }
            ResourceFacingService rfs = optRfs.get();

//            AdministrativeState allocatedState = adminStateRepository.findByName("Allocated");
//            if (allocatedState == null) {
//                return new AssociateResourcesResponse(
//                        "404",
//                        ERROR_PREFIX + "Administrative state 'Allocated' not found",
//                        Instant.now().toString(),
//                        ""
//                );
//            }

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
                    if (sn != null && !"NA".equalsIgnoreCase(sn)) {
                        String devName = "STB_" + sn;
                        System.out.println("----Trace #6: Processing STB device: " + devName + " ----");
                        Optional<LogicalDevice> optDev = deviceRepository.uivFindByGdn(devName);
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
//                        device.setAdministrativeState("allocatedState");
                        if (request.getOntSN() != null && !"NA".equalsIgnoreCase(request.getOntSN())) {
                            device.setDescription(request.getServiceId() + "_" + request.getOntSN().replace("ONT", "_"));
                        } else {
                            device.setDescription(request.getServiceId());
                        }
//                        device.addContained(rfs);
                        deviceRepository.save(device);
                        deviceUpdated = true;
                    }
                }

                for (String sn : apSerials) {
                    if (sn != null && !"NA".equalsIgnoreCase(sn)) {
                        String devName = "AP_" + sn;
                        System.out.println("----Trace #7: Processing AP device: " + devName + " ----");
                        Optional<LogicalDevice> optDev = deviceRepository.uivFindByGdn(devName);
                        if (!optDev.isPresent()) {
                            return new AssociateResourcesResponse(
                                    "404",
                                    ERROR_PREFIX + "Device not found: " + devName,
                                    Instant.now().toString(),
                                    ""
                            );
                        }
                        LogicalDevice device = optDev.get();
//                        device.setAdministrativeState(allocatedState);
                        device.setDescription(request.getServiceId());
//                        device.addManagedResources(rfs);
                        deviceRepository.save(device);
                        deviceUpdated = true;
                    }
                }
            } else {
                // Step 6: Non-IPTV (ONT/CBM)
                System.out.println("----Trace #8: Executing Non-IPTV device association ----");
                String devName = null;
                if (request.getOntSN() != null && !"NA".equalsIgnoreCase(request.getOntSN())) {
                    devName = "ONT_" + request.getOntSN();
                } else if (request.getCbmSN() != null && !"NA".equalsIgnoreCase(request.getCbmSN())) {
                    devName = "CBM" + request.getCbmSN();
                }

                if (devName != null) {
                    Optional<LogicalDevice> optDev = deviceRepository.uivFindByGdn(devName);
                    if (!optDev.isPresent()) {
                        return new AssociateResourcesResponse(
                                "404",
                                ERROR_PREFIX + "Device not found: " + devName,
                                Instant.now().toString(),
                                ""
                        );
                    }
                    LogicalDevice device = optDev.get();
//                    device.setAdministrativeState(allocatedState);
                    device.setDescription(request.getServiceId());
//                    device.setResourceFacingService(rfs);
                    deviceRepository.save(device);
                    deviceUpdated = true;
                }
            }

            // Step 7: Persist RFS changes
            if (deviceUpdated) {
                System.out.println("----Trace #9: Saving RFS changes ----");
                rfsRepository.save(rfs);
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
