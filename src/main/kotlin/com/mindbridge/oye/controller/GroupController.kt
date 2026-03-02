package com.mindbridge.oye.controller

import com.mindbridge.oye.config.AuthenticationResolver
import com.mindbridge.oye.controller.api.GroupApi
import com.mindbridge.oye.dto.ApiResponse
import com.mindbridge.oye.dto.CreateGroupRequest
import com.mindbridge.oye.dto.GroupCompatibilityResponse
import com.mindbridge.oye.dto.GroupDetailResponse
import com.mindbridge.oye.dto.GroupSummaryResponse
import com.mindbridge.oye.dto.GroupTodayCompatibilityResponse
import com.mindbridge.oye.dto.JoinGroupRequest
import com.mindbridge.oye.dto.UpdateGroupRequest
import com.mindbridge.oye.service.GroupService
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/groups")
class GroupController(
    private val groupService: GroupService,
    private val authenticationResolver: AuthenticationResolver
) : GroupApi {

    @PostMapping
    override fun createGroup(
        @AuthenticationPrincipal principal: Any?,
        @RequestBody request: CreateGroupRequest
    ): ApiResponse<GroupSummaryResponse> {
        val user = authenticationResolver.getCurrentUser(principal)
        return ApiResponse.success(groupService.createGroup(user, request))
    }

    @PostMapping("/join")
    override fun joinGroup(
        @AuthenticationPrincipal principal: Any?,
        @RequestBody request: JoinGroupRequest
    ): ApiResponse<GroupSummaryResponse> {
        val user = authenticationResolver.getCurrentUser(principal)
        return ApiResponse.success(groupService.joinGroup(user, request))
    }

    @GetMapping
    override fun getMyGroups(
        @AuthenticationPrincipal principal: Any?
    ): ApiResponse<List<GroupSummaryResponse>> {
        val user = authenticationResolver.getCurrentUser(principal)
        return ApiResponse.success(groupService.getMyGroups(user))
    }

    @GetMapping("/{id}")
    override fun getGroupDetail(
        @AuthenticationPrincipal principal: Any?,
        @PathVariable id: Long
    ): ApiResponse<GroupDetailResponse> {
        val user = authenticationResolver.getCurrentUser(principal)
        return ApiResponse.success(groupService.getGroupDetail(user, id))
    }

    @PatchMapping("/{id}")
    override fun updateGroup(
        @AuthenticationPrincipal principal: Any?,
        @PathVariable id: Long,
        @RequestBody request: UpdateGroupRequest
    ): ApiResponse<GroupDetailResponse> {
        val user = authenticationResolver.getCurrentUser(principal)
        return ApiResponse.success(groupService.updateGroup(user, id, request))
    }

    @DeleteMapping("/{id}")
    override fun deleteGroup(
        @AuthenticationPrincipal principal: Any?,
        @PathVariable id: Long
    ) {
        val user = authenticationResolver.getCurrentUser(principal)
        groupService.deleteGroup(user, id)
    }

    @PostMapping("/{id}/leave")
    override fun leaveGroup(
        @AuthenticationPrincipal principal: Any?,
        @PathVariable id: Long
    ) {
        val user = authenticationResolver.getCurrentUser(principal)
        groupService.leaveGroup(user, id)
    }

    @DeleteMapping("/{id}/members/{userId}")
    override fun kickMember(
        @AuthenticationPrincipal principal: Any?,
        @PathVariable id: Long,
        @PathVariable userId: Long
    ) {
        val user = authenticationResolver.getCurrentUser(principal)
        groupService.kickMember(user, id, userId)
    }

    @GetMapping("/{id}/compatibility")
    override fun getGroupTodayCompatibility(
        @AuthenticationPrincipal principal: Any?,
        @PathVariable id: Long
    ): ApiResponse<GroupTodayCompatibilityResponse> {
        val user = authenticationResolver.getCurrentUser(principal)
        return ApiResponse.success(groupService.getGroupTodayCompatibility(user, id))
    }

    @GetMapping("/{id}/compatibility/{userId}")
    override fun getGroupPairCompatibility(
        @AuthenticationPrincipal principal: Any?,
        @PathVariable id: Long,
        @PathVariable userId: Long
    ): ApiResponse<GroupCompatibilityResponse> {
        val user = authenticationResolver.getCurrentUser(principal)
        return ApiResponse.success(groupService.getGroupPairCompatibility(user, id, userId))
    }
}
