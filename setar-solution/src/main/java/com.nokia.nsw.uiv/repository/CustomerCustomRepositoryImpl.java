package com.nokia.nsw.uiv.repository;

import com.nokia.nsw.uiv.datatype.Neo4jDomainObject;
import com.nokia.nsw.uiv.model.common.party.Customer;
import com.nokia.nsw.uiv.model.common.party.CustomerRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.stream.StreamSupport;

@Repository
@Primary
public class CustomerCustomRepositoryImpl implements CustomerCustomRepository {

    private final CustomerRepository customerRepository;

    public CustomerCustomRepositoryImpl(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    public Customer findByDiscoveredName(String discoveredName) {
        Iterable<Customer> allCustomers = customerRepository.findAll();
        return StreamSupport.stream(allCustomers.spliterator(), false)
                .filter(c -> discoveredName.equals(c.getDiscoveredName()))
                .findFirst()
                .orElse(null);
    }

    @Override
    public Customer findByProperty(String key, String value) {
        Iterable<Customer> allCustomers = customerRepository.findAll();
        return StreamSupport.stream(allCustomers.spliterator(), false)
                .filter(c -> c.getProperties().get(key)!=null?c.getProperties().get(key).equals(value):false)
                .findFirst()
                .orElse(null);
    }

    // Delegate all standard CRUD methods
    @Override
    public <S extends Customer> S save(S entity) {
        return customerRepository.save(entity);
    }

    @Override
    public <S extends Customer> S save(S entity, int depth) {
        return customerRepository.save(entity, depth);
    }

    @Override
    public <S extends Customer> Iterable<S> saveAll(Iterable<S> entities) {
        return customerRepository.saveAll(entities);
    }

    @Override
    public <S extends Customer> Iterable<S> save(Iterable<S> entities, int depth) {
        return customerRepository.save(entities, depth);
    }

    @Override
    public Optional<Customer> findById(String id) {
        return customerRepository.findById(id);
    }

    @Override
    public Optional<Customer> findById(String id, int depth) {
        return customerRepository.findById(id, depth);
    }

    @Override
    public boolean existsById(String id) {
        return customerRepository.existsById(id);
    }

    @Override
    public Iterable<Customer> findAll() {
        return customerRepository.findAll();
    }

    @Override
    public Iterable<Customer> findAll(int depth) {
        return customerRepository.findAll(depth);
    }

    @Override
    public Iterable<Customer> findAll(Sort sort) {
        return customerRepository.findAll(sort);
    }

    @Override
    public Iterable<Customer> findAll(Sort sort, int depth) {
        return customerRepository.findAll(sort, depth);
    }

    @Override
    public Iterable<Customer> findAllById(Iterable<String> ids) {
        return customerRepository.findAllById(ids);
    }

    @Override
    public Iterable<Customer> findAllById(Iterable<String> ids, int depth) {
        return customerRepository.findAllById(ids, depth);
    }

    @Override
    public Iterable<Customer> findAllById(Iterable<String> strings, Sort sort) {
        return null;
    }

    @Override
    public Iterable<Customer> findAllById(Iterable<String> strings, Sort sort, int depth) {
        return null;
    }

    @Override
    public Page<Customer> findAll(Pageable pageable) {
        return null;
    }

    @Override
    public Page<Customer> findAll(Pageable pageable, int depth) {
        return null;
    }

    @Override
    public long count() {
        return customerRepository.count();
    }

    @Override
    public void deleteById(String id) {
        customerRepository.deleteById(id);
    }

    @Override
    public void delete(Customer entity) {
        customerRepository.delete(entity);
    }


    @Override
    public void deleteAll(Iterable<? extends Customer> entities) {
        customerRepository.deleteAll(entities);
    }

    @Override
    public void deleteAll() {
        customerRepository.deleteAll();
    }


    @Override
    public <S extends Customer> S save(S s, String s1) {
        return null;
    }

    @Override
    public <S extends Customer> Iterable<S> saveAll(Iterable<S> iterable, String s) {
        return null;
    }

    @Override
    public Optional<Customer> findById(String s, String s2) {
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
    public void delete(Customer customer, String s) {

    }

    @Override
    public void deleteAll(Iterable<? extends Customer> iterable, String s) {

    }

    @Override
    public void deleteAll(String s) {

    }

    @Override
    public <S extends Customer> S save(S s, int i, String s1) {
        return null;
    }

    @Override
    public <S extends Customer> Iterable<S> save(Iterable<S> iterable, int i, String s) {
        return null;
    }

    @Override
    public Optional<Customer> findById(String s, int i, String s2) {
        return Optional.empty();
    }

    @Override
    public Iterable<Customer> findAll(String s) {
        return null;
    }

    @Override
    public Iterable<Customer> findAll(int i, String s) {
        return null;
    }

    @Override
    public Iterable<Customer> findAllById(Iterable<String> iterable, String s) {
        return null;
    }

    @Override
    public Iterable<Customer> findAllById(Iterable<String> iterable, int i, String s) {
        return null;
    }

    @Override
    public Iterable<Customer> findAll(Sort sort, String s) {
        return null;
    }

    @Override
    public Iterable<Customer> findAll(Sort sort, int i, String s) {
        return null;
    }

    @Override
    public Iterable<Customer> findAllById(Iterable<String> iterable, Sort sort, String s) {
        return null;
    }

    @Override
    public Iterable<Customer> findAllById(Iterable<String> iterable, Sort sort, int i, String s) {
        return null;
    }

    @Override
    public Page<Customer> findAll(Pageable pageable, String s) {
        return null;
    }

    @Override
    public Page<Customer> findAll(Pageable pageable, int i, String s) {
        return null;
    }

    @Override
    public Optional<Customer> uivFindByGdn(String s, String s1) {
        return Optional.empty();
    }

    @Override
    public Optional<Customer> uivFindByGdn(String s, int i, String s1) {
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
        customerRepository.flushSession();
    }

    // Leave unsupported methods empty only if really not needed
    @Override
    public Optional<Customer> uivFindByGdn(String s) { return Optional.empty(); }

    @Override
    public Optional<Customer> uivFindByGdn(String s, int i) { return Optional.empty(); }

    @Override
    public void uivUpdateAssociationProperties(Neo4jDomainObject o1, Neo4jDomainObject o2, String s, Map<String, Object> map) { }

    @Override
    public void updateLdn(String s, String s2, String s1) { }

    @Override
    public void updateLdn(String s, String s2, String s1, String s3) { }

    @Override
    public List<String> uivFindRelationExists(String s1, String s2, String s3, String s4, String s5, String s6) { return List.of(); }

    @Override
    public void refactor(Object o, String s, String s2, String s1, boolean b) { }

    @Override
    public void refactor(Object o, String s, String s2, String s1, String s3, boolean b) { }

    @Override
    public Customer uivFindByTwoEndNode(Map<String, String> map1, Map<String, String> map2, String s1, String s2) { return null; }

}
