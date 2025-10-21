package com.nokia.nsw.uiv.repository;

import com.nokia.nsw.uiv.datatype.Neo4jDomainObject;
import com.nokia.nsw.uiv.model.service.Subscription;
import com.nokia.nsw.uiv.model.service.SubscriptionRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Repository
@Primary
public class SubscriptionCustomRepositoryImpl implements SubscriptionCustomRepository {

    private final SubscriptionRepository subscriptionRepository;

    public SubscriptionCustomRepositoryImpl(SubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
    }

    // ✅ Custom finders
    @Override
    public Optional<Subscription> findByDiscoveredName(String discoveredName) {
        Iterable<Subscription> allSubscriptions = subscriptionRepository.findAll();
        return StreamSupport.stream(allSubscriptions.spliterator(), false)
                .filter(s -> discoveredName.equals(s.getDiscoveredName()))
                .findFirst();
    }

    @Override
    public Optional<Subscription> findByProperty(String key, String value) {
        Iterable<Subscription> allSubscriptions = subscriptionRepository.findAll();
        return StreamSupport.stream(allSubscriptions.spliterator(), false)
                .filter(s -> s.getProperties() != null &&
                        s.getProperties().get(key) != null &&
                        s.getProperties().get(key).equals(value))
                .findFirst();
    }

    // ✅ CRUD Delegations
    @Override
    public <S extends Subscription> S save(S entity) {
        return subscriptionRepository.save(entity);
    }

    @Override
    public <S extends Subscription> S save(S entity, int depth) {
        return subscriptionRepository.save(entity, depth);
    }

    @Override
    public <S extends Subscription> Iterable<S> saveAll(Iterable<S> entities) {
        return subscriptionRepository.saveAll(entities);
    }

    @Override
    public <S extends Subscription> Iterable<S> save(Iterable<S> entities, int depth) {
        return subscriptionRepository.save(entities, depth);
    }

    @Override
    public Optional<Subscription> findById(String id) {
        return subscriptionRepository.findById(id);
    }

    @Override
    public Optional<Subscription> findById(String id, int depth) {
        return subscriptionRepository.findById(id, depth);
    }

    @Override
    public boolean existsById(String id) {
        return subscriptionRepository.existsById(id);
    }

    @Override
    public Iterable<Subscription> findAll() {
        return subscriptionRepository.findAll();
    }

    @Override
    public Iterable<Subscription> findAll(int depth) {
        return subscriptionRepository.findAll(depth);
    }

    @Override
    public Iterable<Subscription> findAll(Sort sort) {
        return subscriptionRepository.findAll(sort);
    }

    @Override
    public Iterable<Subscription> findAll(Sort sort, int depth) {
        return subscriptionRepository.findAll(sort, depth);
    }

    @Override
    public Iterable<Subscription> findAllById(Iterable<String> ids) {
        return subscriptionRepository.findAllById(ids);
    }

    @Override
    public Iterable<Subscription> findAllById(Iterable<String> ids, int depth) {
        return subscriptionRepository.findAllById(ids, depth);
    }

    @Override
    public Iterable<Subscription> findAllById(Iterable<String> ids, Sort sort) {
        return null;
    }

    @Override
    public Iterable<Subscription> findAllById(Iterable<String> ids, Sort sort, int depth) {
        return null;
    }

    @Override
    public Page<Subscription> findAll(Pageable pageable) {
        return null;
    }

    @Override
    public Page<Subscription> findAll(Pageable pageable, int depth) {
        return null;
    }

    @Override
    public long count() {
        return subscriptionRepository.count();
    }

    @Override
    public void deleteById(String id) {
        subscriptionRepository.deleteById(id);
    }

    @Override
    public void delete(Subscription entity) {
        subscriptionRepository.delete(entity);
    }

    @Override
    public void deleteAll(Iterable<? extends Subscription> entities) {
        subscriptionRepository.deleteAll(entities);
    }

    @Override
    public void deleteAll() {
        subscriptionRepository.deleteAll();
    }

    // ✅ Extended Repository (multi-context)
    @Override
    public <S extends Subscription> S save(S s, String s1) {
        return null;
    }

    @Override
    public <S extends Subscription> Iterable<S> saveAll(Iterable<S> iterable, String s) {
        return null;
    }

    @Override
    public Optional<Subscription> findById(String s, String s2) {
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
    public void deleteById(String s, String s2) { }

    @Override
    public void delete(Subscription subscription, String s) { }

    @Override
    public void deleteAll(Iterable<? extends Subscription> iterable, String s) { }

    @Override
    public void deleteAll(String s) { }

    @Override
    public <S extends Subscription> S save(S s, int i, String s1) {
        return null;
    }

    @Override
    public <S extends Subscription> Iterable<S> save(Iterable<S> iterable, int i, String s) {
        return null;
    }

    @Override
    public Optional<Subscription> findById(String s, int i, String s2) {
        return Optional.empty();
    }

    @Override
    public Iterable<Subscription> findAll(String discoveredName) {
        Iterable<Subscription> allSubscriptions = subscriptionRepository.findAll();
        return StreamSupport.stream(allSubscriptions.spliterator(), false)
                .filter(s -> discoveredName.equals(s.getDiscoveredName()))
                .collect(Collectors.toList());
    }

    @Override
    public Iterable<Subscription> findAll(int i, String s) {
        return null;
    }

    @Override
    public Iterable<Subscription> findAllById(Iterable<String> iterable, String s) {
        return null;
    }

    @Override
    public Iterable<Subscription> findAllById(Iterable<String> iterable, int i, String s) {
        return null;
    }

    @Override
    public Iterable<Subscription> findAll(Sort sort, String s) {
        return null;
    }

    @Override
    public Iterable<Subscription> findAll(Sort sort, int i, String s) {
        return null;
    }

    @Override
    public Iterable<Subscription> findAllById(Iterable<String> iterable, Sort sort, String s) {
        return null;
    }

    @Override
    public Iterable<Subscription> findAllById(Iterable<String> iterable, Sort sort, int i, String s) {
        return null;
    }

    @Override
    public Page<Subscription> findAll(Pageable pageable, String s) {
        return null;
    }

    @Override
    public Page<Subscription> findAll(Pageable pageable, int i, String s) {
        return null;
    }

    // ✅ Neo4j-specific implementations
    @Override
    public Optional<Subscription> uivFindByGdn(String s) {
        return Optional.empty();
    }

    @Override
    public Optional<Subscription> uivFindByGdn(String s, int i) {
        return Optional.empty();
    }

    @Override
    public Optional<Subscription> uivFindByGdn(String s, String s1) {
        return Optional.empty();
    }

    @Override
    public Optional<Subscription> uivFindByGdn(String s, int i, String s1) {
        return Optional.empty();
    }

    @Override
    public boolean uivFindById(String s) {
        return false;
    }

    @Override
    public boolean uivFindById(String s, String s1, String s2, boolean b, String s3) {
        return false;
    }

    @Override
    public void flushSession() {
        subscriptionRepository.flushSession();
    }

    @Override
    public void uivUpdateAssociationProperties(Neo4jDomainObject from, Neo4jDomainObject to, String rel, Map<String, Object> props) { }

    @Override
    public void updateLdn(String s, String s2, String s1) { }

    @Override
    public void updateLdn(String s, String s2, String s1, String s3) { }

    @Override
    public List<String> uivFindRelationExists(String s1, String s2, String s3, String s4, String s5, String s6) {
        return List.of();
    }

    @Override
    public void refactor(Object o, String s, String s2, String s1, boolean b) { }

    @Override
    public void refactor(Object o, String s, String s2, String s1, String s3, boolean b) { }

    @Override
    public Subscription uivFindByTwoEndNode(Map<String, String> map1, Map<String, String> map2, String s1, String s2) {
        return null;
    }
}
