package com.web3community.post.repository

import com.web3community.post.entity.Category
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface CategoryRepository : JpaRepository<Category, Long> {
    
    fun findBySlug(slug: String): Optional<Category>
    
    fun findByParentIdOrderByDisplayOrder(parentId: Long?): List<Category>
    
    fun findByIsActiveTrueOrderByDisplayOrder(): List<Category>
    
    fun findByParentIdAndIsActiveTrueOrderByDisplayOrder(parentId: Long?): List<Category>
    
    @Query("SELECT c FROM Category c WHERE c.parentId IS NULL ORDER BY c.displayOrder")
    fun findRootCategories(): List<Category>
    
    @Query("SELECT COUNT(p) FROM Post p WHERE p.category.id = :categoryId AND p.status = 'PUBLISHED'")
    fun countPublishedPostsByCategoryId(@Param("categoryId") categoryId: Long): Long
    
    fun existsBySlug(slug: String): Boolean
    
    fun existsByName(name: String): Boolean
}