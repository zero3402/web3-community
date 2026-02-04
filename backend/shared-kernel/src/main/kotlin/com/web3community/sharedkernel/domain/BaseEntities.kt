package com.web3community.sharedkernel.domain

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

// 생성 시간 추상 클래스
abstract class CreatedAt {
    val createdAt: LocalDateTime = ZonedDateTime.now(ZoneId.of("UTC")).toLocalDateTime()
}

// 업데이트 시간 추상 클래스
abstract class UpdatedAt {
    var updatedAt: LocalDateTime = ZonedDateTime.now(ZoneId.of("UTC")).toLocalDateTime()
        protected set
    
    fun touch() {
        updatedAt = ZonedDateTime.now(ZoneId.of("UTC")).toLocalDateTime()
    }
}

// 생성 및 업데이트 시간 추상 클래스
abstract class Timestamps : CreatedAt(), UpdatedAt()

// 소프트 삭제 가능 추상 클래스
abstract class SoftDeletable : Timestamps() {
    var deletedAt: LocalDateTime? = null
        protected set
    
    val isDeleted: Boolean get() = deletedAt != null
    
    fun delete() {
        deletedAt = ZonedDateTime.now(ZoneId.of("UTC")).toLocalDateTime()
        touch()
    }
    
    fun restore() {
        deletedAt = null
        touch()
    }
}

// 활성화 가능 추상 클래스
abstract class Activatable : Timestamps() {
    var isActive: Boolean = true
        protected set
    
    fun activate() {
        isActive = true
        touch()
    }
    
    fun deactivate() {
        isActive = false
        touch()
    }
}