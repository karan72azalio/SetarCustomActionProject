package com.nokia.nsw.uiv.action;

import com.nokia.nsw.uiv.framework.action.Action;
import com.nokia.nsw.uiv.framework.action.ActionContext;
import com.nokia.nsw.uiv.framework.action.HttpAction;
import com.nokia.nsw.uiv.request.ChangeTechnologyRequest;
import com.nokia.nsw.uiv.response.ChangeTechnologyResponse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@Component
@RestController
@Action
@Slf4j
public class ChangeTechnology implements HttpAction {

    private static final String ERROR_PREFIX = "UIV action ChangeTechnology execution failed - ";

    @Override
    public Class<?> getActionClass() {
        return ChangeTechnologyRequest.class;
    }

    @Override
    public Object doPost(ActionContext actionContext) {
        ChangeTechnologyRequest req = (ChangeTechnologyRequest) actionContext.getObject();
        System.out.println("------Trace #1: Starting ChangeTechnology");

        try {
            // Step 1: Mandatory validation
            if (isEmpty(req.getSubscriberName()) ||
                    isEmpty(req.getProductSubtype()) ||
                    isEmpty(req.getServiceId()) ||
                    isEmpty(req.getOntSN()) ||
                    isEmpty(req.getOntMacAddr()) ||
                    isEmpty(req.getCbmSn()) ||
                    isEmpty(req.getOltName()) ||
                    isEmpty(req.getMenm()) ||
                    isEmpty(req.getVlanId()) ||
                    isEmpty(req.getOntModel()) ||
                    isEmpty(req.getCbmMac())) {
                return errorResponse("400", "Missing mandatory parameter(s)", null, null);
            }
            System.out.println("------Trace #2: Validated mandatory params");

            // Step 3: Prepare names
            String subscriptionName = req.getSubscriberName() + "_" + req.getServiceId();
            String cfsName = "CFS_" + subscriptionName;
            String rfsName = "RFS_" + subscriptionName;
            String cbmName = "CBM" + req.getCbmSn();
            String mgmtVlanName = req.getMenm() + "_" + req.getVlanId();
            String ontName = "ONT_" + req.getOntSN();
            String subscriberNameFibernet = req.getSubscriberName() + "_" + req.getOntSN();
            String subscriberNameCbmKey = req.getSubscriberName() + "_" + req.getCbmMac().replace(":", "");

            System.out.println("------Trace #3: Prepared names");

            // Step 4: Validate name length
            if ("Fibernet".equalsIgnoreCase(req.getProductSubtype()) &&
                    subscriberNameFibernet.length() > 100) {
                return errorResponse("400", "Subscriber name too long", subscriptionName, ontName);
            }
            if (ontName.length() > 100) {
                return errorResponse("400", "ONT name too long", subscriptionName, ontName);
            }

            // Step 5..12 - Repository / Entity operations
            // ⚠️ Stubbed with traces (real implementation would call repositories)
            System.out.println("------Trace #4: Update Subscriber (if found by " + subscriberNameCbmKey + ")");
            System.out.println("------Trace #5: Update Subscription with ONT details, MAC, SN, QoS");
            System.out.println("------Trace #6: Update CFS " + cfsName);
            System.out.println("------Trace #7: Update RFS " + rfsName);
            System.out.println("------Trace #8: Prepare OLT device " + req.getOltName());
            System.out.println("------Trace #9: Prepare ONT device " + ontName);
            System.out.println("------Trace #10: Create/Retrieve VLAN interface " + mgmtVlanName);
            System.out.println("------Trace #11: Remove CBM device " + cbmName);
            System.out.println("------Trace #12: Reassign CPE devices ONT vs CBM");

            // Step 13: Success response
            ChangeTechnologyResponse resp = new ChangeTechnologyResponse();
            resp.setStatus("200");
            resp.setMessage("UIV action ChangeTechnology executed successfully.");
            resp.setTimestamp(Instant.now().toString());
            resp.setSubscriptionName(subscriptionName);
            resp.setOntName(ontName);
            return resp;

        } catch (Exception ex) {
            log.error("Exception in ChangeTechnology", ex);
            return errorResponse("500", "Unexpected error - " + ex.getMessage(), null, null);
        }
    }

    private boolean isEmpty(String val) {
        return val == null || val.trim().isEmpty();
    }

    private ChangeTechnologyResponse errorResponse(String status, String msg,
                                                   String subscriptionName, String ontName) {
        ChangeTechnologyResponse resp = new ChangeTechnologyResponse();
        resp.setStatus(status);
        resp.setMessage(ERROR_PREFIX + msg);
        resp.setTimestamp(Instant.now().toString());
        resp.setSubscriptionName(subscriptionName == null ? "" : subscriptionName);
        resp.setOntName(ontName == null ? "" : ontName);
        return resp;
    }
}
