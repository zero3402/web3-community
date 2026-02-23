package com.web3.community.post.repository

import com.web3.community.post.entity.Category
import org.springframework.data.jpa.repository.JpaRepository

interface CategoryRepository : JpaRepository<Category, Long> {
    fun findByActiveTrueOrderByDisplayOrderAsc(): List<Category>
}
