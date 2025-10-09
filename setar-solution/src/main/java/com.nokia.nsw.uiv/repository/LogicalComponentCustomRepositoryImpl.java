package com.nokia.nsw.uiv.repository;

import com.nokia.nsw.uiv.datatype.Neo4jDomainObject;
import com.nokia.nsw.uiv.model.resource.logical.LogicalComponent;
import com.nokia.nsw.uiv.model.resource.logical.LogicalComponentRepository;
import com.nokia.nsw.uiv.model.resource.logical.LogicalDevice;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.stream.StreamSupport;

@Repository
@Primary
public class LogicalComponentCustomRepositoryImpl implements LogicalComponentCustomRepository {

    private final LogicalComponentRepository logicalComponentRepository;

    public LogicalComponentCustomRepositoryImpl(LogicalComponentRepository logicalComponentRepository) {
        this.logicalComponentRepository = logicalComponentRepository;
    }

    // ✅ Custom finder methods
    @Override
    public Optional<LogicalComponent> findByDiscoveredName(String discoveredName) {
        Iterable<LogicalComponent> allComponents = logicalComponentRepository.findAll();
        return StreamSupport.stream(allComponents.spliterator(), false)
                .filter(c -> discoveredName.equals(c.getDiscoveredName()))
                .findFirst();
    }


    @Override
    public Optional<LogicalComponent> findByProperty(String key, String value) {
        Iterable<LogicalComponent> allComponents = logicalComponentRepository.findAll();
        return StreamSupport.stream(allComponents.spliterator(), false)
                .filter(c -> value.equals(c.getProperties().get(key)))
                .findFirst();
    }

    // ✅ Delegate CRUD operations
    @Override
    public <S extends LogicalComponent> S save(S entity) {
        return logicalComponentRepository.save(entity);
    }

    @Override
    public <S extends LogicalComponent> Iterable<S> saveAll(Iterable<S> entities) {
        return logicalComponentRepository.saveAll(entities);
    }

    @Override
    public Optional<LogicalComponent> findById(String id) {
        return logicalComponentRepository.findById(id);
    }

    @Override
    public boolean existsById(String id) {
        return logicalComponentRepository.existsById(id);
    }

    @Override
    public long count() {
        return logicalComponentRepository.count();
    }

    @Override
    public void deleteById(String id) {
        logicalComponentRepository.deleteById(id);
    }

    @Override
    public void delete(LogicalComponent entity) {
        logicalComponentRepository.delete(entity);
    }

    @Override
    public void deleteAll(Iterable<? extends LogicalComponent> entities) {
        logicalComponentRepository.deleteAll(entities);
    }

    @Override
    public void deleteAll() {
        logicalComponentRepository.deleteAll();
    }

    @Override
    public Iterable<LogicalComponent> findAll() {
        return logicalComponentRepository.findAll();
    }

    @Override
    public Iterable<LogicalComponent> findAll(Sort sort) {
        return logicalComponentRepository.findAll(sort);
    }

    @Override
    public Page<LogicalComponent> findAll(Pageable pageable) {
        return logicalComponentRepository.findAll(pageable);
    }

    // ✅ Extended methods (delegate or placeholders)
    @Override
    public <S extends LogicalComponent> S save(S entity, int depth) {
        return logicalComponentRepository.save(entity, depth);
    }

    @Override
    public <S extends LogicalComponent> Iterable<S> save(Iterable<S> entities, int depth) {
        return null;
    }

    @Override
    public Optional<LogicalComponent> findById(String id, int depth) {
        return Optional.empty();
    }

    @Override
    public Iterable<LogicalComponent> findAll(int depth) {
        return null;
    }

    @Override
    public Iterable<LogicalComponent> findAllById(Iterable<String> ids) {
        return logicalComponentRepository.findAllById(ids);
    }

    @Override
    public Iterable<LogicalComponent> findAllById(Iterable<String> strings, int depth) {
        return null;
    }

    @Override
    public Iterable<LogicalComponent> findAllById(Iterable<String> strings, Sort sort) {
        return null;
    }

    @Override
    public Iterable<LogicalComponent> findAllById(Iterable<String> strings, Sort sort, int depth) {
        return null;
    }

    @Override
    public Iterable<LogicalComponent> findAll(Sort sort, int depth) {
        return null;
    }

    @Override
    public Page<LogicalComponent> findAll(Pageable pageable, int depth) {
        return null;
    }

    // ✅ Placeholder implementations (unused but required)
    @Override
    public Optional<LogicalComponent> uivFindByGdn(String s) { return Optional.empty(); }

    @Override
    public Optional<LogicalComponent> uivFindByGdn(String s, int i) { return Optional.empty(); }

    @Override
    public void uivUpdateAssociationProperties(Neo4jDomainObject from, Neo4jDomainObject to, String rel, Map<String, Object> props) { }

    @Override
    public void updateLdn(String s, String s2, String s1) { }

    @Override
    public void flushSession() { }

    @Override
    public <S extends LogicalComponent> S save(S s, String s1) { return null; }

    @Override
    public <S extends LogicalComponent> Iterable<S> saveAll(Iterable<S> iterable, String s) { return null; }

    @Override
    public Optional<LogicalComponent> findById(String s, String s2) { return Optional.empty(); }

    @Override
    public boolean existsById(String s, String s2) { return false; }

    @Override
    public long count(String s) { return 0; }

    @Override
    public void deleteById(String s, String s2) { }

    @Override
    public void delete(LogicalComponent logicalComponent, String s) { }

    @Override
    public void deleteAll(Iterable<? extends LogicalComponent> iterable, String s) { }

    @Override
    public void deleteAll(String s) { }

    @Override
    public <S extends LogicalComponent> S save(S s, int i, String s1) { return null; }

    @Override
    public <S extends LogicalComponent> Iterable<S> save(Iterable<S> iterable, int i, String s) { return null; }

    @Override
    public Optional<LogicalComponent> findById(String s, int i, String s2) { return Optional.empty(); }

    @Override
    public Iterable<LogicalComponent> findAll(String s) { return null; }

    @Override
    public Iterable<LogicalComponent> findAll(int i, String s) { return null; }

    @Override
    public Iterable<LogicalComponent> findAllById(Iterable<String> iterable, String s) { return null; }

    @Override
    public Iterable<LogicalComponent> findAllById(Iterable<String> iterable, int i, String s) { return null; }

    @Override
    public Iterable<LogicalComponent> findAll(Sort sort, String s) { return null; }

    @Override
    public Iterable<LogicalComponent> findAll(Sort sort, int i, String s) { return null; }

    @Override
    public Iterable<LogicalComponent> findAllById(Iterable<String> iterable, Sort sort, String s) { return null; }

    @Override
    public Iterable<LogicalComponent> findAllById(Iterable<String> iterable, Sort sort, int i, String s) { return null; }

    @Override
    public Page<LogicalComponent> findAll(Pageable pageable, String s) { return null; }

    @Override
    public Page<LogicalComponent> findAll(Pageable pageable, int i, String s) { return null; }

    @Override
    public Optional<LogicalComponent> uivFindByGdn(String s, String s1) { return Optional.empty(); }

    @Override
    public Optional<LogicalComponent> uivFindByGdn(String s, int i, String s1) { return Optional.empty(); }

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
    public LogicalComponent uivFindByTwoEndNode(Map<String, String> map, Map<String, String> map1, String s, String s1) { return null; }
}
