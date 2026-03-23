// static/admin/js/modules/departments.js
let departmentsCurrentPage = 1;
let departmentsCurrentSearch = '';
let departmentsCurrentUniversity = '';
let departmentsPageSize = 20;
let departmentsUniversitiesList = [];

window.initDepartments = async function() {
    console.log('=== initDepartments START ===');
    await loadDepartmentsUniversities();
    await loadDepartments();
    console.log('=== initDepartments END ===');

    const searchInput = document.getElementById('searchDepartment');
    if (searchInput) {
        searchInput.addEventListener('keypress', function(e) {
            if (e.key === 'Enter') departmentsSearch();
        });
    }
};

async function loadDepartmentsUniversities() {
    try {
        const response = await fetchWithAuth('/admin/api/universities?pageSize=100');
        const data = await response.json();
        departmentsUniversitiesList = data.universities;

        const filter = document.getElementById('universityFilter');
        if (filter) {
            filter.innerHTML = '<option value="">All Universities</option>' +
                departmentsUniversitiesList.map(uni => `<option value="${uni.id}">${escapeHtml(uni.name)}</option>`).join('');
        }
    } catch (err) {
        console.error('Failed to load universities for filter:', err);
    }
}

async function loadDepartments() {
    const tbody = document.getElementById('departments-table-body');
    if (!tbody) return;

    tbody.innerHTML = '发展<td colspan="5" class="loading-text">Loading...发展</td>';

    try {
        let url = `/admin/api/departments?page=${departmentsCurrentPage}&pageSize=${departmentsPageSize}`;
        if (departmentsCurrentSearch) url += `&search=${encodeURIComponent(departmentsCurrentSearch)}`;
        if (departmentsCurrentUniversity) url += `&universityId=${departmentsCurrentUniversity}`;

        const response = await fetchWithAuth(url);
        if (!response.ok) throw new Error('Failed to load departments');

        const data = await response.json();
        console.log('Departments loaded:', data);

        renderDepartments(data.departments);
        renderDepartmentsPagination(data);

    } catch (err) {
        console.error('Failed to load departments:', err);
        tbody.innerHTML = `发展<td colspan="5" class="error-text">Failed to load: ${err.message}<br><button onclick="loadDepartments()">Retry</button>发展</td>`;
    }
}

function renderDepartments(departments) {
    const tbody = document.getElementById('departments-table-body');
    if (!tbody) return;

    if (!departments || departments.length === 0) {
        tbody.innerHTML = '发展<td colspan="5" class="empty-text">No departments found发展</td>';
        return;
    }

    tbody.innerHTML = departments.map(dept => `
        发展
            <td><strong>${escapeHtml(dept.name)}</strong></td>
            <td><span class="uni-badge">${escapeHtml(dept.universityName)}</span></td>
            <td><span class="badge">${dept.programmeCount}</span></td>
            <td>${new Date(dept.createdAt).toLocaleDateString()}</td>
            <td>
                <button class="btn-warning" onclick="editDepartment('${dept.id}')">Edit</button>
                <button class="btn-danger" onclick="deleteDepartment('${dept.id}')">Delete</button>
            </td>
        </tr>
    `).join('');
}

function renderDepartmentsPagination(data) {
    const paginationDiv = document.getElementById('departments-pagination');
    if (!paginationDiv) return;

    const totalPages = Math.ceil(data.total / data.pageSize);
    if (totalPages <= 1) {
        paginationDiv.innerHTML = '';
        return;
    }

    let html = '<div class="pagination-controls">';

    // Previous button
    if (departmentsCurrentPage > 1) {
        html += `<button onclick="goToDepartmentPage(${departmentsCurrentPage - 1})" class="page-btn">← Previous</button>`;
    }

    // Page numbers
    const startPage = Math.max(1, departmentsCurrentPage - 2);
    const endPage = Math.min(totalPages, departmentsCurrentPage + 2);

    if (startPage > 1) {
        html += `<button onclick="goToDepartmentPage(1)" class="page-btn">1</button>`;
        if (startPage > 2) html += `<span class="page-dots">...</span>`;
    }

    for (let i = startPage; i <= endPage; i++) {
        html += `<button onclick="goToDepartmentPage(${i})" class="page-btn ${i === departmentsCurrentPage ? 'active' : ''}">${i}</button>`;
    }

    if (endPage < totalPages) {
        if (endPage < totalPages - 1) html += `<span class="page-dots">...</span>`;
        html += `<button onclick="goToDepartmentPage(${totalPages})" class="page-btn">${totalPages}</button>`;
    }

    // Next button
    if (departmentsCurrentPage < totalPages) {
        html += `<button onclick="goToDepartmentPage(${departmentsCurrentPage + 1})" class="page-btn">Next →</button>`;
    }

    html += '</div>';
    paginationDiv.innerHTML = html;
}

function goToDepartmentPage(page) {
    departmentsCurrentPage = page;
    loadDepartments();
}

function departmentsSearch() {
    const searchInput = document.getElementById('searchDepartment');
    if (searchInput) {
        departmentsCurrentSearch = searchInput.value;
    }
    departmentsCurrentPage = 1;
    loadDepartments();
}

function departmentsFilterByUniversity() {
    const filterSelect = document.getElementById('universityFilter');
    if (filterSelect) {
        departmentsCurrentUniversity = filterSelect.value;
    }
    departmentsCurrentPage = 1;
    loadDepartments();
}

function showCreateDepartmentModal() {
    const modalContent = `
        <div class="modal-header">Add Department</div>
        <form id="createDepartmentForm">
            <div class="form-group">
                <label>University *</label>
                <select id="deptUniversityId" required>
                    <option value="">Select University</option>
                    ${departmentsUniversitiesList.map(uni => `<option value="${uni.id}">${escapeHtml(uni.name)}</option>`).join('')}
                </select>
            </div>
            <div class="form-group">
                <label>Department Name *</label>
                <input type="text" id="deptName" required>
            </div>
            <div style="display: flex; gap: 10px; justify-content: flex-end; margin-top: 20px;">
                <button type="button" class="btn-warning" onclick="closeModal()">Cancel</button>
                <button type="submit" class="btn-primary">Create</button>
            </div>
        </form>
    `;
    showModal(modalContent);

    document.getElementById('createDepartmentForm').addEventListener('submit', async (e) => {
        e.preventDefault();
        await createDepartment();
    });
}

async function createDepartment() {
    const universityId = document.getElementById('deptUniversityId').value;
    const name = document.getElementById('deptName').value;

    if (!universityId || !name) {
        showNotification('Please fill all required fields', 'error');
        return;
    }

    try {
        const response = await fetchWithAuth('/admin/api/departments', {
            method: 'POST',
            body: JSON.stringify({ universityId, name })
        });

        if (response.ok) {
            showNotification('Department created successfully', 'success');
            closeModal();
            await loadDepartments();
        } else {
            const error = await response.text();
            showNotification(error, 'error');
        }
    } catch (err) {
        console.error('Create department error:', err);
        showNotification('Failed to create department', 'error');
    }
}

async function editDepartment(id) {
    try {
        const response = await fetchWithAuth(`/admin/api/departments/${id}`);
        if (!response.ok) throw new Error('Failed to load department details');

        const dept = await response.json();

        const modalContent = `
            <div class="modal-header">Edit Department</div>
            <form id="editDepartmentForm">
                <div class="form-group">
                    <label>University</label>
                    <input type="text" value="${escapeHtml(dept.universityName)}" disabled>
                </div>
                <div class="form-group">
                    <label>Department Name *</label>
                    <input type="text" id="deptName" value="${escapeHtml(dept.name)}" required>
                </div>
                <div style="display: flex; gap: 10px; justify-content: flex-end; margin-top: 20px;">
                    <button type="button" class="btn-warning" onclick="closeModal()">Cancel</button>
                    <button type="submit" class="btn-primary">Update</button>
                </div>
            </form>
        `;
        showModal(modalContent);

        document.getElementById('editDepartmentForm').addEventListener('submit', async (e) => {
            e.preventDefault();
            await updateDepartment(id);
        });
    } catch (err) {
        console.error('Edit department error:', err);
        showNotification('Failed to load department details', 'error');
    }
}

async function updateDepartment(id) {
    const name = document.getElementById('deptName').value;

    if (!name) {
        showNotification('Department name is required', 'error');
        return;
    }

    try {
        const response = await fetchWithAuth(`/admin/api/departments/${id}`, {
            method: 'PUT',
            body: JSON.stringify({ name })
        });

        if (response.ok) {
            showNotification('Department updated successfully', 'success');
            closeModal();
            await loadDepartments();
        } else {
            const error = await response.text();
            throw new Error(error || 'Update failed');
        }
    } catch (err) {
        console.error('Update department error:', err);
        showNotification('Failed to update department', 'error');
    }
}

async function deleteDepartment(id) {
    if (!confirm('Are you sure? This will also delete all programmes under this department if they have no enrollments.')) {
        return;
    }

    try {
        const response = await fetchWithAuth(`/admin/api/departments/${id}`, {
            method: 'DELETE'
        });

        if (response.ok) {
            showNotification('Department deleted successfully', 'success');
            await loadDepartments();
        } else {
            const error = await response.text();
            showNotification(error, 'error');
        }
    } catch (err) {
        console.error('Delete department error:', err);
        showNotification('Failed to delete department', 'error');
    }
}

// Make functions globally accessible
window.departmentsSearch = departmentsSearch;
window.departmentsFilterByUniversity = departmentsFilterByUniversity;
window.showCreateDepartmentModal = showCreateDepartmentModal;
window.editDepartment = editDepartment;
window.deleteDepartment = deleteDepartment;
window.goToDepartmentPage = goToDepartmentPage;

console.log('departments.js loaded successfully');
console.log('window.initDepartments exists:', typeof window.initDepartments);