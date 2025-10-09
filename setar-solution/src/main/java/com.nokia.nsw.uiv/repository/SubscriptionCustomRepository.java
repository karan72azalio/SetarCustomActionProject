package com.nokia.nsw.uiv.repository;

import com.nokia.nsw.uiv.model.service.Subscription;
import com.nokia.nsw.uiv.model.service.SubscriptionRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
@Repository
public interface SubscriptionCustomRepository extends SubscriptionRepository {
    Optional<Subscription> findByDiscoveredName(String discoveredName);
    Optional<Subscription> findByProperty(String key, String value);
}
