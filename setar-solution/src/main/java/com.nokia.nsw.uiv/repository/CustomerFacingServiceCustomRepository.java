package com.nokia.nsw.uiv.repository;

import com.setar.uiv.model.product.CustomerFacingService;
import com.setar.uiv.model.product.CustomerFacingServiceRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerFacingServiceCustomRepository extends CustomerFacingServiceRepository {

    // Custom finder methods
    Optional<CustomerFacingService> findByDiscoveredName(String discoveredName);

    Optional<CustomerFacingService> findByProperty(String key, String value);
}
