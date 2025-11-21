package com.salemale.domain.alarm.dto;

import com.salemale.domain.alarm.entity.Alarm;
import java.time.LocalDateTime;

public class AlarmDtos {

    public record AlarmResponse(
            Long alarmId,
            String content,
            boolean isRead,
            LocalDateTime readAt,
            LocalDateTime createdAt
    ) {
        public static AlarmResponse from(Alarm alarm) {
            return new AlarmResponse(
                    alarm.getAlarmId(),
                    alarm.getContent(),
                    alarm.isRead(),
                    alarm.getReadAt(),
                    alarm.getCreatedAt()
            );
        }
    }

    public record CreateAlarmRequest(
            Long userId,
            String content
    ) { }

    // 단건 읽음 요청이 body가 필요 없다면 생략 가능
    public record MarkReadRequest(
            // (선택적 사용)
            Long alarmId
    ) { }
}
