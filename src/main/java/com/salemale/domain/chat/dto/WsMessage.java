package com.salemale.domain.chat.dto;

import com.salemale.domain.chat.entity.Message;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * STOMP 메시지 페이로드 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WsMessage {
    private String content;
    private Message.MessageType type;
}
