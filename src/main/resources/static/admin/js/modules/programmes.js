// static/admin/js/modules/programmes.js
let programmesCurrentPage = 1;
let programmesCurrentSearch = '';
let programmesCurrentUniversity = '';
let programmesCurrentDepartment = '';
let programmesCurrentStatus = '';
let programmesPageSize = 20;
let programmesUniversitiesList = [];
let programmesDepartmentsList = [];

window.initProgrammes = async function() {
    console.log('=== initProgrammes START ===');
    await loadProgrammesUniversities();
    await loadProgrammes();
    console.log('=== initProgrammes END ===');

    const searchInput = document.getElementById('searchProgramme');
    if (searchInput) {
        searchInput.addEventListener('keypress', function(e) {
            if (e.key === 'Enter') programmesSearch();
        });
    }
};

async function loadProgrammesUniversities() {
    try {
        const response = await fetchWithAuth('/admin/api/universities?pageSize=100');
        const data = await response.json();
        programmesUniversitiesList = data.universities;

        const filter = document.getElementById('universityFilterProgramme');
        if (filter) {
            filter.innerHTML = '<option value="">All Universities</option>' +
                programmesUniversitiesList.map(uni => `<option value="${uni.id}">${escapeHtml(uni.name)}</option>`).join('');
        }
    } catch (err) {
        console.error('Failed to load universities:', err);
    }
}

async function loadProgrammesDepartments(universityId) {
    try {
        let url = '/admin/api/departments?pageSize=100';
        if (universityId) url += `&universityId=${universityId}`;

        const response = await fetchWithAuth(url);
        const data = await response.json();
        programmesDepartmentsList = data.departments;

        const filter = document.getElementById('departmentFilterProgramme');
        if (filter) {
            filter.innerHTML = '<option value="">All Departments</option>' +
                programmesDepartmentsList.map(dept => `<option value="${dept.id}">${escapeHtml(dept.name)}</option>`).join('');
        }
    } catch (err) {
        console.error('Failed to load departments:', err);
    }
}

async function loadProgrammes() {
    const tbody = document.getElementById('programmes-table-body');
    if (!tbody) return;

    tbody.innerHTML = '<tr><td colspan="7" class="loading-text">Loading...</td></tr>';

    try {
        let url = `/admin/api/programmes?page=${programmesCurrentPage}&pageSize=${programmesPageSize}`;
        if (programmesCurrentSearch) url += `&search=${encodeURIComponent(programmesCurrentSearch)}`;
        if (programmesCurrentUniversity) url += `&universityId=${programmesCurrentUniversity}`;
        if (programmesCurrentDepartment) url += `&departmentId=${programmesCurrentDepartment}`;
        if (programmesCurrentStatus) url += `&activeOnly=${programmesCurrentStatus === 'true'}`;

        const response = await fetchWithAuth(url);
        if (!response.ok) throw new Error('Failed to load programmes');

        const data = await response.json();
        console.log('Programmes loaded:', data);

        renderProgrammes(data.programmes);
        renderProgrammesPagination(data);

    } catch (err) {
        console.error('Failed to load programmes:', err);
        tbody.innerHTML = `<tr><td colspan="7" class="error-text">Failed to load: ${err.message}<br><button onclick="loadProgrammes()">Retry</button></td></tr>`;
    }
}

function renderProgrammes(programmes) {
    const tbody = document.getElementById('programmes-table-body');
    if (!tbody) return;

    if (!programmes || programmes.length === 0) {
        tbody.innerHTML = '<tr><td colspan="7" class="empty-text">No programmes found</td></tr>';
        return;
    }

    tbody.innerHTML = programmes.map(prog => `
        <tr>
            <td><strong>${escapeHtml(prog.name)}</strong></td>
            <td><span class="uni-badge">${escapeHtml(prog.universityName)}</span></td>
            <td>${prog.departmentName ? `<span class="dept-badge">${escapeHtml(prog.departmentName)}</span>` : 'N/A'}</td>
            <td><span class="badge">${prog.unitCount}</span></td>
            <td>
                ${prog.isActive
                    ? '<span class="status-badge status-active">Active</span>'
                    : '<span class="status-badge status-inactive">Inactive</span>'}
            </td>
            <td>${new Date(prog.createdAt).toLocaleDateString()}</td>
            <td>
                <button class="btn-warning" onclick="editProgramme('${prog.id}')">Edit</button>
                <button class="btn-danger" onclick="deleteProgramme('${prog.id}')">Delete</button>
            </td>
        </tr>
    `).join('');
}

function renderProgrammesPagination(data) {
    const paginationDiv = document.getElementById('programmes-pagination');
    if (!paginationDiv) return;

    const totalPages = Math.ceil(data.total / data.pageSize);
    if (totalPages <= 1) {
        paginationDiv.innerHTML = '';
        return;
    }

    let html = '<div class="pagination-controls">';
    if (programmesCurrentPage > 1) {
        html += `<button onclick="goToProgrammePage(${programmesCurrentPage - 1})" class="page-btn">← Previous</button>`;
    }

    const startPage = Math.max(1, programmesCurrentPage - 2);
    const endPage = Math.min(totalPages, programmesCurrentPage + 2);

    if (startPage > 1) {
        html += `<button onclick="goToProgrammePage(1)" class="page-btn">1</button>`;
        if (startPage > 2) html += `<span class="page-dots">...</span>`;
    }

    for (let i = startPage; i <= endPage; i++) {
        html += `<button onclick="goToProgrammePage(${i})" class="page-btn ${i === programmesCurrentPage ? 'active' : ''}">${i}</button>`;
    }

    if (endPage < totalPages) {
        if (endPage < totalPages - 1) html += `<span class="page-dots">...</span>`;
        html += `<button onclick="goToProgrammePage(${totalPages})" class="page-btn">${totalPages}</button>`;
    }

    if (programmesCurrentPage < totalPages) {
        html += `<button onclick="goToProgrammePage(${programmesCurrentPage + 1})" class="page-btn">Next →</button>`;
    }
    html += '</div>';
    paginationDiv.innerHTML = html;
}

function goToProgrammePage(page) {
    programmesCurrentPage = page;
    loadProgrammes();
}

function programmesSearch() {
    programmesCurrentSearch = document.getElementById('searchProgramme').value;
    programmesCurrentPage = 1;
    loadProgrammes();
}

function programmesFilterByUniversity() {
    programmesCurrentUniversity = document.getElementById('universityFilterProgramme').value;
    programmesCurrentDepartment = '';
    programmesCurrentPage = 1;
    loadProgrammesDepartments(programmesCurrentUniversity);
    loadProgrammes();
}

function programmesFilterByDepartment() {
    programmesCurrentDepartment = document.getElementById('departmentFilterProgramme').value;
    programmesCurrentPage = 1;
    loadProgrammes();
}

function programmesFilterByStatus() {
    programmesCurrentStatus = document.getElementById('statusFilterProgramme').value;
    programmesCurrentPage = 1;
    loadProgrammes();
}

function showCreateProgrammeModal() {
    const modalContent = `
        <div class="modal-header">Add Programme</div>
        <form id="createProgrammeForm">
            <div class="form-group">
                <label>University *</label>
                <select id="progUniversityId" onchange="loadDepartmentsForProgramme()" required>
                    <option value="">Select University</option>
                    ${programmesUniversitiesList.map(uni => `<option value="${uni.id}">${escapeHtml(uni.name)}</option>`).join('')}
                </select>
            </div>
            <div class="form-group">
                <label>Department (Optional)</label>
                <select id="progDepartmentId">
                    <option value="">None</option>
                </select>
            </div>
            <div class="form-group">
                <label>Programme Name *</label>
                <input type="text" id="progName" required>
            </div>
            <div class="form-group">
                <label>Status</label>
                <select id="progStatus">
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

    // Make loadDepartmentsForProgramme globally available for the modal
    window.loadDepartmentsForProgramme = async function() {
        const universityId = document.getElementById('progUniversityId').value;
        const deptSelect = document.getElementById('progDepartmentId');
        if (!universityId) {
            deptSelect.innerHTML = '<option value="">None</option>';
            return;
        }

        try {
            const response = await fetchWithAuth(`/admin/api/departments?universityId=${universityId}&pageSize=100`);
            const data = await response.json();
            deptSelect.innerHTML = '<option value="">None</option>' +
                data.departments.map(dept => `<option value="${dept.id}">${escapeHtml(dept.name)}</option>`).join('');
        } catch (err) {
            console.error('Failed to load departments:', err);
        }
    };

    document.getElementById('createProgrammeForm').addEventListener('submit', async (e) => {
        e.preventDefault();
        await createProgramme();
    });
}

async function createProgramme() {
    const universityId = document.getElementById('progUniversityId').value;
    const departmentId = document.getElementById('progDepartmentId').value || null;
    const name = document.getElementById('progName').value;
    const isActive = document.getElementById('progStatus').value === 'true';

    if (!universityId || !name) {
        showNotification('Please fill all required fields', 'error');
        return;
    }

    try {
        const response = await fetchWithAuth('/admin/api/programmes', {
            method: 'POST',
            body: JSON.stringify({ universityId, departmentId, name, isActive })
        });

        if (response.ok) {
            showNotification('Programme created successfully', 'success');
            closeModal();
            await loadProgrammes();
        } else {
            const error = await response.text();
            showNotification(error, 'error');
        }
    } catch (err) {
        showNotification('Failed to create programme', 'error');
    }
}

async function editProgramme(id) {
    try {
        const response = await fetchWithAuth(`/admin/api/programmes/${id}`);
        const prog = await response.json();

        const modalContent = `
            <div class="modal-header">Edit Programme</div>
            <form id="editProgrammeForm">
                <div class="form-group">
                    <label>University</label>
                    <input type="text" value="${escapeHtml(prog.universityName)}" disabled>
                </div>
                <div class="form-group">
                    <label>Department (Optional)</label>
                    <select id="progDepartmentId">
                        <option value="">None</option>
                        ${programmesDepartmentsList.map(dept =>
                            `<option value="${dept.id}" ${prog.departmentId === dept.id ? 'selected' : ''}>${escapeHtml(dept.name)}</option>`
                        ).join('')}
                    </select>
                </div>
                <div class="form-group">
                    <label>Programme Name *</label>
                    <input type="text" id="progName" value="${escapeHtml(prog.name)}" required>
                </div>
                <div class="form-group">
                    <label>Status</label>
                    <select id="progStatus">
                        <option value="true" ${prog.isActive ? 'selected' : ''}>Active</option>
                        <option value="false" ${!prog.isActive ? 'selected' : ''}>Inactive</option>
                    </select>
                </div>
                <div style="display: flex; gap: 10px; justify-content: flex-end; margin-top: 20px;">
                    <button type="button" class="btn-warning" onclick="closeModal()">Cancel</button>
                    <button type="submit" class="btn-primary">Update</button>
                </div>
            </form>
        `;
        showModal(modalContent);

        document.getElementById('editProgrammeForm').addEventListener('submit', async (e) => {
            e.preventDefault();
            await updateProgramme(id);
        });
    } catch (err) {
        showNotification('Failed to load programme details', 'error');
    }
}

async function updateProgramme(id) {
    const departmentId = document.getElementById('progDepartmentId').value || null;
    const name = document.getElementById('progName').value;
    const isActive = document.getElementById('progStatus').value === 'true';

    try {
        const response = await fetchWithAuth(`/admin/api/programmes/${id}`, {
            method: 'PUT',
            body: JSON.stringify({ name, departmentId, isActive })
        });

        if (response.ok) {
            showNotification('Programme updated successfully', 'success');
            closeModal();
            await loadProgrammes();
        } else {
            throw new Error('Update failed');
        }
    } catch (err) {
        showNotification('Failed to update programme', 'error');
    }
}

async function deleteProgramme(id) {
    if (!confirm('Are you sure? This will also remove all unit links for this programme.')) {
        return;
    }

    try {
        const response = await fetchWithAuth(`/admin/api/programmes/${id}`, { method: 'DELETE' });
        if (response.ok) {
            showNotification('Programme deleted successfully', 'success');
            await loadProgrammes();
        } else {
            const error = await response.text();
            showNotification(error, 'error');
        }
    } catch (err) {
        showNotification('Failed to delete programme', 'error');
    }
}