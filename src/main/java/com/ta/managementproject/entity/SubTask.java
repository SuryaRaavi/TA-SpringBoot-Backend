package com.ta.managementproject.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.*;

import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Table(name = "sub_task")
@Entity
@SQLDelete(sql = "UPDATE sub_task SET is_deleted = true WHERE sub_task_id = ?")
@SQLRestriction("is_deleted IS false")
@Builder(toBuilder = true)
public class SubTask {
    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "sub_task_id", updatable = false, nullable = false)
    private String subTaskId;

    @Column(name = "sub_task_name", nullable = false)
    private String subTaskName;

    @Column(name = "description")
    private String description;

    @Column(name = "due_date", nullable = false)
    private Instant dueDate;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "label")
    private String label;

    @Column(name = "sub_task_order", unique = true)
    private Integer order;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted;

    @ManyToOne
    @JoinColumn(name = "project_member")
    @JsonBackReference
    private ProjectMember projectMember;

    @ManyToOne
    @JoinColumn(name = "task", nullable = false)
    @JsonBackReference
    private Task task;

    @Column(name = "created_at")
    @CreationTimestamp
    private Instant createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private Instant updatedAt;
}
