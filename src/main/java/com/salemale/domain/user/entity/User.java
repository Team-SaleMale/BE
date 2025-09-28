package com.salemale.domain.user.entity;

import com.salemale.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "login_type", nullable = false)
    private LoginType loginType = LoginType.LOCAL;

    @Column(name = "nickname", nullable = false, length = 15)
    private String nickname;

    @Column(name = "email", nullable = false, length = 30)
    private String email;

    @Column(name = "login_pw", nullable = false, length = 30)
    private String loginPw;

    @Column(name = "exchange_score", nullable = false)
    private Short exchangeScore = 0;

    @Column(name = "max_range")
    private Integer maxRange;

    @Column(name = "profile_image", length = 200)
    private String profileImage;

    @Enumerated(EnumType.STRING)
    @Column(name = "alarm_checked", nullable = false)
    private AlarmChecked alarmChecked = AlarmChecked.NO;

    @Column(name = "social_id")
    private Long socialId;

    public User(String nickname, String email, String loginPw, LoginType loginType) {
        this.nickname = nickname;
        this.email = email;
        this.loginPw = loginPw;
        this.loginType = loginType;
    }

    public static User of(String nickname, String email, String loginPw, LoginType loginType) {
        return new User(nickname, email, loginPw, loginType);
    }

    public static User of(String nickname, String email, String profileImage, LoginType loginType, Long socialId) {
        User user = new User();
        user.nickname = nickname;
        user.email = email;
        user.profileImage = profileImage;
        user.loginType = loginType;
        user.socialId = socialId;
        return user;
    }

    public void updateProfile(String nickname, String profileImage, Integer maxRange) {
        this.nickname = nickname;
        this.profileImage = profileImage;
        this.maxRange = maxRange;
    }

    public void updateExchangeScore(Short exchangeScore) {
        this.exchangeScore = exchangeScore;
    }

    public void updateAlarmChecked(AlarmChecked alarmChecked) {
        this.alarmChecked = alarmChecked;
    }

    // Getter
    public Long getId() {
        return id;
    }

    public LoginType getLoginType() {
        return loginType;
    }

    public String getNickname() {
        return nickname;
    }

    public String getEmail() {
        return email;
    }

    public String getLoginPw() {
        return loginPw;
    }

    public Short getExchangeScore() {
        return exchangeScore;
    }

    public Integer getMaxRange() {
        return maxRange;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public AlarmChecked getAlarmChecked() {
        return alarmChecked;
    }

    public Long getSocialId() {
        return socialId;
    }

    // Setter
    public void setId(Long id) {
        this.id = id;
    }

    public void setLoginType(LoginType loginType) {
        this.loginType = loginType;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setLoginPw(String loginPw) {
        this.loginPw = loginPw;
    }

    public void setExchangeScore(Short exchangeScore) {
        this.exchangeScore = exchangeScore;
    }

    public void setMaxRange(Integer maxRange) {
        this.maxRange = maxRange;
    }

    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    public void setAlarmChecked(AlarmChecked alarmChecked) {
        this.alarmChecked = alarmChecked;
    }

    public void setSocialId(Long socialId) {
        this.socialId = socialId;
    }

    public enum LoginType {
        LOCAL, KAKAO, NAVER
    }

    public enum AlarmChecked {
        NO, KAKAO, EMAIL, PHONE
    }
}
