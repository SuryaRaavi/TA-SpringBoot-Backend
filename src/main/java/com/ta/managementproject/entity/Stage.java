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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Table(name = "stage")
@Entity
@SQLDelete(sql = "UPDATE stage SET is_deleted = true WHERE stage_id = ?")
@SQLRestriction("is_deleted IS false")
@Builder(toBuilder = true)
public class Stage {
    @Id
    @Column(name = "stage_id")
    private String stageId;

    @Column(name = "stage_name", nullable = false)
    private String stageName;

    @Column(name = "description")
    private String description;

    @Column(name = "stage_order", nullable = false)
    private Integer order;

    @Column(name = "created_at", nullable = false)
    @CreationTimestamp
    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime createdAt;

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
