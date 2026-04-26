package com.ta.managementproject.measure_test;

import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Predicate;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.ta.managementproject.dto.response.TaskResponseDTO;
import com.ta.managementproject.exception.BadRequestException;
import com.ta.managementproject.repository.TaskDbWithDsl;
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

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskDbWithDslTest {

    @Mock
    private JPAQueryFactory queryFactory;

    @InjectMocks
    private TaskDbWithDsl taskDbWithDsl;

    @Mock
    private JPAQuery<TaskResponseDTO> selectQuery;

    @Mock
    private JPAQuery<Long> countQuery;

    private TaskResponseDTO sampleDto;
    private static final String STAGE_ID = "stage-001";

    @BeforeEach
    void setUp() {
        sampleDto = new TaskResponseDTO(
                UUID.randomUUID().toString(),
                "Task Alpha",
                1,
                Instant.now(),
                "TODO",
                "John Doe",
                "bug",
                Instant.now(),
                Instant.now(),
                1
        );
    }

    // ─── Helper: stub both select (data) and count chains ───────────────────────

    @SuppressWarnings("unchecked")
    private void stubFullChain(List<TaskResponseDTO> results, Long total) {
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
    // findAll — only required filter (stageId)
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    void findAll_withOnlyStageId_shouldReturnPageWithResults() {
        Pageable pageable = PageRequest.of(0, 10);
        stubFullChain(List.of(sampleDto), 1L);

        Page<TaskResponseDTO> result = taskDbWithDsl.findAll(
                STAGE_ID, null, null, null, null, null, null, pageable
        );

        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1L);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTaskName()).isEqualTo("Task Alpha");
    }

    // ════════════════════════════════════════════════════════════════════════════
    // findAll — empty result set
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    void findAll_whenNoResults_shouldReturnEmptyPage() {
        Pageable pageable = PageRequest.of(0, 10);
        stubFullChain(List.of(), 0L);

        Page<TaskResponseDTO> result = taskDbWithDsl.findAll(
                STAGE_ID, null, null, null, null, null, null, pageable
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

        Page<TaskResponseDTO> result = taskDbWithDsl.findAll(
                STAGE_ID, null, null, null, null, null, null, pageable
        );

        assertThat(result.getTotalElements()).isZero();
    }

    // ════════════════════════════════════════════════════════════════════════════
    // findAll — with all optional filters
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    void findAll_withAllOptionalFilters_shouldReturnFilteredPage() {
        Pageable pageable = PageRequest.of(0, 10);
        LocalDate today = LocalDate.now();
        stubFullChain(List.of(sampleDto), 1L);

        Page<TaskResponseDTO> result = taskDbWithDsl.findAll(
                STAGE_ID,
                today,
                today.minusDays(7),
                today,
                1,
                1,
                "Alpha",
                pageable
        );

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1L);
    }

    // ════════════════════════════════════════════════════════════════════════════
    // findAll — filter with dueDate only
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    void findAll_withDueDateFilter_shouldReturnResults() {
        Pageable pageable = PageRequest.of(0, 10);
        stubFullChain(List.of(sampleDto), 1L);

        Page<TaskResponseDTO> result = taskDbWithDsl.findAll(
                STAGE_ID, LocalDate.now(), null, null, null, null, null, pageable
        );

        assertThat(result.getContent()).hasSize(1);
    }

    // ════════════════════════════════════════════════════════════════════════════
    // findAll — filter with priority only
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    void findAll_withPriorityFilter_shouldReturnResults() {
        Pageable pageable = PageRequest.of(0, 10);
        stubFullChain(List.of(sampleDto), 1L);

        Page<TaskResponseDTO> result = taskDbWithDsl.findAll(
                STAGE_ID, null, null, null, 1, null, null, pageable
        );

        assertThat(result.getContent()).hasSize(1);
    }

    // ════════════════════════════════════════════════════════════════════════════
    // findAll — filter with order only
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    void findAll_withOrderFilter_shouldReturnResults() {
        Pageable pageable = PageRequest.of(0, 10);
        stubFullChain(List.of(sampleDto), 1L);

        Page<TaskResponseDTO> result = taskDbWithDsl.findAll(
                STAGE_ID, null, null, null, null, 1, null, pageable
        );

        assertThat(result.getContent()).hasSize(1);
    }

    // ════════════════════════════════════════════════════════════════════════════
    // findAll — filter with keyword only
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    void findAll_withKeywordFilter_shouldReturnResults() {
        Pageable pageable = PageRequest.of(0, 10);
        stubFullChain(List.of(sampleDto), 1L);

        Page<TaskResponseDTO> result = taskDbWithDsl.findAll(
                STAGE_ID, null, null, null, null, null, "Alpha", pageable
        );

        assertThat(result.getContent()).hasSize(1);
    }

    // ════════════════════════════════════════════════════════════════════════════
    // findAll — sorting by taskName ASC
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    void findAll_sortByTaskNameAsc_shouldNotThrow() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("taskName").ascending());
        stubFullChain(List.of(sampleDto), 1L);

        Page<TaskResponseDTO> result = taskDbWithDsl.findAll(
                STAGE_ID, null, null, null, null, null, null, pageable
        );

        assertThat(result).isNotNull();
    }

    // ════════════════════════════════════════════════════════════════════════════
    // findAll — sorting by order DESC
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    void findAll_sortByOrderDesc_shouldNotThrow() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("order").descending());
        stubFullChain(List.of(), 0L);

        Page<TaskResponseDTO> result = taskDbWithDsl.findAll(
                STAGE_ID, null, null, null, null, null, null, pageable
        );

        assertThat(result).isNotNull();
    }

    // ════════════════════════════════════════════════════════════════════════════
    // findAll — sorting by dueDate ASC
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    void findAll_sortByDueDateAsc_shouldNotThrow() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("dueDate").ascending());
        stubFullChain(List.of(), 0L);

        Page<TaskResponseDTO> result = taskDbWithDsl.findAll(
                STAGE_ID, null, null, null, null, null, null, pageable
        );

        assertThat(result).isNotNull();
    }

    // ════════════════════════════════════════════════════════════════════════════
    // findAll — sorting by priority DESC
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    void findAll_sortByPriorityDesc_shouldNotThrow() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("priority").descending());
        stubFullChain(List.of(), 0L);

        Page<TaskResponseDTO> result = taskDbWithDsl.findAll(
                STAGE_ID, null, null, null, null, null, null, pageable
        );

        assertThat(result).isNotNull();
    }

    // ════════════════════════════════════════════════════════════════════════════
    // findAll — sorting by createdAt DESC
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    void findAll_sortByCreatedAtDesc_shouldNotThrow() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());
        stubFullChain(List.of(), 0L);

        Page<TaskResponseDTO> result = taskDbWithDsl.findAll(
                STAGE_ID, null, null, null, null, null, null, pageable
        );

        assertThat(result).isNotNull();
    }

    // ════════════════════════════════════════════════════════════════════════════
    // findAll — sorting by updatedAt ASC
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    void findAll_sortByUpdatedAtAsc_shouldNotThrow() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("updatedAt").ascending());
        stubFullChain(List.of(), 0L);

        Page<TaskResponseDTO> result = taskDbWithDsl.findAll(
                STAGE_ID, null, null, null, null, null, null, pageable
        );

        assertThat(result).isNotNull();
    }

    // ════════════════════════════════════════════════════════════════════════════
    // findAll — invalid sort column throws BadRequestException
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    void findAll_withInvalidSortColumn_shouldThrowBadRequestException() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("invalidColumn").ascending());

        assertThatThrownBy(() -> taskDbWithDsl.findAll(
                STAGE_ID, null, null, null, null, null, null, pageable
        ))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Sorting column is not valid!");
    }

    // ════════════════════════════════════════════════════════════════════════════
    // findAll — default sorting when no sort provided (task.order.asc)
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    void findAll_withNoSort_shouldApplyDefaultOrderAscSorting() {
        Pageable pageable = PageRequest.of(0, 10); // no sort → default task.order.asc()
        stubFullChain(List.of(sampleDto), 1L);

        Page<TaskResponseDTO> result = taskDbWithDsl.findAll(
                STAGE_ID, null, null, null, null, null, null, pageable
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

        Page<TaskResponseDTO> result = taskDbWithDsl.findAll(
                STAGE_ID, null, null, null, null, null, null, pageable
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
        TaskResponseDTO dto2 = new TaskResponseDTO(
                UUID.randomUUID().toString(), "Task Beta", 2, Instant.now(),
                "IN_PROGRESS", "Jane Doe", "feature",
                Instant.now(), Instant.now(), 2
        );
        Pageable pageable = PageRequest.of(0, 10);
        stubFullChain(List.of(sampleDto, dto2), 2L);

        Page<TaskResponseDTO> result = taskDbWithDsl.findAll(
                STAGE_ID, null, null, null, null, null, null, pageable
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

        taskDbWithDsl.findAll(
                STAGE_ID, null, null, null, null, null, null, pageable
        );

        verify(queryFactory, times(2)).select(any(Expression.class));
    }
}