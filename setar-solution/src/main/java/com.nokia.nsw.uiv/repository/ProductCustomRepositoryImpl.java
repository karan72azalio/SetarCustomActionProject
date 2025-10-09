package com.nokia.nsw.uiv.repository;

import com.setar.uiv.model.product.Product;
import com.setar.uiv.model.product.ProductRepository;

import java.util.stream.StreamSupport;

public class ProductCustomRepositoryImpl implements ProductCustomRepository {

    private final ProductRepository productRepository;

    public ProductCustomRepositoryImpl(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public Product findByDiscoveredName(String discoveredName) {
        Iterable<Product> products = productRepository.findAll();
        return StreamSupport.stream(products.spliterator(), false)
                .filter(p -> p != null && discoveredName.equals(p.getDiscoveredName()))
                .findFirst()
                .orElse(null);
    }

    @Override
    public Product findByProperty(String key, String value) {
        Iterable<Product> products = productRepository.findAll();

        return StreamSupport.stream(products.spliterator(), false)
                .filter(p -> p != null
                                && (
                                (p.getProperties() != null && value.equals(p.getProperties().get(key))) ||
                                        (p.getSubscription() != null
                                                && p.getSubscription().getCustomer() != null
                                                && p.getSubscription().getCustomer().getProperties() != null
                                                && value.equals(p.getSubscription().getCustomer().getProperties().get(key)))
                        )
                )
                .findFirst()
                .orElse(null);
    }
}
