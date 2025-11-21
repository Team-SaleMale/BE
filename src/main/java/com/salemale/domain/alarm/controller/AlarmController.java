package com.salemale.domain.alarm.controller;

import com.salemale.domain.alarm.dto.AlarmDtos.*;
import com.salemale.domain.alarm.service.AlarmService;
import com.salemale.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/alarms")
@RequiredArgsConstructor
public class AlarmController {

    private final AlarmService alarmService;

    @PostMapping
    public ApiResponse<Void> createAlarm(@RequestBody CreateAlarmRequest req) {
        alarmService.createAlarm(req);
        return ApiResponse.onSuccess();
    }

    @GetMapping
    public ApiResponse<List<AlarmResponse>> getAlarms(@RequestHeader("user-id") Long me) {
        return ApiResponse.onSuccess(alarmService.getUserAlarms(me));
    }

    @GetMapping("/unread-count")
    public ApiResponse<Long> unreadCount(@RequestHeader("user-id") Long me) {
        return ApiResponse.onSuccess(alarmService.unreadCount(me));
    }

    @PatchMapping("/{id}/read")
    public ApiResponse<Void> read(@RequestHeader("user-id") Long me,
                                  @PathVariable Long id) {
        alarmService.markRead(me, id);
        return ApiResponse.onSuccess();
    }

    @PatchMapping("/read-all")
    public ApiResponse<Integer> readAll(@RequestHeader("user-id") Long me) {
        int changed = alarmService.markAllRead(me);
        return ApiResponse.onSuccess(changed);
    }

    // 모두 삭제(soft delete)
    // @DeleteMapping
    // public ApiResponse<Integer> deleteAll(@RequestHeader("user-id") Long me) {
    //     int count = alarmService.deleteAll(me);
    //     return ApiResponse.onSuccess(count);
    // }
}
