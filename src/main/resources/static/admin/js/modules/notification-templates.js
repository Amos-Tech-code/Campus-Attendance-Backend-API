// static/admin/js/modules/notification-templates.js
let historyCurrentPage = 1;
let historyCurrentSearch = '';
let historyCurrentRecipientType = '';
let historyCurrentNotificationType = '';
let historyPageSize = 20;
let totalHistoryPages = 1;

window.initNotificationTemplates = async function() {
    console.log('=== initNotificationTemplates START ===');
    await loadTemplates();
    await loadNotificationHistory();
    console.log('=== initNotificationTemplates END ===');

    // Setup broadcast form
    document.getElementById('broadcastForm')?.addEventListener('submit', async (e) => {
        e.preventDefault();
        await sendBroadcast();
    });
};

function switchTab(tabName) {
    // Update tab buttons
    document.querySelectorAll('.tab-btn').forEach(btn => btn.classList.remove('active'));
    document.querySelector(`.tab-btn[onclick="switchTab('${tabName}')"]`).classList.add('active');

    // Update tab content
    document.querySelectorAll('.tab-content').forEach(content => content.classList.remove('active'));
    document.getElementById(`${tabName}-tab`).classList.add('active');

    // Refresh data if needed
    if (tabName === 'history') {
        loadNotificationHistory();
    }
}

async function loadTemplates() {
    try {
        const response = await fetchWithAuth('/admin/api/notifications/templates');
        if (!response.ok) throw new Error('Failed to load templates');

        const templates = await response.json();
        renderTemplates(templates);
    } catch (err) {
        console.error('Failed to load templates:', err);
        document.getElementById('templates-grid').innerHTML =
            '<div class="error-container">Failed to load templates</div>';
    }
}

function renderTemplates(templates) {
    const container = document.getElementById('templates-grid');
    const searchTerm = document.getElementById('searchTemplate')?.value.toLowerCase() || '';

    const filteredTemplates = templates.filter(t =>
        t.type.toLowerCase().includes(searchTerm) ||
        t.title.toLowerCase().includes(searchTerm) ||
        t.body.toLowerCase().includes(searchTerm)
    );

    if (filteredTemplates.length === 0) {
        container.innerHTML = '<div class="empty-text">No templates found</div>';
        return;
    }

    container.innerHTML = filteredTemplates.map(template => `
        <div class="template-card">
            <div class="template-header">
                <span class="template-type">${template.type}</span>
                <span class="template-recipient ${template.recipientType.toLowerCase()}">${template.recipientType}</span>
            </div>
            <div class="template-title">
                <strong>Title:</strong> ${escapeHtml(template.title)}
            </div>
            <div class="template-body">
                <strong>Message:</strong> ${escapeHtml(template.body)}
            </div>
            <div class="template-meta">
                <span class="persist-badge ${template.persistToDatabase ? 'persist-true' : 'persist-false'}">
                    ${template.persistToDatabase ? '💾 Persisted' : '⚡ Transient'}
                </span>
                <span class="status-badge ${template.isEnabled ? 'status-active' : 'status-inactive'}">
                    ${template.isEnabled ? 'Enabled' : 'Disabled'}
                </span>
                <button class="btn-info btn-sm" onclick="editTemplate('${template.type}')">Edit</button>
            </div>
        </div>
    `).join('');
}

function filterTemplates() {
    loadTemplates();
}

async function editTemplate(templateType) {
    try {
        const response = await fetchWithAuth('/admin/api/notifications/templates');
        const templates = await response.json();
        const template = templates.find(t => t.type === templateType);

        if (!template) return;

        const modalContent = `
            <div class="modal-header">Edit Notification Template</div>
            <form id="editTemplateForm">
                <div class="form-group">
                    <label>Type</label>
                    <input type="text" value="${escapeHtml(template.type)}" disabled>
                </div>
                <div class="form-group">
                    <label>Title</label>
                    <input type="text" id="editTitle" value="${escapeHtml(template.title)}" required>
                    <small>Default: ${escapeHtml(template.defaultTitle)}</small>
                </div>
                <div class="form-group">
                    <label>Message Body</label>
                    <textarea id="editBody" rows="3" required>${escapeHtml(template.body)}</textarea>
                    <small>Default: ${escapeHtml(template.defaultBody)}</small>
                </div>
                <div class="form-group">
                    <label>
                        <input type="checkbox" id="editEnabled" ${template.isEnabled ? 'checked' : ''}>
                        Enable this notification
                    </label>
                </div>
                <div style="display: flex; gap: 10px; justify-content: flex-end; margin-top: 20px;">
                    <button type="button" class="btn-warning" onclick="closeModal()">Cancel</button>
                    <button type="submit" class="btn-primary">Save Changes</button>
                </div>
            </form>
        `;
        showModal(modalContent);

        document.getElementById('editTemplateForm').addEventListener('submit', async (e) => {
            e.preventDefault();
            await updateTemplate(templateType);
        });
    } catch (err) {
        showNotification('Failed to load template details', 'error');
    }
}

async function updateTemplate(templateType) {
    const title = document.getElementById('editTitle').value;
    const body = document.getElementById('editBody').value;
    const isEnabled = document.getElementById('editEnabled').checked;

    try {
        const response = await fetchWithAuth(`/admin/api/notifications/templates/${templateType}`, {
            method: 'PUT',
            body: JSON.stringify({ title, body, isEnabled })
        });

        if (response.ok) {
            showNotification('Template updated successfully', 'success');
            closeModal();
            await loadTemplates();
        } else {
            throw new Error('Update failed');
        }
    } catch (err) {
        showNotification('Failed to update template', 'error');
    }
}

async function loadNotificationHistory() {
    const tbody = document.getElementById('history-table-body');
    if (!tbody) return;

    tbody.innerHTML = '<tr><td colspan="6" class="loading-text">Loading...</td></tr>';

    try {
        let url = `/admin/api/notifications/history?page=${historyCurrentPage}&pageSize=${historyPageSize}`;
        if (historyCurrentSearch) url += `&search=${encodeURIComponent(historyCurrentSearch)}`;
        if (historyCurrentRecipientType) url += `&recipientType=${historyCurrentRecipientType}`;
        if (historyCurrentNotificationType) url += `&notificationType=${historyCurrentNotificationType}`;

        const response = await fetchWithAuth(url);
        if (!response.ok) throw new Error('Failed to load history');

        const data = await response.json();
        totalHistoryPages = Math.ceil(data.total / data.pageSize);

        renderNotificationHistory(data.notifications);
        renderHistoryPagination(data);
    } catch (err) {
        console.error('Failed to load history:', err);
        tbody.innerHTML = '<tr><td colspan="6" class="error-text">Failed to load history</td></tr>';
    }
}

function renderNotificationHistory(notifications) {
    const tbody = document.getElementById('history-table-body');
    if (!tbody) return;

    if (!notifications || notifications.length === 0) {
        tbody.innerHTML = '<tr><td colspan="6" class="empty-text">No notifications found</td></tr>';
        return;
    }

    tbody.innerHTML = notifications.map(notif => `
        <tr class="${notif.isRead ? '' : 'unread'}">
            <td>
                <strong>${escapeHtml(notif.recipientName)}</strong><br>
                <small class="recipient-type">${notif.recipientType}</small>
            </td>
            <td><span class="notif-type-badge">${escapeHtml(notif.type)}</span></td>
            <td class="notif-title">${escapeHtml(notif.title)}</td>
            <td class="notif-message">${escapeHtml(notif.message.substring(0, 100))}${notif.message.length > 100 ? '...' : ''}</td>
            <td>${notif.isRead ? '✅ Read' : '🟡 Unread'}</td>
            <td>${new Date(notif.createdAt).toLocaleString()}</td>
        </tr>
    `).join('');
}

function renderHistoryPagination(data) {
    const paginationDiv = document.getElementById('history-pagination');
    if (!paginationDiv) return;

    const totalPages = Math.ceil(data.total / data.pageSize);
    if (totalPages <= 1) {
        paginationDiv.innerHTML = '';
        return;
    }

    let html = '<div class="pagination-controls">';
    if (historyCurrentPage > 1) {
        html += `<button onclick="goToHistoryPage(${historyCurrentPage - 1})" class="page-btn">← Previous</button>`;
    }

    const startPage = Math.max(1, historyCurrentPage - 2);
    const endPage = Math.min(totalPages, historyCurrentPage + 2);

    if (startPage > 1) {
        html += `<button onclick="goToHistoryPage(1)" class="page-btn">1</button>`;
        if (startPage > 2) html += `<span class="page-dots">...</span>`;
    }

    for (let i = startPage; i <= endPage; i++) {
        html += `<button onclick="goToHistoryPage(${i})" class="page-btn ${i === historyCurrentPage ? 'active' : ''}">${i}</button>`;
    }

    if (endPage < totalPages) {
        if (endPage < totalPages - 1) html += `<span class="page-dots">...</span>`;
        html += `<button onclick="goToHistoryPage(${totalPages})" class="page-btn">${totalPages}</button>`;
    }

    if (historyCurrentPage < totalPages) {
        html += `<button onclick="goToHistoryPage(${historyCurrentPage + 1})" class="page-btn">Next →</button>`;
    }
    html += '</div>';
    paginationDiv.innerHTML = html;
}

function goToHistoryPage(page) {
    historyCurrentPage = page;
    loadNotificationHistory();
}

async function sendBroadcast() {
    const recipient = document.getElementById('broadcastRecipient').value;
    const type = document.getElementById('broadcastType').value;
    const title = document.getElementById('broadcastTitle').value;
    const message = document.getElementById('broadcastMessage').value;
    const persist = document.getElementById('broadcastPersist').checked;

    if (!title || !message) {
        showNotification('Please fill all required fields', 'error');
        return;
    }

    if (!confirm(`Are you sure you want to send this broadcast to ${recipient}?`)) {
        return;
    }

    try {
        const response = await fetchWithAuth('/admin/api/notifications/broadcast', {
            method: 'POST',
            body: JSON.stringify({
                recipientType: recipient,
                type: type,
                title: title,
                message: message,
                persistToDatabase: persist
            })
        });

        if (response.ok) {
            showNotification('Broadcast sent successfully', 'success');
            document.getElementById('broadcastTitle').value = '';
            document.getElementById('broadcastMessage').value = '';
        } else {
            throw new Error('Failed to send broadcast');
        }
    } catch (err) {
        showNotification('Failed to send broadcast: ' + err.message, 'error');
    }
}

// Setup search on history tab
window.loadNotificationHistory = loadNotificationHistory;
window.filterTemplates = filterTemplates;
window.editTemplate = editTemplate;
window.goToHistoryPage = goToHistoryPage;
window.switchTab = switchTab;

console.log('notification-templates.js loaded successfully');
console.log('window.initNotificationTemplates exists:', typeof window.initNotificationTemplates);