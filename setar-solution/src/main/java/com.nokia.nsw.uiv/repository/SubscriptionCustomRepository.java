package com.nokia.nsw.uiv.repository;

import com.nokia.nsw.uiv.model.service.Subscription;
import com.nokia.nsw.uiv.model.service.SubscriptionRepository;

public interface SubscriptionCustomRepository extends SubscriptionRepository {
    Subscription findByDiscoveredName(String discoveredName);
    Subscription findByProperty(String key, String value);
}
