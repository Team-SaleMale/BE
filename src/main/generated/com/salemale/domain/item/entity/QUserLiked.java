package com.salemale.domain.item.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QUserLiked is a Querydsl query type for UserLiked
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QUserLiked extends EntityPathBase<UserLiked> {

    private static final long serialVersionUID = 190188597L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QUserLiked userLiked = new QUserLiked("userLiked");

    public final com.salemale.global.common.QBaseEntity _super = new com.salemale.global.common.QBaseEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> deletedAt = _super.deletedAt;

    public final QItem item;

    public final BooleanPath liked = createBoolean("liked");

    public final NumberPath<Long> likedId = createNumber("likedId", Long.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final com.salemale.domain.user.entity.QUser user;

    public QUserLiked(String variable) {
        this(UserLiked.class, forVariable(variable), INITS);
    }

    public QUserLiked(Path<? extends UserLiked> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QUserLiked(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QUserLiked(PathMetadata metadata, PathInits inits) {
        this(UserLiked.class, metadata, inits);
    }

    public QUserLiked(Class<? extends UserLiked> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.item = inits.isInitialized("item") ? new QItem(forProperty("item"), inits.get("item")) : null;
        this.user = inits.isInitialized("user") ? new com.salemale.domain.user.entity.QUser(forProperty("user")) : null;
    }

}

