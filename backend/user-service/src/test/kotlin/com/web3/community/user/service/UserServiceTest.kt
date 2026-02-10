package com.web3.community.user.service

import com.web3.community.common.exception.BusinessException
import com.web3.community.common.exception.ErrorCode
import com.web3.community.user.dto.CreateUserRequest
import com.web3.community.user.dto.UpdateUserRequest
import com.web3.community.user.entity.User
import com.web3.community.user.repository.UserRepository
import io.mockk.*
import io.mockk.impl.annotations.MockK
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.util.*

class UserServiceTest {

    @MockK
    private lateinit var userRepository: UserRepository

    private lateinit var userService: UserService

    private val testUser = User(
        id = 1L,
        nickname = "testuser",
        email = "test@test.com",
        role = "USER",
        active = true
    )

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
        userService = UserService(userRepository)
    }

    @Test
    fun `createUser should create user successfully`() {
        every { userRepository.existsByEmail("test@test.com") } returns false
        every { userRepository.save(any()) } returns testUser

        val request = CreateUserRequest(email = "test@test.com", nickname = "testuser")
        val result = userService.createUser(request)

        assertEquals("test@test.com", result.email)
        assertEquals("testuser", result.nickname)
    }

    @Test
    fun `createUser should throw for duplicate email`() {
        every { userRepository.existsByEmail("test@test.com") } returns true

        val request = CreateUserRequest(email = "test@test.com", nickname = "testuser")

        val exception = assertThrows<BusinessException> { userService.createUser(request) }
        assertEquals(ErrorCode.DUPLICATE_EMAIL, exception.errorCode)
    }

    @Test
    fun `getUserById should return user`() {
        every { userRepository.findById(1L) } returns Optional.of(testUser)

        val result = userService.getUserById(1L)

        assertEquals(1L, result.id)
        assertEquals("testuser", result.nickname)
    }

    @Test
    fun `getUserById should throw for non-existent user`() {
        every { userRepository.findById(999L) } returns Optional.empty()

        val exception = assertThrows<BusinessException> { userService.getUserById(999L) }
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.errorCode)
    }

    @Test
    fun `updateUser should update fields`() {
        every { userRepository.findById(1L) } returns Optional.of(testUser)
        every { userRepository.save(any()) } returns testUser

        val request = UpdateUserRequest(nickname = "updated", bio = "new bio")
        val result = userService.updateUser(1L, request)

        assertNotNull(result)
        verify { userRepository.save(any()) }
    }

    @Test
    fun `getAllUsers should return paginated results`() {
        val pageable = PageRequest.of(0, 20)
        val page = PageImpl(listOf(testUser), pageable, 1)
        every { userRepository.findAll(pageable) } returns page

        val result = userService.getAllUsers(pageable)

        assertEquals(1, result.content.size)
        assertEquals(1L, result.totalElements)
    }

    @Test
    fun `updateUserRole should update role`() {
        every { userRepository.findById(1L) } returns Optional.of(testUser)
        every { userRepository.save(any()) } returns testUser

        val result = userService.updateUserRole(1L, "ADMIN")

        assertNotNull(result)
        verify { userRepository.save(any()) }
    }

    @Test
    fun `deactivateUser should set active to false`() {
        every { userRepository.findById(1L) } returns Optional.of(testUser)
        every { userRepository.save(any()) } returns testUser

        userService.deactivateUser(1L)

        verify { userRepository.save(match { !it.active }) }
    }

    @Test
    fun `deactivateUser should throw for non-existent user`() {
        every { userRepository.findById(999L) } returns Optional.empty()

        val exception = assertThrows<BusinessException> { userService.deactivateUser(999L) }
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.errorCode)
    }
}
