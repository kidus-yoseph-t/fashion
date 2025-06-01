package com.project.Fashion.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.util.List;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "users")
public class User {
    @Id
    @UuidGenerator
    @Column(unique = true, updatable = false)
    private String id;

    @Column(name = "firstName", nullable = false)
    private String firstName;

    @Column(name = "lastName", nullable = false)
    private String lastName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String role;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    @JsonManagedReference
    private List<Order> orders;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Review> reviews;

    @OneToMany(mappedBy = "sender", cascade = CascadeType.ALL)
    private List<Message> messages;

    @OneToMany(mappedBy = "user1")
    private List<Conversation> conversationsStarted;

    @OneToMany(mappedBy = "user2")
    private List<Conversation> conversationsReceived;

}

