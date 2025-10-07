package com.nokia.nsw.uiv.repository;


import com.nokia.nsw.uiv.model.common.party.Customer;
import com.nokia.nsw.uiv.model.common.party.CustomerRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomerCustomRepository extends CustomerRepository {

    // Add your new method here
    Customer findByDiscoveredName(String discoveredName);
    Customer findByProperty(String key,String value);
}

