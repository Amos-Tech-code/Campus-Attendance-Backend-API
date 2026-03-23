// modules/admins.js
let currentEditAdminId = null;

// Define the init function globally
window.initAdmins = async function() {
    await loadAdminsList();
};

async function loadAdminsList() {
    const tbody = document.getElementById('admins-table-body');
    if (!tbody) {
        console.error('admins-table-body not found!');
        return;
    }

    tbody.innerHTML = '<td colspan="6" class="loading-text">Loading...</td></tr>';

    try {
        if (typeof AdminAPI === 'undefined') {
            console.error('AdminAPI is not defined!');
            throw new Error('AdminAPI not available');
        }

        const admins = await AdminAPI.getAll();
        console.log('Admins loaded:', admins);

        tbody.innerHTML = admins.map(admin => `
            <tr>
                <td>${escapeHtml(admin.fullName)}</td>
                <td>${escapeHtml(admin.email)}</td>
                <td><span class="role-badge">${admin.role}</span></td>
                <td>${admin.isActive ? '<span class="status-active">✅ Active</span>' : '<span class="status-inactive">❌ Inactive</span>'}</td>
                <td>${admin.lastLoginAt ? new Date(admin.lastLoginAt).toLocaleString() : 'Never'}</td>
                <td>
                    <button class="btn-warning" onclick="window.editAdmin('${admin.id}')">Edit</button>
                    <button class="btn-danger" onclick="window.deleteAdmin('${admin.id}')">Delete</button>
                    <button class="btn-primary" onclick="window.resetAdminPassword('${admin.id}')">Reset Password</button>
                </td>
            </tr>
        `).join('');
    } catch (err) {
        console.error('Failed to load admins:', err);
        tbody.innerHTML = '<tr><td colspan="6" class="error-text">Failed to load admins. <button onclick="loadAdminsList()">Retry</button></td></tr>';
    }
}

window.showCreateAdminModal = function() {
    const modalContent = `
        <div class="modal-header">Create New Admin</div>
        <form id="createAdminForm">
            <div class="form-group">
                <label>Full Name *</label>
                <input type="text" id="fullName" required>
            </div>
            <div class="form-group">
                <label>Email *</label>
                <input type="email" id="email" required>
            </div>
            <div class="form-group">
                <label>Password *</label>
                <input type="password" id="password" required minlength="6">
            </div>
            <div class="form-group">
                <label>Role</label>
                <select id="role">
                    <option value="ADMIN">Admin</option>
                    <option value="SUPER_ADMIN">Super Admin</option>
                </select>
            </div>
            <div style="display: flex; gap: 10px; justify-content: flex-end; margin-top: 20px;">
                <button type="button" class="btn-warning" onclick="closeModal()">Cancel</button>
                <button type="submit" class="btn-primary">Create Admin</button>
            </div>
        </form>
    `;

    showModal(modalContent);

    document.getElementById('createAdminForm').addEventListener('submit', async (e) => {
        e.preventDefault();
        await createAdmin();
    });
};

async function createAdmin() {
    const fullName = document.getElementById('fullName').value;
    const email = document.getElementById('email').value;
    const password = document.getElementById('password').value;
    const role = document.getElementById('role').value;

    if (!fullName || !email || !password) {
        showNotification('Please fill all required fields', 'error');
        return;
    }

    try {
        const result = await AdminAPI.create({ fullName, email, password, role });
        console.log('Create admin result:', result);

        if (result.ok) {
            showNotification('Admin created successfully', 'success');
            closeModal();
            await loadAdminsList();
        } else {
            // Check if it's a duplicate email error (400)
            if (result.status === 400) {
                showNotification(result.error || 'Email already exists', 'error');
            } else {
                showNotification(result.error || 'Failed to create admin', 'error');
            }
        }
    } catch (err) {
        console.error('Create admin error:', err);
        showNotification('Failed to create admin: ' + err.message, 'error');
    }
}

window.editAdmin = async function(adminId) {
    currentEditAdminId = adminId;

    try {
        const admin = await AdminAPI.getById(adminId);

        const modalContent = `
            <div class="modal-header">Edit Admin</div>
            <form id="editAdminForm">
                <div class="form-group">
                    <label>Full Name *</label>
                    <input type="text" id="fullName" value="${escapeHtml(admin.fullName)}" required>
                </div>
                <div class="form-group">
                    <label>Email</label>
                    <input type="email" value="${escapeHtml(admin.email)}" disabled>
                    <small style="color: #999;">Email cannot be changed</small>
                </div>
                <div class="form-group">
                    <label>Role</label>
                    <select id="role">
                        <option value="ADMIN" ${admin.role === 'ADMIN' ? 'selected' : ''}>Admin</option>
                        <option value="SUPER_ADMIN" ${admin.role === 'SUPER_ADMIN' ? 'selected' : ''}>Super Admin</option>
                    </select>
                </div>
                <div class="form-group">
                    <label>Status</label>
                    <select id="isActive">
                        <option value="true" ${admin.isActive ? 'selected' : ''}>Active</option>
                        <option value="false" ${!admin.isActive ? 'selected' : ''}>Inactive</option>
                    </select>
                </div>
                <div style="display: flex; gap: 10px; justify-content: flex-end; margin-top: 20px;">
                    <button type="button" class="btn-warning" onclick="closeModal()">Cancel</button>
                    <button type="submit" class="btn-primary">Update Admin</button>
                </div>
            </form>
        `;

        showModal(modalContent);

        document.getElementById('editAdminForm').addEventListener('submit', async (e) => {
            e.preventDefault();
            await updateAdmin();
        });

    } catch (err) {
        console.error('Failed to load admin details:', err);
        showNotification('Failed to load admin details', 'error');
    }
};

async function updateAdmin() {
    const fullName = document.getElementById('fullName').value;
    const role = document.getElementById('role').value;
    const isActive = document.getElementById('isActive').value === 'true';

    try {
        const result = await AdminAPI.update(currentEditAdminId, { fullName, role, isActive });

        if (result.ok) {
            showNotification('Admin updated successfully', 'success');
            closeModal();
            await loadAdminsList();
        } else {
            showNotification(result.error || 'Failed to update admin', 'error');
        }
    } catch (err) {
        console.error('Update admin error:', err);
        showNotification('Failed to update admin', 'error');
    }
}

window.deleteAdmin = async function(adminId) {
    if (!confirm('Are you sure you want to delete this admin? This action cannot be undone.')) {
        return;
    }

    try {
        const result = await AdminAPI.delete(adminId);

        if (result.ok) {
            showNotification('Admin deleted successfully', 'success');
            await loadAdminsList();
        } else {
            showNotification(result.error || 'Failed to delete admin', 'error');
        }
    } catch (err) {
        console.error('Delete admin error:', err);
        showNotification('Failed to delete admin', 'error');
    }
};

window.resetAdminPassword = async function(adminId) {
    const newPassword = prompt('Enter new password for this admin (minimum 6 characters):');

    if (!newPassword) return;
    if (newPassword.length < 6) {
        showNotification('Password must be at least 6 characters', 'error');
        return;
    }

    try {
        const result = await AdminAPI.resetPassword(adminId, newPassword);

        if (result.ok) {
            showNotification('Password reset successfully', 'success');
        } else {
            showNotification(result.error || 'Failed to reset password', 'error');
        }
    } catch (err) {
        console.error('Reset password error:', err);
        showNotification('Failed to reset password', 'error');
    }
};