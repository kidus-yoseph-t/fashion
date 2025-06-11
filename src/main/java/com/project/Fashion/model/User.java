package com.project.Fashion.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.util.List;

@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_user_role", columnList = "role")
        // email is likely already indexed due to unique constraint
})
@Data
@AllArgsConstructor
@NoArgsConstructor
public class User {
    @Id
    @UuidGenerator
    @Column(unique = true, updatable = false)
    private String id;

    @Column(name = "firstname", nullable = false)
    private String firstName;

    @Column(name = "lastname", nullable = false)
    private String lastName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String role;

    @OneToMany(mappedBy = "user")   // No cascade
    @JsonManagedReference("user-orders")
    private List<Order> orders;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    @JsonManagedReference("user-reviews")
    private List<Review> reviews;

    @OneToMany(mappedBy = "sender", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Message> messages;

    @OneToMany(mappedBy = "user1")
    @JsonIgnore
    private List<Conversation> conversationsStarted;

    @OneToMany(mappedBy = "user2")
    @JsonIgnore
    private List<Conversation> conversationsReceived;

}