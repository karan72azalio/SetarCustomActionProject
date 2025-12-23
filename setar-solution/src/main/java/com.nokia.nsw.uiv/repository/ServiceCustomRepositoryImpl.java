package com.nokia.nsw.uiv.repository;

import com.nokia.nsw.uiv.datatype.Neo4jDomainObject;
import com.nokia.nsw.uiv.model.service.Service;
import com.nokia.nsw.uiv.model.service.ServiceRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.StreamSupport;

@Repository
@Primary
public class ServiceCustomRepositoryImpl
        implements ServiceCustomRepository {

    private final ServiceRepository repo;

    public ServiceCustomRepositoryImpl(ServiceRepository repo) {
        this.repo = repo;
    }

    // ********** CUSTOM METHODS (DO NOT CHANGE) **********

    @Override
    public Optional<Service> findByDiscoveredName(String discoveredName) {
        return StreamSupport.stream(repo.findAll().spliterator(), false)
                .filter(s -> discoveredName.equals(s.getDiscoveredName()))
                .findFirst();
    }

    @Override
    public Optional<Service> findByProperty(String key, String value) {
        return StreamSupport.stream(repo.findAll().spliterator(), false)
                .filter(s -> value.equals(s.getProperties().get(key)))
                .findFirst();
    }

    // ********** BASIC CRUD **********

    @Override
    public <S extends Service> S save(S entity) {
        return repo.save(entity);
    }

    @Override
    public <S extends Service> Iterable<S> saveAll(Iterable<S> entities) {
        return repo.saveAll(entities);
    }

    @Override
    public Optional<Service> findById(String id) {
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
    public void delete(Service entity) {
        repo.delete(entity);
    }

    @Override
    public void deleteAllById(Iterable<? extends String> ids) {
        repo.deleteAllById(ids);
    }

    @Override
    public void deleteAll(Iterable<? extends Service> entities) {
        repo.deleteAll(entities);
    }

    @Override
    public void deleteAll() {
        repo.deleteAll();
    }

    @Override
    public Iterable<Service> findAll() {
        return repo.findAll();
    }

    @Override
    public Iterable<Service> findAll(Sort sort) {
        return repo.findAll(sort);
    }

    @Override
    public Page<Service> findAll(Pageable pageable) {
        return repo.findAll(pageable);
    }

    @Override
    public Iterable<Service> findAllById(Iterable<String> ids) {
        return repo.findAllById(ids);
    }

    @Override
    public Iterable<Service> findAllById(Iterable<String> ids, Sort sort) {
        return repo.findAllById(ids, sort);
    }

    @Override
    public Iterable<Service> findAllById(Iterable<String> ids, Sort sort, int depth) {
        return repo.findAllById(ids, sort, depth);
    }

    // ********** DEPTH-BASED METHODS **********

    @Override
    public <S extends Service> S save(S entity, int depth) {
        return repo.save(entity, depth);
    }

    @Override
    public <S extends Service> Iterable<S> save(Iterable<S> entities, int depth) {
        return repo.save(entities, depth);
    }

    @Override
    public Optional<Service> findById(String id, int depth) {
        return repo.findById(id, depth);
    }

    @Override
    public Iterable<Service> findAll(int depth) {
        return repo.findAll(depth);
    }

    @Override
    public Iterable<Service> findAllById(Iterable<String> ids, int depth) {
        return repo.findAllById(ids, depth);
    }

    @Override
    public Iterable<Service> findAll(Sort sort, int depth) {
        return repo.findAll(sort, depth);
    }

    @Override
    public Page<Service> findAll(Pageable pageable, int depth) {
        return repo.findAll(pageable, depth);
    }

    // ********** STRING CONTEXT METHODS **********

    @Override
    public <S extends Service> S save(S entity, String ctx) {
        return repo.save(entity, ctx);
    }

    @Override
    public <S extends Service> Iterable<S> saveAll(Iterable<S> entities, String ctx) {
        return repo.saveAll(entities, ctx);
    }

    @Override
    public Optional<Service> findById(String id, String ctx) {
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
    public void delete(Service entity, String ctx) {
        repo.delete(entity, ctx);
    }

    @Override
    public void deleteAll(Iterable<? extends Service> entities, String ctx) {
        repo.deleteAll(entities, ctx);
    }

    @Override
    public void deleteAll(String ctx) {
        repo.deleteAll(ctx);
    }

    @Override
    public <S extends Service> S save(S s, int depth, String ctx) {
        return repo.save(s, depth, ctx);
    }

    @Override
    public <S extends Service> Iterable<S> save(Iterable<S> entities, int depth, String ctx) {
        return repo.save(entities, depth, ctx);
    }

    @Override
    public Optional<Service> findById(String id, int depth, String ctx) {
        return repo.findById(id, depth, ctx);
    }

    @Override
    public Iterable<Service> findAll(String ctx) {
        return repo.findAll(ctx);
    }

    @Override
    public Iterable<Service> findAll(int depth, String ctx) {
        return repo.findAll(depth, ctx);
    }

    @Override
    public Iterable<Service> findAllById(Iterable<String> ids, String ctx) {
        return repo.findAllById(ids, ctx);
    }

    @Override
    public Iterable<Service> findAllById(Iterable<String> ids, int depth, String ctx) {
        return repo.findAllById(ids, depth, ctx);
    }

    @Override
    public Iterable<Service> findAll(Sort sort, String ctx) {
        return repo.findAll(sort, ctx);
    }

    @Override
    public Iterable<Service> findAll(Sort sort, int depth, String ctx) {
        return repo.findAll(sort, depth, ctx);
    }

    @Override
    public Iterable<Service> findAllById(Iterable<String> ids, Sort sort, String ctx) {
        return repo.findAllById(ids, sort, ctx);
    }

    @Override
    public Iterable<Service> findAllById(Iterable<String> ids, Sort sort, int depth, String ctx) {
        return repo.findAllById(ids, sort, depth, ctx);
    }

    @Override
    public Page<Service> findAll(Pageable pageable, String ctx) {
        return repo.findAll(pageable, ctx);
    }

    @Override
    public Page<Service> findAll(Pageable pageable, int depth, String ctx) {
        return repo.findAll(pageable, depth, ctx);
    }

    @Override
    public Optional<Service> uivFindByGdn(String gdn) {
        return repo.uivFindByGdn(gdn);
    }

    @Override
    public Optional<Service> uivFindByGdn(String gdn, int depth) {
        return repo.uivFindByGdn(gdn, depth);
    }

    @Override
    public Optional<Service> uivFindByGdn(String gdn, String ctx) {
        return repo.uivFindByGdn(gdn, ctx);
    }

    @Override
    public Optional<Service> uivFindByGdn(String gdn, int depth, String ctx) {
        return repo.uivFindByGdn(gdn, depth, ctx);
    }

    @Override
    public boolean uivFindById(String id) {
        return repo.uivFindById(id);
    }

    @Override
    public boolean uivFindById(String a, String b, String c, boolean d, String e) {
        return repo.uivFindById(a, b, c, d, e);
    }

    // ********** UIV / GRAPH OPS **********

    @Override
    public void uivUpdateAssociationProperties(
            Neo4jDomainObject from,
            Neo4jDomainObject to,
            String rel,
            Map<String, Object> props) {
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
    public List<String> uivFindRelationExists(
            String a, String b, String c, String d, String e, String f) {
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
    public Service uivFindByTwoEndNode(
            Map<String, String> from,
            Map<String, String> to,
            String rel,
            String ctx) {
        return repo.uivFindByTwoEndNode(from, to, rel, ctx);
    }

    @Override
    public void flushSession() {
        repo.flushSession();
    }

    @Override
    public <S extends Service> S batchSave(S s, int depth, String ctx) {
        return repo.batchSave(s, depth, ctx);
    }

    @Override
    public <S extends Service> Iterable<S> batchSaveAll(
            Iterable<S> iterable, int depth, String ctx) {
        return repo.batchSaveAll(iterable, depth, ctx);
    }

    // ********** SPRING DATA EXAMPLE API **********

    @Override
    public <S extends Service> Optional<S> findOne(Example<S> example) {
        return repo.findOne(example);
    }

    @Override
    public <S extends Service> Iterable<S> findAll(Example<S> example) {
        return repo.findAll(example);
    }

    @Override
    public <S extends Service> Iterable<S> findAll(Example<S> example, Sort sort) {
        return repo.findAll(example, sort);
    }

    @Override
    public <S extends Service> Page<S> findAll(
            Example<S> example, Pageable pageable) {
        return repo.findAll(example, pageable);
    }

    @Override
    public <S extends Service> long count(Example<S> example) {
        return repo.count(example);
    }

    @Override
    public <S extends Service> boolean exists(Example<S> example) {
        return repo.exists(example);
    }

    @Override
    public <S extends Service, R> R findBy(
            Example<S> example,
            Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) {
        return repo.findBy(example, queryFunction);
    }
}
