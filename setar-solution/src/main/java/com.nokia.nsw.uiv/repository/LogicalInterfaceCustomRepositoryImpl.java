package com.nokia.nsw.uiv.repository;

import com.nokia.nsw.uiv.datatype.Neo4jDomainObject;
import com.nokia.nsw.uiv.model.resource.logical.LogicalInterface;
import com.nokia.nsw.uiv.model.resource.logical.LogicalInterfaceRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.stream.StreamSupport;

@Repository
@Primary
public class LogicalInterfaceCustomRepositoryImpl implements LogicalInterfaceCustomRepository {

    private final LogicalInterfaceRepository logicalInterfaceRepository;

    public LogicalInterfaceCustomRepositoryImpl(LogicalInterfaceRepository logicalInterfaceRepository) {
        this.logicalInterfaceRepository = logicalInterfaceRepository;
    }

    // ✅ Custom finder methods
    @Override
    public Optional<LogicalInterface> findByDiscoveredName(String discoveredName) {
        Iterable<LogicalInterface> allInterfaces = logicalInterfaceRepository.findAll();
        return StreamSupport.stream(allInterfaces.spliterator(), false)
                .filter(i -> discoveredName.equals(i.getDiscoveredName()))
                .findFirst();
    }

    @Override
    public Optional<LogicalInterface> findByProperty(String key, String value) {
        Iterable<LogicalInterface> allInterfaces = logicalInterfaceRepository.findAll();
        return StreamSupport.stream(allInterfaces.spliterator(), false)
                .filter(i -> i.getProperties().get(key) != null ? i.getProperties().get(key).equals(value) : false)
                .findFirst();
    }

    // ✅ Delegate CRUD operations
    @Override
    public <S extends LogicalInterface> S save(S entity) {
        return logicalInterfaceRepository.save(entity);
    }

    @Override
    public <S extends LogicalInterface> Iterable<S> saveAll(Iterable<S> entities) {
        return logicalInterfaceRepository.saveAll(entities);
    }

    @Override
    public Optional<LogicalInterface> findById(String id) {
        return logicalInterfaceRepository.findById(id);
    }

    @Override
    public boolean existsById(String id) {
        return logicalInterfaceRepository.existsById(id);
    }

    @Override
    public long count() {
        return logicalInterfaceRepository.count();
    }

    @Override
    public void deleteById(String id) {
        logicalInterfaceRepository.deleteById(id);
    }

    @Override
    public void delete(LogicalInterface entity) {
        logicalInterfaceRepository.delete(entity);
    }

    @Override
    public void deleteAll(Iterable<? extends LogicalInterface> entities) {
        logicalInterfaceRepository.deleteAll(entities);
    }

    @Override
    public void deleteAll() {
        logicalInterfaceRepository.deleteAll();
    }

    @Override
    public Iterable<LogicalInterface> findAll() {
        return logicalInterfaceRepository.findAll();
    }

    @Override
    public Iterable<LogicalInterface> findAll(Sort sort) {
        return logicalInterfaceRepository.findAll(sort);
    }

    @Override
    public Page<LogicalInterface> findAll(Pageable pageable) {
        return logicalInterfaceRepository.findAll(pageable);
    }

    // ✅ Extended methods (delegate or placeholders)
    @Override
    public <S extends LogicalInterface> S save(S entity, int depth) {
        return logicalInterfaceRepository.save(entity, depth);
    }

    @Override
    public <S extends LogicalInterface> Iterable<S> save(Iterable<S> entities, int depth) {
        return null;
    }

    @Override
    public Optional<LogicalInterface> findById(String id, int depth) {
        return Optional.empty();
    }

    @Override
    public Iterable<LogicalInterface> findAll(int depth) {
        return null;
    }

    @Override
    public Iterable<LogicalInterface> findAllById(Iterable<String> ids) {
        return logicalInterfaceRepository.findAllById(ids);
    }

    @Override
    public Iterable<LogicalInterface> findAllById(Iterable<String> strings, int depth) {
        return null;
    }

    @Override
    public Iterable<LogicalInterface> findAllById(Iterable<String> strings, Sort sort) {
        return null;
    }

    @Override
    public Iterable<LogicalInterface> findAllById(Iterable<String> strings, Sort sort, int depth) {
        return null;
    }

    @Override
    public Iterable<LogicalInterface> findAll(Sort sort, int depth) {
        return null;
    }

    @Override
    public Page<LogicalInterface> findAll(Pageable pageable, int depth) {
        return null;
    }

    // ✅ Placeholder implementations (unused but required)
    @Override
    public Optional<LogicalInterface> uivFindByGdn(String s) { return Optional.empty(); }

    @Override
    public Optional<LogicalInterface> uivFindByGdn(String s, int i) { return Optional.empty(); }

    @Override
    public void uivUpdateAssociationProperties(Neo4jDomainObject from, Neo4jDomainObject to, String rel, Map<String, Object> props) { }

    @Override
    public void updateLdn(String s, String s2, String s1) { }

    @Override
    public void flushSession() { }

    @Override
    public <S extends LogicalInterface> S save(S s, String s1) { return null; }

    @Override
    public <S extends LogicalInterface> Iterable<S> saveAll(Iterable<S> iterable, String s) { return null; }

    @Override
    public Optional<LogicalInterface> findById(String s, String s2) { return Optional.empty(); }

    @Override
    public boolean existsById(String s, String s2) { return false; }

    @Override
    public long count(String s) { return 0; }

    @Override
    public void deleteById(String s, String s2) { }

    @Override
    public void delete(LogicalInterface logicalInterface, String s) { }

    @Override
    public void deleteAll(Iterable<? extends LogicalInterface> iterable, String s) { }

    @Override
    public void deleteAll(String s) { }

    @Override
    public <S extends LogicalInterface> S save(S s, int i, String s1) { return null; }

    @Override
    public <S extends LogicalInterface> Iterable<S> save(Iterable<S> iterable, int i, String s) { return null; }

    @Override
    public Optional<LogicalInterface> findById(String s, int i, String s2) { return Optional.empty(); }

    @Override
    public Iterable<LogicalInterface> findAll(String s) { return null; }

    @Override
    public Iterable<LogicalInterface> findAll(int i, String s) { return null; }

    @Override
    public Iterable<LogicalInterface> findAllById(Iterable<String> iterable, String s) { return null; }

    @Override
    public Iterable<LogicalInterface> findAllById(Iterable<String> iterable, int i, String s) { return null; }

    @Override
    public Iterable<LogicalInterface> findAll(Sort sort, String s) { return null; }

    @Override
    public Iterable<LogicalInterface> findAll(Sort sort, int i, String s) { return null; }

    @Override
    public Iterable<LogicalInterface> findAllById(Iterable<String> iterable, Sort sort, String s) { return null; }

    @Override
    public Iterable<LogicalInterface> findAllById(Iterable<String> iterable, Sort sort, int i, String s) { return null; }

    @Override
    public Page<LogicalInterface> findAll(Pageable pageable, String s) { return null; }

    @Override
    public Page<LogicalInterface> findAll(Pageable pageable, int i, String s) { return null; }

    @Override
    public Optional<LogicalInterface> uivFindByGdn(String s, String s1) { return Optional.empty(); }

    @Override
    public Optional<LogicalInterface> uivFindByGdn(String s, int i, String s1) { return Optional.empty(); }

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
    public LogicalInterface uivFindByTwoEndNode(Map<String, String> map, Map<String, String> map1, String s, String s1) { return null; }
}
