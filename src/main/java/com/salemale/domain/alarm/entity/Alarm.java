package com.salemale.domain.alarm.entity;

import com.salemale.domain.user.entity.User;
import com.salemale.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "alarm")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Alarm extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "alarm_id")
    private Long alarmId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    public Alarm(User user, String content) {
        this.user = user;
        this.content = content;
    }

    public static Alarm of(User user, String content) {
        return new Alarm(user, content);
    }

    public void updateContent(String content) {
        this.content = content;
    }

    // Getter
    public Long getAlarmId() {
        return alarmId;
    }

    public User getUser() {
        return user;
    }

    public String getContent() {
        return content;
    }

    // Setter
    public void setAlarmId(Long alarmId) {
        this.alarmId = alarmId;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
