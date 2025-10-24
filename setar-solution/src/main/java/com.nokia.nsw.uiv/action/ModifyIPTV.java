package com.nokia.nsw.uiv.action;

import com.nokia.nsw.uiv.exception.BadRequestException;
import com.nokia.nsw.uiv.framework.action.Action;
import com.nokia.nsw.uiv.framework.action.ActionContext;
import com.nokia.nsw.uiv.framework.action.HttpAction;
import com.nokia.nsw.uiv.model.common.party.Customer;
import com.nokia.nsw.uiv.model.common.party.CustomerRepository;
import com.nokia.nsw.uiv.model.service.Subscription;
import com.nokia.nsw.uiv.model.service.SubscriptionRepository;
import com.nokia.nsw.uiv.repository.*;
import com.setar.uiv.model.product.CustomerFacingService;
import com.setar.uiv.model.product.CustomerFacingServiceRepository;
import com.setar.uiv.model.product.ResourceFacingService;
import com.setar.uiv.model.product.ResourceFacingServiceRepository;
import com.setar.uiv.model.product.Product;
import com.setar.uiv.model.product.ProductRepository;
import com.nokia.nsw.uiv.model.resource.logical.LogicalDevice;
import com.nokia.nsw.uiv.model.resource.logical.LogicalDeviceRepository;
import com.nokia.nsw.uiv.model.resource.logical.LogicalInterface;
import com.nokia.nsw.uiv.model.resource.logical.LogicalInterfaceRepository;
import com.nokia.nsw.uiv.request.ModifyIPTVRequest;
import com.nokia.nsw.uiv.response.ModifyIPTVResponse;
import com.nokia.nsw.uiv.utils.Validations;
import com.nokia.nsw.uiv.utils.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
@RestController
@Action
@Slf4j
public class ModifyIPTV implements HttpAction {

    protected static final String ACTION_LABEL = "ModifyIPTV";
    private static final String ERROR_PREFIX = "UIV action ModifyIPTV execution failed - ";

    @Autowired
    private CustomerCustomRepository customerRepository;

    @Autowired
    private SubscriptionCustomRepository subscriptionRepository;

    @Autowired
    private ProductCustomRepository productRepository;

    @Autowired
    private CustomerFacingServiceCustomRepository cfsRepository;

    @Autowired
    private ResourceFacingServiceCustomRepository rfsRepository;

    @Autowired
    private LogicalDeviceCustomRepository stbApCmDeviceRepository;

    @Autowired
    private LogicalDeviceCustomRepository logicalDeviceRepository;

    @Autowired
    private LogicalInterfaceCustomRepository logicalInterfaceRepository;

    @Override
    public Class getActionClass() {
        return ModifyIPTVRequest.class;
    }

    @Override
    public Object doPatch(ActionContext actionContext) throws Exception {
        ModifyIPTVRequest request = (ModifyIPTVRequest) actionContext.getObject();

        try {
            // -------------------- Validate mandatory parameters --------------------
            try{
                Validations.validateMandatoryParams(request.getSubscriberName(), "subscriberName");
                Validations.validateMandatoryParams(request.getProductType(), "productType");
                Validations.validateMandatoryParams(request.getProductSubtype(), "productSubtype");
                Validations.validateMandatoryParams(request.getServiceId(), "serviceId");
                Validations.validateMandatoryParams(request.getModifyType(), "modifyType");

            }catch (BadRequestException bre) {
                return new ModifyIPTVResponse("400", Constants.ERROR_PREFIX + "Missing mandatory parameter : " + bre.getMessage(),
                        java.time.Instant.now().toString(), "","");
            }
            String subscriberName = request.getSubscriberName();
            String subscriptionName = subscriberName + "_" + request.getServiceId();
            String productName = subscriberName+Constants.UNDER_SCORE + request.getProductSubtype()+Constants.UNDER_SCORE + request.getServiceId();
            String cfsName = "CFS_" + subscriptionName;
            String rfsName = "RFS_" + subscriptionName;
            String cbmDeviceName = "CBM" + request.getServiceId();

            // -------------------- Fetch entities --------------------
            Optional<Customer> optSubscriber = customerRepository.findByDiscoveredName(subscriberName);
            Optional<Subscription> optSubscription = subscriptionRepository.findByDiscoveredName(subscriptionName);
            Optional<Product> optProduct = productRepository.findByDiscoveredName(productName);
            Optional<CustomerFacingService> optCFS = cfsRepository.findByDiscoveredName(cfsName);
            Optional<ResourceFacingService> optRFS = rfsRepository.findByDiscoveredName(rfsName);

            if (optSubscriber.isEmpty() || optSubscription.isEmpty() || optProduct.isEmpty() || optCFS.isEmpty() || optRFS.isEmpty()) {
                return new ModifyIPTVResponse(
                        "404",
                        ERROR_PREFIX + "One or more required objects (Subscriber, Subscription, Product, CFS, RFS) not found",
                        String.valueOf(System.currentTimeMillis()),
                        "",
                        ""
                );
            }

            Customer subscriber = optSubscriber.get();
            Subscription subscription = optSubscription.get();
            ResourceFacingService rfs = optRFS.get();

            // -------------------- Update transaction details --------------------
            if (request.getFxOrderID() != null && !request.getFxOrderID().isEmpty()) {
                Map<String, Object> rfsProps = rfs.getProperties();
                if (rfsProps == null) rfsProps = new HashMap<>();
                rfsProps.put("transactionId", request.getFxOrderID());
                rfsProps.put("transactionType", request.getModifyType());
                rfs.setProperties(rfsProps);
                rfsRepository.save(rfs, 2);
            }

            // -------------------- Modify based on type --------------------
            String modifyType = request.getModifyType();

            // Modify ONT MAC
            if (modifyType.contains("ModifyONT")) {
                if (request.getModifyParam1() != null && !request.getModifyParam1().equalsIgnoreCase("NA")) {
                    Map<String, Object> subProps = subscription.getProperties();
                    if (subProps == null) subProps = new HashMap<>();
                    subProps.put("serviceMAC", request.getModifyParam1());
                    if (request.getGatewayMac() != null && !request.getGatewayMac().equalsIgnoreCase("NA")) {
                        subProps.put("gatewayMacAddress", request.getGatewayMac());
                    }
                    subscription.setProperties(subProps);
                    subscriptionRepository.save(subscription, 2);
                }
            }

            // Modify Cable Modem
            if (modifyType.contains("ModfiyCableModem")) {
                Optional<LogicalDevice> optCbM = stbApCmDeviceRepository.findByDiscoveredName(cbmDeviceName);
                LogicalDevice cbmDevice = optCbM.orElse(null);

                if (request.getModifyParam1() != null && !request.getModifyParam1().equalsIgnoreCase("NA")) {
                    Map<String, Object> subProps = subscription.getProperties();
                    if (subProps == null) subProps = new HashMap<>();
                    subProps.put("serviceMAC", request.getModifyParam1());
                    subscription.setProperties(subProps);
                    subscriptionRepository.save(subscription, 2);

                    if (cbmDevice != null) {
                        Map<String, Object> cbmProps = cbmDevice.getProperties();
                        if (cbmProps == null) cbmProps = new HashMap<>();
                        cbmProps.put("macAddress", request.getModifyParam1());
                        cbmDevice.setProperties(cbmProps);
                        stbApCmDeviceRepository.save(cbmDevice, 2);
                    }
                }

                if (request.getModifyParam2() != null && !request.getModifyParam2().equalsIgnoreCase("NA") && cbmDevice != null) {
                    Map<String, Object> cbmProps = cbmDevice.getProperties();
                    if (cbmProps == null) cbmProps = new HashMap<>();
                    cbmProps.put("gatewayMacAddress", request.getModifyParam2());
                    cbmDevice.setProperties(cbmProps);
                    stbApCmDeviceRepository.save(cbmDevice, 2);
                }
            }

            // Modify Customer Group
            if (modifyType.contains("ModifyCustomerGroup")) {
                if (request.getModifyParam1() != null && !request.getModifyParam1().equalsIgnoreCase("NA")) {
                    Map<String, Object> subProps = subscription.getProperties();
                    if (subProps == null) subProps = new HashMap<>();
                    subProps.put("customerGroupId", request.getModifyParam1());
                    subscription.setProperties(subProps);
                    subscriptionRepository.save(subscription, 2);
                }
            }

            // Create User
            if (modifyType.contains("CreateUser")) {
                Map<String, Object> subProps = subscriber.getProperties();
                if (subProps == null) subProps = new HashMap<>();
                if (request.getModifyParam1() != null && !request.getModifyParam1().equalsIgnoreCase("NA")) {
                    subProps.put("miSetarUserName", request.getModifyParam1());
                }
                if (request.getModifyParam2() != null && !request.getModifyParam2().equalsIgnoreCase("NA")) {
                    subProps.put("miSetarPassword", request.getModifyParam2());
                }
                subscriber.setProperties(subProps);
                customerRepository.save(subscriber, 2);
            }

            // Delete User
            if (modifyType.contains("DeleteUser")) {
                Map<String, Object> subProps = subscriber.getProperties();
                if (subProps == null) subProps = new HashMap<>();
                subProps.put("miSetarUserName", "");
                subProps.put("miSetarPassword", "");
                subscriber.setProperties(subProps);
                customerRepository.save(subscriber, 2);
            }

            // Reset Password
            if (modifyType.contains("ResetPassword")) {
                if (request.getModifyParam1() != null && !request.getModifyParam1().equalsIgnoreCase("NA")) {
                    Map<String, Object> subProps = subscriber.getProperties();
                    if (subProps == null) subProps = new HashMap<>();
                    subProps.put("miSetarPassword", request.getModifyParam1());
                    subscriber.setProperties(subProps);
                    customerRepository.save(subscriber, 2);
                }
            }
            return new ModifyIPTVResponse(
                    "200",
                    "UIV action ModifyIPTV executed successfully.",
                    java.time.Instant.now().toString(),
                    subscriber.getName(),
                    subscription.getName()
            );

        } catch (IllegalArgumentException ex) {
            log.error("Mandatory validation failed", ex);
            return new ModifyIPTVResponse(
                    "400",
                    ERROR_PREFIX + "Missing mandatory parameter: " + ex.getMessage(),
                    String.valueOf(System.currentTimeMillis()),
                    "",
                    ""
            );
        } catch (Exception ex) {
            log.error("ModifyIPTV failed", ex);
            return new ModifyIPTVResponse(
                    "500",
                    ERROR_PREFIX + "IPTV request " + request.getModifyType() + " not executed",
                    String.valueOf(System.currentTimeMillis()),
                    "",
                    ""
            );
        }
    }
}
