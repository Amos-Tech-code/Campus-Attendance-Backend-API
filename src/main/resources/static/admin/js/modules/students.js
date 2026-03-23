// src/main/resources/static/admin/js/modules/students.js
// Student Management Module

// Module state
let studentsCurrentPage = 1;
let studentsCurrentSearch = '';
let studentsCurrentStatus = '';
let studentsPageSize = 20;

// Initialize module
window.initStudents = async function() {
    console.log('=== initStudents START ===');
    await loadStudentsData();
    console.log('=== initStudents END ===');

    // Add event listener for enter key in search
    const searchInput = document.getElementById('searchStudent');
    if (searchInput) {
        searchInput.addEventListener('keypress', function(e) {
            if (e.key === 'Enter') {
                studentsSearchStudents();
            }
        });
    }
};

async function loadStudentsData() {
    const tbody = document.getElementById('students-table-body');
    if (!tbody) {
        console.error('students-table-body not found!');
        return;
    }

    tbody.innerHTML = '<tr><td colspan="9" class="loading-text">Loading...</td></tr>';

    try {
        let url = `/admin/api/students?page=${studentsCurrentPage}&pageSize=${studentsPageSize}`;
        if (studentsCurrentSearch) url += `&search=${encodeURIComponent(studentsCurrentSearch)}`;
        if (studentsCurrentStatus) url += `&status=${studentsCurrentStatus}`;

        const response = await fetchWithAuth(url);
        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(`Failed to load students: ${errorText}`);
        }

        const data = await response.json();
        console.log('Students loaded:', data);

        studentsRenderTable(data.students);
        studentsRenderPagination(data);

    } catch (err) {
        console.error('Failed to load students:', err);
        tbody.innerHTML = `<tr><td colspan="9" class="error-text">Failed to load students: ${err.message}<br><button onclick="loadStudentsData()">Retry</button></td></tr>`;
    }
}

function studentsRenderTable(students) {
    const tbody = document.getElementById('students-table-body');
    if (!tbody) return;

    if (!students || students.length === 0) {
        tbody.innerHTML = '<tr><td colspan="9" class="empty-text">No students found</td></tr>';
        return;
    }

    tbody.innerHTML = students.map(student => {
        const primaryEnrollment = student.enrollments && student.enrollments.length > 0 ? student.enrollments[0] : null;

        return `
            <tr>
                <td><strong>${escapeHtml(student.registrationNumber)}</strong></td>
                <td>${escapeHtml(student.fullName)}</td>
                <td>${primaryEnrollment ? escapeHtml(primaryEnrollment.programmeName) : 'N/A'}</td>
                <td>${primaryEnrollment ? escapeHtml(primaryEnrollment.universityName) : 'N/A'}</td>
                <td>${primaryEnrollment ? `Year ${primaryEnrollment.yearOfStudy}` : 'N/A'}</td>
                <td><span class="device-count">📱 ${student.devices}</span></td>
                <td>
                    ${student.isActive
                        ? '<span class="status-active">✅ Active</span>'
                        : '<span class="status-inactive">❌ Inactive</span>'}
                </td>
                <td>${student.lastLoginAt ? new Date(student.lastLoginAt).toLocaleString() : 'Never'}</td>
                <td>
                    <button class="btn-warning" onclick="studentsViewStudent('${student.id}')">View</button>
                    ${!student.isActive
                        ? `<button class="btn-primary" onclick="studentsActivateStudent('${student.id}')">Activate</button>`
                        : `<button class="btn-danger" onclick="studentsDeactivateStudent('${student.id}')">Deactivate</button>`
                    }
                </td>
            </tr>
        `;
    }).join('');
}

function studentsRenderPagination(data) {
    const paginationDiv = document.getElementById('pagination');
    if (!paginationDiv) return;

    const totalPages = Math.ceil(data.total / data.pageSize);
    if (totalPages <= 1) {
        paginationDiv.innerHTML = '';
        return;
    }

    let pagesHtml = '<div class="pagination-controls">';

    if (studentsCurrentPage > 1) {
        pagesHtml += `<button onclick="studentsGoToPage(${studentsCurrentPage - 1})" class="page-btn">← Previous</button>`;
    }

    const startPage = Math.max(1, studentsCurrentPage - 2);
    const endPage = Math.min(totalPages, studentsCurrentPage + 2);

    if (startPage > 1) {
        pagesHtml += `<button onclick="studentsGoToPage(1)" class="page-btn">1</button>`;
        if (startPage > 2) pagesHtml += `<span class="page-dots">...</span>`;
    }

    for (let i = startPage; i <= endPage; i++) {
        pagesHtml += `<button onclick="studentsGoToPage(${i})" class="page-btn ${i === studentsCurrentPage ? 'active' : ''}">${i}</button>`;
    }

    if (endPage < totalPages) {
        if (endPage < totalPages - 1) pagesHtml += `<span class="page-dots">...</span>`;
        pagesHtml += `<button onclick="studentsGoToPage(${totalPages})" class="page-btn">${totalPages}</button>`;
    }

    if (studentsCurrentPage < totalPages) {
        pagesHtml += `<button onclick="studentsGoToPage(${studentsCurrentPage + 1})" class="page-btn">Next →</button>`;
    }

    pagesHtml += '</div>';
    paginationDiv.innerHTML = pagesHtml;
}

function studentsGoToPage(page) {
    studentsCurrentPage = page;
    loadStudentsData();
}

function studentsSearchStudents() {
    const searchInput = document.getElementById('searchStudent');
    if (searchInput) {
        studentsCurrentSearch = searchInput.value;
    }
    studentsCurrentPage = 1;
    loadStudentsData();
}

function studentsFilterStudents() {
    const filterSelect = document.getElementById('statusFilter');
    if (filterSelect) {
        studentsCurrentStatus = filterSelect.value;
    }
    studentsCurrentPage = 1;
    loadStudentsData();
}

async function studentsViewStudent(studentId) {
    try {
        const response = await fetchWithAuth(`/admin/api/students/${studentId}`);
        if (!response.ok) throw new Error('Failed to load student details');

        const student = await response.json();

        const modalContent = `
            <div class="modal-header">Student Details</div>
            <div class="student-details">
                <div class="detail-row">
                    <strong>Registration Number:</strong> ${escapeHtml(student.registrationNumber)}
                </div>
                <div class="detail-row">
                    <strong>Full Name:</strong> ${escapeHtml(student.fullName)}
                </div>
                <div class="detail-row">
                    <strong>Status:</strong> ${student.isActive ? 'Active' : 'Inactive'}
                </div>
                <div class="detail-row">
                    <strong>Last Login:</strong> ${student.lastLoginAt ? new Date(student.lastLoginAt).toLocaleString() : 'Never'}
                </div>
                <div class="detail-row">
                    <strong>Active Devices:</strong> ${student.devices}
                </div>
                <div class="detail-row">
                    <strong>Enrollments:</strong>
                    <div class="enrollments-list">
                        ${student.enrollments && student.enrollments.length > 0
                            ? student.enrollments.map(enrollment => `
                                <div class="enrollment-item">
                                    <div class="enrollment-programme">${escapeHtml(enrollment.programmeName)}</div>
                                    <div class="enrollment-details">
                                        ${escapeHtml(enrollment.universityName)} |
                                        ${escapeHtml(enrollment.academicTerm)} |
                                        Year ${enrollment.yearOfStudy}
                                    </div>
                                    <div class="enrollment-date">Enrolled: ${new Date(enrollment.enrollmentDate).toLocaleDateString()}</div>
                                </div>
                            `).join('')
                            : 'No active enrollments'
                        }
                    </div>
                </div>
            </div>
            <div style="display: flex; gap: 10px; justify-content: flex-end; margin-top: 20px;">
                <button class="btn-primary" onclick="closeModal()">Close</button>
            </div>
        `;

        showModal(modalContent);

    } catch (err) {
        console.error('Failed to load student details:', err);
        showNotification('Failed to load student details: ' + err.message, 'error');
    }
}

async function studentsDeactivateStudent(studentId) {
    if (!confirm('Are you sure you want to deactivate this student? They will not be able to log in.')) {
        return;
    }

    try {
        const response = await fetchWithAuth(`/admin/api/students/${studentId}`, {
            method: 'DELETE'
        });

        if (response.ok) {
            showNotification('Student deactivated successfully', 'success');
            await loadStudentsData();
        } else {
            const error = await response.text();
            throw new Error(error || 'Failed to deactivate student');
        }
    } catch (err) {
        console.error('Deactivate error:', err);
        showNotification('Failed to deactivate student: ' + err.message, 'error');
    }
}

async function studentsActivateStudent(studentId) {
    if (!confirm('Are you sure you want to activate this student?')) {
        return;
    }

    try {
        const response = await fetchWithAuth(`/admin/api/students/${studentId}/activate`, {
            method: 'POST'
        });

        if (response.ok) {
            showNotification('Student activated successfully', 'success');
            await loadStudentsData();
        } else {
            const error = await response.text();
            throw new Error(error || 'Failed to activate student');
        }
    } catch (err) {
        console.error('Activate error:', err);
        showNotification('Failed to activate student: ' + err.message, 'error');
    }
}

// Make functions globally accessible for onclick handlers
window.studentsSearchStudents = studentsSearchStudents;
window.studentsFilterStudents = studentsFilterStudents;
window.studentsViewStudent = studentsViewStudent;
window.studentsDeactivateStudent = studentsDeactivateStudent;
window.studentsActivateStudent = studentsActivateStudent;
window.studentsGoToPage = studentsGoToPage;

console.log('students.js loaded successfully');
console.log('window.initStudents exists:', typeof window.initStudents);