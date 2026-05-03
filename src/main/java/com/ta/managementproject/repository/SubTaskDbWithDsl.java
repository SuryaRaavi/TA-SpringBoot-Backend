package com.ta.managementproject.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.ta.managementproject.dto.response.SubTaskResponseDTO;
import com.ta.managementproject.dto.response.TaskResponseDTO;
import com.ta.managementproject.entity.QSubTask;
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

    private static List<String> SORTING_COLUMNS = List.of
            ("subTaskName", "order", "createdAt", "updatedAt", "dueDate");

    private OrderSpecifier<?>[] getOrderSpecifiers(Pageable pageable) { // CYC: 10, LOC: 29
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

    private BooleanBuilder buildDynamicFilter( // CYC: 6, LOC: 34
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

    // Total CYC: 18, LOC: 101
    public Page<SubTaskResponseDTO> findAll( // CYC: 2, LOC: 38
            String taskId,
            LocalDate dueDate,
            LocalDate createdAt,
            LocalDate updatedAt,
            Integer order,
            String keyword,
            Pageable pageable
    ){
        OrderSpecifier<?>[] orders = getOrderSpecifiers(pageable); // CYC: 10, LOC: 29
        BooleanBuilder predicate = buildDynamicFilter
                (taskId, dueDate, createdAt, updatedAt, order, keyword); // CYC: 6, LOC: 34

        List<SubTaskResponseDTO> results = queryFactory
                .select(Projections.constructor(
                        SubTaskResponseDTO.class,
                        subTask.subTaskId,
                        subTask.subTaskName,
                        subTask.dueDate,
                        subTask.status,
                        subTask.label,
                        subTask.projectMember.username,
                        subTask.createdAt,
                        subTask.updatedAt,
                        subTask.order,
                        subTask.task.taskId,
                        subTask.description,
                        subTask.isDeleted
                ))
                .from(subTask)
                .where(predicate)
                .orderBy(orders)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(subTask.count())
                .from(subTask)
                .where(predicate)
                .fetchOne();

        return new PageImpl<>(results, pageable, total == null ? 0 : total);
    }
}
