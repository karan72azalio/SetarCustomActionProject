package com.nokia.nsw.uiv.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AccountTransferByServiceIDResponse {

    private String status;
    private String message;
    private String timestamp;

    public <V, K> AccountTransferByServiceIDResponse(String number, String msg, String currentTimestamp, Map<K,V> kvMap) {
    }
}
