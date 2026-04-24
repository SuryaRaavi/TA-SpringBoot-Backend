package com.ta.managementproject.test;

import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Predicate;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.ta.managementproject.dto.response.ProjectResponseDTO;
import com.ta.managementproject.exception.BadRequestException;
import com.ta.managementproject.repository.ProjectDbWithDsl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ProjectDbWithDslTest {
    @Mock
    private JPAQueryFactory queryFactory;

    @InjectMocks
    private ProjectDbWithDsl projectDbWithDsl;

    // Reusable JPAQuery mocks
    @Mock
    private JPAQuery<ProjectResponseDTO> selectQuery;

    @Mock
    private JPAQuery<Long> countQuery;

    private ProjectResponseDTO sampleDto;

    @BeforeEach
    void setUp() {
        sampleDto = new ProjectResponseDTO(
                UUID.randomUUID().toString(),
                "Project Alpha",
                "ACTIVE",
                java.time.Instant.now(),
                java.time.Instant.now()
        );
    }

    // ─── Helper: stub select chain for ProjectResponseDTO ───────────────────────

    @SuppressWarnings("unchecked")
    private void stubSelectChain(List<ProjectResponseDTO> returnedResults) {
        when(queryFactory.select(any(Expression.class))).thenReturn((JPAQuery) selectQuery);
        when(selectQuery.from(any(EntityPath.class))).thenReturn((JPAQuery) selectQuery);
        when(selectQuery.where(any(Predicate.class))).thenReturn((JPAQuery) selectQuery);
        when(selectQuery.orderBy(any(OrderSpecifier[].class))).thenReturn((JPAQuery) selectQuery);
        when(selectQuery.offset(anyLong())).thenReturn((JPAQuery) selectQuery);
        when(selectQuery.limit(anyLong())).thenReturn((JPAQuery) selectQuery);
        when(selectQuery.fetch()).thenReturn(returnedResults);
    }

    // ─── Helper: stub count chain ────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private void stubCountChain(Long count) {
        when(queryFactory.select(any(Expression.class)))
                .thenReturn((JPAQuery) selectQuery)   // first call → select query
                .thenReturn((JPAQuery) countQuery);   // second call → count query
        when(countQuery.from(any(EntityPath.class))).thenReturn((JPAQuery) countQuery);
        when(countQuery.where(any(Predicate.class))).thenReturn((JPAQuery) countQuery);
        when(countQuery.fetchOne()).thenReturn(count);
    }

    // ─── Full stub: both select & count chains ──────────────────────────────────

    @SuppressWarnings("unchecked")
    private void stubFullChain(List<ProjectResponseDTO> results, Long total) {
        // First select() call → data query
        // Second select() call → count query
        when(queryFactory.select(any(Expression.class)))
                .thenReturn((JPAQuery) selectQuery)
                .thenReturn((JPAQuery) countQuery);

        // Data query chain
        when(selectQuery.from(any(EntityPath.class))).thenReturn((JPAQuery) selectQuery);
        when(selectQuery.where(any(Predicate.class))).thenReturn((JPAQuery) selectQuery);
        when(selectQuery.orderBy(any(OrderSpecifier[].class))).thenReturn((JPAQuery) selectQuery);
        when(selectQuery.offset(anyLong())).thenReturn((JPAQuery) selectQuery);
        when(selectQuery.limit(anyLong())).thenReturn((JPAQuery) selectQuery);
        when(selectQuery.fetch()).thenReturn(results);

        // Count query chain
        when(countQuery.from(any(EntityPath.class))).thenReturn((JPAQuery) countQuery);
        when(countQuery.where(any(Predicate.class))).thenReturn((JPAQuery) countQuery);
        when(countQuery.fetchOne()).thenReturn(total);
    }

    // ════════════════════════════════════════════════════════════════════════════
    // findAll — filter by pmUsername
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    void findAll_withPmUsername_shouldReturnPageWithResults() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());
        stubFullChain(List.of(sampleDto), 1L);

        Page<ProjectResponseDTO> result = projectDbWithDsl.findAll(
                "pm_user", null, null, null, null, null, null, null, pageable
        );

        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1L);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getProjectName()).isEqualTo("Project Alpha");
    }

    // ════════════════════════════════════════════════════════════════════════════
    // findAll — filter by memberUsername
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    void findAll_withMemberUsername_shouldReturnPageWithResults() {
        Pageable pageable = PageRequest.of(0, 10);
        stubFullChain(List.of(sampleDto), 1L);

        Page<ProjectResponseDTO> result = projectDbWithDsl.findAll(
                null, "member_user", null, null, null, null, null, null, pageable
        );

        assertThat(result.getTotalElements()).isEqualTo(1L);
        assertThat(result.getContent()).hasSize(1);
    }

    // ════════════════════════════════════════════════════════════════════════════
    // findAll — empty result set
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    void findAll_whenNoResults_shouldReturnEmptyPage() {
        Pageable pageable = PageRequest.of(0, 10);
        stubFullChain(List.of(), 0L);

        Page<ProjectResponseDTO> result = projectDbWithDsl.findAll(
                "pm_user", null, null, null, null, null, null, null, pageable
        );

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    // ════════════════════════════════════════════════════════════════════════════
    // findAll — null count (fetchOne returns null)
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    void findAll_whenCountIsNull_shouldReturnTotalZero() {
        Pageable pageable = PageRequest.of(0, 10);
        stubFullChain(List.of(), null);

        Page<ProjectResponseDTO> result = projectDbWithDsl.findAll(
                "pm_user", null, null, null, null, null, null, null, pageable
        );

        assertThat(result.getTotalElements()).isZero();
    }

    // ════════════════════════════════════════════════════════════════════════════
    // findAll — with all optional filters
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    void findAll_withAllFilters_shouldReturnFilteredPage() {
        Pageable pageable = PageRequest.of(0, 10);
        LocalDate today = LocalDate.now();
        stubFullChain(List.of(sampleDto), 1L);

        Page<ProjectResponseDTO> result = projectDbWithDsl.findAll(
                "pm_user", null,
                today.minusDays(30),
                today,
                "ACTIVE",
                today.minusDays(7),
                today,
                "Alpha",
                pageable
        );

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1L);
    }

    // ════════════════════════════════════════════════════════════════════════════
    // findAll — sorting by projectName ASC
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    void findAll_sortByProjectNameAsc_shouldNotThrow() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("projectName").ascending());
        stubFullChain(List.of(sampleDto), 1L);

        Page<ProjectResponseDTO> result = projectDbWithDsl.findAll(
                "pm_user", null, null, null, null, null, null, null, pageable
        );

        assertThat(result).isNotNull();
    }

    // ════════════════════════════════════════════════════════════════════════════
    // findAll — sorting by startDate DESC
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    void findAll_sortByStartDateDesc_shouldNotThrow() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("startDate").descending());
        stubFullChain(List.of(), 0L);

        Page<ProjectResponseDTO> result = projectDbWithDsl.findAll(
                "pm_user", null, null, null, null, null, null, null, pageable
        );

        assertThat(result).isNotNull();
    }

    // ════════════════════════════════════════════════════════════════════════════
    // findAll — sorting by endDate ASC
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    void findAll_sortByEndDateAsc_shouldNotThrow() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("endDate").ascending());
        stubFullChain(List.of(), 0L);

        Page<ProjectResponseDTO> result = projectDbWithDsl.findAll(
                "pm_user", null, null, null, null, null, null, null, pageable
        );

        assertThat(result).isNotNull();
    }

    // ════════════════════════════════════════════════════════════════════════════
    // findAll — sorting by updatedAt DESC
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    void findAll_sortByUpdatedAtDesc_shouldNotThrow() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("updatedAt").descending());
        stubFullChain(List.of(), 0L);

        Page<ProjectResponseDTO> result = projectDbWithDsl.findAll(
                "pm_user", null, null, null, null, null, null, null, pageable
        );

        assertThat(result).isNotNull();
    }

    // ════════════════════════════════════════════════════════════════════════════
    // findAll — invalid sort column throws BadRequestException
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    void findAll_withInvalidSortColumn_shouldThrowBadRequestException() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("invalidColumn").ascending());

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> projectDbWithDsl.findAll(
                        "pm_user", null, null, null, null, null, null, null, pageable));

        assertEquals("Sorting column is not valid!", exception.getMessage());
    }

    // ════════════════════════════════════════════════════════════════════════════
    // findAll — default sorting when no sort provided (unsorted pageable)
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    void findAll_withNoSort_shouldApplyDefaultSorting() {
        Pageable pageable = PageRequest.of(0, 10); // no sort → defaults to createdAt DESC
        stubFullChain(List.of(sampleDto), 1L);

        Page<ProjectResponseDTO> result = projectDbWithDsl.findAll(
                "pm_user", null, null, null, null, null, null, null, pageable
        );

        assertThat(result.getContent()).isNotEmpty();
    }

    // ════════════════════════════════════════════════════════════════════════════
    // findAll — pagination metadata is correct
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    void findAll_shouldReturnCorrectPaginationMetadata() {
        Pageable pageable = PageRequest.of(1, 5); // page 2, size 5
        stubFullChain(List.of(sampleDto), 10L);

        Page<ProjectResponseDTO> result = projectDbWithDsl.findAll(
                "pm_user", null, null, null, null, null, null, null, pageable
        );

        assertThat(result.getNumber()).isEqualTo(1);
        assertThat(result.getSize()).isEqualTo(5);
        assertThat(result.getTotalElements()).isEqualTo(10L);
        assertThat(result.getTotalPages()).isEqualTo(2);
    }

    // ════════════════════════════════════════════════════════════════════════════
    // findAll — multiple results
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    void findAll_withMultipleResults_shouldReturnAllInPage() {
        ProjectResponseDTO dto2 = new ProjectResponseDTO(
                UUID.randomUUID().toString(), "Project Beta", "INACTIVE", java.time.Instant.now(), java.time.Instant.now()
        );
        Pageable pageable = PageRequest.of(0, 10);
        stubFullChain(List.of(sampleDto, dto2), 2L);

        Page<ProjectResponseDTO> result = projectDbWithDsl.findAll(
                "pm_user", null, null, null, null, null, null, null, pageable
        );

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2L);
    }

    // ════════════════════════════════════════════════════════════════════════════
    // findAll — queryFactory.select is called exactly twice (data + count)
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    @SuppressWarnings("unchecked")
    void findAll_shouldCallQueryFactorySelectTwice() {
        Pageable pageable = PageRequest.of(0, 10);
        stubFullChain(List.of(sampleDto), 1L);

        projectDbWithDsl.findAll(
                "pm_user", null, null, null, null, null, null, null, pageable
        );

        verify(queryFactory, times(2)).select(any(Expression.class));
    }
}
