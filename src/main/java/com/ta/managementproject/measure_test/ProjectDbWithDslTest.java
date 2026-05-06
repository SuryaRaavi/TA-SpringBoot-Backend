package com.ta.managementproject.measure_test;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.OrderSpecifier;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectDbWithDslTest {

    @Mock
    private JPAQueryFactory queryFactory;

    @InjectMocks
    private ProjectDbWithDsl projectDbWithDsl;

    // Reusable JPAQuery mock chain
    @Mock private JPAQuery<ProjectResponseDTO> selectQuery;
    @Mock private JPAQuery<Long> countQuery;

    private List<ProjectResponseDTO> mockResults;

    @BeforeEach
    void setUp() {
        mockResults = List.of(
                new ProjectResponseDTO("p1", "Project A", "Desc A", "Manager A", null, null, null, null, false),
                new ProjectResponseDTO("p2", "Project B", "Desc B", "Manager B", null, null, null, null, false)
        );
    }

    // ===================== Helper: stub full query chain =====================

    @SuppressWarnings("unchecked")
    private void stubQueryChain(List<ProjectResponseDTO> results, Long total) {
        when(queryFactory.select(any(com.querydsl.core.types.Expression.class)))
                .thenReturn((JPAQuery) selectQuery)
                .thenReturn((JPAQuery) countQuery);

        when(selectQuery.from((EntityPath<?>) any())).thenReturn(selectQuery);
        when(selectQuery.where(any(BooleanBuilder.class))).thenReturn(selectQuery);
        when(selectQuery.orderBy(any(OrderSpecifier[].class))).thenReturn(selectQuery);
        when(selectQuery.offset(anyLong())).thenReturn(selectQuery);
        when(selectQuery.limit(anyLong())).thenReturn(selectQuery);
        when(selectQuery.fetch()).thenReturn(results);

        when(countQuery.from((EntityPath<?>) any())).thenReturn(countQuery);
        when(countQuery.where(any(BooleanBuilder.class))).thenReturn(countQuery);
        when(countQuery.fetchOne()).thenReturn(total);
    }

    // ===================== findAll — hasil dan paginasi =====================

    @Test
    void findAll_ShouldReturnPageWithResults_WhenDataExists() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt").ascending());
        stubQueryChain(mockResults, 2L);

        Page<ProjectResponseDTO> result = projectDbWithDsl.findAll(
                "user1", null, null, null, null, null, pageable);

        assertEquals(2, result.getContent().size());
    }

    @Test
    void findAll_ShouldReturnCorrectTotalElements_WhenDataExists() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt").ascending());
        stubQueryChain(mockResults, 2L);

        Page<ProjectResponseDTO> result = projectDbWithDsl.findAll(
                "user1", null, null, null, null, null, pageable);

        assertEquals(2L, result.getTotalElements());
    }

    @Test
    void findAll_ShouldReturnZeroTotal_WhenCountIsNull() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt").ascending());
        stubQueryChain(List.of(), null);

        Page<ProjectResponseDTO> result = projectDbWithDsl.findAll(
                "user1", null, null, null, null, null, pageable);

        assertEquals(0L, result.getTotalElements());
    }

    @Test
    void findAll_ShouldReturnEmptyPage_WhenNoDataExists() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt").ascending());
        stubQueryChain(List.of(), 0L);

        Page<ProjectResponseDTO> result = projectDbWithDsl.findAll(
                "user1", null, null, null, null, null, pageable);

        assertTrue(result.getContent().isEmpty());
    }

    // ===================== findAll — filter parameter =====================

    @Test
    void findAll_ShouldExecuteQuery_WhenStartDateProvided() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt").ascending());
        stubQueryChain(mockResults, 2L);

        Page<ProjectResponseDTO> result = projectDbWithDsl.findAll(
                "user1", LocalDate.of(2024, 1, 1), null, null, null, null, pageable);

        assertEquals(2, result.getContent().size());
    }

    @Test
    void findAll_ShouldExecuteQuery_WhenEndDateProvided() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt").ascending());
        stubQueryChain(mockResults, 2L);

        Page<ProjectResponseDTO> result = projectDbWithDsl.findAll(
                "user1", null, LocalDate.of(2024, 12, 31), null, null, null, pageable);

        assertEquals(2, result.getContent().size());
    }

    @Test
    void findAll_ShouldExecuteQuery_WhenCreatedAtProvided() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt").ascending());
        stubQueryChain(mockResults, 2L);

        Page<ProjectResponseDTO> result = projectDbWithDsl.findAll(
                "user1", null, null, LocalDate.of(2024, 6, 1), null, null, pageable);

        assertEquals(2, result.getContent().size());
    }

    @Test
    void findAll_ShouldExecuteQuery_WhenUpdatedAtProvided() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt").ascending());
        stubQueryChain(mockResults, 2L);

        Page<ProjectResponseDTO> result = projectDbWithDsl.findAll(
                "user1", null, null, null, LocalDate.of(2024, 6, 1), null, pageable);

        assertEquals(2, result.getContent().size());
    }

    @Test
    void findAll_ShouldExecuteQuery_WhenKeywordProvided() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt").ascending());
        stubQueryChain(mockResults, 2L);

        Page<ProjectResponseDTO> result = projectDbWithDsl.findAll(
                "user1", null, null, null, null, "Project", pageable);

        assertEquals(2, result.getContent().size());
    }

    @Test
    void findAll_ShouldExecuteQuery_WhenAllFiltersProvided() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("projectName").ascending());
        stubQueryChain(mockResults, 2L);

        Page<ProjectResponseDTO> result = projectDbWithDsl.findAll(
                "user1",
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 12, 31),
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 6, 1),
                "Alpha",
                pageable
        );

        assertEquals(2, result.getContent().size());
    }

    // ===================== getOrderSpecifiers — valid columns =====================

    @Test
    void findAll_ShouldNotThrow_WhenSortByProjectName() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("projectName").ascending());
        stubQueryChain(mockResults, 2L);

        assertDoesNotThrow(() -> projectDbWithDsl.findAll(
                "user1", null, null, null, null, null, pageable));
    }

    @Test
    void findAll_ShouldNotThrow_WhenSortByStartDate() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("startDate").descending());
        stubQueryChain(mockResults, 2L);

        assertDoesNotThrow(() -> projectDbWithDsl.findAll(
                "user1", null, null, null, null, null, pageable));
    }

    @Test
    void findAll_ShouldNotThrow_WhenSortByEndDate() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("endDate").ascending());
        stubQueryChain(mockResults, 2L);

        assertDoesNotThrow(() -> projectDbWithDsl.findAll(
                "user1", null, null, null, null, null, pageable));
    }

    @Test
    void findAll_ShouldNotThrow_WhenSortByCreatedAt() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());
        stubQueryChain(mockResults, 2L);

        assertDoesNotThrow(() -> projectDbWithDsl.findAll(
                "user1", null, null, null, null, null, pageable));
    }

    @Test
    void findAll_ShouldNotThrow_WhenSortByUpdatedAt() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("updatedAt").ascending());
        stubQueryChain(mockResults, 2L);

        assertDoesNotThrow(() -> projectDbWithDsl.findAll(
                "user1", null, null, null, null, null, pageable));
    }

    // ===================== getOrderSpecifiers — invalid column =====================

    @Test
    void findAll_ShouldThrowBadRequestException_WhenSortColumnInvalid() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("invalidColumn").ascending());

        assertThrows(BadRequestException.class, () -> projectDbWithDsl.findAll(
                "user1", null, null, null, null, null, pageable));
    }

    @Test
    void findAll_ShouldThrowBadRequestException_WithCorrectMessage_WhenSortColumnInvalid() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("hacked").ascending());

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> projectDbWithDsl.findAll("user1", null, null, null, null, null, pageable));

        assertEquals("Sorting column is not valid!", ex.getMessage());
    }

    // ===================== getOrderSpecifiers — default sorting =====================

    @Test
    void findAll_ShouldUseDefaultSorting_WhenNoSortProvided() {
        Pageable pageable = PageRequest.of(0, 10, Sort.unsorted());
        stubQueryChain(mockResults, 2L);

        assertDoesNotThrow(() -> projectDbWithDsl.findAll(
                "user1", null, null, null, null, null, pageable));
    }

    // ===================== findAll — paginasi offset & limit =====================

    @Test
    void findAll_ShouldRespectPageSize() {
        Pageable pageable = PageRequest.of(0, 5, Sort.by("createdAt").ascending());
        stubQueryChain(mockResults, 2L);

        Page<ProjectResponseDTO> result = projectDbWithDsl.findAll(
                "user1", null, null, null, null, null, pageable);

        assertEquals(5, result.getSize());
    }

    @Test
    void findAll_ShouldRespectPageNumber() {
        Pageable pageable = PageRequest.of(2, 10, Sort.by("createdAt").ascending());
        stubQueryChain(mockResults, 2L);

        Page<ProjectResponseDTO> result = projectDbWithDsl.findAll(
                "user1", null, null, null, null, null, pageable);

        assertEquals(2, result.getNumber());
    }
}