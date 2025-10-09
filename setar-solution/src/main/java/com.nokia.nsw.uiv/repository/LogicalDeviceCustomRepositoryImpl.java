package com.nokia.nsw.uiv.repository;

import com.nokia.nsw.uiv.datatype.Neo4jDomainObject;
import com.nokia.nsw.uiv.model.common.party.Customer;
import com.nokia.nsw.uiv.model.resource.logical.LogicalDevice;
import com.nokia.nsw.uiv.model.resource.logical.LogicalDeviceRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.stream.StreamSupport;

@Repository
@Primary
public class LogicalDeviceCustomRepositoryImpl implements LogicalDeviceCustomRepository {

    private final LogicalDeviceRepository logicalDeviceRepository;

    public LogicalDeviceCustomRepositoryImpl(LogicalDeviceRepository logicalDeviceRepository) {
        this.logicalDeviceRepository = logicalDeviceRepository;
    }

    // ✅ Custom finder 1
    @Override
    public Optional<LogicalDevice> findByDiscoveredName(String discoveredName) {
        Iterable<LogicalDevice> allDevices = logicalDeviceRepository.findAll();
        return StreamSupport.stream(allDevices.spliterator(), false)
                .filter(c -> discoveredName.equals(c.getDiscoveredName()))
                .findFirst();
    }

    @Override
    public Optional<LogicalDevice> findByProperty(String key, String value) {
        Iterable<LogicalDevice> allCustomers = logicalDeviceRepository.findAll();
        return StreamSupport.stream(allCustomers.spliterator(), false)
                .filter(c -> value.equals(c.getProperties().get(key)))
                .findFirst();
    }



    // ✅ Delegate CRUD operations to logicalDeviceRepository
    @Override
    public <S extends LogicalDevice> S save(S entity) {
        return logicalDeviceRepository.save(entity);
    }

    @Override
    public <S extends LogicalDevice> Iterable<S> saveAll(Iterable<S> entities) {
        return logicalDeviceRepository.saveAll(entities);
    }

    @Override
    public Optional<LogicalDevice> findById(String id) {
        return logicalDeviceRepository.findById(id);
    }

    @Override
    public boolean existsById(String id) {
        return logicalDeviceRepository.existsById(id);
    }

    @Override
    public <S extends LogicalDevice> S save(S s, int depth) {
        return  logicalDeviceRepository.save(s,depth);
    }

    @Override
    public <S extends LogicalDevice> Iterable<S> save(Iterable<S> entities, int depth) {
        return null;
    }

    @Override
    public Optional<LogicalDevice> findById(String s, int depth) {
        return Optional.empty();
    }

    @Override
    public Iterable<LogicalDevice> findAll() {
        return logicalDeviceRepository.findAll();
    }

    @Override
    public Iterable<LogicalDevice> findAll(int depth) {
        return null;
    }

    @Override
    public Iterable<LogicalDevice> findAllById(Iterable<String> ids) {
        return logicalDeviceRepository.findAllById(ids);
    }

    @Override
    public Iterable<LogicalDevice> findAllById(Iterable<String> strings, int depth) {
        return null;
    }

    @Override
    public Iterable<LogicalDevice> findAllById(Iterable<String> strings, Sort sort) {
        return null;
    }

    @Override
    public Iterable<LogicalDevice> findAllById(Iterable<String> strings, Sort sort, int depth) {
        return null;
    }

    @Override
    public long count() {
        return logicalDeviceRepository.count();
    }

    @Override
    public void deleteById(String id) {
        logicalDeviceRepository.deleteById(id);
    }

    @Override
    public void delete(LogicalDevice entity) {
        logicalDeviceRepository.delete(entity);
    }

    @Override
    public void deleteAll(Iterable<? extends LogicalDevice> entities) {
        logicalDeviceRepository.deleteAll(entities);
    }

    @Override
    public void deleteAll() {
        logicalDeviceRepository.deleteAll();
    }

    @Override
    public Iterable<LogicalDevice> findAll(Sort sort) {
        return logicalDeviceRepository.findAll(sort);
    }

    @Override
    public Iterable<LogicalDevice> findAll(Sort sort, int depth) {
        return null;
    }

    @Override
    public Page<LogicalDevice> findAll(Pageable pageable) {
        return logicalDeviceRepository.findAll(pageable);
    }

    @Override
    public Page<LogicalDevice> findAll(Pageable pageable, int depth) {
        return null;
    }

    // ✅ Methods specific to your interface but not used — keep placeholders
    @Override
    public Optional<LogicalDevice> uivFindByGdn(String s) { return Optional.empty(); }

    @Override
    public Optional<LogicalDevice> uivFindByGdn(String s, int i) { return Optional.empty(); }

    @Override
    public void uivUpdateAssociationProperties(Neo4jDomainObject from, Neo4jDomainObject to, String rel, Map<String, Object> props) { }

    @Override
    public void updateLdn(String s, String s2, String s1) { }

    @Override
    public void flushSession() { }

    @Override
    public <S extends LogicalDevice> S save(S s, String s1) {
        return null;
    }

    @Override
    public <S extends LogicalDevice> Iterable<S> saveAll(Iterable<S> iterable, String s) {
        return null;
    }

    @Override
    public Optional<LogicalDevice> findById(String s, String s2) {
        return Optional.empty();
    }

    @Override
    public boolean existsById(String s, String s2) {
        return false;
    }

    @Override
    public long count(String s) {
        return 0;
    }

    @Override
    public void deleteById(String s, String s2) {

    }

    @Override
    public void delete(LogicalDevice logicalDevice, String s) {

    }

    @Override
    public void deleteAll(Iterable<? extends LogicalDevice> iterable, String s) {

    }

    @Override
    public void deleteAll(String s) {

    }

    @Override
    public <S extends LogicalDevice> S save(S s, int i, String s1) {
        return null;
    }

    @Override
    public <S extends LogicalDevice> Iterable<S> save(Iterable<S> iterable, int i, String s) {
        return null;
    }

    @Override
    public Optional<LogicalDevice> findById(String s, int i, String s2) {
        return Optional.empty();
    }

    @Override
    public Iterable<LogicalDevice> findAll(String s) {
        return null;
    }

    @Override
    public Iterable<LogicalDevice> findAll(int i, String s) {
        return null;
    }

    @Override
    public Iterable<LogicalDevice> findAllById(Iterable<String> iterable, String s) {
        return null;
    }

    @Override
    public Iterable<LogicalDevice> findAllById(Iterable<String> iterable, int i, String s) {
        return null;
    }

    @Override
    public Iterable<LogicalDevice> findAll(Sort sort, String s) {
        return null;
    }

    @Override
    public Iterable<LogicalDevice> findAll(Sort sort, int i, String s) {
        return null;
    }

    @Override
    public Iterable<LogicalDevice> findAllById(Iterable<String> iterable, Sort sort, String s) {
        return null;
    }

    @Override
    public Iterable<LogicalDevice> findAllById(Iterable<String> iterable, Sort sort, int i, String s) {
        return null;
    }

    @Override
    public Page<LogicalDevice> findAll(Pageable pageable, String s) {
        return null;
    }

    @Override
    public Page<LogicalDevice> findAll(Pageable pageable, int i, String s) {
        return null;
    }

    @Override
    public Optional<LogicalDevice> uivFindByGdn(String s, String s1) {
        return Optional.empty();
    }

    @Override
    public Optional<LogicalDevice> uivFindByGdn(String s, int i, String s1) {
        return Optional.empty();
    }

    @Override
    public boolean uivFindById(String s) { return false; }

    @Override
    public boolean uivFindById(String s, String s1, String s2, boolean b, String s3) { return false; }

    @Override
    public void updateLdn(String s, String s2, String s1, String s3) { }

    @Override
    public List<String> uivFindRelationExists(String s, String s1, String s2, String s3, String s4, String s5) {
        return List.of();
    }

    @Override
    public void refactor(Object o, String s, String s2, String s1, boolean b) { }

    @Override
    public void refactor(Object o, String s, String s2, String s1, String s3, boolean b) { }

    @Override
    public LogicalDevice uivFindByTwoEndNode(Map<String, String> map, Map<String, String> map1, String s, String s1) {
        return null;
    }
}
