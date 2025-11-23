package com.salemale.domain.alarm.repository;

import com.salemale.domain.alarm.entity.Alarm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlarmRepository extends JpaRepository<Alarm, Long> {

    List<Alarm> findByUser_IdOrderByCreatedAtDesc(Long userId);

    long countByUser_IdAndIsReadFalse(Long userId);

    @Modifying
    @Query("""update Alarm a set a.isRead = true, a.readAt = :readAt
where a.user.id = :userId and a.isRead = false and a.deletedAt is null
""")
int markAllRead(@Param("userId") Long userId,
                @Param("readAt") LocalDateTime readAt);
}
