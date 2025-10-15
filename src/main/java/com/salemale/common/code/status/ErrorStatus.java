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
    NICKNAME_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "USER4004", "이미 사용 중인 닉네임입니다."),
    USER_REGION_NOT_SET(HttpStatus.BAD_REQUEST, "USER4005", "활동 동네를 먼저 설정해주세요."),
    PASSWORD_REUSE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "USER4006", "새 비밀번호는 현재 비밀번호와 달라야 합니다."),
    
    // 인증 관련 에러
    AUTH_INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "AUTH4011", "아이디 또는 비밀번호가 올바르지 않습니다."),
    AUTH_NOT_LOCAL_ACCOUNT(HttpStatus.FORBIDDEN, "AUTH4031", "소셜 로그인 계정은 비밀번호를 변경할 수 없습니다."),
    
    // 지역 관련 에러
    REGION_NOT_FOUND(HttpStatus.NOT_FOUND, "REGION4001", "지역을 찾을 수 없습니다."),

    // 경매 물품 관련 에러
    ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "ITEM4001", "경매 물품을 찾을 수 없습니다."),
    ITEM_SELF_LIKE_FORBIDDEN(HttpStatus.BAD_REQUEST, "ITEM4002", "본인 경매 물품은 찜할 수 없습니다."),
    ITEM_ALREADY_LIKED(HttpStatus.BAD_REQUEST, "ITEM4003", "이미 찜한 상품입니다."),
    ITEM_NOT_LIKED(HttpStatus.BAD_REQUEST, "ITEM4004", "찜하지 않은 상품입니다."),
    REGION_NOT_SET(HttpStatus.BAD_REQUEST, "ITEM4005", "상품 등록을 위해 대표 동네가 설정되어 있지 않습니다."),
    INVALID_END_TIME(HttpStatus.BAD_REQUEST, "ITEM4006", "경매 종료 시간이 유효하지 않습니다."),

    // 경매 관련 에러 (나중에 추가)
    AUCTION_NOT_FOUND(HttpStatus.NOT_FOUND, "AUCTION4001", "경매를 찾을 수 없습니다."),
    AUCTION_ALREADY_ENDED(HttpStatus.BAD_REQUEST, "AUCTION4002", "이미 종료된 경매입니다."),
    AUCTION_NOT_BIDDING(HttpStatus.BAD_REQUEST, "AUCTION4003", "현재 입찰할 수 없는 상품입니다."),

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