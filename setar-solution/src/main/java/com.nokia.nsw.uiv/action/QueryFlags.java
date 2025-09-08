package com.nokia.nsw.uiv.action;


import com.nokia.nsw.uiv.exception.AccessForbiddenException;
import com.nokia.nsw.uiv.exception.BadRequestException;
import com.nokia.nsw.uiv.exception.ModificationNotAllowedException;
import com.nokia.nsw.uiv.framework.action.Action;
import com.nokia.nsw.uiv.framework.action.ActionContext;
import com.nokia.nsw.uiv.framework.action.HttpAction;
import com.nokia.nsw.uiv.model.resource.Resource;
import com.nokia.nsw.uiv.model.resource.logical.*;
import com.nokia.nsw.uiv.model.service.Subscription;
import com.nokia.nsw.uiv.request.QueryFlagsRequest;
import com.nokia.nsw.uiv.response.ChangeStateResponse;
import com.nokia.nsw.uiv.response.QueryFlagsResponse;
import com.nokia.nsw.uiv.utils.Constants;
import com.nokia.nsw.uiv.utils.Validations;
import com.setar.uiv.model.product.ResourceFacingService;
import com.setar.uiv.model.product.ResourceFacingServiceRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@Component
@RestController
@Action
@Slf4j
public class QueryFlags implements HttpAction {

    protected static final String ACTION_LABEL = Constants.QUERY_FLAGS;

    @Autowired
    private LogicalDeviceRepository deviceRepository;

    @Autowired
    private LogicalComponentRepository componentRepository;

    @Autowired
    private ResourceFacingServiceRepository rfsRepository;

    @Override
    public Class getActionClass() {
        return QueryFlagsRequest.class;
    }

    @Override
    public Object doPost(ActionContext actionContext) throws Exception {
        log.warn(Constants.EXECUTING_ACTION, ACTION_LABEL);
        QueryFlagsRequest request = (QueryFlagsRequest) actionContext.getObject();
        String serviceLink = "";
        String serviceSN = "";
        String serviceMAC = "";
        String bridgeService="";
        String ontSN = "";
        String ontName = "";
        String serviceID = request.getServiceId();
        String subscriber = request.getSubscriberName();
        String actionType = request.getActionType();
        String productType = request.getProductType();
        try {
            log.info("Validating mandatory parameters...");
            try{
                Validations.validateMandatory(request.getSubscriberName(), "subscriberName");
                Validations.validateMandatory(request.getProductType(), "productType");
                Validations.validateMandatory(request.getProductSubType(), "productSubType");
                Validations.validateMandatory(request.getOntSN(), "ontSN");
                Validations.validateMandatory(request.getOntPort(), "ontPort");
            }catch (BadRequestException bre) {
                return new QueryFlagsResponse("400", Constants.ERROR_PREFIX + "Missing mandatory parameter : " + bre.getMessage(),
                        java.time.Instant.now().toString(), new HashMap<>());
            }
            log.info("Fetching subscriber logical devices...");
            Map<String, String> flags = new HashMap<>();
            //2. Determine VOIP port availability for non-voice products
            if (!request.getProductType().equalsIgnoreCase("VOIP") &&
                    !request.getProductType().equalsIgnoreCase("Voice")) {
                flags.put("SERVICE_VOIP_NUMBER1", "Available");
                flags.put("SERVICE_VOIP_NUMBER2", "Available");
            }
            //3.Identify Service Link from ONT Serial Number
            serviceLink = "NA";
            if (request.getOntSN().startsWith("ALCL")) {
                serviceLink = "ONT";
            } else if (request.getOntSN().startsWith("CW")) {
                serviceLink = "SRX";
            }
            flags.put("SERVICE_LINK", serviceLink);

            //4. Discover RFS for Broadband, Voice, Enterprise, or Bridged Services
            if(Validations.equalsAny(request.getProductSubType(),"Broadband","Voice","CloudStarter","Bridge") || request.getProductType().equalsIgnoreCase("ENTERPRISE") && request.getActionType().equalsIgnoreCase("Configure")){
                if(request.getOntSN()==null || request.getOntSN().equals("") || request.getOntSN().equalsIgnoreCase("NA")){
                    try{
                        ResourceFacingService setarRFS = new ResourceFacingService();
                        String rfsLocalName = "RFS_"+request.getSubscriberName()+"_"+request.getServiceId();
                        Iterable<ResourceFacingService> rfsList = rfsRepository.findAll(rfsLocalName);
                        Iterator<ResourceFacingService> iterator = rfsList.iterator();
                        while(iterator.hasNext()){
                            ResourceFacingService rfs = iterator.next();
                            String serviceIdTemp=rfs.getContainingCfs().getContainingProduct().getSubscription().getProperties().get("serviceID").toString();
                            if(serviceIdTemp!=null && serviceIdTemp.equals(request.getServiceId())){
                               Set<Resource> resourceList = rfs.getUsedResource();
                               for(Resource resource:resourceList){
                                   if(resource.getLocalName().contains("ONT")){
                                       ontSN = resource.getProperties().get("serialNo").toString();
                                       serviceLink = "ONT";
                                       serviceSN = ontSN;
                                   }else if(resource.getLocalName().contains("CBM")){
                                       serviceLink = "Cable_Modem";
                                   }
                               }
                               break;
                            }
                        }
                        bridgeService = getBridgeServiceId(iterator,request.getOntSN());

                    }catch(Exception e){

                    }
                }

            }
            //6.Set Account and Service Status Flags Based on Action
            if(ontSN!=null && serviceID!=null && subscriber!=null){
//                Map<String, String> outParams = setServiceAndAccountFlag(ontSN, serviceID, actionType,productType);
            }
            // Fetch LogicalDevice by ONT SN
            ontName = "ONT" + request.getOntSN();
            Optional<LogicalDevice> optOnt = deviceRepository.uivFindByGdn(ontName);
            if (optOnt.isPresent()) {
                LogicalDevice ont = optOnt.get();
                flags.put("ONT_MODEL", (String) ont.getProperties().getOrDefault("deviceModel", ""));
                flags.put("SERVICE_SN", (String) ont.getProperties().getOrDefault("serialNo", ""));
                flags.put("SERVICE_MAC", (String) ont.getProperties().getOrDefault("macAddress", ""));
            } else {
                flags.put("ONT_MODEL", "");
                flags.put("SERVICE_SN", request.getOntSN());
                flags.put("SERVICE_MAC", "");
            }

            // Fetch Ethernet ports and VLANs for this ONT
            Optional<LogicalDevice> optOnt1 = deviceRepository.uivFindByGdn(ontName);
            if (optOnt1.isPresent()) {
                LogicalDevice ont = optOnt1.get();
                ArrayList<LogicalResource> ethPorts = new ArrayList<>(ont.getContained()); // get all child ports
                for (LogicalResource port : ethPorts) {
                    String portName = port.getLocalName();
                    if (portName.contains("ETH_2")) flags.put("SERVICE_PORT2_EXIST", "Exist");
                    if (portName.contains("ETH_3")) flags.put("SERVICE_PORT3_EXIST", "Exist");
                    if (portName.contains("ETH_4")) flags.put("SERVICE_PORT4_EXIST", "Exist");
                    if (portName.contains("ETH_5")) flags.put("SERVICE_PORT5_EXIST", "Exist");
                }
            }

            // Simulate IPTV and Fibernet counts
            flags.put("IPTV_COUNT", "0");
            flags.put("FIBERNET_COUNT", "0");

            // Account flags (New/Exist)
            flags.put("ACCOUNT_EXIST", "Exist");
            flags.put("SERVICE_FLAG", "Exist");
            flags.put("CBM_ACCOUNT_EXIST", "Exist");

            // QoS and templates placeholders
            flags.put("QOS_PROFILE", "");
            flags.put("SERVICE_TEMPLATE_CARD", "New");
            flags.put("SERVICE_TEMPLATE_VOIP", "New");
            flags.put("SERVICE_TEMPLATE_HSI", "New");
            flags.put("SERVICE_TEMPLATE_VEIP", "New");
            flags.put("SERVICE_TEMPLATE_IPTV", "New");

            log.info(Constants.ACTION_COMPLETED);
            return new QueryFlagsResponse("200",
                    "UIV action QueryFlags executed successfully.",
                    getCurrentTimestamp(),
                    flags);

        }  catch (Exception ex) {
            log.error("Unhandled exception during QueryFlags", ex);
            String msg = "UIV action QueryFlags execution failed - Internal server error occurred";
            return new QueryFlagsResponse("500", msg + " - " + ex.getMessage(), String.valueOf(System.currentTimeMillis()), Collections.emptyMap());
        }
    }

    private String getCurrentTimestamp() {
        return java.time.Instant.now().toString();
    }
    private String getBridgeServiceId(Iterator<ResourceFacingService> iterator,String ontSN){
        String serviceId = "NA";
        while(iterator.hasNext()){
            ResourceFacingService rfs = iterator.next();
            Subscription sub=rfs.getContainingCfs().getContainingProduct().getSubscription();
            if(sub!=null && sub.getProperties().get("serviceSubType").toString().equalsIgnoreCase("Bridged") && sub.getLocalName().contains(ontSN)){
                serviceId = sub.getProperties().get("serviceID").toString();
            }
        }
        return serviceId;
    }
}
