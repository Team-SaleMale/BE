package com.salemale.domain.item.entity;

import com.salemale.domain.user.entity.User;
import com.salemale.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_liked")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserLiked extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "likes_id")
    private Long likesId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Column(name = "liked", nullable = false)
    private boolean liked = false;

    public UserLiked(User user, Item item, boolean liked) {
        this.user = user;
        this.item = item;
        this.liked = liked;
    }

    public static UserLiked of(User user, Item item, boolean liked) {
        return new UserLiked(user, item, liked);
    }

    public void toggleLike() {
        this.liked = !this.liked;
    }

    public void setLiked(boolean liked) {
        this.liked = liked;
    }

    // Getter
    public Long getLikesId() {
        return likesId;
    }

    public User getUser() {
        return user;
    }

    public Item getItem() {
        return item;
    }

    public boolean getLiked() {
        return liked;
    }

    // Setter
    public void setLikesId(Long likesId) {
        this.likesId = likesId;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void setItem(Item item) {
        this.item = item;
    }
}
