package com.loanzo.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mediation_meetings")
data class MediationMeetingEntity(
    @PrimaryKey
    val meetingId: String,
    val title: String,
    val agenda: String,
    val loanId: String? = null,
    val complaintId: String? = null,
    val borrowerId: String? = null,
    val borrowerName: String? = null,
    val borrowerPhone: String? = null,
    val lenderId: String? = null,
    val lenderName: String? = null,
    val lenderPhone: String? = null,
    val agentId: String? = null,
    val agentName: String? = null,
    val meetingType: String, // GOOGLE_MEET, IN_PERSON_BRANCH, PHYSICAL_VAULT
    val meetingLinkOrLocation: String, // e.g. https://meet.google.com/loa-nzo-med
    val scheduledDateTime: Long,
    val scheduledTimeSlotStr: String, // e.g. "Tomorrow, 03:30 PM - 04:15 PM"
    val status: String, // SCHEDULED, COMPLETED, CANCELLED, RESCHEDULED
    val adminNotes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
