package com.nokia.nsw.uiv.repository;

import com.nokia.nsw.uiv.datatype.Neo4jDomainObject;
import com.setar.uiv.model.product.CustomerFacingService;
import com.setar.uiv.model.product.CustomerFacingServiceRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.*;
import org.springframework.data.repository.query.FluentQuery;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.function.Function;
import java.util.stream.StreamSupport;

@Repository
@Primary
public class CustomerFacingServiceCustomRepositoryImpl implements CustomerFacingServiceCustomRepository {

    private final CustomerFacingServiceRepository repo;

    public CustomerFacingServiceCustomRepositoryImpl(CustomerFacingServiceRepository repo) {
        this.repo = repo;
    }

    // ********** CUSTOM METHODS **********

    @Override
    public Optional<CustomerFacingService> findByDiscoveredName(String discoveredName) {
        return StreamSupport.stream(repo.findAll().spliterator(), false)
                .filter(s -> discoveredName.equals(s.getDiscoveredName()))
                .findFirst();
    }

    @Override
    public Optional<CustomerFacingService> findByProperty(String key, String value) {
        return StreamSupport.stream(repo.findAll().spliterator(), false)
                .filter(s -> value.equals(s.getProperties().get(key)))
                .findFirst();
    }

    // ********** BASIC CRUD **********

    @Override
    public <S extends CustomerFacingService> S save(S entity) {
        return repo.save(entity);
    }

    @Override
    public <S extends CustomerFacingService> Iterable<S> saveAll(Iterable<S> entities) {
        return repo.saveAll(entities);
    }

    @Override
    public Optional<CustomerFacingService> findById(String id) {
        return repo.findById(id);
    }

    @Override
    public boolean existsById(String id) {
        return repo.existsById(id);
    }

    @Override
    public long count() {
        return repo.count();
    }

    @Override
    public void deleteById(String id) {
        repo.deleteById(id);
    }

    @Override
    public void delete(CustomerFacingService entity) {
        repo.delete(entity);
    }

    @Override
    public void deleteAllById(Iterable<? extends String> ids) {
        repo.deleteAllById(ids);
    }

    @Override
    public void deleteAll(Iterable<? extends CustomerFacingService> entities) {
        repo.deleteAll(entities);
    }

    @Override
    public void deleteAll() {
        repo.deleteAll();
    }

    @Override
    public Iterable<CustomerFacingService> findAll() {
        return repo.findAll();
    }

    @Override
    public Iterable<CustomerFacingService> findAll(Sort sort) {
        return repo.findAll(sort);
    }

    @Override
    public Page<CustomerFacingService> findAll(Pageable pageable) {
        return repo.findAll(pageable);
    }

    @Override
    public Iterable<CustomerFacingService> findAllById(Iterable<String> ids) {
        return repo.findAllById(ids);
    }

    @Override
    public Iterable<CustomerFacingService> findAllById(Iterable<String> ids, Sort sort) {
        return repo.findAllById(ids, sort);
    }

    // ********** DEPTH-BASED METHODS **********

    @Override
    public <S extends CustomerFacingService> S save(S entity, int depth) {
        return repo.save(entity, depth);
    }

    @Override
    public <S extends CustomerFacingService> Iterable<S> save(Iterable<S> entities, int depth) {
        return repo.save(entities, depth);
    }

    @Override
    public Optional<CustomerFacingService> findById(String id, int depth) {
        return repo.findById(id, depth);
    }

    @Override
    public Iterable<CustomerFacingService> findAll(int depth) {
        return repo.findAll(depth);
    }

    @Override
    public Iterable<CustomerFacingService> findAllById(Iterable<String> ids, int depth) {
        return repo.findAllById(ids, depth);
    }

    @Override
    public Iterable<CustomerFacingService> findAllById(Iterable<String> ids, Sort sort, int depth) {
        return repo.findAllById(ids, sort, depth);
    }

    @Override
    public Iterable<CustomerFacingService> findAll(Sort sort, int depth) {
        return repo.findAll(sort, depth);
    }

    @Override
    public Page<CustomerFacingService> findAll(Pageable pageable, int depth) {
        return repo.findAll(pageable, depth);
    }

    // ********** STRING-CONTEXT METHODS (UIV CUSTOM) **********

    @Override
    public <S extends CustomerFacingService> S save(S entity, String ctx) {
        return repo.save(entity, ctx);
    }

    @Override
    public <S extends CustomerFacingService> Iterable<S> saveAll(Iterable<S> entities, String ctx) {
        return repo.saveAll(entities, ctx);
    }

    @Override
    public Optional<CustomerFacingService> findById(String id, String ctx) {
        return repo.findById(id, ctx);
    }

    @Override
    public boolean existsById(String id, String ctx) {
        return repo.existsById(id, ctx);
    }

    @Override
    public long count(String ctx) {
        return repo.count(ctx);
    }

    @Override
    public void deleteById(String id, String ctx) {
        repo.deleteById(id, ctx);
    }

    @Override
    public void delete(CustomerFacingService entity, String ctx) {
        repo.delete(entity, ctx);
    }

    @Override
    public void deleteAll(Iterable<? extends CustomerFacingService> entities, String ctx) {
        repo.deleteAll(entities, ctx);
    }

    @Override
    public void deleteAll(String ctx) {
        repo.deleteAll(ctx);
    }

    @Override
    public <S extends CustomerFacingService> S save(S entity, int depth, String ctx) {
        return repo.save(entity, depth, ctx);
    }

    @Override
    public <S extends CustomerFacingService> Iterable<S> save(Iterable<S> entities, int depth, String ctx) {
        return repo.save(entities, depth, ctx);
    }

    @Override
    public Optional<CustomerFacingService> findById(String id, int depth, String ctx) {
        return repo.findById(id, depth, ctx);
    }

    @Override
    public Iterable<CustomerFacingService> findAll(String ctx) {
        return repo.findAll(ctx);
    }

    @Override
    public Iterable<CustomerFacingService> findAll(int depth, String ctx) {
        return repo.findAll(depth, ctx);
    }

    @Override
    public Iterable<CustomerFacingService> findAllById(Iterable<String> ids, String ctx) {
        return repo.findAllById(ids, ctx);
    }

    @Override
    public Iterable<CustomerFacingService> findAllById(Iterable<String> ids, int depth, String ctx) {
        return repo.findAllById(ids, depth, ctx);
    }

    @Override
    public Iterable<CustomerFacingService> findAll(Sort sort, String ctx) {
        return repo.findAll(sort, ctx);
    }

    @Override
    public Iterable<CustomerFacingService> findAll(Sort sort, int depth, String ctx) {
        return repo.findAll(sort, depth, ctx);
    }

    @Override
    public Iterable<CustomerFacingService> findAllById(Iterable<String> ids, Sort sort, String ctx) {
        return repo.findAllById(ids, sort, ctx);
    }

    @Override
    public Iterable<CustomerFacingService> findAllById(Iterable<String> ids, Sort sort, int depth, String ctx) {
        return repo.findAllById(ids, sort, depth, ctx);
    }

    @Override
    public Page<CustomerFacingService> findAll(Pageable pageable, String ctx) {
        return repo.findAll(pageable, ctx);
    }

    @Override
    public Page<CustomerFacingService> findAll(Pageable pageable, int depth, String ctx) {
        return repo.findAll(pageable, depth, ctx);
    }

    @Override
    public Optional<CustomerFacingService> uivFindByGdn(String gdn, String ctx) {
        return repo.uivFindByGdn(gdn, ctx);
    }

    @Override
    public Optional<CustomerFacingService> uivFindByGdn(String gdn, int depth, String ctx) {
        return repo.uivFindByGdn(gdn, depth, ctx);
    }

    @Override
    public Optional<CustomerFacingService> uivFindByGdn(String gdn) {
        return repo.uivFindByGdn(gdn);
    }

    @Override
    public Optional<CustomerFacingService> uivFindByGdn(String gdn, int depth) {
        return repo.uivFindByGdn(gdn, depth);
    }

    // ********** UIV GRAPH OPS **********

    @Override
    public void uivUpdateAssociationProperties(Neo4jDomainObject from, Neo4jDomainObject to, String rel, Map<String, Object> props) {
        repo.uivUpdateAssociationProperties(from, to, rel, props);
    }

    @Override
    public void updateLdn(String old, String newVal, String ctx) {
        repo.updateLdn(old, newVal, ctx);
    }

    @Override
    public void updateLdn(String old, String newVal, String ctx, String mode) {
        repo.updateLdn(old, newVal, ctx, mode);
    }

    @Override
    public List<String> uivFindRelationExists(String a, String b, String c, String d, String e, String f) {
        return repo.uivFindRelationExists(a, b, c, d, e, f);
    }

    @Override
    public void refactor(Object a, String b, String c, String d, boolean e) {
        repo.refactor(a, b, c, d, e);
    }

    @Override
    public void refactor(Object a, String b, String c, String d, String e, boolean f) {
        repo.refactor(a, b, c, d, e, f);
    }

    @Override
    public void flushSession() {
        repo.flushSession();
    }

    @Override
    public boolean uivFindById(String id) {
        return repo.uivFindById(id);
    }

    @Override
    public boolean uivFindById(String a, String b, String c, boolean d, String e) {
        return repo.uivFindById(a, b, c, d, e);
    }

    @Override
    public CustomerFacingService uivFindByTwoEndNode(Map<String, String> a, Map<String, String> b, String r, String ctx) {
        return repo.uivFindByTwoEndNode(a, b, r, ctx);
    }

    @Override
    public <S extends CustomerFacingService> S batchSave(S s, int d, String ctx) {
        return repo.batchSave(s, d, ctx);
    }

    @Override
    public <S extends CustomerFacingService> Iterable<S> batchSaveAll(Iterable<S> it, int d, String ctx) {
        return repo.batchSaveAll(it, d, ctx);
    }

    // ********** SPRING DATA EXAMPLE API **********

    @Override
    public <S extends CustomerFacingService> Optional<S> findOne(Example<S> example) {
        return repo.findOne(example);
    }

    @Override
    public <S extends CustomerFacingService> Iterable<S> findAll(Example<S> example) {
        return repo.findAll(example);
    }

    @Override
    public <S extends CustomerFacingService> Iterable<S> findAll(Example<S> example, Sort sort) {
        return repo.findAll(example, sort);
    }

    @Override
    public <S extends CustomerFacingService> Page<S> findAll(Example<S> example, Pageable pageable) {
        return repo.findAll(example, pageable);
    }

    @Override
    public <S extends CustomerFacingService> long count(Example<S> example) {
        return repo.count(example);
    }

    @Override
    public <S extends CustomerFacingService> boolean exists(Example<S> example) {
        return repo.exists(example);
    }

    @Override
    public <S extends CustomerFacingService, R> R findBy(
            Example<S> example,
            Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction
    ) {
        return repo.findBy(example, queryFunction);
    }
}
