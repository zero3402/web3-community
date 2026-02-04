package com.web3community.post.repository

import com.web3community.post.entity.PostMetric
import com.web3community.post.entity.MetricType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface PostMetricRepository : JpaRepository<PostMetric, Long> {
    
    fun findByPostIdAndMetricType(postId: Long, metricType: MetricType): Optional<PostMetric>
    
    fun findByPostId(postId: Long): List<PostMetric>
    
    fun findByMetricTypeOrderByCountDesc(metricType: MetricType): List<PostMetric>
    
    @Query("SELECT pm FROM PostMetric pm WHERE pm.post.authorId = :authorId AND pm.metricType = :metricType ORDER BY pm.count DESC")
    fun findByAuthorIdAndMetricTypeOrderByCountDesc(@Param("authorId") authorId: Long, @Param("metricType") metricType: MetricType): List<PostMetric>
    
    @Query("SELECT SUM(pm.count) FROM PostMetric pm WHERE pm.metricType = :metricType")
    fun getTotalCountByMetricType(@Param("metricType") metricType: MetricType): Long?
    
    @Query("SELECT pm.post.id FROM PostMetric pm WHERE pm.metricType = :metricType ORDER BY pm.count DESC")
    fun findTopPostIdsByMetricType(@Param("metricType") metricType: MetricType): List<Long>
}