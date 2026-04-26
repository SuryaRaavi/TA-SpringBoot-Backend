package com.ta.managementproject;

import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Predicate;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.ta.managementproject.dto.response.StageResponseDTO;
import com.ta.managementproject.repository.StageDbWithDsl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class StageDbWithDslTest {

    @Mock
    private JPAQueryFactory queryFactory;

    @InjectMocks
    private StageDbWithDsl stageDbWithDsl;

    @Mock
    private JPAQuery<StageResponseDTO> selectQuery;

    @Mock
    private JPAQuery<Long> countQuery;

    private static final String PROJECT_ID     = "project-001";
    private static final String PM_USERNAME    = "pm_user";
    private static final String MEMBER_USERNAME = "member_user";

    private StageResponseDTO sampleDto;

    /**
     * Konstruktor StageResponseDTO sesuai urutan Projections.constructor di repository:
     * stageId, stageName, order, finishedTask(null), inProgressTask(null),
     * todoTask(null), totalTask(null), progress(null)
     */
    @BeforeEach
    void setUp() {
        sampleDto = new StageResponseDTO(
                UUID.randomUUID().toString(), // stageId
                "Stage Alpha",               // stageName
                1,                           // order
                null,                        // finishedTask
                null,                        // inProgressTask
                null,                        // todoTask
                null,                        // totalTask
                null                         // progress
        );
    }

    // ─── Helper: stub findAll chain ──────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private void stubFindAllChain(List<StageResponseDTO> results) {
        when(queryFactory.select(any(Expression.class))).thenReturn((JPAQuery) selectQuery);
        when(selectQuery.from(any(EntityPath.class))).thenReturn((JPAQuery) selectQuery);
        when(selectQuery.where(any(Predicate.class))).thenReturn((JPAQuery) selectQuery);
        when(selectQuery.fetch()).thenReturn(results);
    }

    // ─── Helper: stub totalStageByProject chain ──────────────────────────────────

    @SuppressWarnings("unchecked")
    private void stubCountChain(Long count) {
        when(queryFactory.select(any(Expression.class))).thenReturn((JPAQuery) countQuery);
        when(countQuery.from(any(EntityPath.class))).thenReturn((JPAQuery) countQuery);
        when(countQuery.where(any(Predicate.class))).thenReturn((JPAQuery) countQuery);
        when(countQuery.fetchOne()).thenReturn(count);
    }

    // ════════════════════════════════════════════════════════════════════════════
    // totalStageByProject — count normal
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    void totalStageByProject_withExistingStages_shouldReturnCount() {
        stubCountChain(3L);

        Long result = stageDbWithDsl.totalStageByProject(PROJECT_ID);

        assertThat(result).isEqualTo(3L);
    }

    // ════════════════════════════════════════════════════════════════════════════
    // totalStageByProject — fetchOne null → kembalikan 0
    // ════════════════════════════════════════════════════════════════════════════

    /**
     * Repository: total == null ? 0L : total
     */
    @Test
    void totalStageByProject_whenCountIsNull_shouldReturnZero() {
        stubCountChain(null);

        Long result = stageDbWithDsl.totalStageByProject(PROJECT_ID);

        assertThat(result).isZero();
    }

    // ════════════════════════════════════════════════════════════════════════════
    // totalStageByProject — tidak ada stage → 0
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    void totalStageByProject_withNoStages_shouldReturnZero() {
        stubCountChain(0L);

        Long result = stageDbWithDsl.totalStageByProject(PROJECT_ID);

        assertThat(result).isZero();
    }

    // ════════════════════════════════════════════════════════════════════════════
    // totalStageByProject — queryFactory.select dipanggil sekali
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    void totalStageByProject_shouldCallQueryFactorySelectOnce() {
        stubCountChain(2L);

        stageDbWithDsl.totalStageByProject(PROJECT_ID);

        verify(queryFactory, times(1)).select(any(Expression.class));
    }

    // ════════════════════════════════════════════════════════════════════════════
    // findAll — filter by pmUsername, ada hasil
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    void findAll_withPmUsername_shouldReturnListOfStages() {
        stubFindAllChain(List.of(sampleDto));

        List<StageResponseDTO> result = stageDbWithDsl.findAll(PM_USERNAME, null, PROJECT_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStageName()).isEqualTo("Stage Alpha");
    }

    // ════════════════════════════════════════════════════════════════════════════
    // findAll — filter by memberUsername, ada hasil
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    void findAll_withMemberUsername_shouldReturnListOfStages() {
        stubFindAllChain(List.of(sampleDto));

        List<StageResponseDTO> result = stageDbWithDsl.findAll(null, MEMBER_USERNAME, PROJECT_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStageName()).isEqualTo("Stage Alpha");
    }

    // ════════════════════════════════════════════════════════════════════════════
    // findAll — tidak ada stage → list kosong
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    void findAll_whenNoStages_shouldReturnEmptyList() {
        stubFindAllChain(List.of());

        List<StageResponseDTO> result = stageDbWithDsl.findAll(PM_USERNAME, null, PROJECT_ID);

        assertThat(result).isEmpty();
    }

    // ════════════════════════════════════════════════════════════════════════════
    // findAll — multiple stages
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    void findAll_withMultipleStages_shouldReturnAllStages() {
        StageResponseDTO dto2 = new StageResponseDTO(
                UUID.randomUUID().toString(),
                "Stage Beta",
                2,
                null, null, null, null, null
        );
        stubFindAllChain(List.of(sampleDto, dto2));

        List<StageResponseDTO> result = stageDbWithDsl.findAll(PM_USERNAME, null, PROJECT_ID);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getStageName()).isEqualTo("Stage Alpha");
        assertThat(result.get(1).getStageName()).isEqualTo("Stage Beta");
    }

    // ════════════════════════════════════════════════════════════════════════════
    // findAll — progress-related fields null (belum dihitung)
    // ════════════════════════════════════════════════════════════════════════════

    /**
     * Repository mengisi finishedTask, inProgressTask, todoTask, totalTask, progress
     * dengan Expressions.nullExpression — semua field ini harus null di hasil query.
     * Kalkulasi dilakukan di service layer (getStageStatistics).
     */
    @Test
    void findAll_shouldReturnStagesWithNullProgressFields() {
        stubFindAllChain(List.of(sampleDto));

        List<StageResponseDTO> result = stageDbWithDsl.findAll(PM_USERNAME, null, PROJECT_ID);

        StageResponseDTO dto = result.get(0);
        assertThat(dto.getFinishedTask()).isNull();
        assertThat(dto.getInProgressTask()).isNull();
        assertThat(dto.getTodoTask()).isNull();
        assertThat(dto.getTotalTask()).isNull();
        assertThat(dto.getProgress()).isNull();
    }

    // ════════════════════════════════════════════════════════════════════════════
    // findAll — queryFactory.select dipanggil tepat sekali
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    void findAll_shouldCallQueryFactorySelectOnce() {
        stubFindAllChain(List.of(sampleDto));

        stageDbWithDsl.findAll(PM_USERNAME, null, PROJECT_ID);

        verify(queryFactory, times(1)).select(any(Expression.class));
    }

    // ════════════════════════════════════════════════════════════════════════════
    // findAll — order field terisi dengan benar
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    void findAll_shouldReturnStageWithCorrectOrder() {
        stubFindAllChain(List.of(sampleDto));

        List<StageResponseDTO> result = stageDbWithDsl.findAll(PM_USERNAME, null, PROJECT_ID);

        assertThat(result.get(0).getOrder()).isEqualTo(1);
    }
}