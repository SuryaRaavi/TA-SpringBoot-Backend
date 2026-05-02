package com.ta.managementproject.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.ta.managementproject.dto.response.StageResponseDTO;
import com.ta.managementproject.entity.QStage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class StageDbWithDsl {
    private final JPAQueryFactory queryFactory;
    private final QStage stage = QStage.stage;

    public List<StageResponseDTO> findAll(String projectId) { // CYC: 1, LOC: 14

        BooleanBuilder predicate = new BooleanBuilder();

        predicate.and(stage.project.projectId.eq(projectId));

        return queryFactory
                .select(Projections.constructor(
                        StageResponseDTO.class,
                        stage.stageId,
                        stage.stageName,
                        stage.order
                ))
                .from(stage)
                .where(predicate)
                .fetch();
    }
}
