package api.dtos.requests

import kotlinx.serialization.Serializable

@Serializable
data class LecturerMarkAttendanceRequest(
    val sessionCode: String,
    val unitCode: String,
    val studentRegNo: String,
    
)