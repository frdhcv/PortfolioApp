package com.example.portfolio.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
public class ProjectEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;

    private String title;
    private String description;
    private String githubLink;

    @Column(name = "image_url")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String imageUrl;

    @Column(name = "video_url")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String videoUrl;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private int likes;

    @Version
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Integer version;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name = "portfolio_id")
    @JsonIgnore
    private PortfolioEntity portfolio;

    @ElementCollection
    @CollectionTable(name = "project_comments", joinColumns = @JoinColumn(name = "project_id"))
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private List<ProjectComment> comments = new ArrayList<>();

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public String getCreatedByUsername() {
        if (portfolio != null && portfolio.getUser() != null) {
            return portfolio.getUser().getUsername();
        }
        return null;
    }
}
