//package com.ta.managementproject.measure_test;
//
//import com.querydsl.core.BooleanBuilder;
//import com.querydsl.core.types.EntityPath;
//import com.querydsl.core.types.OrderSpecifier;
//import com.querydsl.jpa.impl.JPAQuery;
//import com.querydsl.jpa.impl.JPAQueryFactory;
//import com.ta.managementproject.dto.response.StageResponseDTO;
//import com.ta.managementproject.exception.BadRequestException;
//import com.ta.managementproject.repository.StageDbWithDsl;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.domain.Sort;
//
//import java.time.LocalDate;
//import java.util.List;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class StageDbWithDslTest {
//
//    @Mock
//    private JPAQueryFactory queryFactory;
//
//    @InjectMocks
//    private StageDbWithDsl stageDbWithDsl;
//
//    @Mock private JPAQuery<StageResponseDTO> selectQuery;
//    @Mock private JPAQuery<Long> countQuery;
//
//    private List<StageResponseDTO> mockResults;
//
//    @BeforeEach
//    void setUp() {
//        mockResults = List.of(
//                new StageResponseDTO("stage-1", "Stage A", 1, "Desc A", "project-1", false),
//                new StageResponseDTO("stage-2", "Stage B", 2, "Desc B", "project-1", false)
//        );
//    }
//
//    // ===================== Helper: stub full query chain =====================
//
//    @SuppressWarnings("unchecked")
//    private void stubQueryChain(List<StageResponseDTO> results, Long total) {
//        when(queryFactory.select(any(com.querydsl.core.types.Expression.class)))
//                .thenReturn((JPAQuery) selectQuery)
//                .thenReturn((JPAQuery) countQuery);
//
//        when(selectQuery.from((EntityPath<?>) any())).thenReturn(selectQuery);
//        when(selectQuery.where(any(BooleanBuilder.class))).thenReturn(selectQuery);
//        when(selectQuery.orderBy(any(OrderSpecifier[].class))).thenReturn(selectQuery);
//        when(selectQuery.offset(anyLong())).thenReturn(selectQuery);
//        when(selectQuery.limit(anyLong())).thenReturn(selectQuery);
//        when(selectQuery.fetch()).thenReturn(results);
//
//        when(countQuery.from((EntityPath<?>) any())).thenReturn(countQuery);
//        when(countQuery.where(any(BooleanBuilder.class))).thenReturn(countQuery);
//        when(countQuery.fetchOne()).thenReturn(total);
//    }
//
//    // ===================== findAll — hasil dan paginasi =====================
//
//    @Test
//    void findAll_ShouldReturnPageWithResults_WhenDataExists() {
//        Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt").ascending());
//        stubQueryChain(mockResults, 2L);
//
//        Page<StageResponseDTO> result = stageDbWithDsl.findAll(
//                "project-1", null, null, null, "user1", pageable);
//
//        assertEquals(2, result.getContent().size());
//    }
//
//    @Test
//    void findAll_ShouldReturnCorrectTotalElements_WhenDataExists() {
//        Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt").ascending());
//        stubQueryChain(mockResults, 2L);
//
//        Page<StageResponseDTO> result = stageDbWithDsl.findAll(
//                "project-1", null, null, null, "user1", pageable);
//
//        assertEquals(2L, result.getTotalElements());
//    }
//
//    @Test
//    void findAll_ShouldReturnZeroTotal_WhenCountIsNull() {
//        Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt").ascending());
//        stubQueryChain(List.of(), null);
//
//        Page<StageResponseDTO> result = stageDbWithDsl.findAll(
//                "project-1", null, null, null, "user1", pageable);
//
//        assertEquals(0L, result.getTotalElements());
//    }
//
//    @Test
//    void findAll_ShouldReturnEmptyContent_WhenNoDataExists() {
//        Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt").ascending());
//        stubQueryChain(List.of(), 0L);
//
//        Page<StageResponseDTO> result = stageDbWithDsl.findAll(
//                "project-1", null, null, null, "user1", pageable);
//
//        assertTrue(result.getContent().isEmpty());
//    }
//
//    // ===================== findAll — filter parameter =====================
//
//    @Test
//    void findAll_ShouldExecuteQuery_WhenCreatedAtProvided() {
//        Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt").ascending());
//        stubQueryChain(mockResults, 2L);
//
//        Page<StageResponseDTO> result = stageDbWithDsl.findAll(
//                "project-1", LocalDate.of(2024, 6, 1), null, null, "user1", pageable);
//
//        assertEquals(2, result.getContent().size());
//    }
//
//    @Test
//    void findAll_ShouldExecuteQuery_WhenUpdatedAtProvided() {
//        Pageable pageable = PageRequest.of(0, 10, Sort.by("updatedAt").ascending());
//        stubQueryChain(mockResults, 2L);
//
//        Page<StageResponseDTO> result = stageDbWithDsl.findAll(
//                "project-1", null, LocalDate.of(2024, 6, 1), null, "user1", pageable);
//
//        assertEquals(2, result.getContent().size());
//    }
//
//    @Test
//    void findAll_ShouldExecuteQuery_WhenKeywordProvided() {
//        Pageable pageable = PageRequest.of(0, 10, Sort.by("stageName").ascending());
//        stubQueryChain(mockResults, 2L);
//
//        Page<StageResponseDTO> result = stageDbWithDsl.findAll(
//                "project-1", null, null, "Stage", "user1", pageable);
//
//        assertEquals(2, result.getContent().size());
//    }
//
//    @Test
//    void findAll_ShouldExecuteQuery_WhenAllFiltersProvided() {
//        Pageable pageable = PageRequest.of(0, 10, Sort.by("stageName").ascending());
//        stubQueryChain(mockResults, 2L);
//
//        Page<StageResponseDTO> result = stageDbWithDsl.findAll(
//                "project-1",
//                LocalDate.of(2024, 1, 1),
//                LocalDate.of(2024, 6, 1),
//                "Stage",
//                "user1",
//                pageable
//        );
//
//        assertEquals(2, result.getContent().size());
//    }
//
//    @Test
//    void findAll_ShouldExecuteQuery_WhenAllFiltersNull() {
//        Pageable pageable = PageRequest.of(0, 10, Sort.by("order").ascending());
//        stubQueryChain(mockResults, 2L);
//
//        Page<StageResponseDTO> result = stageDbWithDsl.findAll(
//                "project-1", null, null, null, "user1", pageable);
//
//        assertEquals(2, result.getContent().size());
//    }
//
//    // ===================== getOrderSpecifiers — valid columns =====================
//
//    @Test
//    void findAll_ShouldNotThrow_WhenSortByStageName() {
//        Pageable pageable = PageRequest.of(0, 10, Sort.by("stageName").ascending());
//        stubQueryChain(mockResults, 2L);
//
//        assertDoesNotThrow(() -> stageDbWithDsl.findAll(
//                "project-1", null, null, null, "user1", pageable));
//    }
//
//    @Test
//    void findAll_ShouldNotThrow_WhenSortByOrder() {
//        Pageable pageable = PageRequest.of(0, 10, Sort.by("order").descending());
//        stubQueryChain(mockResults, 2L);
//
//        assertDoesNotThrow(() -> stageDbWithDsl.findAll(
//                "project-1", null, null, null, "user1", pageable));
//    }
//
//    @Test
//    void findAll_ShouldNotThrow_WhenSortByCreatedAt() {
//        Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());
//        stubQueryChain(mockResults, 2L);
//
//        assertDoesNotThrow(() -> stageDbWithDsl.findAll(
//                "project-1", null, null, null, "user1", pageable));
//    }
//
//    @Test
//    void findAll_ShouldNotThrow_WhenSortByUpdatedAt() {
//        Pageable pageable = PageRequest.of(0, 10, Sort.by("updatedAt").ascending());
//        stubQueryChain(mockResults, 2L);
//
//        assertDoesNotThrow(() -> stageDbWithDsl.findAll(
//                "project-1", null, null, null, "user1", pageable));
//    }
//
//    // ===================== getOrderSpecifiers — invalid column =====================
//
//    @Test
//    void findAll_ShouldThrowBadRequestException_WhenSortColumnInvalid() {
//        Pageable pageable = PageRequest.of(0, 10, Sort.by("invalidColumn").ascending());
//
//        assertThrows(BadRequestException.class, () -> stageDbWithDsl.findAll(
//                "project-1", null, null, null, "user1", pageable));
//    }
//
//    @Test
//    void findAll_ShouldThrowBadRequestException_WithCorrectMessage_WhenSortColumnInvalid() {
//        Pageable pageable = PageRequest.of(0, 10, Sort.by("hacked").ascending());
//
//        BadRequestException ex = assertThrows(BadRequestException.class,
//                () -> stageDbWithDsl.findAll(
//                        "project-1", null, null, null, "user1", pageable));
//
//        assertEquals("Sorting column is not valid!", ex.getMessage());
//    }
//
//    // ===================== getOrderSpecifiers — default sorting =====================
//
//    @Test
//    void findAll_ShouldUseDefaultSorting_WhenNoSortProvided() {
//        Pageable pageable = PageRequest.of(0, 10, Sort.unsorted());
//        stubQueryChain(mockResults, 2L);
//
//        assertDoesNotThrow(() -> stageDbWithDsl.findAll(
//                "project-1", null, null, null, "user1", pageable));
//    }
//
//    // ===================== findAll — paginasi =====================
//
//    @Test
//    void findAll_ShouldRespectPageSize() {
//        Pageable pageable = PageRequest.of(0, 5, Sort.by("order").ascending());
//        stubQueryChain(mockResults, 2L);
//
//        Page<StageResponseDTO> result = stageDbWithDsl.findAll(
//                "project-1", null, null, null, "user1", pageable);
//
//        assertEquals(5, result.getSize());
//    }
//
//    @Test
//    void findAll_ShouldRespectPageNumber() {
//        Pageable pageable = PageRequest.of(2, 10, Sort.by("order").ascending());
//        stubQueryChain(mockResults, 2L);
//
//        Page<StageResponseDTO> result = stageDbWithDsl.findAll(
//                "project-1", null, null, null, "user1", pageable);
//
//        assertEquals(2, result.getNumber());
//    }
//
//    @Test
//    void findAll_ShouldReturnCorrectPageable() {
//        Pageable pageable = PageRequest.of(1, 5, Sort.by("stageName").ascending());
//        stubQueryChain(mockResults, 10L);
//
//        Page<StageResponseDTO> result = stageDbWithDsl.findAll(
//                "project-1", null, null, null, "user1", pageable);
//
//        assertEquals(1, result.getNumber());
//        assertEquals(5, result.getSize());
//        assertEquals(10L, result.getTotalElements());
//    }
//}