package com.salemale.domain.chat.repository.projection;

import java.time.LocalDateTime;

public interface ChatSummaryRow {
    Long getChatId();
    Long getPartnerId();
    String getPartnerNickname();
    String getPartnerProfileImage();

    String getLastContent();       // nullable
    String getLastType();          // nullable (TEXT/IMAGE/URL)
    LocalDateTime getLastSentAt(); // nullable

    Long getUnreadCount();      // not null

    Long getItemId();
    String getItemTitle();
    String getItemImageUrl();
    Integer getWinningPrice();
}
