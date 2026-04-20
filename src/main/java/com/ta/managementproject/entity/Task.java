package com.ta.managementproject.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.CascadeType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;
import org.hibernate.annotations.*;

import com.fasterxml.jackson.annotation.JsonBackReference;

import java.time.Instant;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Table(name = "task",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"stage_id", "task_order"})
})
@Entity
@SQLDelete(sql = "UPDATE task SET is_deleted = true WHERE task_id = ?")
@SQLRestriction("is_deleted IS false")
@Builder(toBuilder = true)
public class Task {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "task_id", updatable = false, nullable = false)
    private String taskId;

    @Column(name = "task_name", nullable = false)
    private String taskName;

    @Column(name = "description")
    private String description;

    @Column(name = "priority", nullable = false)
    private Integer priority;

    @Column(name = "task_order", nullable = false)
    private Integer order;

    @Column(name = "label")
    private String label;

    @Column(name = "due_date", nullable = false)
    private Instant dueDate;

    @Column(name = "status", nullable = false)
    private String status;

    @ManyToOne
    @JoinColumn(name = "project_member")
    @JsonBackReference
    private ProjectMember projectMember;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted;

    @Column(name = "created_at")
    @CreationTimestamp
    private Instant createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private Instant updatedAt;

    @ManyToOne
    @JoinColumn(name = "stage", nullable = false)
    @JsonBackReference
    private Stage stage;

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<SubTask> subTaskList = new ArrayList<>();
}
