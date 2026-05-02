package com.ta.managementproject.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.ta.managementproject.dto.response.ProjectResponseDTO;
import com.ta.managementproject.entity.QProject;
import com.ta.managementproject.entity.QStage;
import com.ta.managementproject.entity.QSubTask;
import com.ta.managementproject.entity.QTask;
import com.ta.managementproject.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ProjectDbWithDsl {
    private final JPAQueryFactory queryFactory;
    private final QProject project = QProject.project;
    private final QStage stage = QStage.stage;
    private final QTask task = QTask.task;
    private final QSubTask subTask = QSubTask.subTask;

    private static List<String> SORTING_COLUMNS = List.of("projectName", "startDate", "endDate", "createdAt", "updatedAt");

    private OrderSpecifier<?>[] getOrderSpecifiers(Pageable pageable) { // CYC: 10, LOC: 29
        List<OrderSpecifier<?>> orders = new ArrayList<>();

        for (Sort.Order order : pageable.getSort()) {

            String property = order.getProperty();

            // 🔒 whitelist supaya aman
            if (!SORTING_COLUMNS.contains(property)) throw new BadRequestException("Sorting column is not valid!");

            Order direction = order.isAscending() ? Order.ASC : Order.DESC;

            switch (property) {
                case "projectName":
                    orders.add(new OrderSpecifier<>(direction, project.projectName));
                    break;

                case "startDate":
                    orders.add(new OrderSpecifier<>(direction, project.startDate));
                    break;

                case "endDate":
                    orders.add(new OrderSpecifier<>(direction, project.endDate));
                    break;

                case "createdAt":
                    orders.add(new OrderSpecifier<>(direction, project.createdAt));
                    break;

                case "updatedAt":
                    orders.add(new OrderSpecifier<>(direction, project.updatedAt));
                    break;
            }
        }

        // 🔥 default sorting kalau kosong
        if (orders.isEmpty()) {
            orders.add(project.createdAt.desc());
        }

        return orders.toArray(new OrderSpecifier[0]);
    }

    private BooleanBuilder buildDynamicFilter( // CYC: 7, LOC: 39
            String pmUsername,
            String memberUsername,
            LocalDate startDate,
            LocalDate endDate,
            LocalDate createdAt,
            LocalDate updatedAt,
            String keyword
    ){
        BooleanBuilder builder = new BooleanBuilder();

        if (pmUsername != null){
            builder.and(project.projectManager.username.eq(pmUsername));
        }else{
            builder.and(project.memberInProjectList.any().projectMember.username.eq(memberUsername));
        }

        if (startDate != null) {
            Instant instant = startDate.atStartOfDay(ZoneOffset.UTC).toInstant();
            builder.and(project.startDate.goe(instant));
        }

        if (endDate != null){
            Instant instant = endDate.atTime(23, 59, 59).toInstant(ZoneOffset.UTC);
            builder.and(project.startDate.loe(instant));
        }

        if (createdAt != null){
            Instant start = createdAt.atStartOfDay(ZoneOffset.UTC).toInstant();
            Instant end = createdAt.atTime(23, 59, 59).toInstant(ZoneOffset.UTC);
            builder.and(project.createdAt.between(start, end));
        }

        if (updatedAt != null){
            Instant start = updatedAt.atStartOfDay(ZoneOffset.UTC).toInstant();
            Instant end = updatedAt.atTime(23, 59, 59).toInstant(ZoneOffset.UTC);
            builder.and(project.updatedAt.between(start, end));
        }

        if (keyword != null){
            builder.and(project.projectName.containsIgnoreCase(keyword))
                    .or(project.description.containsIgnoreCase(keyword));
        }

        return builder;
    }

    // CYC: 19, LOC: 112
    public Page<ProjectResponseDTO> findAll(
            String pmUsername,
            String memberUsername,
            LocalDate startDate,
            LocalDate endDate,
            LocalDate createdAt,
            LocalDate updatedAt,
            String keyword,
            Pageable pageable
    ){
        OrderSpecifier<?>[] orders = getOrderSpecifiers(pageable); // CYC: 10, LOC: 29
        BooleanBuilder predicate = buildDynamicFilter(pmUsername, memberUsername, startDate, endDate, createdAt, updatedAt, keyword); // CYC: 7, LOC: 39

        List<ProjectResponseDTO> results = queryFactory
                .select(Projections.constructor(
                        ProjectResponseDTO.class,
                        project.projectId,
                        project.projectName,
                        project.description,
                        project.projectManager.fullName,
                        project.startDate,
                        project.endDate,
                        project.createdAt,
                        project.updatedAt,
                        project.isCancelled
                ))
                .from(project)
                .where(predicate)
                .orderBy(orders)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(project.count())
                .from(project)
                .where(predicate)
                .fetchOne();

        return new PageImpl<>(results, pageable, total == null ? 0 : total);
    }
}
