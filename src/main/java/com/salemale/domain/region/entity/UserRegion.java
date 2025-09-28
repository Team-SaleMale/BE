package com.salemale.domain.region.entity;

import com.salemale.domain.user.entity.User;
import com.salemale.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_region")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserRegion extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id", nullable = false)
    private Region region;

    @Column(name = "is_primary", nullable = false)
    private Boolean isPrimary = false;

    public UserRegion(User user, Region region, Boolean isPrimary) {
        this.user = user;
        this.region = region;
        this.isPrimary = isPrimary;
    }

    public static UserRegion of(User user, Region region, Boolean isPrimary) {
        return new UserRegion(user, region, isPrimary);
    }

    public void setAsPrimary() {
        this.isPrimary = true;
    }

    public void setAsSecondary() {
        this.isPrimary = false;
    }

    // Getter
    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public Region getRegion() {
        return region;
    }

    public Boolean getIsPrimary() {
        return isPrimary;
    }

    // Setter
    public void setId(Long id) {
        this.id = id;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void setRegion(Region region) {
        this.region = region;
    }

    public void setIsPrimary(Boolean isPrimary) {
        this.isPrimary = isPrimary;
    }
}
