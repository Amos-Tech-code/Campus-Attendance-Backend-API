package com.amos_tech_code.domain.services

import com.amos_tech_code.api.dtos.requests.StudentLookupRequest
import com.amos_tech_code.api.dtos.response.StudentLookupResponse
import java.util.UUID

interface StudentLookUpService {

    suspend fun lookupStudent(
        lecturerId: UUID,
        request: StudentLookupRequest
    ): StudentLookupResponse

}