package com.web3community.post.repository

import com.web3community.post.entity.Tag
import com.web3community.post.entity.PostTag
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface TagRepository : JpaRepository<Tag, Long> {
    
    fun findBySlug(slug: String): Optional<Tag>
    
    fun findByNameContainingIgnoreCase(name: String): List<Tag>
    
    fun findByPostCountGreaterThanOrderByPostCountDesc(minPostCount: Long): List<Tag>
    
    @Query("SELECT t FROM Tag t JOIN t.posts pt WHERE pt.post.id = :postId")
    fun findByPostId(@Param("postId") postId: Long): List<Tag>
    
    @Query("SELECT t FROM Tag t WHERE t.name IN :names")
    fun findByNameIn(@Param("names") names: List<String>): List<Tag>
    
    @Query("SELECT t FROM Tag t WHERE t.slug IN :slugs")
    fun findBySlugIn(@Param("slugs") slugs: List<String>): List<Tag>
    
    fun existsBySlug(slug: String): Boolean
    
    fun existsByName(name: String): Boolean
}

@Repository
interface PostTagRepository : JpaRepository<PostTag, Long> {
    
    fun findByPostId(postId: Long): List<PostTag>
    
    fun findByTagId(tagId: Long): List<PostTag>
    
    fun findByPostIdAndTagId(postId: Long, tagId: Long): Optional<PostTag>
    
    @Query("SELECT COUNT(pt) FROM PostTag pt WHERE pt.tag.id = :tagId")
    fun countByTagId(@Param("tagId") tagId: Long): Long
    
    @Query("SELECT COUNT(pt) FROM PostTag pt WHERE pt.post.id = :postId")
    fun countByPostId(@Param("postId") postId: Long): Long
}