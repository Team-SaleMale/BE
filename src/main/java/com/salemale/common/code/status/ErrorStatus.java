package com.salemale.common.code.status;

import com.salemale.common.code.BaseErrorCode;
import com.salemale.common.code.ErrorReasonDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorStatus implements BaseErrorCode {
    // 공통 에러
    _INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON500", "서버 에러, 관리자에게 문의 바랍니다."),
    _BAD_REQUEST(HttpStatus.BAD_REQUEST,"COMMON400","잘못된 요청입니다."),
    _UNAUTHORIZED(HttpStatus.UNAUTHORIZED,"COMMON401","인증이 필요합니다."),
    _FORBIDDEN(HttpStatus.FORBIDDEN, "COMMON403", "금지된 요청입니다."),
    _NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON404", "요청한 리소스를 찾을 수 없습니다."),
    _TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "COMMON429", "요청이 너무 많습니다. 잠시 후 다시 시도해주세요."),

    // 테스트용
    TEMP_EXCEPTION(HttpStatus.BAD_REQUEST, "TEMP4001", "테스트용 예외입니다."),

    // 사용자 관련 에러
    USER_NOT_FOUND(HttpStatus.BAD_REQUEST, "USER4001", "사용자가 없습니다."),
    NICKNAME_NOT_EXIST(HttpStatus.BAD_REQUEST, "USER4002", "닉네임은 필수입니다."),
    USER_EMAIL_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "USER4003", "이미 가입된 이메일입니다."),

    // 경매 관련 에러 (나중에 추가)
    AUCTION_NOT_FOUND(HttpStatus.NOT_FOUND, "AUCTION4001", "경매를 찾을 수 없습니다."),
    AUCTION_ALREADY_ENDED(HttpStatus.BAD_REQUEST, "AUCTION4002", "이미 종료된 경매입니다."),

    // 입찰 관련 에러 (나중에 추가)
    BID_AMOUNT_TOO_LOW(HttpStatus.BAD_REQUEST, "BID4001", "최소 입찰 금액보다 낮습니다."),
    BID_SELF_AUCTION(HttpStatus.BAD_REQUEST, "BID4002", "본인 경매에는 입찰할 수 없습니다.")
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    @Override
    public ErrorReasonDTO getReason() {
        return ErrorReasonDTO.builder()
                .message(message)
                .code(code)
                .isSuccess(false)
                .build();
    }

    @Override
    public ErrorReasonDTO getReasonHttpStatus() {
        return ErrorReasonDTO.builder()
                .message(message)
                .code(code)
                .isSuccess(false)
                .httpStatus(httpStatus)
                .build();
    }
}