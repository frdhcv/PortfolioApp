package com.example.portfolio.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "users")
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;

    @Column(name = "profile_image_url")
    private String profileImageUrl;


    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    @JsonIgnore // Prevents user from exposing portfolio in response unless needed
    private PortfolioEntity portfolio;

    @ManyToMany
    @JoinTable(name = "user_following", joinColumns = @JoinColumn(name = "follower_id"), inverseJoinColumns = @JoinColumn(name = "followed_id"))
    @JsonIgnore
    private List<UserEntity> following = new ArrayList<>();

    @ManyToMany(mappedBy = "following")
    @JsonIgnore
    private List<UserEntity> followers = new ArrayList<>();

    @ManyToMany
    @JoinTable(name = "users_liked_projects", joinColumns = @JoinColumn(name = "user_entity_id"), inverseJoinColumns = @JoinColumn(name = "liked_projects_id"))
    private List<ProjectEntity> likedProjects = new ArrayList<>();

    @ManyToMany
    @JoinTable(name = "users_saved_projects", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "project_id"))
    private List<ProjectEntity> savedProjects = new ArrayList<>();
}
