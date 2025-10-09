package com.nokia.nsw.uiv.repository;

import com.setar.uiv.model.product.Product;
import com.setar.uiv.model.product.ProductRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductCustomRepository extends ProductRepository {

    // Custom finder methods
    Optional<Product> findByDiscoveredName(String discoveredName);

    Optional<Product> findByProperty(String key, String value);
}
