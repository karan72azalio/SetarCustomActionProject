package com.nokia.nsw.uiv.repository;

import com.nokia.nsw.uiv.model.resource.logical.LogicalDevice;
import com.nokia.nsw.uiv.model.resource.logical.LogicalInterface;
import com.nokia.nsw.uiv.model.resource.logical.LogicalInterfaceRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LogicalInterfaceCustomRepository extends LogicalInterfaceRepository {

    // Custom finder methods
    Optional<LogicalInterface>  findByDiscoveredName(String discoveredName);

    Optional<LogicalInterface>  findByProperty(String key, String value);
}
