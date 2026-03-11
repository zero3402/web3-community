package com.web3community.user.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/**
 * UserUpdateRequest - 사용자 프로필 수정 요청 DTO
 *
 * PUT /users/me 엔드포인트에서 사용자가 자신의 프로필을 수정할 때 사용하는 요청 DTO이다.
 *
 * 수정 가능 필드:
 * - [nickname]: 필수. 2자 이상 30자 이하.
 * - [profileImageUrl]: 선택. null이면 프로필 이미지를 삭제(제거)한다.
 *
 * 수정 불가 필드 (의도적 제외):
 * - email: 이메일 변경은 별도의 인증 프로세스 필요 (현재 미지원)
 * - password: 비밀번호 변경은 별도의 API로 처리 (현재 미지원)
 * - provider, providerId: 인증 제공자 정보는 변경 불가
 * - status: 관리자 전용 API로 처리
 *
 * 유효성 검사:
 * - @NotBlank: null, 빈 문자열, 공백만 있는 문자열 모두 거부
 * - @Size: 최소/최대 길이 제한
 *
 * @property nickname 변경할 닉네임. 2자 이상 30자 이하, 공백 불가.
 * @property profileImageUrl 변경할 프로필 이미지 URL. null이면 이미지 제거.
 */
data class UserUpdateRequest(

    /**
     * 변경할 닉네임.
     *
     * 2자 이상 30자 이하여야 하며 공백만으로 구성될 수 없다.
     * 닉네임 중복 검사는 [com.web3community.user.service.UserService.updateMe]에서 처리한다.
     */
    @field:NotBlank(message = "닉네임은 필수입니다.")
    @field:Size(min = 2, max = 30, message = "닉네임은 2자 이상 30자 이하여야 합니다.")
    val nickname: String,

    /**
     * 변경할 프로필 이미지 URL.
     *
     * null이면 기존 프로필 이미지를 제거한다.
     * 실제 이미지 파일 업로드는 별도의 파일 업로드 서비스에서 처리하며,
     * 여기서는 업로드 완료 후 반환된 URL만 저장한다.
     */
    val profileImageUrl: String? = null,
)
