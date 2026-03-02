package com.mindbridge.oye.service

import com.mindbridge.oye.domain.CalendarType
import com.mindbridge.oye.domain.Gender
import com.mindbridge.oye.domain.Inquiry
import com.mindbridge.oye.domain.InquiryComment
import com.mindbridge.oye.domain.InquiryStatus
import com.mindbridge.oye.domain.Role
import com.mindbridge.oye.domain.User
import com.mindbridge.oye.dto.InquiryCommentCreateRequest
import com.mindbridge.oye.dto.InquiryCreateRequest
import com.mindbridge.oye.exception.ForbiddenException
import com.mindbridge.oye.exception.InquiryNotFoundException
import com.mindbridge.oye.repository.InquiryCommentRepository
import com.mindbridge.oye.repository.InquiryRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.LocalDate
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class InquiryServiceTest {

    @Mock
    private lateinit var inquiryRepository: InquiryRepository

    @Mock
    private lateinit var inquiryCommentRepository: InquiryCommentRepository

    @InjectMocks
    private lateinit var inquiryService: InquiryService

    private val testUser = User(
        id = 1L,
        name = "테스트유저",
        birthDate = LocalDate.of(1990, 1, 15),
        gender = Gender.MALE,
        calendarType = CalendarType.SOLAR
    )

    private val adminUser = User(
        id = 100L,
        name = "관리자",
        birthDate = LocalDate.of(1985, 5, 10),
        gender = Gender.MALE,
        calendarType = CalendarType.SOLAR,
        role = Role.ADMIN
    )

    private val otherUser = User(
        id = 2L,
        name = "다른유저",
        birthDate = LocalDate.of(1992, 3, 20),
        gender = Gender.FEMALE,
        calendarType = CalendarType.SOLAR
    )

    @Test
    fun `createInquiry - 문의 작성 성공`() {
        val request = InquiryCreateRequest(title = "테스트 문의", content = "문의 내용입니다")
        val savedInquiry = Inquiry(
            id = 1L,
            user = testUser,
            title = request.title,
            content = request.content
        )
        whenever(inquiryRepository.save(any<Inquiry>())).thenReturn(savedInquiry)

        val result = inquiryService.createInquiry(testUser, request)

        assertEquals(1L, result.id)
        assertEquals("테스트 문의", result.title)
        assertEquals("문의 내용입니다", result.content)
        assertEquals(InquiryStatus.PENDING, result.status)
    }

    @Test
    fun `getMyInquiries - 내 문의 목록 페이지네이션 조회 성공`() {
        val inquiries = listOf(
            Inquiry(id = 2L, user = testUser, title = "두번째 문의", content = "내용2"),
            Inquiry(id = 1L, user = testUser, title = "첫번째 문의", content = "내용1")
        )
        val pageable = PageRequest.of(0, 20)
        val page = PageImpl(inquiries, pageable, 2)

        whenever(inquiryRepository.findByUserOrderByCreatedAtDesc(testUser, pageable)).thenReturn(page)

        val result = inquiryService.getMyInquiries(testUser, 0, 20)

        assertEquals(2, result.content.size)
        assertEquals(0, result.page)
        assertEquals(20, result.size)
        assertEquals(2L, result.totalElements)
        assertEquals(1, result.totalPages)
    }


    @Test
    fun `getAllInquiries - 관리자 전체 문의 목록 조회 성공`() {
        val inquiries = listOf(
            Inquiry(id = 2L, user = otherUser, title = "다른유저 문의", content = "내용2"),
            Inquiry(id = 1L, user = testUser, title = "테스트 문의", content = "내용1")
        )
        val pageable = PageRequest.of(0, 20)
        val page = PageImpl(inquiries, pageable, 2)

        whenever(inquiryRepository.findAllWithUserOrderByCreatedAtDesc(pageable)).thenReturn(page)

        val result = inquiryService.getAllInquiries(adminUser, 0, 20)

        assertEquals(2, result.content.size)
        assertEquals(2L, result.totalElements)
    }

    @Test
    fun `getAllInquiries - 관리자가 아닌 사용자가 조회 시 ForbiddenException`() {
        assertThrows<ForbiddenException> {
            inquiryService.getAllInquiries(testUser, 0, 20)
        }
    }

    @Test
    fun `getInquiry - 본인 문의 상세 조회 성공`() {
        val inquiry = Inquiry(id = 1L, user = testUser, title = "테스트 문의", content = "내용")
        whenever(inquiryRepository.findById(1L)).thenReturn(Optional.of(inquiry))
        whenever(inquiryCommentRepository.findByInquiryOrderByCreatedAtAsc(inquiry)).thenReturn(emptyList())

        val result = inquiryService.getInquiry(testUser, 1L)

        assertEquals(1L, result.id)
        assertEquals("테스트 문의", result.title)
        assertEquals(0, result.comments.size)
    }

    @Test
    fun `getInquiry - 관리자는 타인 문의 조회 가능`() {
        val inquiry = Inquiry(id = 1L, user = testUser, title = "테스트 문의", content = "내용")
        whenever(inquiryRepository.findById(1L)).thenReturn(Optional.of(inquiry))
        whenever(inquiryCommentRepository.findByInquiryOrderByCreatedAtAsc(inquiry)).thenReturn(emptyList())

        val result = inquiryService.getInquiry(adminUser, 1L)

        assertEquals(1L, result.id)
    }

    @Test
    fun `getInquiry - 타인 문의 조회 시 ForbiddenException`() {
        val inquiry = Inquiry(id = 1L, user = testUser, title = "테스트 문의", content = "내용")
        whenever(inquiryRepository.findById(1L)).thenReturn(Optional.of(inquiry))

        assertThrows<ForbiddenException> {
            inquiryService.getInquiry(otherUser, 1L)
        }
    }

    @Test
    fun `getInquiry - 존재하지 않는 문의 조회 시 InquiryNotFoundException`() {
        whenever(inquiryRepository.findById(999L)).thenReturn(Optional.empty())

        assertThrows<InquiryNotFoundException> {
            inquiryService.getInquiry(testUser, 999L)
        }
    }

    @Test
    fun `addComment - 관리자 댓글 작성 성공 및 상태 변경`() {
        val inquiry = Inquiry(id = 1L, user = testUser, title = "테스트 문의", content = "내용")
        val request = InquiryCommentCreateRequest(content = "답변 내용입니다")
        val comment = InquiryComment(id = 1L, inquiry = inquiry, admin = adminUser, content = request.content)

        whenever(inquiryRepository.findById(1L)).thenReturn(Optional.of(inquiry))
        whenever(inquiryCommentRepository.save(any<InquiryComment>())).thenReturn(comment)
        whenever(inquiryCommentRepository.findByInquiryOrderByCreatedAtAsc(inquiry)).thenReturn(listOf(comment))

        val result = inquiryService.addComment(adminUser, 1L, request)

        assertEquals(1L, result.id)
        assertEquals(InquiryStatus.ANSWERED, result.status)
        assertEquals(1, result.comments.size)
        assertEquals("답변 내용입니다", result.comments[0].content)
    }

    @Test
    fun `addComment - 관리자가 아닌 사용자가 댓글 작성 시 ForbiddenException`() {
        assertThrows<ForbiddenException> {
            inquiryService.addComment(testUser, 1L, InquiryCommentCreateRequest(content = "댓글"))
        }
    }

    @Test
    fun `addComment - 존재하지 않는 문의에 댓글 작성 시 InquiryNotFoundException`() {
        whenever(inquiryRepository.findById(999L)).thenReturn(Optional.empty())

        assertThrows<InquiryNotFoundException> {
            inquiryService.addComment(adminUser, 999L, InquiryCommentCreateRequest(content = "댓글"))
        }
    }
}
