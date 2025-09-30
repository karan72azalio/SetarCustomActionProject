package com.nokia.nsw.uiv.action;

import com.nokia.nsw.uiv.exception.BadRequestException;
import com.nokia.nsw.uiv.framework.action.Action;
import com.nokia.nsw.uiv.framework.action.ActionContext;
import com.nokia.nsw.uiv.framework.action.HttpAction;
import com.nokia.nsw.uiv.request.QueryAllServicesByCPERequest;
import com.nokia.nsw.uiv.response.QueryAllServicesByCPEResponse;
import com.nokia.nsw.uiv.model.resource.logical.LogicalDevice;
import com.nokia.nsw.uiv.model.resource.logical.LogicalDeviceRepository;
import com.setar.uiv.model.product.ResourceFacingService;
import com.setar.uiv.model.product.ResourceFacingServiceRepository;

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
public class QueryAllServicesByCPE implements HttpAction {

    private static final String ERROR_PREFIX = "UIV action QueryAllServicesByCPE execution failed - ";

    @Autowired private LogicalDeviceRepository logicalDeviceRepo;
    @Autowired private ResourceFacingServiceRepository rfsRepo;
    // also need SubscriptionRepo, ProductRepo, SubscriberRepo...

    @Override
    public Class<?> getActionClass() {
        return QueryAllServicesByCPERequest.class;
    }

    @Override
    public Object doPost(ActionContext actionContext) {
        log.info("Executing QueryAllServicesByCPE action...");
        QueryAllServicesByCPERequest req = (QueryAllServicesByCPERequest) actionContext.getObject();

        try {
            // Step 1: Mandatory validation
            if (req.getOntSn() == null || req.getOntSn().isEmpty()) {
                return errorResponse("400", "Missing mandatory parameter: ontSn");
            }
            System.out.println("------Trace #1: Input validated, ontSn=" + req.getOntSn());

            // Step 2: Identify ONT
            String ontName = "ONT" + req.getOntSn();
            Optional<LogicalDevice> ontOpt = logicalDeviceRepo.uivFindByGdn(ontName);
            if (!ontOpt.isPresent()) {
                return errorResponse("404", "CPE/ONT not found");
            }
            LogicalDevice ont = ontOpt.get();
            System.out.println("------Trace #2: ONT located: " + ontName);

            // Step 3: Init counters
            int bbCount = 0, voiceCount = 0, entCount = 0, iptvCount = 0;
            System.out.println("------Trace #3: Counters initialized");

            // Step 4: Traverse linked RFS
            for (ResourceFacingService rfs : rfsRepo.findAll()) {
                String prodType = (String) rfs.getProperties().get("productType");
                if (prodType == null) continue;

                switch (prodType) {
                    case "Broadband":
                    case "Fiber":
                        bbCount++;
                        System.out.println("------Trace #4: Broadband service detected, count=" + bbCount);
                        // populate Broadband_x_ fields in response...
                        break;

                    case "Voice":
                    case "VoIP":
                        voiceCount++;
                        System.out.println("------Trace #5: Voice service detected, count=" + voiceCount);
                        // populate Voice_x_ fields...
                        break;

                    case "Enterprise":
                    case "EVPN":
                        entCount++;
                        System.out.println("------Trace #6: Enterprise service detected, count=" + entCount);
                        // populate Enterprise_x_ fields...
                        break;

                    case "IPTV":
                        iptvCount++;
                        System.out.println("------Trace #7: IPTV service detected, count=" + iptvCount);
                        // reset stbIndex/apIndex/prodIndex, populate IPTV_x_ fields...
                        break;
                }
            }

            // Step 5: Aggregate totals
            QueryAllServicesByCPEResponse resp = new QueryAllServicesByCPEResponse();
            resp.setStatus("200");
            resp.setMessage("UIV action QueryAllServicesByCPE executed successfully.");
            resp.setTimestamp(Instant.now().toString());
            resp.setBbCount(String.valueOf(bbCount));
            resp.setVoiceCount(String.valueOf(voiceCount));
            resp.setEntCount(String.valueOf(entCount));
            resp.setIptvCount(String.valueOf(iptvCount));

            System.out.println("------Trace #8: Totals set");

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
