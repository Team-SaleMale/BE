package com.salemale.domain.alarm.controller;

import com.salemale.domain.alarm.dto.AlarmDtos.*;
import com.salemale.domain.alarm.service.AlarmService;
import com.salemale.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/alarms")
@RequiredArgsConstructor
public class AlarmController {

    private final AlarmService alarmService;

    @Operation(summary = "알람 보내기(test)", description = "알람을 생성합니다.")
    @PostMapping
    public ApiResponse<Void> createAlarm(@Valid @RequestBody CreateAlarmRequest req) {
        alarmService.createAlarm(req);
        return ApiResponse.onSuccess();
    }

    @Operation(summary = "알람 목록 조회", description = "받은 모든 알람 조회.")
    @GetMapping
    public ApiResponse<List<AlarmResponse>> getAlarms(@RequestHeader("user-id") Long me) {
        return ApiResponse.onSuccess(alarmService.getUserAlarms(me));
    }

    @Operation(summary = "미읽음 알람 개수 조회", description = "읽지 않은 알람의 개수를 조회.")
    @GetMapping("/unread-count")
    public ApiResponse<Long> unreadCount(@RequestHeader("user-id") Long me) {
        return ApiResponse.onSuccess(alarmService.unreadCount(me));
    }

    @Operation(summary = "단일 알람 읽음(test)", description = "플래그 테스트용.")
    @PatchMapping("/{id}/read")
    public ApiResponse<Void> read(@RequestHeader("user-id") Long me,
                                  @PathVariable Long id) {
        alarmService.markRead(me, id);
        return ApiResponse.onSuccess();
    }

    @Operation(summary = "알람 모두 읽기", description = "읽지 않은 알람을 모두 읽음.")
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
