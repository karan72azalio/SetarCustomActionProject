package com.nokia.nsw.uiv.repository;


import lombok.Data;


import javax.persistence.*;

@Entity
@Data
public class SubscriberStatus {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;
}

