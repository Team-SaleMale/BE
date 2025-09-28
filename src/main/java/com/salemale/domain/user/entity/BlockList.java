package com.salemale.domain.user.entity;

import com.salemale.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "block_list")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BlockList extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocker_id", nullable = false)
    private User blocker;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocked_id", nullable = false)
    private User blocked;

    public BlockList() {
    }

    public BlockList(User blocker, User blocked) {
        this.blocker = blocker;
        this.blocked = blocked;
    }

    public static BlockList of(User blocker, User blocked) {
        return new BlockList(blocker, blocked);
    }

    // Getter
    public Long getId() {
        return id;
    }

    public User getBlocker() {
        return blocker;
    }

    public User getBlocked() {
        return blocked;
    }

    // Setter
    public void setId(Long id) {
        this.id = id;
    }

    public void setBlocker(User blocker) {
        this.blocker = blocker;
    }

    public void setBlocked(User blocked) {
        this.blocked = blocked;
    }
}
