package com.salemale.domain.user.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QBlockList is a Querydsl query type for BlockList
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QBlockList extends EntityPathBase<BlockList> {

    private static final long serialVersionUID = -1299583322L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QBlockList blockList = new QBlockList("blockList");

    public final com.salemale.global.common.QBaseEntity _super = new com.salemale.global.common.QBaseEntity(this);

    public final QUser blocked;

    public final QUser blocker;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> deletedAt = _super.deletedAt;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QBlockList(String variable) {
        this(BlockList.class, forVariable(variable), INITS);
    }

    public QBlockList(Path<? extends BlockList> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QBlockList(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QBlockList(PathMetadata metadata, PathInits inits) {
        this(BlockList.class, metadata, inits);
    }

    public QBlockList(Class<? extends BlockList> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.blocked = inits.isInitialized("blocked") ? new QUser(forProperty("blocked")) : null;
        this.blocker = inits.isInitialized("blocker") ? new QUser(forProperty("blocker")) : null;
    }

}

