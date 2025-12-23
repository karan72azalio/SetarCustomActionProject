package com.nokia.nsw.uiv.repository;

import com.nokia.nsw.uiv.model.service.Service;
import com.nokia.nsw.uiv.model.service.ServiceRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ServiceCustomRepository extends ServiceRepository {

    // Custom finder methods
    Optional<Service> findByDiscoveredName(String discoveredName);

    Optional<Service> findByProperty(String key, String value);
}
