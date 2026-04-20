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

    public Long totalStageByProject(String projectId){
        Long total = queryFactory
                .select(stage.count())
                .from(stage)
                .where(stage.project.projectId.eq(projectId))
                .fetchOne();
        return total == null ? 0L : total;
    }

    public List<StageResponseDTO> findAll(String pmUsername, String memberUsername, String projectId) {

        BooleanBuilder predicate = new BooleanBuilder();

        if (pmUsername != null) {
            predicate.and(stage.project.projectManager.username.eq(pmUsername));
        } else {
            predicate.and(stage.project.memberInProjectList.any().projectMember.username.eq(memberUsername));
        }

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
