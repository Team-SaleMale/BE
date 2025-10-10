package com.salemale.domain.chat.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QChat is a Querydsl query type for Chat
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QChat extends EntityPathBase<Chat> {

    private static final long serialVersionUID = 672464554L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QChat chat = new QChat("chat");

    public final com.salemale.global.common.QBaseEntity _super = new com.salemale.global.common.QBaseEntity(this);

    public final com.salemale.domain.user.entity.QUser buyer;

    public final DateTimePath<java.time.LocalDateTime> buyerDeletedAt = createDateTime("buyerDeletedAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> chatId = createNumber("chatId", Long.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> deletedAt = _super.deletedAt;

    public final com.salemale.domain.item.entity.QItem item;

    public final DateTimePath<java.time.LocalDateTime> lastMessageAt = createDateTime("lastMessageAt", java.time.LocalDateTime.class);

    public final com.salemale.domain.user.entity.QUser seller;

    public final DateTimePath<java.time.LocalDateTime> sellerDeletedAt = createDateTime("sellerDeletedAt", java.time.LocalDateTime.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QChat(String variable) {
        this(Chat.class, forVariable(variable), INITS);
    }

    public QChat(Path<? extends Chat> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QChat(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QChat(PathMetadata metadata, PathInits inits) {
        this(Chat.class, metadata, inits);
    }

    public QChat(Class<? extends Chat> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.buyer = inits.isInitialized("buyer") ? new com.salemale.domain.user.entity.QUser(forProperty("buyer")) : null;
        this.item = inits.isInitialized("item") ? new com.salemale.domain.item.entity.QItem(forProperty("item"), inits.get("item")) : null;
        this.seller = inits.isInitialized("seller") ? new com.salemale.domain.user.entity.QUser(forProperty("seller")) : null;
    }

}

