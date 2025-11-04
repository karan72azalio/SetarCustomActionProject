package com.nokia.nsw.uiv.repository;

import com.nokia.nsw.uiv.datatype.Neo4jDomainObject;
import com.nokia.nsw.uiv.model.resource.logical.LogicalDevice;
import com.nokia.nsw.uiv.model.resource.logical.LogicalDeviceRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.*;
import org.springframework.data.repository.query.FluentQuery;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.function.Function;
import java.util.stream.StreamSupport;

@Repository
@Primary
public class LogicalDeviceCustomRepositoryImpl implements LogicalDeviceCustomRepository {

    private final LogicalDeviceRepository repo;

    public LogicalDeviceCustomRepositoryImpl(LogicalDeviceRepository repo) {
        this.repo = repo;
    }

    // ✅ CUSTOM METHODS
    @Override
    public Optional<LogicalDevice> findByDiscoveredName(String discoveredName) {
        return StreamSupport.stream(repo.findAll().spliterator(), false)
                .filter(d -> discoveredName.equals(d.getDiscoveredName()))
                .findFirst();
    }

    @Override
    public Optional<LogicalDevice> findByProperty(String key, String value) {
        return StreamSupport.stream(repo.findAll().spliterator(), false)
                .filter(d -> value.equals(d.getProperties().get(key)))
                .findFirst();
    }

    // ✅ BASIC CRUD
    @Override public <S extends LogicalDevice> S save(S entity) { return repo.save(entity); }
    @Override public <S extends LogicalDevice> Iterable<S> saveAll(Iterable<S> entities) { return repo.saveAll(entities); }
    @Override public Optional<LogicalDevice> findById(String id) { return repo.findById(id); }
    @Override public boolean existsById(String id) { return repo.existsById(id); }
    @Override public long count() { return repo.count(); }
    @Override public void deleteById(String id) { repo.deleteById(id); }
    @Override public void delete(LogicalDevice entity) { repo.delete(entity); }
    @Override public void deleteAllById(Iterable<? extends String> ids) { repo.deleteAllById(ids); }
    @Override public void deleteAll(Iterable<? extends LogicalDevice> entities) { repo.deleteAll(entities); }
    @Override public void deleteAll() { repo.deleteAll(); }

    @Override public Iterable<LogicalDevice> findAll() { return repo.findAll(); }
    @Override public Iterable<LogicalDevice> findAll(Sort sort) { return repo.findAll(sort); }
    @Override public Page<LogicalDevice> findAll(Pageable pageable) { return repo.findAll(pageable); }
    @Override public Iterable<LogicalDevice> findAllById(Iterable<String> ids) { return repo.findAllById(ids); }
    @Override public Iterable<LogicalDevice> findAllById(Iterable<String> ids, Sort sort) { return repo.findAllById(ids, sort); }

    // ✅ DEPTH-BASED METHODS
    @Override public <S extends LogicalDevice> S save(S entity, int depth) { return repo.save(entity, depth); }
    @Override public <S extends LogicalDevice> Iterable<S> save(Iterable<S> entities, int depth) { return repo.save(entities, depth); }
    @Override public Optional<LogicalDevice> findById(String id, int depth) { return repo.findById(id, depth); }
    @Override public Iterable<LogicalDevice> findAll(int depth) { return repo.findAll(depth); }
    @Override public Iterable<LogicalDevice> findAllById(Iterable<String> ids, int depth) { return repo.findAllById(ids, depth); }
    @Override public Iterable<LogicalDevice> findAllById(Iterable<String> ids, Sort sort, int depth) { return repo.findAllById(ids, sort, depth); }
    @Override public Iterable<LogicalDevice> findAll(Sort sort, int depth) { return repo.findAll(sort, depth); }
    @Override public Page<LogicalDevice> findAll(Pageable pageable, int depth) { return repo.findAll(pageable, depth); }

    // ✅ CONTEXT (ctx)
    @Override public <S extends LogicalDevice> S save(S entity, String ctx) { return repo.save(entity, ctx); }
    @Override public <S extends LogicalDevice> Iterable<S> saveAll(Iterable<S> entities, String ctx) { return repo.saveAll(entities, ctx); }
    @Override public Optional<LogicalDevice> findById(String id, String ctx) { return repo.findById(id, ctx); }
    @Override public Optional<LogicalDevice> findById(String id, int depth, String ctx) { return repo.findById(id, depth, ctx); }
    @Override public boolean existsById(String id, String ctx) { return repo.existsById(id, ctx); }
    @Override public long count(String ctx) { return repo.count(ctx); }
    @Override public void deleteById(String id, String ctx) { repo.deleteById(id, ctx); }
    @Override public void delete(LogicalDevice entity, String ctx) { repo.delete(entity, ctx); }
    @Override public void deleteAll(Iterable<? extends LogicalDevice> entities, String ctx) { repo.deleteAll(entities, ctx); }
    @Override public void deleteAll(String ctx) { repo.deleteAll(ctx); }

    @Override public <S extends LogicalDevice> S save(S entity, int depth, String ctx) { return repo.save(entity, depth, ctx); }
    @Override public <S extends LogicalDevice> Iterable<S> save(Iterable<S> entities, int depth, String ctx) { return repo.save(entities, depth, ctx); }

    @Override public Iterable<LogicalDevice> findAll(String ctx) { return repo.findAll(ctx); }
    @Override public Iterable<LogicalDevice> findAll(int depth, String ctx) { return repo.findAll(depth, ctx); }
    @Override public Iterable<LogicalDevice> findAllById(Iterable<String> ids, String ctx) { return repo.findAllById(ids, ctx); }
    @Override public Iterable<LogicalDevice> findAllById(Iterable<String> ids, int depth, String ctx) { return repo.findAllById(ids, depth, ctx); }
    @Override public Iterable<LogicalDevice> findAll(Sort sort, String ctx) { return repo.findAll(sort, ctx); }
    @Override public Iterable<LogicalDevice> findAll(Sort sort, int depth, String ctx) { return repo.findAll(sort, depth, ctx); }
    @Override public Iterable<LogicalDevice> findAllById(Iterable<String> ids, Sort sort, String ctx) { return repo.findAllById(ids, sort, ctx); }
    @Override public Iterable<LogicalDevice> findAllById(Iterable<String> ids, Sort sort, int depth, String ctx) { return repo.findAllById(ids, sort, depth, ctx); }
    @Override public Page<LogicalDevice> findAll(Pageable pageable, String ctx) { return repo.findAll(pageable, ctx); }
    @Override public Page<LogicalDevice> findAll(Pageable pageable, int depth, String ctx) { return repo.findAll(pageable, depth, ctx); }

    // ✅ GDN
    @Override public Optional<LogicalDevice> uivFindByGdn(String gdn) { return repo.uivFindByGdn(gdn); }
    @Override public Optional<LogicalDevice> uivFindByGdn(String gdn, int depth) { return repo.uivFindByGdn(gdn, depth); }
    @Override public Optional<LogicalDevice> uivFindByGdn(String gdn, String ctx) { return repo.uivFindByGdn(gdn, ctx); }
    @Override public Optional<LogicalDevice> uivFindByGdn(String gdn, int depth, String ctx) { return repo.uivFindByGdn(gdn, depth, ctx); }

    // ✅ Graph Ops
    @Override public void uivUpdateAssociationProperties(Neo4jDomainObject from, Neo4jDomainObject to, String rel, Map<String, Object> props) {
        repo.uivUpdateAssociationProperties(from, to, rel, props);
    }

    @Override public void updateLdn(String old, String newValue, String ctx) { repo.updateLdn(old, newValue, ctx); }
    @Override public void updateLdn(String old, String newValue, String ctx, String mode) { repo.updateLdn(old, newValue, ctx, mode); }

    @Override public List<String> uivFindRelationExists(String a, String b, String c, String d, String e, String f) {
        return repo.uivFindRelationExists(a, b, c, d, e, f);
    }

    @Override public void refactor(Object a, String b, String c, String d, boolean e) { repo.refactor(a, b, c, d, e); }
    @Override public void refactor(Object a, String b, String c, String d, String e, boolean f) { repo.refactor(a, b, c, d, e, f); }

    @Override public LogicalDevice uivFindByTwoEndNode(Map<String, String> a, Map<String, String> b, String r, String ctx) {
        return repo.uivFindByTwoEndNode(a, b, r, ctx);
    }

    @Override public void flushSession() { repo.flushSession(); }
    @Override public boolean uivFindById(String id) { return repo.uivFindById(id); }
    @Override public boolean uivFindById(String a, String b, String c, boolean d, String e) { return repo.uivFindById(a, b, c, d, e); }

    // ✅ Batch Save
    @Override public <S extends LogicalDevice> S batchSave(S s, int d, String ctx) { return repo.batchSave(s, d, ctx); }
    @Override public <S extends LogicalDevice> Iterable<S> batchSaveAll(Iterable<S> it, int d, String ctx) { return repo.batchSaveAll(it, d, ctx); }

    // ✅ SPRING Example API
    @Override public <S extends LogicalDevice> Optional<S> findOne(Example<S> example) { return repo.findOne(example); }
    @Override public <S extends LogicalDevice> Iterable<S> findAll(Example<S> example) { return repo.findAll(example); }
    @Override public <S extends LogicalDevice> Iterable<S> findAll(Example<S> example, Sort sort) { return repo.findAll(example, sort); }
    @Override public <S extends LogicalDevice> Page<S> findAll(Example<S> example, Pageable pageable) { return repo.findAll(example, pageable); }
    @Override public <S extends LogicalDevice> long count(Example<S> example) { return repo.count(example); }
    @Override public <S extends LogicalDevice> boolean exists(Example<S> example) { return repo.exists(example); }
    @Override public <S extends LogicalDevice, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> func) { return repo.findBy(example, func); }
}
