package com.nokia.nsw.uiv.action;

import com.nokia.nsw.uiv.exception.BadRequestException;
import com.nokia.nsw.uiv.framework.action.Action;
import com.nokia.nsw.uiv.framework.action.ActionContext;
import com.nokia.nsw.uiv.framework.action.HttpAction;
import com.nokia.nsw.uiv.repository.CustomerCustomRepository;
import com.nokia.nsw.uiv.repository.SubscriptionCustomRepository;
import com.nokia.nsw.uiv.request.UpdateVOIPServiceRequest;
import com.nokia.nsw.uiv.response.UpdateVOIPServiceResponse;
import com.nokia.nsw.uiv.utils.Validations;

import com.nokia.nsw.uiv.model.common.party.Customer;
import com.nokia.nsw.uiv.model.common.party.CustomerRepository;
import com.nokia.nsw.uiv.model.service.Subscription;
import com.nokia.nsw.uiv.model.service.SubscriptionRepository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@Component
@RestController
@Action
@Slf4j
public class UpdateVOIPService implements HttpAction {

    private static final String ERROR_PREFIX = "UIV action UpdateVOIPService execution failed - ";

    @Autowired private CustomerCustomRepository customerRepo;
    @Autowired private SubscriptionCustomRepository subscriptionRepo;

    @Override
    public Class<?> getActionClass() {
        return UpdateVOIPServiceRequest.class;
    }

    @Override
    public Object doPatch(ActionContext actionContext) {
        log.info("Executing UpdateVOIPService...");
        UpdateVOIPServiceRequest req = (UpdateVOIPServiceRequest) actionContext.getObject();

        try {
            // Step 1: Validate mandatory params
            try {
                Validations.validateMandatoryParams(req.getSubscriberName(), "subscriberName");
                Validations.validateMandatoryParams(req.getOntSN(), "ontSN");
                Validations.validateMandatoryParams(req.getServiceId(), "serviceId");
                Validations.validateMandatoryParams(req.getSimaSubsId(), "simaSubsId");
            } catch (BadRequestException bre) {
                return new UpdateVOIPServiceResponse(
                        "400",
                        ERROR_PREFIX + "Missing mandatory parameter: " + bre.getMessage(),
                        Instant.now().toString(),
                        null,
                        null
                );
            }

            // Step 2: Construct Names
            String subscriptionName = req.getSubscriberName() + "_" + req.getServiceId() + "_" + req.getOntSN();
            String subscriberNameStr = req.getSubscriberName() + "_" + req.getOntSN();

            boolean updatedFlag = false;

            // Step 4: Locate existing records
            Optional<Subscription> subscriptionOpt = subscriptionRepo.findByDiscoveredName(subscriptionName);
            Optional<Customer> subscriberOpt = customerRepo.findByDiscoveredName(subscriberNameStr);

            if (subscriptionOpt.isEmpty() && subscriberOpt.isEmpty()) {
                return new UpdateVOIPServiceResponse(
                        "404",
                        ERROR_PREFIX + "No entry found for update",
                        Instant.now().toString(),
                        null,
                        null
                );
            }

            // Step 5: Update Subscriber
            if (subscriberOpt.isPresent() && req.getSimaCustId() != null && !req.getSimaCustId().isEmpty()) {
                Customer subscriber = subscriberOpt.get();
                Map<String, Object> props = subscriber.getProperties();
                props.put("simaCustId", req.getSimaCustId());
                subscriber.setProperties(props);
                try {
                    customerRepo.save(subscriber);
                    updatedFlag = true;
                } catch (Exception e) {
                    return new UpdateVOIPServiceResponse(
                            "500",
                            ERROR_PREFIX + "Persistence error while saving subscriber",
                            Instant.now().toString(),
                            null,
                            null
                    );
                }
            }

            // Step 6: Update Subscription
            if (subscriptionOpt.isPresent()) {
                Subscription subs = subscriptionOpt.get();
                Map<String, Object> props = subs.getProperties();

                if (req.getSimaCustId() != null && !req.getSimaCustId().isEmpty()) {
                    props.put("simaCustId", req.getSimaCustId());
                    updatedFlag = true;
                }
                if (req.getSimaSubsId() != null && !req.getSimaSubsId().isEmpty()) {
                    props.put("simaSubsId", req.getSimaSubsId());
                    updatedFlag = true;
                }
                if (req.getSimaEndpointId() != null && !req.getSimaEndpointId().isEmpty()) {
                    props.put("simaEndpointId", req.getSimaEndpointId());
                    updatedFlag = true;
                }

                subs.setProperties(props);

                try {
                    subscriptionRepo.save(subs);
                } catch (Exception e) {
                    return new UpdateVOIPServiceResponse(
                            "500",
                            ERROR_PREFIX + "Persistence error while saving subscription",
                            Instant.now().toString(),
                            null,
                            null
                    );
                }
            }

            // Step 8: Final Response
            if (updatedFlag) {
                return new UpdateVOIPServiceResponse(
                        "200",
                        "UIV action UpdateVOIPService executed successfully.",
                        Instant.now().toString(),
                        req.getSubscriberName(),
                        req.getServiceId()
                );
            } else {
                return new UpdateVOIPServiceResponse(
                        "404",
                        ERROR_PREFIX + "No subscription found to update",
                        Instant.now().toString(),
                        null,
                        null
                );
            }

        } catch (Exception ex) {
            log.error("Exception in UpdateVOIPService", ex);
            return new UpdateVOIPServiceResponse(
                    "500",
                    ERROR_PREFIX + "Error occurred during update - " + ex.getMessage(),
                    Instant.now().toString(),
                    null,
                    null
            );
        }
    }
}
