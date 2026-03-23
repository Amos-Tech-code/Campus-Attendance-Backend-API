// static/admin/js/modules/academic-terms.js
let academicTermsCurrentPage = 1;
let academicTermsCurrentSearch = '';
let academicTermsCurrentUniversity = '';
let academicTermsCurrentStatus = '';
let academicTermsPageSize = 20;
let academicTermsUniversitiesList = [];

window.initAcademicTerms = async function() {
    console.log('=== initAcademicTerms START ===');
    await loadAcademicTermsUniversities();
    await loadAcademicTerms();
    console.log('=== initAcademicTerms END ===');

    const searchInput = document.getElementById('searchTerm');
    if (searchInput) {
        searchInput.addEventListener('keypress', function(e) {
            if (e.key === 'Enter') academicTermsSearch();
        });
    }
};

async function loadAcademicTermsUniversities() {
    try {
        const response = await fetchWithAuth('/admin/api/universities?pageSize=100');
        const data = await response.json();
        academicTermsUniversitiesList = data.universities;

        const filter = document.getElementById('universityFilterTerm');
        if (filter) {
            filter.innerHTML = '<option value="">All Universities</option>' +
                academicTermsUniversitiesList.map(uni => `<option value="${uni.id}">${escapeHtml(uni.name)}</option>`).join('');
        }
    } catch (err) {
        console.error('Failed to load universities:', err);
    }
}

async function loadAcademicTerms() {
    const tbody = document.getElementById('academic-terms-table-body');
    if (!tbody) return;

    tbody.innerHTML = '<tr><td colspan="7" class="loading-text">Loading...</td></tr>';

    try {
        let url = `/admin/api/academic-terms?page=${academicTermsCurrentPage}&pageSize=${academicTermsPageSize}`;
        if (academicTermsCurrentSearch) url += `&search=${encodeURIComponent(academicTermsCurrentSearch)}`;
        if (academicTermsCurrentUniversity) url += `&universityId=${academicTermsCurrentUniversity}`;
        if (academicTermsCurrentStatus) url += `&activeOnly=${academicTermsCurrentStatus === 'true'}`;

        const response = await fetchWithAuth(url);
        if (!response.ok) throw new Error('Failed to load academic terms');

        const data = await response.json();
        console.log('Academic terms loaded:', data);

        renderAcademicTerms(data.terms);
        renderAcademicTermsPagination(data);

    } catch (err) {
        console.error('Failed to load academic terms:', err);
        tbody.innerHTML = `<tr><td colspan="7" class="error-text">Failed to load: ${err.message}<br><button onclick="loadAcademicTerms()">Retry</button></td></tr>`;
    }
}

function renderAcademicTerms(terms) {
    const tbody = document.getElementById('academic-terms-table-body');
    if (!tbody) return;

    if (!terms || terms.length === 0) {
        tbody.innerHTML = '<tr><td colspan="7" class="empty-text">No academic terms found</td></tr>';
        return;
    }

    tbody.innerHTML = terms.map(term => `
        <tr>
            <td><strong>${escapeHtml(term.academicYear)}</strong></td>
            <td><span class="semester-badge">Semester ${term.semester}</span></td>
            <td><span class="uni-badge">${escapeHtml(term.universityName)}</span></td>
            <td><span class="weeks-badge">${term.weekCount} weeks</span></td>
            <td>
                ${term.isActive
                    ? '<span class="status-badge status-active">✅ Active</span>'
                    : '<span class="status-badge status-inactive">❌ Inactive</span>'}
            </td>
            <td>${new Date(term.createdAt).toLocaleDateString()}</td>
            <td>
                ${!term.isActive
                    ? `<button class="btn-primary" onclick="setActiveTerm('${term.id}')">Set Active</button>`
                    : ''
                }
                <button class="btn-warning" onclick="editAcademicTerm('${term.id}')">Edit</button>
                <button class="btn-danger" onclick="deleteAcademicTerm('${term.id}')">Delete</button>
            </td>
        </tr>
    `).join('');
}

function renderAcademicTermsPagination(data) {
    const paginationDiv = document.getElementById('academic-terms-pagination');
    if (!paginationDiv) return;

    const totalPages = Math.ceil(data.total / data.pageSize);
    if (totalPages <= 1) {
        paginationDiv.innerHTML = '';
        return;
    }

    let html = '<div class="pagination-controls">';

    if (academicTermsCurrentPage > 1) {
        html += `<button onclick="goToAcademicTermPage(${academicTermsCurrentPage - 1})" class="page-btn">← Previous</button>`;
    }

    const startPage = Math.max(1, academicTermsCurrentPage - 2);
    const endPage = Math.min(totalPages, academicTermsCurrentPage + 2);

    if (startPage > 1) {
        html += `<button onclick="goToAcademicTermPage(1)" class="page-btn">1</button>`;
        if (startPage > 2) html += `<span class="page-dots">...</span>`;
    }

    for (let i = startPage; i <= endPage; i++) {
        html += `<button onclick="goToAcademicTermPage(${i})" class="page-btn ${i === academicTermsCurrentPage ? 'active' : ''}">${i}</button>`;
    }

    if (endPage < totalPages) {
        if (endPage < totalPages - 1) html += `<span class="page-dots">...</span>`;
        html += `<button onclick="goToAcademicTermPage(${totalPages})" class="page-btn">${totalPages}</button>`;
    }

    if (academicTermsCurrentPage < totalPages) {
        html += `<button onclick="goToAcademicTermPage(${academicTermsCurrentPage + 1})" class="page-btn">Next →</button>`;
    }

    html += '</div>';
    paginationDiv.innerHTML = html;
}

function goToAcademicTermPage(page) {
    academicTermsCurrentPage = page;
    loadAcademicTerms();
}

function academicTermsSearch() {
    academicTermsCurrentSearch = document.getElementById('searchTerm').value;
    academicTermsCurrentPage = 1;
    loadAcademicTerms();
}

function academicTermsFilterByUniversity() {
    academicTermsCurrentUniversity = document.getElementById('universityFilterTerm').value;
    academicTermsCurrentPage = 1;
    loadAcademicTerms();
}

function academicTermsFilterByStatus() {
    academicTermsCurrentStatus = document.getElementById('statusFilterTerm').value;
    academicTermsCurrentPage = 1;
    loadAcademicTerms();
}

function showCreateAcademicTermModal() {
    const currentYear = new Date().getFullYear();
    const nextYear = currentYear + 1;

    const modalContent = `
        <div class="modal-header">Add Academic Term</div>
        <form id="createAcademicTermForm">
            <div class="form-group">
                <label>University *</label>
                <select id="termUniversityId" required>
                    <option value="">Select University</option>
                    ${academicTermsUniversitiesList.map(uni => `<option value="${uni.id}">${escapeHtml(uni.name)}</option>`).join('')}
                </select>
            </div>
            <div class="form-group">
                <label>Academic Year *</label>
                <select id="termAcademicYear" required>
                    <option value="${currentYear}-${nextYear}">${currentYear}-${nextYear}</option>
                    <option value="${currentYear-1}-${currentYear}">${currentYear-1}-${currentYear}</option>
                    <option value="${currentYear+1}-${currentYear+2}">${currentYear+1}-${currentYear+2}</option>
                </select>
            </div>
            <div class="form-group">
                <label>Semester *</label>
                <select id="termSemester" required>
                    <option value="1">Semester 1</option>
                    <option value="2">Semester 2</option>
                </select>
            </div>
            <div class="form-group">
                <label>Week Count</label>
                <input type="number" id="termWeekCount" value="14" min="1" max="20">
            </div>
            <div class="form-group">
                <label>Status</label>
                <select id="termStatus">
                    <option value="true">Active</option>
                    <option value="false">Inactive</option>
                </select>
            </div>
            <div style="display: flex; gap: 10px; justify-content: flex-end; margin-top: 20px;">
                <button type="button" class="btn-warning" onclick="closeModal()">Cancel</button>
                <button type="submit" class="btn-primary">Create</button>
            </div>
        </form>
    `;
    showModal(modalContent);

    document.getElementById('createAcademicTermForm').addEventListener('submit', async (e) => {
        e.preventDefault();
        await createAcademicTerm();
    });
}

async function createAcademicTerm() {
    const universityId = document.getElementById('termUniversityId').value;
    const academicYear = document.getElementById('termAcademicYear').value;
    const semester = parseInt(document.getElementById('termSemester').value);
    const weekCount = parseInt(document.getElementById('termWeekCount').value);
    const isActive = document.getElementById('termStatus').value === 'true';

    if (!universityId || !academicYear) {
        showNotification('Please fill all required fields', 'error');
        return;
    }

    try {
        const response = await fetchWithAuth('/admin/api/academic-terms', {
            method: 'POST',
            body: JSON.stringify({ universityId, academicYear, semester, weekCount, isActive })
        });

        if (response.ok) {
            showNotification('Academic term created successfully', 'success');
            closeModal();
            await loadAcademicTerms();
        } else {
            const error = await response.text();
            showNotification(error, 'error');
        }
    } catch (err) {
        showNotification('Failed to create academic term', 'error');
    }
}

async function editAcademicTerm(id) {
    try {
        const response = await fetchWithAuth(`/admin/api/academic-terms/${id}`);
        const term = await response.json();

        const modalContent = `
            <div class="modal-header">Edit Academic Term</div>
            <form id="editAcademicTermForm">
                <div class="form-group">
                    <label>University</label>
                    <input type="text" value="${escapeHtml(term.universityName)}" disabled>
                </div>
                <div class="form-group">
                    <label>Academic Year</label>
                    <input type="text" value="${escapeHtml(term.academicYear)}" disabled>
                    <small>Academic year cannot be changed</small>
                </div>
                <div class="form-group">
                    <label>Semester</label>
                    <input type="text" value="Semester ${term.semester}" disabled>
                    <small>Semester cannot be changed</small>
                </div>
                <div class="form-group">
                    <label>Week Count</label>
                    <input type="number" id="termWeekCount" value="${term.weekCount}" min="1" max="20" required>
                </div>
                <div class="form-group">
                    <label>Status</label>
                    <select id="termStatus">
                        <option value="true" ${term.isActive ? 'selected' : ''}>Active</option>
                        <option value="false" ${!term.isActive ? 'selected' : ''}>Inactive</option>
                    </select>
                </div>
                <div style="display: flex; gap: 10px; justify-content: flex-end; margin-top: 20px;">
                    <button type="button" class="btn-warning" onclick="closeModal()">Cancel</button>
                    <button type="submit" class="btn-primary">Update</button>
                </div>
            </form>
        `;
        showModal(modalContent);

        document.getElementById('editAcademicTermForm').addEventListener('submit', async (e) => {
            e.preventDefault();
            await updateAcademicTerm(id);
        });
    } catch (err) {
        showNotification('Failed to load academic term details', 'error');
    }
}

async function updateAcademicTerm(id) {
    const weekCount = parseInt(document.getElementById('termWeekCount').value);
    const isActive = document.getElementById('termStatus').value === 'true';

    try {
        const response = await fetchWithAuth(`/admin/api/academic-terms/${id}`, {
            method: 'PUT',
            body: JSON.stringify({ weekCount, isActive })
        });

        if (response.ok) {
            showNotification('Academic term updated successfully', 'success');
            closeModal();
            await loadAcademicTerms();
        } else {
            throw new Error('Update failed');
        }
    } catch (err) {
        showNotification('Failed to update academic term', 'error');
    }
}

async function setActiveTerm(id) {
    if (!confirm('Setting this term as active will deactivate all other terms for this university. Continue?')) {
        return;
    }

    try {
        const response = await fetchWithAuth(`/admin/api/academic-terms/${id}/set-active`, {
            method: 'POST'
        });

        if (response.ok) {
            showNotification('Academic term set as active successfully', 'success');
            await loadAcademicTerms();
        } else {
            const error = await response.text();
            showNotification(error, 'error');
        }
    } catch (err) {
        showNotification('Failed to set active term', 'error');
    }
}

async function deleteAcademicTerm(id) {
    if (!confirm('Are you sure? This will also delete all attendance sessions and enrollments for this term if any exist.')) {
        return;
    }

    try {
        const response = await fetchWithAuth(`/admin/api/academic-terms/${id}`, { method: 'DELETE' });
        if (response.ok) {
            showNotification('Academic term deleted successfully', 'success');
            await loadAcademicTerms();
        } else {
            const error = await response.text();
            showNotification(error, 'error');
        }
    } catch (err) {
        showNotification('Failed to delete academic term', 'error');
    }
}

// Make functions globally accessible
window.academicTermsSearch = academicTermsSearch;
window.academicTermsFilterByUniversity = academicTermsFilterByUniversity;
window.academicTermsFilterByStatus = academicTermsFilterByStatus;
window.showCreateAcademicTermModal = showCreateAcademicTermModal;
window.editAcademicTerm = editAcademicTerm;
window.deleteAcademicTerm = deleteAcademicTerm;
window.setActiveTerm = setActiveTerm;
window.goToAcademicTermPage = goToAcademicTermPage;

console.log('academic-terms.js loaded successfully');
console.log('window.initAcademicTerms exists:', typeof window.initAcademicTerms);