package com.nokia.nsw.uiv.repository;


import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class SubscriberType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;
}

