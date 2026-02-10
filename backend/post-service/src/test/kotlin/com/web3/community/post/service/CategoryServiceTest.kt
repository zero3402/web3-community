package com.web3.community.post.service

import com.web3.community.common.exception.BusinessException
import com.web3.community.common.exception.ErrorCode
import com.web3.community.post.document.Category
import com.web3.community.post.dto.CategoryRequest
import com.web3.community.post.repository.CategoryRepository
import io.mockk.*
import io.mockk.impl.annotations.MockK
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.*

class CategoryServiceTest {

    @MockK
    private lateinit var categoryRepository: CategoryRepository

    private lateinit var categoryService: CategoryService

    private val testCategory = Category(id = "cat1", name = "General", description = "General category", displayOrder = 0)

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
        categoryService = CategoryService(categoryRepository)
    }

    @Test
    fun `getAllCategories should return active categories`() {
        every { categoryRepository.findByActiveTrueOrderByDisplayOrderAsc() } returns listOf(testCategory)

        val result = categoryService.getAllCategories()

        assertEquals(1, result.size)
        assertEquals("General", result[0].name)
    }

    @Test
    fun `createCategory should save and return category`() {
        every { categoryRepository.save(any()) } returns testCategory

        val request = CategoryRequest(name = "General", description = "General category", displayOrder = 0)
        val result = categoryService.createCategory(request)

        assertEquals("General", result.name)
        verify { categoryRepository.save(any()) }
    }

    @Test
    fun `updateCategory should update fields`() {
        every { categoryRepository.findById("cat1") } returns Optional.of(testCategory)
        every { categoryRepository.save(any()) } returns testCategory

        val request = CategoryRequest(name = "Updated", description = "Updated desc", displayOrder = 1)
        val result = categoryService.updateCategory("cat1", request)

        assertNotNull(result)
        verify { categoryRepository.save(any()) }
    }

    @Test
    fun `updateCategory should throw for non-existent category`() {
        every { categoryRepository.findById("none") } returns Optional.empty()

        val request = CategoryRequest(name = "Updated", displayOrder = 0)

        val exception = assertThrows<BusinessException> { categoryService.updateCategory("none", request) }
        assertEquals(ErrorCode.CATEGORY_NOT_FOUND, exception.errorCode)
    }

    @Test
    fun `deleteCategory should soft-delete`() {
        every { categoryRepository.findById("cat1") } returns Optional.of(testCategory)
        every { categoryRepository.save(any()) } returns testCategory

        categoryService.deleteCategory("cat1")

        verify { categoryRepository.save(match { !it.active }) }
    }

    @Test
    fun `getCategoryById should return category`() {
        every { categoryRepository.findById("cat1") } returns Optional.of(testCategory)

        val result = categoryService.getCategoryById("cat1")

        assertEquals("General", result.name)
    }

    @Test
    fun `getCategoryById should throw for non-existent category`() {
        every { categoryRepository.findById("none") } returns Optional.empty()

        val exception = assertThrows<BusinessException> { categoryService.getCategoryById("none") }
        assertEquals(ErrorCode.CATEGORY_NOT_FOUND, exception.errorCode)
    }
}
