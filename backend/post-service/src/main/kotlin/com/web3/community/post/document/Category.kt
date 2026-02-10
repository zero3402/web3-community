package com.web3.community.post.document

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "categories")
data class Category(
    @Id
    val id: String? = null,
    var name: String,
    var description: String? = null,
    var displayOrder: Int = 0,
    var active: Boolean = true
)
