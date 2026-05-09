package com.ta.managementproject.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.ta.managementproject.dto.response.StageResponseDTO;
import com.ta.managementproject.entity.QStage;
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
public class StageDbWithDsl {
    private final JPAQueryFactory queryFactory;
    private final QStage stage = QStage.stage;
    private static List<String> SORTING_COLUMNS = List.of("stageName", "order", "createdAt", "updatedAt");

    protected OrderSpecifier<?>[] getOrderSpecifiers(Pageable pageable) { // CYC: 9, LOC: 26, COG: 9
        List<OrderSpecifier<?>> orders = new ArrayList<>();

        for (Sort.Order order : pageable.getSort()) {

            String property = order.getProperty();

            // 🔒 whitelist supaya aman
            if (!SORTING_COLUMNS.contains(property)) throw new BadRequestException("Sorting column is not valid!");

            Order direction = order.isAscending() ? Order.ASC : Order.DESC;

            switch (property) {
                case "stageName":
                    orders.add(new OrderSpecifier<>(direction, stage.stageName));
                    break;

                case "order":
                    orders.add(new OrderSpecifier<>(direction, stage.order));
                    break;

                case "createdAt":
                    orders.add(new OrderSpecifier<>(direction, stage.createdAt));
                    break;

                case "updatedAt":
                    orders.add(new OrderSpecifier<>(direction, stage.updatedAt));
                    break;
            }
        }

        // 🔥 default sorting kalau kosong
        if (orders.isEmpty()) {
            orders.add(stage.order.asc());
        }

        return orders.toArray(new OrderSpecifier[0]);
    }

    protected BooleanBuilder buildDynamicFilter( // CYC: 4, LOC: 29, COG: 3
       String projectId,
       LocalDate createdAt,
       LocalDate updatedAt,
       String email,
       String keyword
    ){
        BooleanBuilder builder = new BooleanBuilder();
        builder.and(stage.project.projectId.eq(projectId));
        builder.and(
                stage.project.projectManager.email.eq(email)
                .or(stage.project.memberInProjectList.any().projectMember.email.eq(email))
        );

        if (createdAt != null){
            Instant start = createdAt.atStartOfDay(ZoneOffset.UTC).toInstant();
            Instant end = createdAt.atTime(23, 59, 59).toInstant(ZoneOffset.UTC);
            builder.and(stage.createdAt.between(start, end));
        }

        if (updatedAt != null){
            Instant start = updatedAt.atStartOfDay(ZoneOffset.UTC).toInstant();
            Instant end = updatedAt.atTime(23, 59, 59).toInstant(ZoneOffset.UTC);
            builder.and(stage.updatedAt.between(start, end));
        }

        if (keyword != null){
            builder.and(stage.stageName.containsIgnoreCase(keyword))
                    .or(stage.description.containsIgnoreCase(keyword));
        }

        return builder;
    }

    // Total CYC: 15, LOC: 81, COG: 13
    public Page<StageResponseDTO> findAll(String projectId, LocalDate createdAt, LocalDate updatedAt, String keyword, String email, Pageable pageable) {
    // CYC: 2, LOC: 26, COG: 1
        OrderSpecifier<?>[] orders = getOrderSpecifiers(pageable); // CYC: 9, LOC: 26, COG: 9
        BooleanBuilder predicate = buildDynamicFilter(projectId, createdAt, updatedAt, email, keyword); // CYC: 4, LOC: 29, COG: 3

        List<StageResponseDTO> results = queryFactory
                .select(Projections.constructor(
                        StageResponseDTO.class,
                        stage.stageId,
                        stage.stageName,
                        stage.order,
                        stage.description,
                        stage.project.projectId,
                        stage.isDeleted
                ))
                .from(stage)
                .where(predicate)
                .orderBy(orders)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(stage.count())
                .from(stage)
                .where(predicate)
                .fetchOne();

        return new PageImpl<>(results, pageable, total == null ? 0 : total);
    }
}
