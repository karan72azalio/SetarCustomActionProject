package com.nokia.nsw.uiv.action;

import com.nokia.nsw.uiv.framework.action.Action;
import com.nokia.nsw.uiv.framework.action.ActionContext;
import com.nokia.nsw.uiv.framework.action.HttpAction;
import com.nokia.nsw.uiv.model.resource.logical.LogicalDevice;
import com.nokia.nsw.uiv.model.resource.logical.LogicalDeviceRepository;
import com.nokia.nsw.uiv.repository.LogicalDeviceCustomRepository;
import com.nokia.nsw.uiv.request.QueryONTPositionRequest;
import com.nokia.nsw.uiv.response.QueryONTPositionResponse;
import com.nokia.nsw.uiv.utils.Validations;


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
public class QueryONTPosition implements HttpAction {

    private static final String ERROR_PREFIX = "UIV action QueryONTPosition execution failed - ";

    @Autowired
    private LogicalDeviceCustomRepository ontRepo;

    @Override
    public Class<?> getActionClass() {
        return QueryONTPositionRequest.class;
    }

    @Override
    public Object doPost(ActionContext actionContext) {
        System.out.println("------------Trace # 1--------------- QueryONTPosition started");
        QueryONTPositionRequest req = (QueryONTPositionRequest) actionContext.getObject();

        try {
            // 1. Mandatory validation
            try {
                Validations.validateMandatory(req.getOntSn(), "ontSn");
            } catch (Exception bre) {
                System.out.println("------------Trace # 2--------------- Missing param: " + bre.getMessage());
                return new QueryONTPositionResponse(
                        "400",
                        ERROR_PREFIX + "Missing mandatory parameter: " + bre.getMessage(),
                        Instant.now().toString(),
                        ""
                );
            }

            String ontSn = req.getOntSn();
            System.out.println("------------Trace # 3--------------- Input ontSn=" + ontSn);

            // 2. Build ONT Name and length check
            String ontName = "ONT" + ontSn;
            System.out.println("------------Trace # 4--------------- Constructed ONT name: " + ontName);

            if (ontName.length() > 100) {
                System.out.println("------------Trace # 5--------------- ONT name too long");
                return new QueryONTPositionResponse(
                        "400",
                        ERROR_PREFIX + "ONT name too long.",
                        Instant.now().toString(),
                        ""
                );
            }

            // 3. Locate ONT
            Optional<LogicalDevice> ontOpt = ontRepo.findByDiscoveredName(ontName);
            if (!ontOpt.isPresent()) {
                System.out.println("------------Trace # 6--------------- No ONT found with name=" + ontName);
                return new QueryONTPositionResponse(
                        "404",
                        ERROR_PREFIX + "No ONT found.",
                        Instant.now().toString(),
                        ""
                );
            }

            LogicalDevice ont = ontOpt.get();
            System.out.println("------------Trace # 7--------------- ONT found, checking linked OLT");

            Set<LogicalDevice> managingDevices =  ont.getManagingDevices();
            LogicalDevice olt = managingDevices.stream().findFirst().get();
            if (olt == null) {
                System.out.println("------------Trace # 8--------------- No OLT linked to ONT=" + ontName);
                return new QueryONTPositionResponse(
                        "404",
                        ERROR_PREFIX + "No ONT Object ID found.",
                        Instant.now().toString(),
                        ""
                );
            }

            // 4. Determine OLT Object ID
            String objectId = olt.getProperties().get("OltPosition")==null?"":olt.getProperties().get("OltPosition").toString();
            if (objectId == null || objectId.isEmpty()) {
                objectId = olt.getName();
            }

            if (objectId == null || objectId.isEmpty()) {
                System.out.println("------------Trace # 9--------------- OLT Object ID is empty");
                return new QueryONTPositionResponse(
                        "404",
                        ERROR_PREFIX + "No ONT Object ID found.",
                        Instant.now().toString(),
                        ""
                );
            }

            System.out.println("------------Trace # 10--------------- Resolved OLT Object ID=" + objectId);

            // 5. Success response
            return new QueryONTPositionResponse(
                    "200",
                    "UIV action QueryONTPosition executed successfully.",
                    Instant.now().toString(),
                    objectId
            );

        } catch (Exception ex) {
            log.error("Unhandled exception in QueryONTPosition", ex);
            return new QueryONTPositionResponse(
                    "500",
                    ERROR_PREFIX + "Internal server error occurred - " + ex.getMessage(),
                    Instant.now().toString(),
                    ""
            );
        }
    }
}
