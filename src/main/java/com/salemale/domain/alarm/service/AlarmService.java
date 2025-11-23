package com.salemale.domain.alarm.service;

import com.salemale.domain.alarm.dto.AlarmDtos.AlarmResponse;
import com.salemale.domain.alarm.dto.AlarmDtos.CreateAlarmRequest;
import com.salemale.domain.alarm.dto.AlarmDtos.DeleteManyRequest;
import com.salemale.domain.alarm.entity.Alarm;
import com.salemale.domain.alarm.repository.AlarmRepository;
import com.salemale.domain.user.entity.User;
import com.salemale.domain.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AlarmService {

    private final AlarmRepository alarmRepository;
    private final UserRepository userRepository;

    // 알람 생성
    @Transactional
    public void createAlarm(CreateAlarmRequest req) {
        User user = userRepository.findById(req.userId())
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));

        Alarm alarm = Alarm.builder()
                .user(user)
                .content(req.content())
                .isRead(false)
                .readAt(null)
                .build();

        alarmRepository.save(alarm);
    }

    // 알람 목록 조회
    public List<AlarmResponse> getUserAlarms(Long userId) {
        return alarmRepository.findActiveByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(AlarmResponse::from)
                .toList();
    }

    // 안 읽은 알람 개수
    public long unreadCount(Long userId) {
        return alarmRepository.countByUser_IdAndIsReadFalseAndDeletedAtIsNull(userId);
    }

    // 전체 읽음 처리
    @Transactional
    public int markAllRead(Long userId) {
        return alarmRepository.markAllRead(userId, LocalDateTime.now());
    }

    // 단일 삭제
    @Transactional
    public void deleteOne(Long userId, Long alarmId) {      // [ADD]
        int updated = alarmRepository.softDeleteOne(userId, alarmId, LocalDateTime.now());
        if (updated == 0) {
            // 내 알람이 아니거나 이미 삭제된 경우
            throw new EntityNotFoundException("삭제할 알람이 없거나 권한이 없습니다.");
        }
    }

    // 여러 개 삭제
    @Transactional
    public void deleteMany(Long userId, List<Long> alarmIds) {  // [ADD]
        if (alarmIds == null || alarmIds.isEmpty()) {
            return;
        }
        alarmRepository.softDeleteMany(userId, alarmIds, LocalDateTime.now());
    }

    // 전체 삭제
    @Transactional
    public void deleteAll(Long userId) {                     // [ADD]
        alarmRepository.softDeleteAll(userId, LocalDateTime.now());
    }
}
