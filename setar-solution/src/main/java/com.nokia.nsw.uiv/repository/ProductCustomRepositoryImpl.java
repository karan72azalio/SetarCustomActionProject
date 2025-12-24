package com.nokia.nsw.uiv.repository;

import com.nokia.nsw.uiv.datatype.Neo4jDomainObject;
import com.nokia.nsw.uiv.model.service.Product;
import com.nokia.nsw.uiv.model.service.ProductRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.function.Function;
import java.util.stream.StreamSupport;

@Repository
@Primary
public class ProductCustomRepositoryImpl implements ProductCustomRepository {

    private final ProductRepository productRepository;

    public ProductCustomRepositoryImpl(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    // ✅ Custom finder methods
    @Override
    public Optional<Product> findByDiscoveredName(String discoveredName) {
        Iterable<Product> allProducts = productRepository.findAll();
        return StreamSupport.stream(allProducts.spliterator(), false)
                .filter(p -> discoveredName.equals(p.getDiscoveredName()))
                .findFirst();
    }

    @Override
    public Optional<Product> findByProperty(String key, String value) {
        Iterable<Product> allProducts = productRepository.findAll();
        return StreamSupport.stream(allProducts.spliterator(), false)
                .filter(p -> p.getProperties() != null && value.equals(p.getProperties().get(key)))
                .findFirst();
    }

    // ✅ Delegate CRUD operations
    @Override
    public <S extends Product> S save(S entity) {
        return productRepository.save(entity);
    }

    @Override
    public <S extends Product> Iterable<S> saveAll(Iterable<S> entities) {
        return productRepository.saveAll(entities);
    }

    @Override
    public Optional<Product> findById(String id) {
        return productRepository.findById(id);
    }

    @Override
    public boolean existsById(String id) {
        return productRepository.existsById(id);
    }

    @Override
    public long count() {
        return productRepository.count();
    }

    @Override
    public void deleteById(String id) {
        productRepository.deleteById(id);
    }

    @Override
    public void delete(Product entity) {
        productRepository.delete(entity);
    }

    @Override
    public void deleteAllById(Iterable<? extends String> strings) {

    }

    @Override
    public void deleteAll(Iterable<? extends Product> entities) {
        productRepository.deleteAll(entities);
    }

    @Override
    public void deleteAll() {
        productRepository.deleteAll();
    }

    @Override
    public Iterable<Product> findAll() {
        return productRepository.findAll();
    }

    @Override
    public Iterable<Product> findAll(Sort sort) {
        return productRepository.findAll(sort);
    }

    @Override
    public Page<Product> findAll(Pageable pageable) {
        return productRepository.findAll(pageable);
    }

    // ✅ Extended methods (delegate or placeholders)
    @Override
    public <S extends Product> S save(S entity, int depth) {
        return productRepository.save(entity, depth);
    }

    @Override
    public <S extends Product> Iterable<S> save(Iterable<S> entities, int depth) {
        return null;
    }

    @Override
    public Optional<Product> findById(String id, int depth) {
        return Optional.empty();
    }

    @Override
    public Iterable<Product> findAll(int depth) {
        return null;
    }

    @Override
    public Iterable<Product> findAllById(Iterable<String> ids) {
        return productRepository.findAllById(ids);
    }

    @Override
    public Iterable<Product> findAllById(Iterable<String> strings, int depth) {
        return null;
    }

    @Override
    public Iterable<Product> findAllById(Iterable<String> strings, Sort sort) {
        return null;
    }

    @Override
    public Iterable<Product> findAllById(Iterable<String> strings, Sort sort, int depth) {
        return null;
    }

    @Override
    public Iterable<Product> findAll(Sort sort, int depth) {
        return null;
    }

    @Override
    public Page<Product> findAll(Pageable pageable, int depth) {
        return null;
    }

    // ✅ Placeholder implementations (unused but required)
    @Override
    public Optional<Product> uivFindByGdn(String s) { return Optional.empty(); }

    @Override
    public Optional<Product> uivFindByGdn(String s, int i) { return Optional.empty(); }

    @Override
    public void uivUpdateAssociationProperties(Neo4jDomainObject from, Neo4jDomainObject to, String rel, Map<String, Object> props) { }

    @Override
    public void updateLdn(String s, String s2, String s1) { }

    @Override
    public void flushSession() { }

    @Override
    public <S extends Product> S batchSave(S s, int i, String s1) {
        return null;
    }

    @Override
    public <S extends Product> Iterable<S> batchSaveAll(Iterable<S> iterable, int i, String s) {
        return null;
    }

    @Override
    public <S extends Product> S save(S s, String s1) { return null; }

    @Override
    public <S extends Product> Iterable<S> saveAll(Iterable<S> iterable, String s) { return null; }

    @Override
    public Optional<Product> findById(String s, String s2) { return Optional.empty(); }

    @Override
    public boolean existsById(String s, String s2) { return false; }

    @Override
    public long count(String s) { return 0; }

    @Override
    public void deleteById(String s, String s2) { }

    @Override
    public void delete(Product product, String s) { }

    @Override
    public void deleteAll(Iterable<? extends Product> iterable, String s) { }

    @Override
    public void deleteAll(String s) { }

    @Override
    public <S extends Product> S save(S s, int i, String s1) { return null; }

    @Override
    public <S extends Product> Iterable<S> save(Iterable<S> iterable, int i, String s) { return null; }

    @Override
    public Optional<Product> findById(String s, int i, String s2) { return Optional.empty(); }

    @Override
    public Iterable<Product> findAll(String s) { return null; }

    @Override
    public Iterable<Product> findAll(int i, String s) { return null; }

    @Override
    public Iterable<Product> findAllById(Iterable<String> iterable, String s) { return null; }

    @Override
    public Iterable<Product> findAllById(Iterable<String> iterable, int i, String s) { return null; }

    @Override
    public Iterable<Product> findAll(Sort sort, String s) { return null; }

    @Override
    public Iterable<Product> findAll(Sort sort, int i, String s) { return null; }

    @Override
    public Iterable<Product> findAllById(Iterable<String> iterable, Sort sort, String s) { return null; }

    @Override
    public Iterable<Product> findAllById(Iterable<String> iterable, Sort sort, int i, String s) { return null; }

    @Override
    public Page<Product> findAll(Pageable pageable, String s) { return null; }

    @Override
    public Page<Product> findAll(Pageable pageable, int i, String s) { return null; }

    @Override
    public Optional<Product> uivFindByGdn(String s, String s1) { return Optional.empty(); }

    @Override
    public Optional<Product> uivFindByGdn(String s, int i, String s1) { return Optional.empty(); }

    @Override
    public boolean uivFindById(String s) { return false; }

    @Override
    public boolean uivFindById(String s, String s1, String s2, boolean b, String s3) { return false; }

    @Override
    public void updateLdn(String s, String s2, String s1, String s3) { }

    @Override
    public List<String> uivFindRelationExists(String s, String s1, String s2, String s3, String s4, String s5) { return List.of(); }

    @Override
    public void refactor(Object o, String s, String s2, String s1, boolean b) { }

    @Override
    public void refactor(Object o, String s, String s2, String s1, String s3, boolean b) { }

    @Override
    public Product uivFindByTwoEndNode(Map<String, String> map, Map<String, String> map1, String s, String s1) { return null; }

    @Override
    public <S extends Product> Optional<S> findOne(Example<S> example) {
        return Optional.empty();
    }

    @Override
    public <S extends Product> Iterable<S> findAll(Example<S> example) {
        return null;
    }

    @Override
    public <S extends Product> Iterable<S> findAll(Example<S> example, Sort sort) {
        return null;
    }

    @Override
    public <S extends Product> Page<S> findAll(Example<S> example, Pageable pageable) {
        return null;
    }

    @Override
    public <S extends Product> long count(Example<S> example) {
        return 0;
    }

    @Override
    public <S extends Product> boolean exists(Example<S> example) {
        return false;
    }

    @Override
    public <S extends Product, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) {
        return null;
    }
}
