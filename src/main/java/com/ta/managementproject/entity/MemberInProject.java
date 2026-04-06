package com.ta.managementproject.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "member_in_project")
@Builder(toBuilder = true)
public class MemberInProject {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "projectMember", nullable = false)
    @JsonBackReference
    private ProjectMember projectMember;

    @ManyToOne
    @JoinColumn(name = "project", nullable = false)
    @JsonBackReference
    private Project project;

    // created at untuk project
    @Column(name = "created_at")
    private Instant createdAt;
}
