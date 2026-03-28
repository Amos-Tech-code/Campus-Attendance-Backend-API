package domain.dtos.requests

import kotlinx.serialization.Serializable

@Serializable
data class AddAcademicTermRequest(
    val academicYear: String,
    val semester: Int,
    val weekCount: Int = 14
)

@Serializable
data class AddProgrammeWithUnitsRequest(
    val name: String,
    val departmentId: String? = null,  // Optional - if not provided, create new department
    val departmentName: String? = null,  // Required if departmentId is null
    val yearOfStudy: Int,
    val expectedStudentCount: Int,
    val units: List<AddUnitToProgrammeRequest> // At least one unit required
)

@Serializable
data class UpdateProgrammeDetailsRequest(
    val name: String? = null,
    val yearOfStudy: Int? = null,
    val expectedStudentCount: Int? = null,
    val isActive: Boolean? = null
)

@Serializable
data class AddUnitToProgrammeRequest(
    val code: String,
    val name: String,
    val semester: Int,
    val departmentId: String? = null,
    val lectureDay: String? = null,
    val lectureTime: String? = null,
    val lectureVenue: String? = null
)