package com.salemale.domain.item.entity;

import com.salemale.domain.user.entity.User;
import com.salemale.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "review")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Review extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id")
    private Long reviewId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Enumerated(EnumType.STRING)
    @Column(name = "rating", nullable = false)
    private Rating rating;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    public Review() {
    }

    public Review(User user, Item item, Rating rating, String content) {
        this.user = user;
        this.item = item;
        this.rating = rating;
        this.content = content;
    }

    public static Review of(User user, Item item, Rating rating, String content) {
        return new Review(user, item, rating, content);
    }

    public void updateContent(String content) {
        this.content = content;
    }

    public void updateRating(Rating rating) {
        this.rating = rating;
    }

    // Getter
    public Long getReviewId() {
        return reviewId;
    }

    public User getUser() {
        return user;
    }

    public Item getItem() {
        return item;
    }

    public Rating getRating() {
        return rating;
    }

    public String getContent() {
        return content;
    }

    // Setter
    public void setReviewId(Long reviewId) {
        this.reviewId = reviewId;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public void setRating(Rating rating) {
        this.rating = rating;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public enum Rating {
        ONE(1), TWO(2), THREE(3), FOUR(4), FIVE(5);

        private final int value;

        Rating(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }
}
