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
@Table(name = "project")
@Entity
@SQLDelete(sql = "UPDATE project SET is_deleted = true WHERE project_id = ?")
@SQLRestriction("is_deleted IS false")
@Builder(toBuilder = true)
public class Project {
    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "project_id", updatable = false, nullable = false)
    private String projectId;

    @Column(name = "project_name", nullable = false)
    private String projectName;

    @Column(name = "description")
    private String description;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "start_date")
    private Instant startDate;

    @Column(name = "end_date")
    private Instant endDate;

    @ManyToOne
    @JoinColumn(name = "project_manager", nullable = false)
    @JsonBackReference
    private ProjectManager projectManager;

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<Stage> stageList = new ArrayList<>();

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted;

    @Column(name = "created_at")
    @CreationTimestamp
    private Instant createdAt;

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<MemberInProject> memberInProjectList = new ArrayList<>();

    @Column(unique = true, name = "join_code")
    private String joinCode;

    @Column(name = "jc_expired_at")
    private Instant joinCodeExpiredAt;
}
