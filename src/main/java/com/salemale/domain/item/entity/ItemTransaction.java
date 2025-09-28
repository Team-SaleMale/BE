package com.salemale.domain.item.entity;

import com.salemale.domain.user.entity.User;
import com.salemale.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "item_transaction")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ItemTransaction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    private Long transactionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_state", nullable = false)
    private TransactionState transactionState;

    @Column(name = "bid_price", nullable = false)
    private Integer bidPrice;


    public ItemTransaction(User buyer, Item item, TransactionState transactionState, Integer bidPrice) {
        this.buyer = buyer;
        this.item = item;
        this.transactionState = transactionState;
        this.bidPrice = bidPrice;
    }

    public static ItemTransaction of(User buyer, Item item, TransactionState transactionState, Integer bidPrice) {
        return new ItemTransaction(buyer, item, transactionState, bidPrice);
    }

    public void updateTransactionState(TransactionState transactionState) {
        this.transactionState = transactionState;
    }

    public void updateBidPrice(Integer bidPrice) {
        this.bidPrice = bidPrice;
    }

    // Getter
    public Long getTransactionId() {
        return transactionId;
    }

    public User getBuyer() {
        return buyer;
    }

    public Item getItem() {
        return item;
    }

    public TransactionState getTransactionState() {
        return transactionState;
    }

    public Integer getBidPrice() {
        return bidPrice;
    }

    // Setter
    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
    }

    public void setBuyer(User buyer) {
        this.buyer = buyer;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public void setTransactionState(TransactionState transactionState) {
        this.transactionState = transactionState;
    }

    public void setBidPrice(Integer bidPrice) {
        this.bidPrice = bidPrice;
    }

    public enum TransactionState {
        BIDDING, SUCCESS, FAIL
    }
}
