package com.nokia.nsw.uiv.repository;


import com.nokia.nsw.uiv.model.common.party.Customer;
import com.nokia.nsw.uiv.model.common.party.CustomerRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerCustomRepository extends CustomerRepository {

    // Add your new method here
    Optional<Customer> findByDiscoveredName(String discoveredName);
    Optional<Customer> findByProperty(String key,String value);
}

