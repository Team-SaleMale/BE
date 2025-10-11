package com.salemale.domain.user.dto.request; // 닉네임 변경 요청 DTO

import jakarta.validation.constraints.NotBlank; // 빈 문자열 검증
import jakarta.validation.constraints.Size; // 길이 검증
import lombok.AllArgsConstructor; // Lombok: 모든 필드를 받는 생성자 자동 생성
import lombok.Getter; // Lombok: getter 자동 생성
import lombok.NoArgsConstructor; // Lombok: 기본 생성자 자동 생성

/**
 * NicknameUpdateRequest: 사용자 닉네임 변경 요청을 담는 DTO입니다.
 *
 * - 사용자가 프로필 설정에서 닉네임을 수정할 때 사용됩니다.
 * - 닉네임은 1자 이상 15자 이하로 제한됩니다.
 * - 빈 문자열이나 공백만 있는 닉네임은 허용되지 않습니다.
 *
 * 검증 규칙:
 * - @NotBlank: null, 빈 문자열, 공백만 있는 문자열을 거부합니다.
 * - @Size: 최소 1자, 최대 15자로 제한합니다.
 *
 * 사용 예시:
 * PATCH /api/users/nickname
 * {
 *   "nickname": "새로운닉네임"
 * }
 */
@Getter // getter 메서드를 자동으로 생성합니다.
@NoArgsConstructor // 기본 생성자를 자동으로 생성합니다(JSON 역직렬화에 필요).
@AllArgsConstructor // 모든 필드를 받는 생성자를 자동으로 생성합니다.
public class NicknameUpdateRequest {

    /**
     * 새로운 닉네임
     *
     * - 1자 이상 15자 이하여야 합니다.
     * - 빈 문자열이나 공백만 있는 문자열은 허용되지 않습니다.
     * - 앞뒤 공백은 자동으로 제거됩니다(trim).
     */
    @NotBlank(message = "닉네임은 필수입니다.") // null, 빈 문자열, 공백만 있는 문자열을 거부
    @Size(min = 1, max = 15, message = "닉네임은 1자 이상 15자 이하여야 합니다.") // 길이 제한
    private String nickname;
}

