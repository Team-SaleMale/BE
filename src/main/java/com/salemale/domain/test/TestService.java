package com.salemale.domain.test;

import com.salemale.common.code.status.ErrorStatus;
import com.salemale.common.exception.GeneralException;
import org.springframework.stereotype.Service;

@Service
public class TestService {

    public String getSuccessData() {
        // 비즈니스 로직 예시
        return "우리 동네 실시간 경매 플랫폼 - 서비스 단에서 처리한 데이터";
    }

    public void throwError() {
        // 예외 발생 로직
        throw new GeneralException(ErrorStatus._BAD_REQUEST);
    }
}