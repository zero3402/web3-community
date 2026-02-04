package com.web3community.sharedkernel.domain

// 도메인 이벤트 기본 클래스
abstract class DomainEvent {
    val eventId: String = java.util.UUID.randomUUID().toString()
    val occurredAt: java.time.LocalDateTime = java.time.LocalDateTime.now()
    val aggregateId: String
    val aggregateType: String
    
    constructor(aggregateId: String, aggregateType: String) {
        this.aggregateId = aggregateId
        this.aggregateType = aggregateType
    }
}

// 애그리게이트 루트 인터페이스
interface AggregateRoot<T> {
    val id: T
    val domainEvents: List<DomainEvent>
    
    fun addDomainEvent(event: DomainEvent)
    fun clearDomainEvents()
}

// 애그리게이트 루트 기본 구현
abstract class BaseAggregateRoot<T> : AggregateRoot<T> {
    private val _domainEvents = mutableListOf<DomainEvent>()
    
    override val domainEvents: List<DomainEvent>
        get() = _domainEvents.toList()
    
    override fun addDomainEvent(event: DomainEvent) {
        _domainEvents.add(event)
    }
    
    override fun clearDomainEvents() {
        _domainEvents.clear()
    }
}

// 도메인 예외 기본 클래스
abstract class DomainException(message: String, cause: Throwable? = null) : Exception(message, cause)

// 비즈니스 규칙 위반 예외
class BusinessRuleViolationException(message: String) : DomainException(message)

// 도메인 객체 찾을 수 없음 예외
class DomainObjectNotFoundException(entityType: String, id: Any) : DomainException("$entityType with id $id not found")

// 권한 없음 예외
class PermissionDeniedException(message: String) : DomainException(message)

// 유효하지 않은 상태 예외
class InvalidStateException(message: String) : DomainException(message)