// static/admin/js/modules/universities.js
let universitiesCurrentPage = 1;
let universitiesCurrentSearch = '';
let universitiesPageSize = 20;

window.initUniversities = async function() {
    console.log('=== initUniversities START ===');
    await loadUniversities();
    console.log('=== initUniversities END ===');

    const searchInput = document.getElementById('searchUniversity');
    if (searchInput) {
        searchInput.addEventListener('keypress', function(e) {
            if (e.key === 'Enter') universitiesSearch();
        });
    }
};

async function loadUniversities() {
    const tbody = document.getElementById('universities-table-body');
    if (!tbody) return;

    tbody.innerHTML = '<tr><td colspan="6" class="loading-text">Loading...</td></tr>';

    try {
        let url = `/admin/api/universities?page=${universitiesCurrentPage}&pageSize=${universitiesPageSize}`;
        if (universitiesCurrentSearch) url += `&search=${encodeURIComponent(universitiesCurrentSearch)}`;

        const response = await fetchWithAuth(url);
        if (!response.ok) throw new Error('Failed to load universities');

        const data = await response.json();
        console.log('Universities loaded:', data);

        renderUniversities(data.universities);
        renderUniversitiesPagination(data);

    } catch (err) {
        console.error('Failed to load universities:', err);
        tbody.innerHTML = `<tr><td colspan="6" class="error-text">Failed to load: ${err.message}<br><button onclick="loadUniversities()">Retry</button></td></tr>`;
    }
}

function renderUniversities(universities) {
    const tbody = document.getElementById('universities-table-body');
    if (!tbody) return;

    if (!universities || universities.length === 0) {
        tbody.innerHTML = '<tr><td colspan="6" class="empty-text">No universities found</td></tr>';
        return;
    }

    tbody.innerHTML = universities.map(uni => `
        <tr>
            <td><strong>${escapeHtml(uni.name)}</strong></td>
            <td><span class="badge">${uni.departmentCount}</span></td>
            <td><span class="badge">${uni.programmeCount}</span></td>
            <td><span class="badge">${uni.unitCount}</span></td>
            <td>${new Date(uni.createdAt).toLocaleDateString()}</td>
            <td>
                <button class="btn-warning" onclick="editUniversity('${uni.id}')">Edit</button>
                <button class="btn-danger" onclick="deleteUniversity('${uni.id}')">Delete</button>
            </td>
        </tr>
    `).join('');
}

function renderUniversitiesPagination(data) {
    const paginationDiv = document.getElementById('pagination');
    if (!paginationDiv) return;

    const totalPages = Math.ceil(data.total / data.pageSize);
    if (totalPages <= 1) {
        paginationDiv.innerHTML = '';
        return;
    }

    let html = '<div class="pagination-controls">';
    if (universitiesCurrentPage > 1) {
        html += `<button onclick="goToUniversityPage(${universitiesCurrentPage - 1})" class="page-btn">← Previous</button>`;
    }

    for (let i = 1; i <= Math.min(totalPages, 5); i++) {
        html += `<button onclick="goToUniversityPage(${i})" class="page-btn ${i === universitiesCurrentPage ? 'active' : ''}">${i}</button>`;
    }

    if (universitiesCurrentPage < totalPages) {
        html += `<button onclick="goToUniversityPage(${universitiesCurrentPage + 1})" class="page-btn">Next →</button>`;
    }
    html += '</div>';
    paginationDiv.innerHTML = html;
}

function goToUniversityPage(page) {
    universitiesCurrentPage = page;
    loadUniversities();
}

function universitiesSearch() {
    universitiesCurrentSearch = document.getElementById('searchUniversity').value;
    universitiesCurrentPage = 1;
    loadUniversities();
}

function showCreateUniversityModal() {
    const modalContent = `
        <div class="modal-header">Add University</div>
        <form id="createUniversityForm">
            <div class="form-group">
                <label>University Name *</label>
                <input type="text" id="uniName" required>
            </div>
            <div style="display: flex; gap: 10px; justify-content: flex-end; margin-top: 20px;">
                <button type="button" class="btn-warning" onclick="closeModal()">Cancel</button>
                <button type="submit" class="btn-primary">Create</button>
            </div>
        </form>
    `;
    showModal(modalContent);
    document.getElementById('createUniversityForm').addEventListener('submit', async (e) => {
        e.preventDefault();
        await createUniversity();
    });
}

async function createUniversity() {
    const name = document.getElementById('uniName').value;
    if (!name) {
        showNotification('University name is required', 'error');
        return;
    }

    try {
        const response = await fetchWithAuth('/admin/api/universities', {
            method: 'POST',
            body: JSON.stringify({ name })
        });

        if (response.ok) {
            showNotification('University created successfully', 'success');
            closeModal();
            await loadUniversities();
        } else {
            const error = await response.text();
            showNotification(error, 'error');
        }
    } catch (err) {
        showNotification('Failed to create university', 'error');
    }
}

async function editUniversity(id) {
    try {
        const response = await fetchWithAuth(`/admin/api/universities/${id}`);
        const uni = await response.json();

        const modalContent = `
            <div class="modal-header">Edit University</div>
            <form id="editUniversityForm">
                <div class="form-group">
                    <label>University Name *</label>
                    <input type="text" id="uniName" value="${escapeHtml(uni.name)}" required>
                </div>
                <div style="display: flex; gap: 10px; justify-content: flex-end; margin-top: 20px;">
                    <button type="button" class="btn-warning" onclick="closeModal()">Cancel</button>
                    <button type="submit" class="btn-primary">Update</button>
                </div>
            </form>
        `;
        showModal(modalContent);

        document.getElementById('editUniversityForm').addEventListener('submit', async (e) => {
            e.preventDefault();
            await updateUniversity(id);
        });
    } catch (err) {
        showNotification('Failed to load university details', 'error');
    }
}

async function updateUniversity(id) {
    const name = document.getElementById('uniName').value;
    try {
        const response = await fetchWithAuth(`/admin/api/universities/${id}`, {
            method: 'PUT',
            body: JSON.stringify({ name })
        });

        if (response.ok) {
            showNotification('University updated successfully', 'success');
            closeModal();
            await loadUniversities();
        } else {
            throw new Error('Update failed');
        }
    } catch (err) {
        showNotification('Failed to update university', 'error');
    }
}

async function deleteUniversity(id) {
    if (!confirm('Are you sure? This will also delete all departments, programmes, and units under this university.')) {
        return;
    }

    try {
        const response = await fetchWithAuth(`/admin/api/universities/${id}`, { method: 'DELETE' });
        if (response.ok) {
            showNotification('University deleted successfully', 'success');
            await loadUniversities();
        } else {
            const error = await response.text();
            showNotification(error, 'error');
        }
    } catch (err) {
        showNotification('Failed to delete university', 'error');
    }
}