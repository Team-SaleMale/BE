package com.salemale.domain.item.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QItem is a Querydsl query type for Item
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QItem extends EntityPathBase<Item> {

    private static final long serialVersionUID = -1448211040L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QItem item = new QItem("item");

    public final com.salemale.global.common.QBaseEntity _super = new com.salemale.global.common.QBaseEntity(this);

    public final EnumPath<com.salemale.global.common.enums.Category> category = createEnum("category", com.salemale.global.common.enums.Category.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> deletedAt = _super.deletedAt;

    public final StringPath description = createString("description");

    public final DateTimePath<java.time.LocalDateTime> endTime = createDateTime("endTime", java.time.LocalDateTime.class);

    public final NumberPath<Long> itemId = createNumber("itemId", Long.class);

    public final EnumPath<com.salemale.global.common.enums.ItemStatus> itemStatus = createEnum("itemStatus", com.salemale.global.common.enums.ItemStatus.class);

    public final StringPath name = createString("name");

    public final StringPath photoUrl = createString("photoUrl");

    public final NumberPath<Integer> price = createNumber("price", Integer.class);

    public final com.salemale.domain.user.entity.QUser seller;

    public final NumberPath<Integer> startPrice = createNumber("startPrice", Integer.class);

    public final StringPath title = createString("title");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final com.salemale.domain.user.entity.QUser winner;

    public QItem(String variable) {
        this(Item.class, forVariable(variable), INITS);
    }

    public QItem(Path<? extends Item> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QItem(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QItem(PathMetadata metadata, PathInits inits) {
        this(Item.class, metadata, inits);
    }

    public QItem(Class<? extends Item> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.seller = inits.isInitialized("seller") ? new com.salemale.domain.user.entity.QUser(forProperty("seller")) : null;
        this.winner = inits.isInitialized("winner") ? new com.salemale.domain.user.entity.QUser(forProperty("winner")) : null;
    }

}

