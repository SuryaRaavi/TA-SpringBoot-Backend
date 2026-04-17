package com.ta.managementproject.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Table(name = "stage",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"project_id", "stage_order"})
        })
@Entity
@SQLDelete(sql = "UPDATE stage SET is_deleted = true WHERE stage_id = ?")
@SQLRestriction("is_deleted IS false")
@Builder(toBuilder = true)
public class Stage {
    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "stage_id", updatable = false, nullable = false)
    private String stageId;

    @Column(name = "stage_name", nullable = false)
    private String stageName;

    @Column(name = "description")
    private String description;

    @Column(name = "stage_order", nullable = false)
    private Integer order;

    @Column(name = "created_at", nullable = false)
    @CreationTimestamp
    private Instant createdAt;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted;

    @OneToMany(mappedBy = "stage", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<Task> taskList = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "project", nullable = false)
    @JsonBackReference
    private Project project;
}
