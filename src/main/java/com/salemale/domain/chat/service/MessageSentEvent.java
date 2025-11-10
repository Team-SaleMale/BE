package com.salemale.domain.chat.service;

import com.salemale.domain.chat.dto.MessageDtos.MessageResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class MessageSentEvent {
    private final MessageResponse payload;
}
