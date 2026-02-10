package com.web3.community.post.repository

import com.web3.community.post.document.Category
import org.springframework.data.mongodb.repository.MongoRepository

interface CategoryRepository : MongoRepository<Category, String> {
    fun findByActiveTrueOrderByDisplayOrderAsc(): List<Category>
}
