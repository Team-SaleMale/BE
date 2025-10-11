package com.salemale.domain.user.dto.request; // 비밀번호 변경 요청 DTO

import jakarta.validation.constraints.NotBlank; // 빈 문자열 검증
import jakarta.validation.constraints.Pattern; // 정규식 검증
import jakarta.validation.constraints.Size; // 길이 검증
import lombok.AllArgsConstructor; // Lombok: 모든 필드를 받는 생성자 자동 생성
import lombok.Getter; // Lombok: getter 자동 생성
import lombok.NoArgsConstructor; // Lombok: 기본 생성자 자동 생성

/**
 * PasswordUpdateRequest: 비밀번호 변경 요청을 담는 DTO입니다.
 *
 * - 사용자가 현재 비밀번호를 확인한 후 새 비밀번호로 변경할 때 사용됩니다.
 * - 보안을 위해 현재 비밀번호를 먼저 검증합니다.
 * - 새 비밀번호는 8자 이상 50자 이하로 제한됩니다.
 *
 * 검증 규칙:
 * - @NotBlank: null, 빈 문자열, 공백만 있는 문자열을 거부합니다.
 * - @Size: 최소 8자, 최대 50자로 제한합니다.
 *
 * 사용 예시:
 * PATCH /api/users/password
 * {
 *   "currentPassword": "old123456",
 *   "newPassword": "new123456"
 * }
 */
@Getter // getter 메서드를 자동으로 생성합니다.
@NoArgsConstructor // 기본 생성자를 자동으로 생성합니다(JSON 역직렬화에 필요).
@AllArgsConstructor // 모든 필드를 받는 생성자를 자동으로 생성합니다.
public class PasswordUpdateRequest {

    /**
     * 현재 비밀번호
     *
     * - 사용자 본인 확인을 위해 현재 비밀번호를 먼저 검증합니다.
     * - 현재 비밀번호가 일치하지 않으면 변경이 거부됩니다.
     */
    @NotBlank(message = "현재 비밀번호는 필수입니다.") // null, 빈 문자열, 공백만 있는 문자열을 거부
    private String currentPassword;

    /**
     * 새로운 비밀번호
     *
     * - 8자 이상 50자 이하여야 합니다.
     * - 보안 정책: 최소 1개의 대문자, 1개의 소문자, 1개의 숫자, 1개의 특수문자 포함
     * - 특수문자: @$!%*?&#^()_+=- 허용
     * - 서버에서 BCrypt 등의 해시 알고리즘으로 암호화되어 저장됩니다.
     * 
     * 예시:
     * - 올바른 비밀번호: "Pass123!@#", "MyP@ssw0rd", "Secure#Pass1"
     * - 잘못된 비밀번호: "password" (대문자, 숫자, 특수문자 없음), "12345678" (문자 없음)
     */
    @NotBlank(message = "새 비밀번호는 필수입니다.") // null, 빈 문자열, 공백만 있는 문자열을 거부
    @Size(min = 8, max = 50, message = "비밀번호는 8자 이상 50자 이하여야 합니다.") // 길이 제한
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#^()_+=\\-])[A-Za-z\\d@$!%*?&#^()_+=\\-]{8,50}$",
        message = "비밀번호는 대문자, 소문자, 숫자, 특수문자(@$!%*?&#^()_+=-)를 각각 최소 1개 이상 포함해야 합니다."
    )
    private String newPassword;
}

