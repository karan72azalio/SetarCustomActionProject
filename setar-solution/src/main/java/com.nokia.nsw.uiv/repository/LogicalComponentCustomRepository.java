package com.nokia.nsw.uiv.repository;

import com.nokia.nsw.uiv.model.resource.logical.LogicalComponent;
import com.nokia.nsw.uiv.model.resource.logical.LogicalComponentRepository;
import com.nokia.nsw.uiv.model.resource.logical.LogicalInterface;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LogicalComponentCustomRepository extends LogicalComponentRepository {

    // Custom finder methods
    Optional<LogicalComponent>  findByDiscoveredName(String discoveredName);

    Optional<LogicalComponent> findByProperty(String key, String value);
}
