package com.ta.managementproject.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
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
public class TaskDbWithDsl {
    private final JPAQueryFactory queryFactory;
    private final QTask task = QTask.task;
    private final QStage qStage = new QStage("qStage");
    private final QProject qProject = new QProject("qProject");
    private final QProjectManager qPM = new QProjectManager("qPM");
    private final QMemberInProject qMIP = new QMemberInProject("qMIP");
    private final QProjectMember qMember = new QProjectMember("qMember");

    // ✅ Treat subclass ke parent QUser untuk akses field email
    private final QUser qPMAsUser = new QUser("qPM");        // alias HARUS sama dengan qPM
    private final QUser qMemberAsUser = new QUser("qMember"); // alias HARUS sama dengan qMember
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

    protected BooleanBuilder buildDynamicFilter( // CYC: 7, LOC: 38, COG: 6
            String stageId,
            LocalDate dueDate,
            LocalDate createdAt,
            LocalDate updatedAt,
            Integer priority,
            Integer order,
            String keyword
    ){
        BooleanBuilder builder = new BooleanBuilder();

        builder.and(task.stage.stageId.eq(stageId));

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

    // Total CYC: 20, LOC: 127, COG: 16
    public Page<TaskResponseDTO> findAll( // CYC: 2, LOC: 57, COG: 1
            String stageId,
            LocalDate dueDate,
            LocalDate createdAt,
            LocalDate updatedAt,
            Integer priority,
            Integer order,
            String keyword,
            String email,
            Pageable pageable
    ){
        OrderSpecifier<?>[] orders = getOrderSpecifiers(pageable); // CYC: 11, LOC: 32, COG: 9
        BooleanBuilder predicate = buildDynamicFilter(
                stageId, dueDate, createdAt, updatedAt, priority, order, keyword); // CYC: 7, LOC: 38, COG: 6

        // ✅ Authorization filter pakai QUser alias yang sama dengan join alias
        BooleanBuilder authFilter = new BooleanBuilder(
                qPMAsUser.email.eq(email).or(qMemberAsUser.email.eq(email))
        );

        List<TaskResponseDTO> results = queryFactory
                .select(Projections.constructor(
                        TaskResponseDTO.class,
                        task.taskId,
                        task.taskName,
                        task.priority,
                        task.dueDate,
                        task.status,
                        task.projectMember.email,
                        task.label,
                        task.createdAt,
                        task.updatedAt,
                        task.order,
                        task.stage.stageId,
                        task.isDeleted
                ))
                .from(task)
                .join(task.stage, qStage)
                .join(qStage.project, qProject)
                .join(qProject.projectManager, qPM)         // join sebagai QProjectManager
                .leftJoin(qProject.memberInProjectList, qMIP)
                .leftJoin(qMIP.projectMember, qMember)      // join sebagai QProjectMember
                .where(predicate.and(authFilter))
                .orderBy(orders)
                .distinct()
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(task.countDistinct())
                .from(task)
                .join(task.stage, qStage)
                .join(qStage.project, qProject)
                .join(qProject.projectManager, qPM)
                .leftJoin(qProject.memberInProjectList, qMIP)
                .leftJoin(qMIP.projectMember, qMember)
                .where(predicate.and(authFilter))
                .fetchOne();

        return new PageImpl<>(results, pageable, total == null ? 0 : total);
    }
}
