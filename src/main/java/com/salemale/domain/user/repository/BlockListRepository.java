package com.salemale.domain.user.repository;

import com.salemale.domain.user.entity.BlockList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BlockListRepository extends JpaRepository<BlockList, Long> {

    boolean existsByBlocker_IdAndBlocked_Id(Long blockerId, Long blockedId);

    void deleteByBlocker_IdAndBlocked_Id(Long blockerId, Long blockedId);

    @Query("""
        select bl.blocked.id
        from BlockList bl
        where bl.blocker.id = :blockerId
    """)
    List<Long> findBlockedUserIds(@Param("blockerId") Long blockerId);
}
