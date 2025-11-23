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
    public ApiResponse<Void> createAlarm(@RequestBody CreateAlarmRequest req) {
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


    @Operation(summary = "알람 모두 읽기", description = "읽지 않은 알람을 모두 읽음.")
    @PatchMapping("/read-all")
    public ApiResponse<Integer> readAll(@RequestHeader("user-id") Long me) {
        int changed = alarmService.markAllRead(me);
        return ApiResponse.onSuccess(changed);
    }

    // 단일 삭제
    @Operation(summary = "알람 단일 삭제", description = "알람 하나를 삭제합니다.")
    @DeleteMapping("/{alarmId}")
    public ApiResponse<Void> deleteOne(
            @RequestHeader("user-id") Long me,
            @PathVariable Long alarmId
    ) {
        alarmService.deleteOne(me, alarmId);
        return ApiResponse.onSuccess();
    }

    // 여러 개 삭제
    @Operation(summary = "알람 여러 개 삭제", description = "요청한 알람 ID 목록을 모두 삭제합니다.")
    @DeleteMapping
    public ApiResponse<Void> deleteMany(
            @RequestHeader("user-id") Long me,
            @RequestBody DeleteManyRequest req
    ) {
        alarmService.deleteMany(me, req.alarmIds());
        return ApiResponse.onSuccess();
    }

    // 전체 삭제
    @Operation(summary = "알람 전체 삭제", description = "현재 사용자의 모든 알람을 삭제합니다.")
    @DeleteMapping("/all")
    public ApiResponse<Void> deleteAll(
            @RequestHeader("user-id") Long me
    ) {
        alarmService.deleteAll(me);
        return ApiResponse.onSuccess();
    }
}
