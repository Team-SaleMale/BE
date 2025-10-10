package com.salemale.domain.region.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QUserRegion is a Querydsl query type for UserRegion
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QUserRegion extends EntityPathBase<UserRegion> {

    private static final long serialVersionUID = 1479590349L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QUserRegion userRegion = new QUserRegion("userRegion");

    public final com.salemale.global.common.QBaseEntity _super = new com.salemale.global.common.QBaseEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> deletedAt = _super.deletedAt;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final BooleanPath isPrimary = createBoolean("isPrimary");

    public final QRegion region;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final com.salemale.domain.user.entity.QUser user;

    public QUserRegion(String variable) {
        this(UserRegion.class, forVariable(variable), INITS);
    }

    public QUserRegion(Path<? extends UserRegion> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QUserRegion(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QUserRegion(PathMetadata metadata, PathInits inits) {
        this(UserRegion.class, metadata, inits);
    }

    public QUserRegion(Class<? extends UserRegion> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.region = inits.isInitialized("region") ? new QRegion(forProperty("region")) : null;
        this.user = inits.isInitialized("user") ? new com.salemale.domain.user.entity.QUser(forProperty("user")) : null;
    }

}

