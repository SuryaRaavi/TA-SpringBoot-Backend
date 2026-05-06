package com.ta.managementproject.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.ta.managementproject.dto.response.TaskResponseDTO;
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
public class TaskDbWithDsl {
    private final JPAQueryFactory queryFactory;
    private final QTask task = QTask.task;

    private static List<String> SORTING_COLUMNS = List.of
            ("taskName", "order", "createdAt", "updatedAt", "dueDate", "priority");

    protected OrderSpecifier<?>[] getOrderSpecifiers(Pageable pageable) { // CYC: 11, LOC: 32, COG: 9
        List<OrderSpecifier<?>> orders = new ArrayList<>();

        for (Sort.Order order : pageable.getSort()) {

            String property = order.getProperty();

            // 🔒 whitelist supaya aman
            if (!SORTING_COLUMNS.contains(property)) throw new BadRequestException("Sorting column is not valid!");

            Order direction = order.isAscending() ? Order.ASC : Order.DESC;

            switch (property) {
                case "taskName":
                    orders.add(new OrderSpecifier<>(direction, task.taskName));
                    break;

                case "order":
                    orders.add(new OrderSpecifier<>(direction, task.order));
                    break;

                case "createdAt":
                    orders.add(new OrderSpecifier<>(direction, task.createdAt));
                    break;

                case "updatedAt":
                    orders.add(new OrderSpecifier<>(direction, task.updatedAt));
                    break;

                case "dueDate":
                    orders.add(new OrderSpecifier<>(direction, task.dueDate));
                    break;

                case "priority":
                    orders.add(new OrderSpecifier<>(direction, task.priority));
                    break;
            }
        }

        // 🔥 default sorting kalau kosong
        if (orders.isEmpty()) {
            orders.add(task.order.asc());
        }

        return orders.toArray(new OrderSpecifier[0]);
    }

    protected BooleanBuilder buildDynamicFilter( // CYC: 7, LOC: 43, COG: 6
            String stageId,
            LocalDate dueDate,
            LocalDate createdAt,
            LocalDate updatedAt,
            Integer priority,
            Integer order,
            String username,
            String keyword
    ){
        BooleanBuilder builder = new BooleanBuilder();

        builder.and(task.stage.stageId.eq(stageId));
        builder.and(
                task.stage.project.projectManager.username.eq(username)
                .or(task.stage.project.memberInProjectList.any().projectMember.username.eq(username))
        );

        if (dueDate != null) {
            Instant instant = dueDate.atStartOfDay(ZoneOffset.UTC).toInstant();
            builder.and(task.dueDate.eq(instant));
        }

        if (createdAt != null){
            Instant start = createdAt.atStartOfDay(ZoneOffset.UTC).toInstant();
            Instant end = createdAt.atTime(23, 59, 59).toInstant(ZoneOffset.UTC);
            builder.and(task.createdAt.between(start, end));
        }

        if (updatedAt != null){
            Instant start = updatedAt.atStartOfDay(ZoneOffset.UTC).toInstant();
            Instant end = updatedAt.atTime(23, 59, 59).toInstant(ZoneOffset.UTC);
            builder.and(task.updatedAt.between(start, end));
        }

        if (priority != null){
            builder.and(task.priority.eq(priority));
        }

        if (order != null){
            builder.and(task.order.eq(order));
        }

        if (keyword != null){
            builder.and(task.taskName.containsIgnoreCase(keyword))
                    .or(task.description.containsIgnoreCase(keyword))
                    .or(task.label.containsIgnoreCase(keyword));
        }

        return builder;
    }

    // Total CYC: 20, LOC: 117, COG: 16
    public Page<TaskResponseDTO> findAll( // CYC: 2, LOC: 42, COG: 1
            String stageId,
            LocalDate dueDate,
            LocalDate createdAt,
            LocalDate updatedAt,
            Integer priority,
            Integer order,
            String keyword,
            String username,
            Pageable pageable
    ){
        OrderSpecifier<?>[] orders = getOrderSpecifiers(pageable); // // CYC: 11, LOC: 32, COG: 9
        BooleanBuilder predicate = buildDynamicFilter
                (stageId, dueDate, createdAt, updatedAt, priority, order, username, keyword); // // CYC: 7, LOC: 43, COG: 6

        List<TaskResponseDTO> results = queryFactory
                .select(Projections.constructor(
                        TaskResponseDTO.class,
                        task.taskId,
                        task.taskName,
                        task.priority,
                        task.dueDate,
                        task.status,
                        task.projectMember.username,
                        task.label,
                        task.createdAt,
                        task.updatedAt,
                        task.order,
                        task.stage.stageId,
                        task.isDeleted
                ))
                .from(task)
                .where(predicate)
                .orderBy(orders)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(task.count())
                .from(task)
                .where(predicate)
                .fetchOne();

        return new PageImpl<>(results, pageable, total == null ? 0 : total);
    }
}
