package com.salemale.domain.alarm.dto;

import com.salemale.domain.alarm.entity.Alarm;
import java.time.LocalDateTime;
import java.util.List;
import jakarta.validation.constraints.NotNull;

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
            @NotNull Long userId,
            @NotNull String content
    ) { }

    public record DeleteManyRequest(
             @NotNull List<Long> alarmIds
    ) { }
}
