package com.salemale.domain.item.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QItemTransaction is a Querydsl query type for ItemTransaction
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QItemTransaction extends EntityPathBase<ItemTransaction> {

    private static final long serialVersionUID = -720787170L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QItemTransaction itemTransaction = new QItemTransaction("itemTransaction");

    public final com.salemale.global.common.QBaseEntity _super = new com.salemale.global.common.QBaseEntity(this);

    public final NumberPath<Integer> bidPrice = createNumber("bidPrice", Integer.class);

    public final com.salemale.domain.user.entity.QUser buyer;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> deletedAt = _super.deletedAt;

    public final QItem item;

    public final NumberPath<Long> transactionId = createNumber("transactionId", Long.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QItemTransaction(String variable) {
        this(ItemTransaction.class, forVariable(variable), INITS);
    }

    public QItemTransaction(Path<? extends ItemTransaction> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QItemTransaction(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QItemTransaction(PathMetadata metadata, PathInits inits) {
        this(ItemTransaction.class, metadata, inits);
    }

    public QItemTransaction(Class<? extends ItemTransaction> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.buyer = inits.isInitialized("buyer") ? new com.salemale.domain.user.entity.QUser(forProperty("buyer")) : null;
        this.item = inits.isInitialized("item") ? new QItem(forProperty("item"), inits.get("item")) : null;
    }

}

