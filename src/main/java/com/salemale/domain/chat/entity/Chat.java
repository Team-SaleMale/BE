package com.salemale.domain.chat.entity;

import com.salemale.domain.item.entity.Item;
import com.salemale.domain.user.entity.User;
import com.salemale.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Chat extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_id")
    private Long chatId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Column(name = "last_message_at", nullable = false)
    private LocalDateTime lastMessageAt;

    @Column(name = "seller_deleted_at")
    private LocalDateTime sellerDeletedAt;

    @Column(name = "buyer_deleted_at")
    private LocalDateTime buyerDeletedAt;

    public Chat(User seller, User buyer, Item item, LocalDateTime lastMessageAt,
                LocalDateTime sellerDeletedAt, LocalDateTime buyerDeletedAt) {
        this.seller = seller;
        this.buyer = buyer;
        this.item = item;
        this.lastMessageAt = lastMessageAt;
        this.sellerDeletedAt = sellerDeletedAt;
        this.buyerDeletedAt = buyerDeletedAt;
    }

    public static Chat of(User seller, User buyer, Item item) {
        Chat chat = new Chat();
        chat.seller = seller;
        chat.buyer = buyer;
        chat.item = item;
        chat.lastMessageAt = LocalDateTime.now();
        return chat;
    }

    public void updateLastMessageAt(LocalDateTime lastMessageAt) {
        this.lastMessageAt = lastMessageAt;
    }

    public void deleteBySeller() {
        this.sellerDeletedAt = LocalDateTime.now();
    }

    public void deleteByBuyer() {
        this.buyerDeletedAt = LocalDateTime.now();
    }

    public void restoreBySeller() {
        this.sellerDeletedAt = null;
    }

    public void restoreByBuyer() {
        this.buyerDeletedAt = null;
    }

    // Getter
    public Long getChatId() {
        return chatId;
    }

    public User getSeller() {
        return seller;
    }

    public User getBuyer() {
        return buyer;
    }

    public Item getItem() {
        return item;
    }

    public LocalDateTime getLastMessageAt() {
        return lastMessageAt;
    }

    public LocalDateTime getSellerDeletedAt() {
        return sellerDeletedAt;
    }

    public LocalDateTime getBuyerDeletedAt() {
        return buyerDeletedAt;
    }

    // Setter
    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }

    public void setSeller(User seller) {
        this.seller = seller;
    }

    public void setBuyer(User buyer) {
        this.buyer = buyer;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public void setLastMessageAt(LocalDateTime lastMessageAt) {
        this.lastMessageAt = lastMessageAt;
    }

    public void setSellerDeletedAt(LocalDateTime sellerDeletedAt) {
        this.sellerDeletedAt = sellerDeletedAt;
    }

    public void setBuyerDeletedAt(LocalDateTime buyerDeletedAt) {
        this.buyerDeletedAt = buyerDeletedAt;
    }
}
