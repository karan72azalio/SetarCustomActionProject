package com.nokia.nsw.uiv.response;

import lombok.Getter;
import lombok.Setter;
import javax.validation.constraints.NotNull;
import java.util.List;

@Getter
@Setter
public class QueryIPTVByServiceIDResponse {

    @NotNull
    private String status;                         // M
    @NotNull
    private String message;                        // M
    @NotNull
    private String timestamp;                      // M

    private String customerGroupId;                // C
    private String serviceLink;                    // C
    private String cpeMacAddr1;                    // C
    private String cpeGwMacAddr1;                  // C

    // For repeating serviceComponent_1 … serviceComponent_n
    private List<String> serviceComponents;        // C

    // For AP details (apSerialNo_1 … apSerialNo_n etc.)
    private List<String> apSerialNos;              // C
    private List<String> apMacAddrs;               // C
    private List<String> apPreShareKeys;           // C
    private List<String> apStatuses;               // C
    private List<String> apModels;                 // C

    // For STB details
    private List<String> stbSerialNos;             // C
    private List<String> stbMacAddrs;              // C
    private List<String> stbPreShareKeys;          // C
    private List<String> stbCustomerGroupIds;      // C
    private List<String> stbStatuses;              // C
    private List<String> stbModels;                // C

    private String cpeModel1;                      // C
    private String cpeSerialNumber1;               // C
    private String ontObjectId;                    // C
    private String templateNameONT;                // C
    private String templateNameIPTV;               // C
    private String templateNameIGMP;               // C
    private String templateNameVEIP;               // C
}
