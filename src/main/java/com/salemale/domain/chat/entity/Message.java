package com.salemale.domain.chat.entity;

import com.salemale.domain.user.entity.User;
import com.salemale.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "message")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Message extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_id")
    private Long messageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_id", nullable = false)
    private Chat chat;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Column(name = "content", nullable = false, length = 300)
    private String content;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private MessageType type;

    public Message(Chat chat, User sender, String content, LocalDateTime sentAt,
                   Boolean isRead, Boolean isDeleted, MessageType type) {
        this.chat = chat;
        this.sender = sender;
        this.content = content;
        this.sentAt = sentAt;
        this.isRead = isRead;
        this.isDeleted = isDeleted;
        this.type = type;
    }

    public static Message of(Chat chat, User sender, String content, MessageType type) {
        Message message = new Message();
        message.chat = chat;
        message.sender = sender;
        message.content = content;
        message.sentAt = LocalDateTime.now();
        message.type = type;
        return message;
    }

    public void markAsRead() {
        this.isRead = true;
    }

    public void markAsDeleted() {
        this.isDeleted = true;
    }

    public void updateContent(String content) {
        this.content = content;
    }

    // Getter
    public Long getMessageId() {
        return messageId;
    }

    public Chat getChat() {
        return chat;
    }

    public User getSender() {
        return sender;
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public Boolean getIsRead() {
        return isRead;
    }

    public Boolean getIsDeleted() {
        return isDeleted;
    }

    public MessageType getType() {
        return type;
    }

    // Setter
    public void setMessageId(Long messageId) {
        this.messageId = messageId;
    }

    public void setChat(Chat chat) {
        this.chat = chat;
    }

    public void setSender(User sender) {
        this.sender = sender;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }

    public void setIsRead(Boolean isRead) {
        this.isRead = isRead;
    }

    public void setIsDeleted(Boolean isDeleted) {
        this.isDeleted = isDeleted;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public enum MessageType {
        TEXT, IMAGE, URL
    }
}
