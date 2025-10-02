package com.salemale.domain.user.entity;

import com.salemale.global.common.BaseEntity;
import com.salemale.global.common.enums.AlarmChecked;
import com.salemale.global.common.enums.LoginType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_email", columnNames = {"email"}),
                @UniqueConstraint(name = "uk_user_login_type_social_id", columnNames = {"login_type","social_id"})
        },
        indexes = {
                @Index(name = "idx_user_login_type", columnList = "login_type"),
                @Index(name = "idx_user_social_id", columnList = "social_id")
        }
)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "login_type", nullable = false, columnDefinition = "VARCHAR(20)")
    private LoginType loginType;

    @Column(name = "nickname", nullable = false, length = 15)
    private String nickname;

    @Column(name = "email", nullable = false, length = 254)
    private String email;

    @Column(name = "login_pw", length = 100)  // BCrypt(60) or Argon2(95) 수용
    private String loginPw;

    // 경매지수(혹은 매너지수)
    @Builder.Default
    @Column(name = "manner_score", nullable = false)
    private Integer mannerScore = 50;

    // 반영할 거리 설정(1: 가까움, 2: 조금 먼, 3: 먼)
    @Enumerated(EnumType.STRING)
    @Column(name = "range_setting")
    private rangeSetting rangeSetting;

    @Column(name = "profile_image", length = 200)
    private String profileImage;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "alarm_checked", nullable = false, columnDefinition = "VARCHAR(20)")
    private AlarmChecked alarmChecked = AlarmChecked.NO;

    @Column(name = "social_id")
    private String socialId;

    public enum rangeSetting {
        가까움, 조금먼, 먼
    }
}
