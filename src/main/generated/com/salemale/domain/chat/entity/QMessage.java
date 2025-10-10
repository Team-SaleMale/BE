package com.salemale.domain.chat.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QMessage is a Querydsl query type for Message
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QMessage extends EntityPathBase<Message> {

    private static final long serialVersionUID = 1879964533L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QMessage message = new QMessage("message");

    public final com.salemale.global.common.QBaseEntity _super = new com.salemale.global.common.QBaseEntity(this);

    public final QChat chat;

    public final StringPath content = createString("content");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> deletedAt = _super.deletedAt;

    public final BooleanPath isDeleted = createBoolean("isDeleted");

    public final BooleanPath isRead = createBoolean("isRead");

    public final NumberPath<Long> messageId = createNumber("messageId", Long.class);

    public final com.salemale.domain.user.entity.QUser sender;

    public final DateTimePath<java.time.LocalDateTime> sentAt = createDateTime("sentAt", java.time.LocalDateTime.class);

    public final EnumPath<Message.MessageType> type = createEnum("type", Message.MessageType.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QMessage(String variable) {
        this(Message.class, forVariable(variable), INITS);
    }

    public QMessage(Path<? extends Message> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QMessage(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QMessage(PathMetadata metadata, PathInits inits) {
        this(Message.class, metadata, inits);
    }

    public QMessage(Class<? extends Message> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.chat = inits.isInitialized("chat") ? new QChat(forProperty("chat"), inits.get("chat")) : null;
        this.sender = inits.isInitialized("sender") ? new com.salemale.domain.user.entity.QUser(forProperty("sender")) : null;
    }

}

