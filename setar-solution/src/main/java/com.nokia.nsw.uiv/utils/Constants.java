package com.nokia.nsw.uiv.utils;

public class Constants {

    //Actions
    public static final String IMPORT_CPE_DEVICE = "ImportCPEDevice";
    public static final String QUERY_CPE_DEVICE = "QueryCPEDevice";
    public static final String CREATE_SERVICE_FIBERNET = "CreateServiceFibernet";
    public static final String CREATE_SERVICE_CBM = "CreateServiceCBM";
    public static final String MODIFY_CBM = "ModifyCBM";
    public static final String DELETE_CBM = "DeleteCBM";
    public static final String MODIFY_SPR = "ModifySPR";
    public static final String QUERY_FLAGS = "QueryFlags";
    public static final String DELETE_SPR = "DeleteSPR";
    public static final String CHANGE_STATE = "ChangeState";
    public static final String CREATE_PRODUCT_SUBSCRIPTION = "CreateProductSubscription";
    public static final String QUERY_PRODUCT_SUBSCRIPTION = "QueryProductSubscription";
    public static final String DELETE_PRODUCT_SUBSCRIPTION = "DeleteProductSubscription";
    public static final String CREATE_SERVICE_IPTV = "CreateServiceIPTV";
    public static final String MODIFY_IPTV = "ModifyIPTV";
    public static final String DELETE_IPTV = "DeleteIPTV";
    public static final String QUERY_RESOURCE = "QueryResource";
    public static final String DETACH_RESOURCES = "DetachResources";
    public static final String ASSOCIATE_RESOURCES = "AssociateResources";
    public static final String QUERY_ALL_EQUIPMENT = "QueryAllEquipment";
    public static final String CREATE_SERVICE_VOIP = "CreateServiceVOIP";
    public static final String QUERY_VOIP_NUMBER = "QueryVoipNumber";
    public static final String UPDATE_VOIP_SERVICE = "UpdateVOIPService";
    public static final String CREATE_SERVICE_CBM_VOICE = "CreateServiceCbmVoice";
    public static final String QUERY_IPTV_BY_SERVICE_ID = "QueryIPTVByServiceID";
    public static final String QUERY_EQUIPMENT = "QueryEquipment";
    public static final String QUERY_ALL_SERVICES_BY_CPE = "QueryAllServicesByCPE";
    public static final String ACCOUNT_TRANSFER_BY_SERVICE_ID = "AccountTransferByServiceID";
    public static final String CHANGE_TECHNOLOGY = "ChangeTechnology";
    public static final String CHANGE_TECHNOLOGY_VOICE = "ChangeTechnologyVoice";

    //Kinds
    public static final String SETAR_KIND_SETAR_SUBSCRIBER = "SetarSubscriber";
    public static final String SETAR_KIND_SETAR_SUBSCRIPTION = "SetarSubscription";
    public static final String SETAR_KIND_SETAR_PRODUCT = "SetarProduct";
    public static final String SETAR_KIND_SETAR_CFS = "SetarCFS";
    public static final String SETAR_KIND_SETAR_RFS = "SetarRFS";

    public static final String SETAR_KIND_OLT_DEVICE = "OLTDevice";
    public static final String SETAR_KIND_CPE_DEVICE = "CPEDevice";
    public static final String SETAR_KIND_ONT_DEVICE = "ONTDevice";
    public static final String SETAR_KIND_PHYSICAL_DEVICES = "PhysicalDevices";
    public static final String SETAR_KIND_STB_AP_CM_DEVICE = "StbApCmDevice";

    public static final String SETAR_KIND_CPE_PORT = "CPEPort";

    public static final String SETAR_KIND_CPE_VLAN = "CPEVlan";
    public static final String SETAR_KIND_VLAN_INTERFACE = "VLANInterface";
    public static final String SETAR_KIND_TEMPLATE_INTERFACE = "TEMPLATEInterface";



    //Action Messages
    public static final String EXECUTING_ACTION = "Executing action: {}";
    public static final String ACTION_STARTED = "---------------------Action-Started---------------------";
    public static final String ACTION_SUCCESSFUL = "Action successful";
    public static final String ACTION_COMPLETED = "---------------------Action-Completed---------------------";
    public static final String MANDATORY_PARAMS_VALIDATION_STARTED = "MANDATORY PARAMS VALIDATION STARTED";
    public static final String MANDATORY_PARAMS_VALIDATION_COMPLETED = "MANDATORY PARAMS VALIDATION COMPLETED";
    public static final String ERROR_PREFIX = "UIV action ChangeState execution failed - ";
    public static final String UNDER_SCORE = "_";
}
