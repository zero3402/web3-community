package com.web3community.post.entity

import jakarta.persistence.*

@Entity
@Table(name = "post_metrics")
data class PostMetric(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    val post: com.web3community.post.entity.Post,
    
    @Column(nullable = false)
    val metricType: MetricType,
    
    @Column(nullable = false)
    val count: Long = 0,
    
    @Column(name = "last_updated_at", nullable = false)
    val lastUpdatedAt: java.time.LocalDateTime = java.time.LocalDateTime.now()
)

enum class MetricType {
    VIEWS,
    LIKES,
    SHARES,
    COMMENTS,
    BOOKMARKS,
    REPORTS
}