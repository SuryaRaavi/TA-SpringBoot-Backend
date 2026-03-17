package com.ta.managementproject.repository;

import com.ta.managementproject.dto.response.ProjectResponseDTO;
import com.ta.managementproject.dto.response.UsersInProjectResponseDTO;
import com.ta.managementproject.entity.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface ProjectDb extends JpaRepository<Project, String> {
    @Query(value = "SELECT COUNT(p) FROM PROYEK p", nativeQuery = true)
    Long getTotalProject();
//
//    @Query("""
//            SELECT new com.project.safetypatrol.dto.response.AllProyekResponseDTO(
//                p.kodeProyek,
//                p.penyelenggara,
//                p.namaProyek,
//                p.lokasi
//            )
//            FROM Proyek p
//            WHERE p.isDeleted = false
//            """
//    )
//    Page<ProjectResponseDTO> findAllProyek(
//            Pageable pageable
//    );

    @Query("""
            SELECT new com.ta.managementproject.dto.response.ProjectResponseDTO(
                p.projectId,
                p.projectName,
                p.status
            )
            FROM Project p
            WHERE p.projectManager.username = :username
            """
    )
    Page<ProjectResponseDTO> findAllByProjectManager(
            @Param("username") String username,
            Pageable pageable
    );

    @Query("""
                 SELECT new com.ta.managementproject.dto.response.ProjectResponseDTO(
                    p.projectId,
                    p.projectName,
                    p.status
                 )
                 FROM Project p
                 WHERE p.projectManager.username = :username
                   AND p.startDate >= :startDate
                   AND p.endDate <= :endDate
            """)
    Page<ProjectResponseDTO> findAllByProjectManagerAndStartEndDate(
            @Param("username") String username,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable
    );

//    @Query("""
//                 SELECT new com.project.safetypatrol.dto.response.AllProyekResponseDTO(
//                     p.kodeProyek,
//                     p.penyelenggara,
//                     p.namaProyek,
//                     p.lokasi
//                 )
//                 FROM Proyek p
//                 WHERE p.tanggalMulai >= :tanggalMulai
//                   AND p.tanggalSelesai <= :tanggalSelesai
//                   AND p.isDeleted = false
//            """)
//    Page<ProjectResponseDTO> findAllByStartEndDate(
//            @Param("tanggalMulai") LocalDate tanggalMulai,
//            @Param("tanggalSelesai") LocalDate tanggalSelesai,
//            Pageable pageable
//    );

    Project findByProjectId(String projectId);

    @Query("""
             SELECT new com.ta.managementproject.dto.response.ProjectResponseDTO(
                p.projectId,
                p.projectName,
                p.status
             )
             FROM Project p
             WHERE p.projectManager.username = :username
               AND (LOWER(p.projectName) LIKE LOWER(CONCAT('%', :parameter, '%'))
               OR LOWER(p.projectId) LIKE LOWER(CONCAT('%', :parameter, '%')))
    """)
    Page<ProjectResponseDTO> findPMProjectByProjectNameOrProjectId(
            @Param("username") String username,
            @Param("parameter") String parameter,
            Pageable pageable
    );
//
//    @Query("""
//             SELECT new com.project.safetypatrol.dto.response.AllProyekResponseDTO(
//                 p.kodeProyek,
//                 p.penyelenggara,
//                 p.namaProyek,
//                 p.lokasi
//             )
//             FROM Proyek p
//             WHERE (LOWER(p.namaProyek) LIKE LOWER(CONCAT('%', :parameter, '%'))
//               OR LOWER(p.kodeProyek) LIKE LOWER(CONCAT('%', :parameter, '%')))
//               AND p.isDeleted = false
//    """)
//    Page<ProjectResponseDTO> findProyekByNamaOrKodeProyek(@Param("parameter") String parameter, Pageable pageable);
//
    @Query("""
            SELECT new com.ta.managementproject.dto.response.UsersInProjectResponseDTO(
                   p.projectManager.username,
                   p.projectManager.fullName,
                   p.projectManager.role.name
              ) FROM Project p
                WHERE p.projectId = :projectId
    """)
    Page<UsersInProjectResponseDTO> findProjectPM(
            @Param("projectId") String projectId,
            Pageable pageable
    );

    @Query("""
            SELECT p
                FROM Project p
                WHERE p.joinCode = :joinCode
                AND p.joinCodeExpiredAt >= CURRENT_TIMESTAMP
            """
    )
    Project findProjectByJoinCode(@Param("joinCode") String joinCode);
}
