// static/admin/js/modules/units.js
let unitsCurrentPage = 1;
let unitsCurrentSearch = '';
let unitsCurrentUniversity = '';
let unitsCurrentDepartment = '';
let unitsCurrentStatus = '';
let unitsPageSize = 20;
let unitsUniversitiesList = [];
let unitsDepartmentsList = [];

window.initUnits = async function() {
    console.log('=== initUnits START ===');
    await loadUnitsUniversities();
    await loadUnits();
    console.log('=== initUnits END ===');

    const searchInput = document.getElementById('searchUnit');
    if (searchInput) {
        searchInput.addEventListener('keypress', function(e) {
            if (e.key === 'Enter') unitsSearch();
        });
    }
};

async function loadUnitsUniversities() {
    try {
        const response = await fetchWithAuth('/admin/api/universities?pageSize=100');
        const data = await response.json();
        unitsUniversitiesList = data.universities;

        const filter = document.getElementById('universityFilterUnit');
        if (filter) {
            filter.innerHTML = '<option value="">All Universities</option>' +
                unitsUniversitiesList.map(uni => `<option value="${uni.id}">${escapeHtml(uni.name)}</option>`).join('');
        }
    } catch (err) {
        console.error('Failed to load universities:', err);
    }
}

async function loadUnitsDepartments(universityId) {
    try {
        let url = '/admin/api/departments?pageSize=100';
        if (universityId) url += `&universityId=${universityId}`;

        const response = await fetchWithAuth(url);
        const data = await response.json();
        unitsDepartmentsList = data.departments;

        const filter = document.getElementById('departmentFilterUnit');
        if (filter) {
            filter.innerHTML = '<option value="">All Departments</option>' +
                unitsDepartmentsList.map(dept => `<option value="${dept.id}">${escapeHtml(dept.name)}</option>`).join('');
        }
    } catch (err) {
        console.error('Failed to load departments:', err);
    }
}

async function loadUnits() {
    const tbody = document.getElementById('units-table-body');
    if (!tbody) return;

    tbody.innerHTML = '<tr><td colspan="8" class="loading-text">Loading...</td></tr>';

    try {
        let url = `/admin/api/units?page=${unitsCurrentPage}&pageSize=${unitsPageSize}`;
        if (unitsCurrentSearch) url += `&search=${encodeURIComponent(unitsCurrentSearch)}`;
        if (unitsCurrentUniversity) url += `&universityId=${unitsCurrentUniversity}`;
        if (unitsCurrentDepartment) url += `&departmentId=${unitsCurrentDepartment}`;
        if (unitsCurrentStatus) url += `&activeOnly=${unitsCurrentStatus === 'true'}`;

        const response = await fetchWithAuth(url);
        if (!response.ok) throw new Error('Failed to load units');

        const data = await response.json();
        console.log('Units loaded:', data);

        renderUnits(data.units);
        renderUnitsPagination(data);

    } catch (err) {
        console.error('Failed to load units:', err);
        tbody.innerHTML = `<tr><td colspan="8" class="error-text">Failed to load: ${err.message}<br><button onclick="loadUnits()">Retry</button></td></tr>`;
    }
}

function renderUnits(units) {
    const tbody = document.getElementById('units-table-body');
    if (!tbody) return;

    if (!units || units.length === 0) {
        tbody.innerHTML = '<tr><td colspan="8" class="empty-text">No units found</td></tr>';
        return;
    }

    tbody.innerHTML = units.map(unit => `
        <tr>
            <td><strong>${escapeHtml(unit.code)}</strong></td>
            <td>${escapeHtml(unit.name)}</td>
            <td><span class="uni-badge">${escapeHtml(unit.universityName)}</span></td>
            <td>${unit.departmentName ? `<span class="dept-badge">${escapeHtml(unit.departmentName)}</span>` : 'N/A'}</td>
            <td class="linked-programmes">
                ${unit.programmes && unit.programmes.length > 0
                    ? unit.programmes.map(p => `<span class="programme-tag" title="Year ${p.yearOfStudy}">${escapeHtml(p.name)}</span>`).join('')
                    : '<span class="empty-text">Not linked</span>'}
            </td>
            <td>
                ${unit.isActive
                    ? '<span class="status-badge status-active">Active</span>'
                    : '<span class="status-badge status-inactive">Inactive</span>'}
            </td>
            <td>${new Date(unit.createdAt).toLocaleDateString()}</td>
            <td>
                <button class="btn-warning" onclick="editUnit('${unit.id}')">Edit</button>
                <button class="btn-info" onclick="manageUnitLinks('${unit.id}')">Link Programme</button>
                <button class="btn-danger" onclick="deleteUnit('${unit.id}')">Delete</button>
            </td>
        </tr>
    `).join('');
}

function renderUnitsPagination(data) {
    const paginationDiv = document.getElementById('units-pagination');
    if (!paginationDiv) return;

    const totalPages = Math.ceil(data.total / data.pageSize);
    if (totalPages <= 1) {
        paginationDiv.innerHTML = '';
        return;
    }

    let html = '<div class="pagination-controls">';
    if (unitsCurrentPage > 1) {
        html += `<button onclick="goToUnitPage(${unitsCurrentPage - 1})" class="page-btn">← Previous</button>`;
    }

    const startPage = Math.max(1, unitsCurrentPage - 2);
    const endPage = Math.min(totalPages, unitsCurrentPage + 2);

    if (startPage > 1) {
        html += `<button onclick="goToUnitPage(1)" class="page-btn">1</button>`;
        if (startPage > 2) html += `<span class="page-dots">...</span>`;
    }

    for (let i = startPage; i <= endPage; i++) {
        html += `<button onclick="goToUnitPage(${i})" class="page-btn ${i === unitsCurrentPage ? 'active' : ''}">${i}</button>`;
    }

    if (endPage < totalPages) {
        if (endPage < totalPages - 1) html += `<span class="page-dots">...</span>`;
        html += `<button onclick="goToUnitPage(${totalPages})" class="page-btn">${totalPages}</button>`;
    }

    if (unitsCurrentPage < totalPages) {
        html += `<button onclick="goToUnitPage(${unitsCurrentPage + 1})" class="page-btn">Next →</button>`;
    }
    html += '</div>';
    paginationDiv.innerHTML = html;
}

function goToUnitPage(page) {
    unitsCurrentPage = page;
    loadUnits();
}

function unitsSearch() {
    unitsCurrentSearch = document.getElementById('searchUnit').value;
    unitsCurrentPage = 1;
    loadUnits();
}

function unitsFilterByUniversity() {
    unitsCurrentUniversity = document.getElementById('universityFilterUnit').value;
    unitsCurrentDepartment = '';
    unitsCurrentPage = 1;
    loadUnitsDepartments(unitsCurrentUniversity);
    loadUnits();
}

function unitsFilterByDepartment() {
    unitsCurrentDepartment = document.getElementById('departmentFilterUnit').value;
    unitsCurrentPage = 1;
    loadUnits();
}

function unitsFilterByStatus() {
    unitsCurrentStatus = document.getElementById('statusFilterUnit').value;
    unitsCurrentPage = 1;
    loadUnits();
}

function showCreateUnitModal() {
    const modalContent = `
        <div class="modal-header">Add Unit</div>
        <form id="createUnitForm">
            <div class="form-group">
                <label>University *</label>
                <select id="unitUniversityId" onchange="loadDepartmentsForUnit()" required>
                    <option value="">Select University</option>
                    ${unitsUniversitiesList.map(uni => `<option value="${uni.id}">${escapeHtml(uni.name)}</option>`).join('')}
                </select>
            </div>
            <div class="form-group">
                <label>Department (Optional)</label>
                <select id="unitDepartmentId">
                    <option value="">None</option>
                </select>
            </div>
            <div class="form-group">
                <label>Unit Code *</label>
                <input type="text" id="unitCode" required placeholder="e.g., CS101">
            </div>
            <div class="form-group">
                <label>Unit Name *</label>
                <input type="text" id="unitName" required>
            </div>
            <div class="form-group">
                <label>Status</label>
                <select id="unitStatus">
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

    window.loadDepartmentsForUnit = async function() {
        const universityId = document.getElementById('unitUniversityId').value;
        const deptSelect = document.getElementById('unitDepartmentId');
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

    document.getElementById('createUnitForm').addEventListener('submit', async (e) => {
        e.preventDefault();
        await createUnit();
    });
}

async function createUnit() {
    const universityId = document.getElementById('unitUniversityId').value;
    const departmentId = document.getElementById('unitDepartmentId').value || null;
    const code = document.getElementById('unitCode').value;
    const name = document.getElementById('unitName').value;
    const isActive = document.getElementById('unitStatus').value === 'true';

    if (!universityId || !code || !name) {
        showNotification('Please fill all required fields', 'error');
        return;
    }

    try {
        const response = await fetchWithAuth('/admin/api/units', {
            method: 'POST',
            body: JSON.stringify({ universityId, departmentId, code, name, isActive })
        });

        if (response.ok) {
            showNotification('Unit created successfully', 'success');
            closeModal();
            await loadUnits();
        } else {
            const error = await response.text();
            showNotification(error, 'error');
        }
    } catch (err) {
        showNotification('Failed to create unit', 'error');
    }
}

async function editUnit(id) {
    try {
        const response = await fetchWithAuth(`/admin/api/units/${id}`);
        const unit = await response.json();

        const modalContent = `
            <div class="modal-header">Edit Unit</div>
            <form id="editUnitForm">
                <div class="form-group">
                    <label>University</label>
                    <input type="text" value="${escapeHtml(unit.universityName)}" disabled>
                </div>
                <div class="form-group">
                    <label>Department (Optional)</label>
                    <select id="unitDepartmentId">
                        <option value="">None</option>
                        ${unitsDepartmentsList.map(dept =>
                            `<option value="${dept.id}" ${unit.departmentId === dept.id ? 'selected' : ''}>${escapeHtml(dept.name)}</option>`
                        ).join('')}
                    </select>
                </div>
                <div class="form-group">
                    <label>Unit Code *</label>
                    <input type="text" id="unitCode" value="${escapeHtml(unit.code)}" required>
                </div>
                <div class="form-group">
                    <label>Unit Name *</label>
                    <input type="text" id="unitName" value="${escapeHtml(unit.name)}" required>
                </div>
                <div class="form-group">
                    <label>Status</label>
                    <select id="unitStatus">
                        <option value="true" ${unit.isActive ? 'selected' : ''}>Active</option>
                        <option value="false" ${!unit.isActive ? 'selected' : ''}>Inactive</option>
                    </select>
                </div>
                <div style="display: flex; gap: 10px; justify-content: flex-end; margin-top: 20px;">
                    <button type="button" class="btn-warning" onclick="closeModal()">Cancel</button>
                    <button type="submit" class="btn-primary">Update</button>
                </div>
            </form>
        `;
        showModal(modalContent);

        document.getElementById('editUnitForm').addEventListener('submit', async (e) => {
            e.preventDefault();
            await updateUnit(id);
        });
    } catch (err) {
        showNotification('Failed to load unit details', 'error');
    }
}

async function updateUnit(id) {
    const departmentId = document.getElementById('unitDepartmentId').value || null;
    const code = document.getElementById('unitCode').value;
    const name = document.getElementById('unitName').value;
    const isActive = document.getElementById('unitStatus').value === 'true';

    try {
        const response = await fetchWithAuth(`/admin/api/units/${id}`, {
            method: 'PUT',
            body: JSON.stringify({ code, name, departmentId, isActive })
        });

        if (response.ok) {
            showNotification('Unit updated successfully', 'success');
            closeModal();
            await loadUnits();
        } else {
            throw new Error('Update failed');
        }
    } catch (err) {
        showNotification('Failed to update unit', 'error');
    }
}

async function deleteUnit(id) {
    if (!confirm('Are you sure? This will also remove all programme links for this unit.')) {
        return;
    }

    try {
        const response = await fetchWithAuth(`/admin/api/units/${id}`, { method: 'DELETE' });
        if (response.ok) {
            showNotification('Unit deleted successfully', 'success');
            await loadUnits();
        } else {
            const error = await response.text();
            showNotification(error, 'error');
        }
    } catch (err) {
        showNotification('Failed to delete unit', 'error');
    }
}

async function manageUnitLinks(unitId) {
    try {
        const unitResponse = await fetchWithAuth(`/admin/api/units/${unitId}`);
        const unit = await unitResponse.json();

        const programmesResponse = await fetchWithAuth('/admin/api/programmes?activeOnly=true&pageSize=100');
        const programmesData = await programmesResponse.json();

        const modalContent = `
            <div class="modal-header">Link Unit to Programmes</div>
            <div class="unit-info">
                <strong>${escapeHtml(unit.code)} - ${escapeHtml(unit.name)}</strong>
            </div>
            <div class="linked-programmes-list">
                <h4>Currently Linked Programmes:</h4>
                <div id="currentLinks">
                    ${unit.programmes && unit.programmes.length > 0
                        ? unit.programmes.map(p => `
                            <div class="linked-programme-item">
                                <span>${escapeHtml(p.name)} (Year ${p.yearOfStudy})</span>
                                <button class="btn-danger btn-sm" onclick="unlinkProgramme('${unitId}', '${p.id}')">Remove</button>
                            </div>
                        `).join('')
                        : '<p>No linked programmes</p>'}
                </div>
            </div>
            <div class="link-programme-form">
                <h4>Link to Programme:</h4>
                <form id="linkProgrammeForm">
                    <div class="form-group">
                        <label>Programme</label>
                        <select id="linkProgrammeId" required>
                            <option value="">Select Programme</option>
                            ${programmesData.programmes.map(p => `<option value="${p.id}">${escapeHtml(p.name)}</option>`).join('')}
                        </select>
                    </div>
                    <div class="form-group">
                        <label>Year of Study</label>
                        <select id="linkYearOfStudy" required>
                            <option value="1">Year 1</option>
                            <option value="2">Year 2</option>
                            <option value="3">Year 3</option>
                            <option value="4">Year 4</option>
                        </select>
                    </div>
                    <div class="form-group">
                        <label>Semester</label>
                        <select id="linkSemester">
                            <option value="1">Semester 1</option>
                            <option value="2">Semester 2</option>
                        </select>
                    </div>
                    <div style="display: flex; gap: 10px; justify-content: flex-end; margin-top: 20px;">
                        <button type="button" class="btn-warning" onclick="closeModal()">Close</button>
                        <button type="submit" class="btn-primary">Link Programme</button>
                    </div>
                </form>
            </div>
        `;
        showModal(modalContent);

        document.getElementById('linkProgrammeForm').addEventListener('submit', async (e) => {
            e.preventDefault();
            await linkProgrammeToUnit(unitId);
        });

        window.unlinkProgramme = async function(unitId, programmeId) {
            if (!confirm('Remove this programme from the unit?')) return;

            try {
                const response = await fetchWithAuth(`/admin/api/units/${unitId}/programmes/${programmeId}`, {
                    method: 'DELETE'
                });

                if (response.ok) {
                    showNotification('Programme unlinked successfully', 'success');
                    closeModal();
                    await manageUnitLinks(unitId);
                    await loadUnits();
                } else {
                    throw new Error('Failed to unlink');
                }
            } catch (err) {
                showNotification('Failed to unlink programme', 'error');
            }
        };
    } catch (err) {
        showNotification('Failed to load unit details', 'error');
    }
}

async function linkProgrammeToUnit(unitId) {
    const programmeId = document.getElementById('linkProgrammeId').value;
    const yearOfStudy = parseInt(document.getElementById('linkYearOfStudy').value);
    const semester = parseInt(document.getElementById('linkSemester').value);

    if (!programmeId) {
        showNotification('Please select a programme', 'error');
        return;
    }

    try {
        const response = await fetchWithAuth(`/admin/api/units/${unitId}/link-programme`, {
            method: 'POST',
            body: JSON.stringify({ programmeId, yearOfStudy, semester })
        });

        if (response.ok) {
            showNotification('Programme linked successfully', 'success');
            closeModal();
            await loadUnits();
        } else {
            const error = await response.text();
            showNotification(error, 'error');
        }
    } catch (err) {
        showNotification('Failed to link programme', 'error');
    }
}