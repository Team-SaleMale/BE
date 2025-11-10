package com.salemale.domain.hotdeal.entity;

/**
 * 핫딜 가게 승인 상태
 * - PENDING: 승인 대기 중
 * - APPROVED: 승인됨
 * - REJECTED: 거절됨
 */
public enum ApprovalStatus {
    PENDING,   // 승인 대기
    APPROVED,  // 승인됨
    REJECTED   // 거절됨
}