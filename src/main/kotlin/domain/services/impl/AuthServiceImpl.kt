package domain.services.impl

import com.amos_tech_code.config.JwtConfig
import com.amos_tech_code.data.repository.LecturerRepository
import com.amos_tech_code.data.repository.StudentRepository
import com.amos_tech_code.domain.dtos.requests.DeviceInfo
import com.amos_tech_code.domain.dtos.requests.StudentRegistrationRequest
import com.amos_tech_code.domain.dtos.response.LecturerAuthResponse
import api.dtos.response.StudentAuthResponse
import com.amos_tech_code.domain.models.Device
import com.amos_tech_code.domain.models.Lecturer
import com.amos_tech_code.domain.models.Student
import domain.models.UserRole
import com.amos_tech_code.services.AuthService
import com.amos_tech_code.services.GoogleAuthService
import com.amos_tech_code.utils.AppException
import com.amos_tech_code.utils.AuthenticationException
import com.amos_tech_code.utils.ConflictException
import com.amos_tech_code.utils.InternalServerException
import com.amos_tech_code.utils.ResourceNotFoundException
import com.amos_tech_code.utils.ValidationException
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*

class AuthServiceImpl(
    private val lecturerRepository: LecturerRepository,
    private val studentRepository: StudentRepository,
    private val googleAuthService: GoogleAuthService,
) : AuthService {

    private val logger = LoggerFactory.getLogger(AuthServiceImpl::class.java)

    /**
     * Mock implementation of authenticateLecturerWithGoogle for testing
     * Simulates Google authentication without actually calling Google API
     */
    override suspend fun mockAuthenticateLecturerWithGoogle(idToken: String): LecturerAuthResponse {
        try {
            val DEFAULT_LECTURER_EMAIL = "amosk5132@gmail.com"

            if (idToken.isBlank()) throw ValidationException("Google id token is required.")

            // Simulate finding existing lecturer
            val existingLecturer = lecturerRepository.findByEmail(DEFAULT_LECTURER_EMAIL)

            return if (existingLecturer != null) {
                // Simulate updating last login
                lecturerRepository.updateLastLogin(existingLecturer.id, LocalDateTime.now())

                LecturerAuthResponse(
                    token = JwtConfig.generateToken(existingLecturer.id.toString(), UserRole.LECTURER),
                    name = existingLecturer.name ?: "Unknown",
                    email = existingLecturer.email,
                    profileComplete = existingLecturer.isProfileComplete,
                    userType = UserRole.LECTURER
                )
            } else {
                throw ResourceNotFoundException("User not found")
            }
        } catch (ex: Exception) {
            when (ex) {
                is AppException -> throw ex
                else -> throw InternalServerException("An unknown error occurred: Mock implementation of authenticateLecturerWithGoogle.")
            }
        }
    }

    override suspend fun authenticateLecturerWithGoogle(idToken: String): LecturerAuthResponse {
        try {
            if (idToken.isBlank()) throw ValidationException("Google id token is required.")
            val googleUser = googleAuthService.validateGoogleToken(idToken)
                ?: throw AuthenticationException(
                    "Unable to verify your Google account. Please try again."
                )

            val existingLecturer = lecturerRepository.findByEmail(googleUser.email)

            return if (existingLecturer != null) {
                // Update last login
                lecturerRepository.updateLastLogin(existingLecturer.id, LocalDateTime.now())

                LecturerAuthResponse(
                    token = JwtConfig.generateToken(existingLecturer.id.toString(), UserRole.LECTURER),
                    name = existingLecturer.name ?: "Unknown",
                    email = existingLecturer.email,
                    profileComplete = existingLecturer.isProfileComplete,
                    userType = UserRole.LECTURER
                )
            } else {
                // Create new lecturer
                val newLecturer = Lecturer(
                    id = UUID.randomUUID(),
                    email = googleUser.email,
                    name = googleUser.name,
                    isProfileComplete = false,
                    createdAt = LocalDateTime.now(),
                    updatedAt = LocalDateTime.now()
                )

                val savedLecturer = lecturerRepository.create(newLecturer)

                LecturerAuthResponse(
                    token = JwtConfig.generateToken(savedLecturer.id.toString(), UserRole.LECTURER),
                    name = savedLecturer.name ?: "Unknown",
                    email = savedLecturer.email,
                    profileComplete = false,
                    userType = UserRole.LECTURER
                )
            }
        } catch (ex: Exception) {
            logger.error("Lecturer sign in failed: $ex")
            when(ex) {
                is AppException -> throw ex
                else -> throw InternalServerException("An unknown error occurred while verifying your google account.")
            }
        }
    }

    override suspend fun registerStudent(request: StudentRegistrationRequest): StudentAuthResponse {
        try {
            request.validate()
            request.deviceInfo.validate()
            // Check if student already exists
            if (studentRepository.findByRegistrationNumber(request.registrationNumber) != null) {
                throw ConflictException(
                    "Student with registration number ${request.registrationNumber} already exists."
                )
            }

            // Create student with device
            val studentId = generateUUID()
            val now = LocalDateTime.now()
            // Add device
            val device = Device(
                id = generateUUID(),
                deviceId = request.deviceInfo.deviceId,
                model = request.deviceInfo.model,
                os = request.deviceInfo.os,
                fcmToken = request.deviceInfo.fcmToken,
                lastSeen = now,
                createdAt = now
            )
            val newStudent = Student(
                id = studentId,
                registrationNumber = request.registrationNumber,
                fullName = request.fullName,
                createdAt = now,
                device = device,
            )

            val savedStudent = studentRepository.createStudentWithDevice(newStudent)

            return StudentAuthResponse(
                token = JwtConfig.generateToken(savedStudent.id.toString(), UserRole.STUDENT),
                userType = UserRole.STUDENT,
                fullName = savedStudent.fullName,
                regNumber = savedStudent.registrationNumber,
            )
        } catch (ex: Exception) {
            logger.error("Student registration failed: $ex")
            when(ex) {
                is AppException -> throw ex
                else -> throw InternalServerException("An error occurred during student registration.")
            }
        }

    }

    override suspend fun loginStudent(
        registrationNumber: String,
        deviceInfo: DeviceInfo
    ): StudentAuthResponse {
        try {
            if (registrationNumber.isBlank()) throw ValidationException("Registration number is required.")
            deviceInfo.validate()

            val student = studentRepository.findByRegistrationNumber(registrationNumber)
                ?: throw ResourceNotFoundException(
                    "Student with registration number $registrationNumber not found. Please check your registration number or register first."
                )

            val registeredDevice = studentRepository.findDeviceByStudentId(student.id)
                ?: // Student exists but device record missing — this should not normally happen.
                throw IllegalStateException("No device registered for student: ${student.registrationNumber}")

            if (registeredDevice.deviceId == deviceInfo.deviceId) {
                // Correct device → update lastSeen
                studentRepository.updateDeviceLastSeen(registeredDevice.deviceId, LocalDateTime.now())
            } else {
                // Different device → flag suspicious
                studentRepository.flagSuspiciousLogin(student.id, deviceInfo)
            }

            // Always update last login timestamp
            studentRepository.updateLastLogin(student.id, LocalDateTime.now())

            return StudentAuthResponse(
                token = JwtConfig.generateToken(student.id.toString(), UserRole.STUDENT),
                userType = UserRole.STUDENT,
                fullName = student.fullName,
                regNumber = student.registrationNumber
            )

        } catch (ex: Exception) {
            logger.error("Student login failed: $ex")
            when(ex) {
                is AppException -> throw ex
                else -> throw InternalServerException("An error occurred during student login. Please try again.")
            }
        }
    }

    fun generateUUID() : UUID {
        return UUID.randomUUID()
    }

    fun StudentRegistrationRequest.validate() {
        if (registrationNumber.isBlank()) throw ValidationException("Registration number is required.")
        if (fullName.isBlank()) throw ValidationException("Full name is required.")
        deviceInfo.validate()
    }

    fun DeviceInfo.validate() {
        if (deviceId.isBlank()) throw ValidationException("Device ID is required.")
        if (model.isBlank()) throw ValidationException("Device model is required.")
        if (os.isBlank()) throw ValidationException("Device Information is required.")
    }


}