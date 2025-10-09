package com.nokia.nsw.uiv.repository;


import com.setar.uiv.model.product.ResourceFacingService;
import com.setar.uiv.model.product.ResourceFacingServiceRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ResourceFacingServiceCustomRepository extends ResourceFacingServiceRepository {

    // Custom finder methods
    Optional<ResourceFacingService> findByDiscoveredName(String discoveredName);

    Optional<ResourceFacingService> findByProperty(String key, String value);
}
