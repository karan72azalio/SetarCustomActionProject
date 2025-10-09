package com.nokia.nsw.uiv.repository;

import com.nokia.nsw.uiv.datatype.Neo4jDomainObject;
import com.nokia.nsw.uiv.model.service.Subscription;
import com.nokia.nsw.uiv.model.service.SubscriptionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.StreamSupport;

public class SubscriptionCustomRepositoryImpl implements SubscriptionCustomRepository{

    private final SubscriptionRepository subscriptionRepository;

    public SubscriptionCustomRepositoryImpl(SubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
    }


    @Override
    public Subscription findByDiscoveredName(String discoveredName) {
        Iterable<Subscription> subscriptions = subscriptionRepository.findAll();
        return StreamSupport.stream(subscriptions.spliterator(), false)
                .filter(sub -> discoveredName.equals(sub.getDiscoveredName()))
                .findFirst()
                .orElse(null);
    }


    @Override
    public Subscription findByProperty(String key, String value) {
        Iterable<Subscription> subscriptions = subscriptionRepository.findAll();
        return StreamSupport.stream(subscriptions.spliterator(), false)
                .filter(sub -> sub.getCustomer() != null
                        && sub.getCustomer().getProperties() != null
                        && value.equals(sub.getCustomer().getProperties().get(key)))
                .findFirst()
                .orElse(null);
    }


    @Override
    public Optional<Subscription> uivFindByGdn(String s) {
        return Optional.empty();
    }

    @Override
    public Optional<Subscription> uivFindByGdn(String s, int i) {
        return Optional.empty();
    }

    @Override
    public void uivUpdateAssociationProperties(Neo4jDomainObject neo4jDomainObject, Neo4jDomainObject neo4jDomainObject1, String s, Map<String, Object> map) {

    }

    @Override
    public void updateLdn(String s, String s2, String s1) {

    }

    @Override
    public void flushSession() {

    }

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
    public void deleteById(String s, String s2) {

    }

    @Override
    public void delete(Subscription subscription, String s) {

    }

    @Override
    public void deleteAll(Iterable<? extends Subscription> iterable, String s) {

    }

    @Override
    public void deleteAll(String s) {

    }

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
    public Iterable<Subscription> findAll(String s) {
        return null;
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
    public void updateLdn(String s, String s2, String s1, String s3) {

    }

    @Override
    public List<String> uivFindRelationExists(String s, String s1, String s2, String s3, String s4, String s5) {
        return List.of();
    }

    @Override
    public void refactor(Object o, String s, String s2, String s1, boolean b) {

    }

    @Override
    public void refactor(Object o, String s, String s2, String s1, String s3, boolean b) {

    }

    @Override
    public Subscription uivFindByTwoEndNode(Map<String, String> map, Map<String, String> map1, String s, String s1) {
        return null;
    }

    @Override
    public <S extends Subscription> S save(S s, int depth) {
        return null;
    }

    @Override
    public <S extends Subscription> Iterable<S> save(Iterable<S> entities, int depth) {
        return null;
    }

    @Override
    public Optional<Subscription> findById(String s, int depth) {
        return Optional.empty();
    }

    @Override
    public <S extends Subscription> S save(S entity) {
        return null;
    }

    @Override
    public <S extends Subscription> Iterable<S> saveAll(Iterable<S> entities) {
        return null;
    }

    @Override
    public Optional<Subscription> findById(String s) {
        return Optional.empty();
    }

    @Override
    public boolean existsById(String s) {
        return false;
    }

    @Override
    public Iterable<Subscription> findAll() {
        return null;
    }

    @Override
    public Iterable<Subscription> findAll(int depth) {
        return null;
    }

    @Override
    public Iterable<Subscription> findAll(Sort sort) {
        return null;
    }

    @Override
    public Iterable<Subscription> findAll(Sort sort, int depth) {
        return null;
    }

    @Override
    public Iterable<Subscription> findAllById(Iterable<String> strings) {
        return null;
    }

    @Override
    public long count() {
        return 0;
    }

    @Override
    public void deleteById(String s) {

    }

    @Override
    public void delete(Subscription entity) {

    }

    @Override
    public void deleteAll(Iterable<? extends Subscription> entities) {

    }

    @Override
    public void deleteAll() {

    }

    @Override
    public Iterable<Subscription> findAllById(Iterable<String> strings, int depth) {
        return null;
    }

    @Override
    public Iterable<Subscription> findAllById(Iterable<String> strings, Sort sort) {
        return null;
    }

    @Override
    public Iterable<Subscription> findAllById(Iterable<String> strings, Sort sort, int depth) {
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
}
