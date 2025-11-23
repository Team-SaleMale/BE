package com.salemale.domain.alarm.service;

import com.salemale.domain.alarm.dto.AlarmDtos.*;
import com.salemale.domain.alarm.entity.Alarm;
import com.salemale.domain.alarm.repository.AlarmRepository;
import com.salemale.domain.user.entity.User;
import com.salemale.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
// import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AlarmService {

    private final AlarmRepository alarmRepository;
    private final UserRepository userRepository;

    @Transactional// (propagation = Propagation.REQUIRES_NEW)
    public void createAlarm(CreateAlarmRequest req) {
        User user = userRepository.findById(req.userId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Alarm alarm = Alarm.builder()
                .user(user)
                .content(req.content())
                // isRead 기본값 false / readAt null
                .build();

        alarmRepository.save(alarm);
    }

    public List<AlarmResponse> getUserAlarms(Long userId) {
        return alarmRepository.findByUser_IdOrderByCreatedAtDesc(userId)
                .stream()
                .filter(a -> !a.isDeleted())
                .map(AlarmResponse::from)
                .toList();
    }

    public long unreadCount(Long userId) {
        return alarmRepository.countByUser_IdAndIsReadFalse(userId);
    }

    @Transactional
    public void markRead(Long me, Long alarmId) {
        var alarm = alarmRepository.findById(alarmId)
                .orElseThrow(() -> new IllegalArgumentException("Alarm not found"));
        if (!alarm.getUser().getId().equals(me)) {
            throw new AccessDeniedException("not your alarm");
        }
        alarm.markRead();
    }

    @Transactional
    public int markAllRead(Long me) {
        return alarmRepository.markAllRead(me);
    }

    // (선택) 모두 삭제(soft delete) 기존 메서드가 있었다면 유지
    // @Transactional
    // public int deleteAll(Long userId) { return alarmRepository.softDeleteAllByUser(userId); }
}
