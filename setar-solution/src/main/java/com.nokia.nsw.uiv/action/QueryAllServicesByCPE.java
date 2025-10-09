package com.nokia.nsw.uiv.action;

import com.nokia.nsw.uiv.framework.action.Action;
import com.nokia.nsw.uiv.framework.action.ActionContext;
import com.nokia.nsw.uiv.framework.action.HttpAction;
import com.nokia.nsw.uiv.model.resource.logical.LogicalDevice;
import com.nokia.nsw.uiv.repository.*;
import com.nokia.nsw.uiv.request.QueryAllServicesByCPERequest;
import com.nokia.nsw.uiv.response.QueryAllServicesByCPEResponse;
import com.setar.uiv.model.product.ResourceFacingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
@RestController
@Action
@Slf4j
public class QueryAllServicesByCPE implements HttpAction {

    private static final String ERROR_PREFIX = "UIV action QueryAllServicesByCPE execution failed - ";

    @Autowired
    private LogicalDeviceCustomRepository logicalDeviceRepo;

    @Autowired
    private ResourceFacingServiceCustomRepository rfsRepo;

    @Autowired
    private SubscriptionCustomRepository subscriptionRepo;

    @Autowired
    private ProductCustomRepository productRepo;

    @Autowired
    private CustomerCustomRepository subscriberRepo;

    @Autowired
    private CustomerFacingServiceCustomRepository cfsRepo;

    @Override
    public Class<?> getActionClass() {
        return QueryAllServicesByCPERequest.class;
    }

    @Override
    public Object doPost(ActionContext actionContext) {
        log.info("Executing QueryAllServicesByCPE action...");
        QueryAllServicesByCPERequest req = (QueryAllServicesByCPERequest) actionContext.getObject();

        try {
            // 1) Mandatory validation
            if (req.getOntSn() == null || req.getOntSn().isEmpty()) {
                return errorResponse("400", "Missing mandatory parameter(s): ontSn");
            }
            String ontName = "ONT" + req.getOntSn();

            // 2) Identify the ONT
            Optional<LogicalDevice> ontOpt = logicalDeviceRepo.findByDiscoveredName(ontName);
            if (!ontOpt.isPresent()) {
                return errorResponse("404", "CPE/ONT not found");
            }
            LogicalDevice ont = ontOpt.get();
            log.info("ONT located: {}", ontName);

            // Collect linked RFS entries
            List<ResourceFacingService> linkedRfsList = (List<ResourceFacingService>) ont.getOwningService();
            if (linkedRfsList.isEmpty()) {
                return errorResponse("404", "No services linked to CPE");
            }

            // 3) Initialize counters
            int bbCount = 0, voiceCount = 0, entCount = 0, iptvCount = 0;
            int stbIndex = 1, apIndex = 1, prodIndex = 1;

            QueryAllServicesByCPEResponse resp = new QueryAllServicesByCPEResponse();
            resp.setStatus("200");
            resp.setMessage("UIV action QueryAllServicesByCPE executed successfully.");
            resp.setTimestamp(Instant.now().toString());

            // 4) Traverse services linked to ONT
            for (ResourceFacingService rfs : linkedRfsList) {
                String prodType = (String) rfs.getProperties().get("productType");
                if (prodType == null) continue;

                switch (prodType) {

                    // 4A) Broadband / Fiber
                    case "Broadband":
                    case "Fiber":
                        bbCount++;
                        resp.setBbCount(String.valueOf(bbCount));
                        resp.setBroadband1ServiceId((String) rfs.getProperties().get("serviceId"));
                        resp.setBroadband1ServiceSubtype((String) rfs.getProperties().get("serviceSubtype"));
                        resp.setBroadband1ServiceType("Broadband");
                        resp.setBroadband1QosProfile((String) rfs.getProperties().get("qosProfile"));
                        resp.setBroadband1OntTemplate((String) rfs.getProperties().get("ontTemplate"));
                        resp.setBroadband1ServiceTemplateVeip((String) rfs.getProperties().get("serviceTemplateVeip"));
                        resp.setBroadband1ServiceTemplateHsi((String) rfs.getProperties().get("serviceTemplateHsi"));

                        resp.setBroadband1Hhid((String) rfs.getProperties().get("hhId"));
                        resp.setBroadband1AccountNumber((String) rfs.getProperties().get("accountNumber"));
                        resp.setBroadband1FirstName((String) rfs.getProperties().get("firstName"));
                        resp.setBroadband1LastName((String) rfs.getProperties().get("lastName"));
                        resp.setBroadband1Email((String) rfs.getProperties().get("email"));
                        resp.setBroadband1EmailPassword((String) rfs.getProperties().get("emailPassword"));
                        resp.setBroadband1CompanyName((String) rfs.getProperties().get("companyName"));
                        resp.setBroadband1ContactPhone((String) rfs.getProperties().get("contactPhone"));
                        resp.setBroadband1SubsAddress((String) rfs.getProperties().get("subsAddress"));
                        break;

                    // 4B) Voice / VoIP
                    case "Voice":
                    case "VoIP":
                        voiceCount++;
                        resp.setVoiceCount(String.valueOf(voiceCount));
                        resp.setVoice1ServiceId((String) rfs.getProperties().get("serviceId"));
                        resp.setVoice1ServiceSubtype((String) rfs.getProperties().get("serviceSubtype"));
                        resp.setVoice1ServiceType("Voice");
                        resp.setVoice1CustomerId((String) rfs.getProperties().get("customerId"));
                        resp.setVoice1SimaSubsId((String) rfs.getProperties().get("simaSubsId"));
                        resp.setVoice1SimaEndpointId((String) rfs.getProperties().get("simaEndpointId"));
                        resp.setVoice1VoipNumber1((String) rfs.getProperties().get("voipNumber1"));
                        resp.setVoice1VoipCode1((String) rfs.getProperties().get("voipCode1"));
                        resp.setVoice1QosProfile((String) rfs.getProperties().get("qosProfile"));
                        resp.setVoice1OntTemplate((String) rfs.getProperties().get("ontTemplate"));
                        resp.setVoice1ServiceTemplateVoip((String) rfs.getProperties().get("serviceTemplateVoip"));
                        resp.setVoice1ServiceTemplatePots1((String) rfs.getProperties().get("serviceTemplatePots1"));
                        resp.setVoice1ServiceTemplatePots2((String) rfs.getProperties().get("serviceTemplatePots2"));
                        resp.setVoice1FirstName((String) rfs.getProperties().get("firstName"));
                        resp.setVoice1LastName((String) rfs.getProperties().get("lastName"));
                        break;

                    // 4C) Enterprise / EVPN
                    case "Enterprise":
                    case "EVPN":
                        entCount++;
                        resp.setEntCount(String.valueOf(entCount));
                        resp.setEnterprise1ServiceId((String) rfs.getProperties().get("serviceId"));
                        resp.setEnterprise1ServiceSubtype((String) rfs.getProperties().get("serviceSubtype"));
                        resp.setEnterprise1ServiceType("Enterprise");
                        resp.setEnterprise1QosProfile((String) rfs.getProperties().get("qosProfile"));
                        resp.setEnterprise1KenanSubsId((String) rfs.getProperties().get("kenanSubsId"));
                        resp.setEnterprise1Port((String) rfs.getProperties().get("port"));
                        resp.setEnterprise1Vlan((String) rfs.getProperties().get("vlan"));
                        resp.setEnterprise1TemplateNameVlan((String) rfs.getProperties().get("templateNameVlan"));
                        resp.setEnterprise1TemplateNameVlanCreate((String) rfs.getProperties().get("templateNameVlanCreate"));
                        resp.setEnterprise1TemplateNameVpls((String) rfs.getProperties().get("templateNameVpls"));
                        break;

                    // 4D) IPTV
                    case "IPTV":
                        iptvCount++;
                        stbIndex = 1;
                        apIndex = 1;
                        prodIndex = 1;
                        resp.setIptvCount(String.valueOf(iptvCount));
                        resp.setIptv1ServiceId((String) rfs.getProperties().get("serviceId"));
                        resp.setIptv1ServiceSubtype((String) rfs.getProperties().get("serviceSubtype"));
                        resp.setIptv1ServiceType("IPTV");
                        resp.setIptv1QosProfile((String) rfs.getProperties().get("qosProfile"));
                        resp.setIptv1CustomerGroupId((String) rfs.getProperties().get("customerGroupId"));
                        resp.setIptv1TemplateNameIptv((String) rfs.getProperties().get("templateNameIptv"));
                        resp.setIptv1TemplateNameIgmp((String) rfs.getProperties().get("templateNameIgmp"));
                        resp.setIptv1Vlan((String) rfs.getProperties().get("vlan"));

                        resp.setIptv1StbSn1((String) rfs.getProperties().get("stbSn" + stbIndex));
                        resp.setIptv1StbMac1((String) rfs.getProperties().get("stbMac" + stbIndex));
                        resp.setIptv1StbModel1((String) rfs.getProperties().get("stbModel" + stbIndex));
                        resp.setIptv1ApSn1((String) rfs.getProperties().get("apSn" + apIndex));
                        resp.setIptv1ApMac1((String) rfs.getProperties().get("apMac" + apIndex));
                        resp.setIptv1ApModel1((String) rfs.getProperties().get("apModel" + apIndex));

                        resp.setIptv1ProdName1((String) rfs.getProperties().get("prodName" + prodIndex));
                        resp.setIptv1ProdVariant1((String) rfs.getProperties().get("prodVariant" + prodIndex));
                        break;
                }
            }

            return resp;

        } catch (Exception ex) {
            log.error("Exception in QueryAllServicesByCPE", ex);
            return errorResponse("500", "Error occurred - " + ex.getMessage());
        }
    }

    private QueryAllServicesByCPEResponse errorResponse(String status, String msg) {
        QueryAllServicesByCPEResponse resp = new QueryAllServicesByCPEResponse();
        resp.setStatus(status);
        resp.setMessage(ERROR_PREFIX + msg);
        resp.setTimestamp(Instant.now().toString());
        return resp;
    }
}
