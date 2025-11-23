package com.salemale.domain.alarm.repository;

import com.salemale.domain.alarm.entity.Alarm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AlarmRepository extends JpaRepository<Alarm, Long> {

    // 사용자 알람 목록 (삭제 안 된 것만)
    @Query("""
           select a 
           from Alarm a
           where a.user.id = :userId
             and a.deletedAt is null
           order by a.createdAt desc
           """)
    List<Alarm> findActiveByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    // 안 읽은 알람 개수 (삭제 안 된 것만)
    long countByUser_IdAndIsReadFalseAndDeletedAtIsNull(Long userId);

    // 전체 읽음 처리
    @Modifying
    @Query("""
           update Alarm a 
              set a.isRead = true, a.readAt = :readAt
           where a.user.id = :userId
             and a.isRead = false
             and a.deletedAt is null
           """)
    int markAllRead(@Param("userId") Long userId,
                    @Param("readAt") LocalDateTime readAt);

    // 단일 알람 soft delete
    @Modifying                                         // [ADD]
    @Query("""
           update Alarm a
              set a.deletedAt = :deletedAt
           where a.alarmId = :alarmId
             and a.user.id = :userId
             and a.deletedAt is null
           """)
    int softDeleteOne(@Param("userId") Long userId,
                      @Param("alarmId") Long alarmId,
                      @Param("deletedAt") LocalDateTime deletedAt);

    // 여러 개 soft delete
    @Modifying                                         // [ADD]
    @Query("""
           update Alarm a
              set a.deletedAt = :deletedAt
           where a.user.id = :userId
             and a.alarmId in :alarmIds
             and a.deletedAt is null
           """)
    int softDeleteMany(@Param("userId") Long userId,
                       @Param("alarmIds") List<Long> alarmIds,
                       @Param("deletedAt") LocalDateTime deletedAt);

    // 전체 soft delete
    @Modifying                                         // [ADD]
    @Query("""
           update Alarm a
              set a.deletedAt = :deletedAt
           where a.user.id = :userId
             and a.deletedAt is null
           """)
    int softDeleteAll(@Param("userId") Long userId,
                      @Param("deletedAt") LocalDateTime deletedAt);
}
