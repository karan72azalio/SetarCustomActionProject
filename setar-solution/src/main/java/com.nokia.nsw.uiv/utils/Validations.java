package com.nokia.nsw.uiv.utils;

import com.nokia.nsw.uiv.exception.BadRequestException;
import org.apache.commons.lang.StringUtils;

public class Validations {

    public static void validateMandatoryParams(String ParamValue, String ParamName) throws BadRequestException {
        if (StringUtils.isBlank(ParamValue)) {
            String errorMsg = String.format("ERR001: Missing mandatory parameter: %s", ParamName);
            throw new BadRequestException(errorMsg);
        }
    }

    public static void validateMandatoryParams(String ParamValue, String ParamName, String appendMsg) throws BadRequestException {
        if (StringUtils.isBlank(ParamValue)) {
            String errorMsg = String.format("ERR001: Missing mandatory parameter: %s %s", ParamName, appendMsg);
            throw new BadRequestException(errorMsg);
        }
    }

    public static void validateMandatory(String val, String name) throws BadRequestException {
        if (val == null || val.trim().isEmpty()) throw new BadRequestException(name);
    }

    public static boolean equalsAny(String data, String ...args){
        for(String arg:args){
            if(data.equalsIgnoreCase(arg)){
                return true;
            }
        }
        return false;
    }
    public static String getGlobalName(String localName){
        if(localName.trim().isEmpty() || localName==null){
            return "";
        }
        return Constants.SETAR+ Constants.COMMA+localName;
    }
}
