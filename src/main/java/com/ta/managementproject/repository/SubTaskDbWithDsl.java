package com.ta.managementproject.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.ta.managementproject.dto.response.SubTaskResponseDTO;
import com.ta.managementproject.dto.response.TaskResponseDTO;
import com.ta.managementproject.entity.*;
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
public class SubTaskDbWithDsl {
    private final JPAQueryFactory queryFactory;
    private final QSubTask subTask = QSubTask.subTask;
    private final QTask qTask = new QTask("qTask");
    private final QStage qStage = new QStage("qStage");
    private final QProject qProject = new QProject("qProject");
    private final QProjectManager qPM = new QProjectManager("qPM");
    private final QMemberInProject qMIP = new QMemberInProject("qMIP");
    private final QProjectMember qMember = new QProjectMember("qMember");

    // ✅ Alias HARUS sama dengan alias di atas untuk JOINED inheritance
    private final QUser qPMAsUser = new QUser("qPM");
    private final QUser qMemberAsUser = new QUser("qMember");

    private static List<String> SORTING_COLUMNS = List.of
            ("subTaskName", "order", "createdAt", "updatedAt", "dueDate");

    protected OrderSpecifier<?>[] getOrderSpecifiers(Pageable pageable) { // CYC: 10, LOC: 29, COG: 9
        List<OrderSpecifier<?>> orders = new ArrayList<>();

        for (Sort.Order order : pageable.getSort()) {

            String property = order.getProperty();

            // 🔒 whitelist supaya aman
            if (!SORTING_COLUMNS.contains(property)) throw new BadRequestException("Sorting column is not valid!");

            Order direction = order.isAscending() ? Order.ASC : Order.DESC;

            switch (property) {
                case "subTaskName":
                    orders.add(new OrderSpecifier<>(direction, subTask.subTaskName));
                    break;

                case "order":
                    orders.add(new OrderSpecifier<>(direction, subTask.order));
                    break;

                case "createdAt":
                    orders.add(new OrderSpecifier<>(direction, subTask.createdAt));
                    break;

                case "updatedAt":
                    orders.add(new OrderSpecifier<>(direction, subTask.updatedAt));
                    break;

                case "dueDate":
                    orders.add(new OrderSpecifier<>(direction, subTask.dueDate));
                    break;
            }
        }

        // 🔥 default sorting kalau kosong
        if (orders.isEmpty()) {
            orders.add(subTask.order.asc());
        }

        return orders.toArray(new OrderSpecifier[0]);
    }

    protected BooleanBuilder buildDynamicFilter( // CYC: 6, LOC: 34, COG: 5
            String taskId,
            LocalDate dueDate,
            LocalDate createdAt,
            LocalDate updatedAt,
            Integer order,
            String keyword
    ){
        BooleanBuilder builder = new BooleanBuilder();

        builder.and(subTask.task.taskId.eq(taskId));

        if (dueDate != null) {
            Instant instant = dueDate.atStartOfDay(ZoneOffset.UTC).toInstant();
            builder.and(subTask.dueDate.eq(instant));
        }

        if (createdAt != null){
            Instant start = createdAt.atStartOfDay(ZoneOffset.UTC).toInstant();
            Instant end = createdAt.atTime(23, 59, 59).toInstant(ZoneOffset.UTC);
            builder.and(subTask.createdAt.between(start, end));
        }

        if (updatedAt != null){
            Instant start = updatedAt.atStartOfDay(ZoneOffset.UTC).toInstant();
            Instant end = updatedAt.atTime(23, 59, 59).toInstant(ZoneOffset.UTC);
            builder.and(subTask.updatedAt.between(start, end));
        }

        if (order != null){
            builder.and(subTask.order.eq(order));
        }

        if (keyword != null){
            builder.and(subTask.subTaskName.containsIgnoreCase(keyword))
                    .or(subTask.description.containsIgnoreCase(keyword))
                    .or(subTask.label.containsIgnoreCase(keyword));
        }

        return builder;
    }

    // Total CYC: 18, LOC: 121, COG: 15
    public Page<SubTaskResponseDTO> findAll( // CYC: 2, LOC: 58, COG: 1
            String taskId,
            LocalDate dueDate,
            LocalDate createdAt,
            LocalDate updatedAt,
            Integer order,
            String keyword,
            String email,
            Pageable pageable
    ){
        OrderSpecifier<?>[] orders = getOrderSpecifiers(pageable); // CYC: 10, LOC: 29, COG: 9
        BooleanBuilder predicate = buildDynamicFilter
                (taskId, dueDate, createdAt, updatedAt, order, keyword); // CYC: 6, LOC: 34, COG: 5

        BooleanBuilder authFilter = new BooleanBuilder(
                qPMAsUser.email.eq(email).or(qMemberAsUser.email.eq(email))
        );

        List<SubTaskResponseDTO> results = queryFactory
                .select(Projections.constructor(
                        SubTaskResponseDTO.class,
                        subTask.subTaskId,
                        subTask.subTaskName,
                        subTask.dueDate,
                        subTask.status,
                        subTask.label,
                        subTask.projectMember.email,
                        subTask.createdAt,
                        subTask.updatedAt,
                        subTask.order,
                        subTask.task.taskId,
                        subTask.description,
                        subTask.isDeleted
                ))
                .from(subTask)
                .join(subTask.task, qTask)              // ✅ alias unik
                .join(qTask.stage, qStage)
                .join(qStage.project, qProject)
                .join(qProject.projectManager, qPM)
                .leftJoin(qProject.memberInProjectList, qMIP)
                .leftJoin(qMIP.projectMember, qMember)
                .where(predicate.and(authFilter))
                .orderBy(orders)
                .distinct()
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(subTask.countDistinct())
                .from(subTask)
                .join(subTask.task, qTask)              // ✅ JOIN sama dengan query utama
                .join(qTask.stage, qStage)
                .join(qStage.project, qProject)
                .join(qProject.projectManager, qPM)
                .leftJoin(qProject.memberInProjectList, qMIP)
                .leftJoin(qMIP.projectMember, qMember)
                .where(predicate.and(authFilter))
                .fetchOne();

        return new PageImpl<>(results, pageable, total == null ? 0 : total);
    }
}
