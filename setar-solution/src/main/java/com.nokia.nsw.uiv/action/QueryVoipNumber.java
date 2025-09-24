package com.nokia.nsw.uiv.action;

import com.nokia.nsw.uiv.exception.BadRequestException;
import com.nokia.nsw.uiv.framework.action.Action;
import com.nokia.nsw.uiv.framework.action.ActionContext;
import com.nokia.nsw.uiv.framework.action.HttpAction;
import com.nokia.nsw.uiv.model.resource.logical.LogicalDevice;
import com.nokia.nsw.uiv.model.resource.logical.LogicalDeviceRepository;
import com.nokia.nsw.uiv.model.service.Subscription;
import com.nokia.nsw.uiv.model.service.SubscriptionRepository;
import com.nokia.nsw.uiv.request.QueryVoipNumberRequest;
import com.nokia.nsw.uiv.response.QueryVoipNumberResponse;
import com.nokia.nsw.uiv.response.UpdateVOIPServiceResponse;
import com.nokia.nsw.uiv.utils.Validations;
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
public class QueryVoipNumber implements HttpAction {

    private static final String ERROR_PREFIX = "UIV action QueryVoipNumber execution failed - ";

    @Autowired private LogicalDeviceRepository logicalDeviceRepo;
    @Autowired private SubscriptionRepository subscriptionRepo;

    @Override
    public Class<?> getActionClass() {
        return QueryVoipNumberRequest.class;
    }

    @Override
    public Object doPost(ActionContext actionContext) {
        QueryVoipNumberRequest req = (QueryVoipNumberRequest) actionContext.getObject();
        log.info("Executing QueryVoipNumber action...");

        try {
            // Step 1: Mandatory validation
            try {
                Validations.validateMandatoryParams(req.getOntSN(), "ontSN");
            } catch (BadRequestException bre) {
                return new QueryVoipNumberResponse(
                        "400",
                        ERROR_PREFIX + "Missing mandatory parameter: " + bre.getMessage(),
                        Instant.now().toString(),
                        "",
                        "","","","","","","","","","","",""
                );
            }

            // Step 2: Service link
            String linkType = (req.getServiceLink() != null && "CBM".equalsIgnoreCase(req.getServiceLink()))
                    ? "CBM" : "ONT";
            String ontName = "ONT_" + req.getOntSN();

            // Step 3: Prepare empty fields
            String voipNumber1 = "";
            String voipNumber2 = "";
            String simaCustId = "";
            String simaCustId2 = "";
            String simaSubsId = "";
            String simaSubsId2 = "";
            String simaEndpointId = "";
            String simaEndpointId2 = "";
            String voipCode1 = "";
            String voipCode2 = "";
            String voipPackage = "";
            String firstName = "";
            String lastName = "";

            // Step 4: Get ONT device (only for ONT link)
            if ("ONT".equals(linkType)) {
                String ontGdn = Validations.getGlobalName(ontName);
                Optional<LogicalDevice> ontOpt = logicalDeviceRepo.uivFindByGdn(ontGdn);
                if (ontOpt.isEmpty()) {
                    return errorResponse("404", "ONT device not found: " + ontName);
                }
                LogicalDevice ont = ontOpt.get();
                voipNumber1 = ont.getProperties().getOrDefault("potsPort1Number", "").toString();
                voipNumber2 = ont.getProperties().getOrDefault("potsPort2Number", "").toString();
            }

            // Step 5: Exact subscription lookup
            if (req.getSubscriberName() != null && req.getServiceId() != null) {
                String subscriptionName = ("CBM".equals(linkType))
                        ? req.getSubscriberName() + "_" + req.getServiceId()
                        : req.getSubscriberName() + "_" + req.getServiceId() + "_" + req.getOntSN();

                String subscriptionGdn = Validations.getGlobalName(subscriptionName);
                Optional<Subscription> subsOpt = subscriptionRepo.uivFindByGdn(subscriptionGdn);
                if (subsOpt.isPresent()) {
                    Subscription subs = subsOpt.get();
                    simaCustId     = (String) subs.getProperties().get("simaCustId");
                    simaSubsId     = (String) subs.getProperties().get("simaSubsId");
                    simaEndpointId = (String) subs.getProperties().get("simaEndpointId");
                    voipCode1      = (String) subs.getProperties().get("voipServiceCode");
                    voipPackage    = (String) subs.getProperties().get("voipPackage");
                    firstName      = (String) subs.getProperties().get("firstName");
                    lastName       = (String) subs.getProperties().get("lastName");

                    if ("CBM".equals(linkType)) {
                        voipNumber1 = (String) subs.getProperties().get("voipNumber1");
                        voipNumber2 = "";
                    }

                }
            }

            // Step 6: Fallback search (simplified for this version)
            // TODO: implement complex search logic by subtype if exact match not found

            // Step 7: Final response
            if (simaCustId != null && !simaCustId.isEmpty()) {
                return new QueryVoipNumberResponse(
                        "200",
                        "UIV action QueryVoipNumber executed successfully.",
                        Instant.now().toString(),
                        voipNumber1, voipNumber2,
                        simaCustId, simaCustId2,
                        simaSubsId, simaSubsId2,
                        simaEndpointId, simaEndpointId2,
                        voipCode1, voipCode2,
                        voipPackage, firstName, lastName
                );
            } else {
                return errorResponse("404", "No SIMA Customer ID found");
            }

        } catch (Exception ex) {
            log.error("Exception in QueryVoipNumber", ex);
            return errorResponse("500", "Error occurred while retrieving VoIP details — " + ex.getMessage());
        }
    }

    private QueryVoipNumberResponse errorResponse(String status, String msg) {
        return new QueryVoipNumberResponse(
                status,
                ERROR_PREFIX + msg,
                Instant.now().toString(),
                "", "", "", "", "", "",
                "", "", "", "", "", "", ""
        );
    }
}
