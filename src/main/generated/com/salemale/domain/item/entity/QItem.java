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

    public final NumberPath<Integer> bidIncrement = createNumber("bidIncrement", Integer.class);

    public final EnumPath<com.salemale.global.common.enums.Category> category = createEnum("category", com.salemale.global.common.enums.Category.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final NumberPath<Integer> currentPrice = createNumber("currentPrice", Integer.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> deletedAt = _super.deletedAt;

    public final StringPath description = createString("description");

    public final DateTimePath<java.time.LocalDateTime> endTime = createDateTime("endTime", java.time.LocalDateTime.class);

    public final ListPath<ItemImage, QItemImage> images = this.<ItemImage, QItemImage>createList("images", ItemImage.class, QItemImage.class, PathInits.DIRECT2);

    public final NumberPath<Long> itemId = createNumber("itemId", Long.class);

    public final EnumPath<com.salemale.global.common.enums.ItemStatus> itemStatus = createEnum("itemStatus", com.salemale.global.common.enums.ItemStatus.class);

    public final StringPath name = createString("name");

    public final com.salemale.domain.region.entity.QRegion region;

    public final com.salemale.domain.user.entity.QUser seller;

    public final NumberPath<Integer> startPrice = createNumber("startPrice", Integer.class);

    public final StringPath title = createString("title");

    public final StringPath tradeDetails = createString("tradeDetails");

    public final ListPath<com.salemale.global.common.enums.TradeMethod, EnumPath<com.salemale.global.common.enums.TradeMethod>> tradeMethods = this.<com.salemale.global.common.enums.TradeMethod, EnumPath<com.salemale.global.common.enums.TradeMethod>>createList("tradeMethods", com.salemale.global.common.enums.TradeMethod.class, EnumPath.class, PathInits.DIRECT2);

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
        this.region = inits.isInitialized("region") ? new com.salemale.domain.region.entity.QRegion(forProperty("region")) : null;
        this.seller = inits.isInitialized("seller") ? new com.salemale.domain.user.entity.QUser(forProperty("seller")) : null;
        this.winner = inits.isInitialized("winner") ? new com.salemale.domain.user.entity.QUser(forProperty("winner")) : null;
    }

}

