package com.mindbridge.oye.event

import com.mindbridge.oye.domain.Group
import com.mindbridge.oye.domain.User

data class GroupMemberJoinedEvent(val group: Group, val newMember: User)
