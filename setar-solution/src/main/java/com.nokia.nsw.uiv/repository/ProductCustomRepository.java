package com.nokia.nsw.uiv.repository;

import com.nokia.nsw.uiv.model.service.Subscription;
import com.setar.uiv.model.product.Product;

public interface ProductCustomRepository {
    Product findByDiscoveredName(String discoveredName);
    Product findByProperty(String key, String value);
}
