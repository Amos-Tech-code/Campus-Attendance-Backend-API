// static/admin/js/modules/lecturers.js
let currentPage = 1;
let currentSearch = '';
let currentStatus = '';
let pageSize = 20;

// Make sure the function is globally accessible with the correct name
window.initLecturers = async function() {
    console.log('=== initLecturers START ===');
    console.log('Searching for elements...');
    console.log('searchLecturer element:', document.getElementById('searchLecturer'));
    console.log('statusFilter element:', document.getElementById('statusFilter'));
    console.log('lecturers-table-body element:', document.getElementById('lecturers-table-body'));

    await loadLecturers();
    console.log('=== initLecturers END ===');

    // Add event listener for enter key in search
    const searchInput = document.getElementById('searchLecturer');
    if (searchInput) {
        searchInput.addEventListener('keypress', function(e) {
            if (e.key === 'Enter') {
                searchLecturers();
            }
        });
    } else {
        console.warn('searchLecturer input not found');
    }
};

async function loadLecturers() {
    const tbody = document.getElementById('lecturers-table-body');
    if (!tbody) {
        console.error('lecturers-table-body not found!');
        return;
    }

    tbody.innerHTML = '<td colspan="8" class="loading-text">Loading...</td></tr>';

    try {
        let url = `/admin/api/lecturers?page=${currentPage}&pageSize=${pageSize}`;
        if (currentSearch) url += `&search=${encodeURIComponent(currentSearch)}`;
        if (currentStatus) url += `&status=${currentStatus}`;

        console.log('Fetching lecturers from:', url);

        const response = await fetchWithAuth(url);
        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(`Failed to load lecturers: ${errorText}`);
        }

        const data = await response.json();
        console.log('Lecturers loaded:', data);

        renderLecturers(data.lecturers);
        renderPagination(data);

    } catch (err) {
        console.error('Failed to load lecturers:', err);
        tbody.innerHTML = `发展<td colspan="8" class="error-text">Failed to load lecturers: ${err.message}<br><button onclick="loadLecturers()">Retry</button></td></tr>`;
    }
}

function renderLecturers(lecturers) {
    const tbody = document.getElementById('lecturers-table-body');
    if (!tbody) return;

    if (!lecturers || lecturers.length === 0) {
        tbody.innerHTML = '发展<td colspan="8" class="empty-text">No lecturers found</td></tr>';
        return;
    }

    tbody.innerHTML = lecturers.map(lecturer => `
        <tr>
            <td>${escapeHtml(lecturer.fullName || 'N/A')}</td>
            <td>${escapeHtml(lecturer.email)}</td>
            <td>
                ${lecturer.universities && lecturer.universities.length > 0
                    ? lecturer.universities.map(u => `<span class="uni-badge">${escapeHtml(u.name)}</span>`).join('')
                    : 'N/A'}
            </td>
            <td><span class="assignment-count">${lecturer.teachingAssignments || 0}</span></td>
            <td>
                ${lecturer.isProfileComplete
                    ? '<span class="status-success">✅ Complete</span>'
                    : '<span class="status-warning">⚠️ Incomplete</span>'}
            </td>
            <td>
                ${lecturer.isActive
                    ? '<span class="status-active">✅ Active</span>'
                    : '<span class="status-inactive">❌ Inactive</span>'}
            </td>
            <td>${lecturer.lastLoginAt ? new Date(lecturer.lastLoginAt).toLocaleString() : 'Never'}</td>
            <td>
                <button class="btn-warning" onclick="viewLecturer('${lecturer.id}')">View</button>
                ${!lecturer.isActive
                    ? `<button class="btn-primary" onclick="activateLecturer('${lecturer.id}')">Activate</button>`
                    : `<button class="btn-danger" onclick="deactivateLecturer('${lecturer.id}')">Deactivate</button>`
                }
            </td>
        </tr>
    `).join('');
}

function renderPagination(data) {
    const paginationDiv = document.getElementById('pagination');
    if (!paginationDiv) return;

    const totalPages = Math.ceil(data.total / data.pageSize);
    if (totalPages <= 1) {
        paginationDiv.innerHTML = '';
        return;
    }

    let pagesHtml = '<div class="pagination-controls">';

    // Previous button
    if (currentPage > 1) {
        pagesHtml += `<button onclick="goToPage(${currentPage - 1})" class="page-btn">← Previous</button>`;
    }

    // Page numbers
    const startPage = Math.max(1, currentPage - 2);
    const endPage = Math.min(totalPages, currentPage + 2);

    if (startPage > 1) {
        pagesHtml += `<button onclick="goToPage(1)" class="page-btn">1</button>`;
        if (startPage > 2) pagesHtml += `<span class="page-dots">...</span>`;
    }

    for (let i = startPage; i <= endPage; i++) {
        pagesHtml += `<button onclick="goToPage(${i})" class="page-btn ${i === currentPage ? 'active' : ''}">${i}</button>`;
    }

    if (endPage < totalPages) {
        if (endPage < totalPages - 1) pagesHtml += `<span class="page-dots">...</span>`;
        pagesHtml += `<button onclick="goToPage(${totalPages})" class="page-btn">${totalPages}</button>`;
    }

    // Next button
    if (currentPage < totalPages) {
        pagesHtml += `<button onclick="goToPage(${currentPage + 1})" class="page-btn">Next →</button>`;
    }

    pagesHtml += '</div>';
    paginationDiv.innerHTML = pagesHtml;
}

function goToPage(page) {
    currentPage = page;
    loadLecturers();
}

function searchLecturers() {
    const searchInput = document.getElementById('searchLecturer');
    if (searchInput) {
        currentSearch = searchInput.value;
    }
    currentPage = 1;
    loadLecturers();
}

function filterLecturers() {
    const filterSelect = document.getElementById('statusFilter');
    if (filterSelect) {
        currentStatus = filterSelect.value;
    }
    currentPage = 1;
    loadLecturers();
}

async function viewLecturer(lecturerId) {
    try {
        const response = await fetchWithAuth(`/admin/api/lecturers/${lecturerId}`);
        if (!response.ok) throw new Error('Failed to load lecturer details');

        const lecturer = await response.json();

        const modalContent = `
            <div class="modal-header">Lecturer Details</div>
            <div class="lecturer-details">
                <div class="detail-row">
                    <strong>Name:</strong> ${escapeHtml(lecturer.fullName || 'N/A')}
                </div>
                <div class="detail-row">
                    <strong>Email:</strong> ${escapeHtml(lecturer.email)}
                </div>
                <div class="detail-row">
                    <strong>Profile Complete:</strong> ${lecturer.isProfileComplete ? 'Yes' : 'No'}
                </div>
                <div class="detail-row">
                    <strong>Status:</strong> ${lecturer.isActive ? 'Active' : 'Inactive'}
                </div>
                <div class="detail-row">
                    <strong>Last Login:</strong> ${lecturer.lastLoginAt ? new Date(lecturer.lastLoginAt).toLocaleString() : 'Never'}
                </div>
                <div class="detail-row">
                    <strong>Universities:</strong>
                    <div class="uni-list">
                        ${lecturer.universities && lecturer.universities.length > 0
                            ? lecturer.universities.map(u => `<span class="uni-badge">${escapeHtml(u.name)}</span>`).join('')
                            : 'None'}
                    </div>
                </div>
                <div class="detail-row">
                    <strong>Teaching Assignments:</strong> ${lecturer.teachingAssignments || 0}
                </div>
            </div>
            <div style="display: flex; gap: 10px; justify-content: flex-end; margin-top: 20px;">
                <button class="btn-primary" onclick="closeModal()">Close</button>
            </div>
        `;

        showModal(modalContent);

    } catch (err) {
        console.error('Failed to load lecturer details:', err);
        showNotification('Failed to load lecturer details: ' + err.message, 'error');
    }
}

async function deactivateLecturer(lecturerId) {
    if (!confirm('Are you sure you want to deactivate this lecturer? They will not be able to log in.')) {
        return;
    }

    try {
        const response = await fetchWithAuth(`/admin/api/lecturers/${lecturerId}`, {
            method: 'DELETE'
        });

        if (response.ok) {
            showNotification('Lecturer deactivated successfully', 'success');
            await loadLecturers();
        } else {
            const error = await response.text();
            throw new Error(error || 'Failed to deactivate lecturer');
        }
    } catch (err) {
        console.error('Deactivate error:', err);
        showNotification('Failed to deactivate lecturer: ' + err.message, 'error');
    }
}

async function activateLecturer(lecturerId) {
    if (!confirm('Are you sure you want to activate this lecturer?')) {
        return;
    }

    try {
        const response = await fetchWithAuth(`/admin/api/lecturers/${lecturerId}/activate`, {
            method: 'POST'
        });

        if (response.ok) {
            showNotification('Lecturer activated successfully', 'success');
            await loadLecturers();
        } else {
            const error = await response.text();
            throw new Error(error || 'Failed to activate lecturer');
        }
    } catch (err) {
        console.error('Activate error:', err);
        showNotification('Failed to activate lecturer: ' + err.message, 'error');
    }
}