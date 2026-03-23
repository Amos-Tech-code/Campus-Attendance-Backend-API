// static/admin/js/modules/device-requests.js
let deviceRequestsCurrentPage = 1;
let deviceRequestsCurrentSearch = '';
let deviceRequestsCurrentStatus = '';
let deviceRequestsPageSize = 20;

window.initDeviceRequests = async function() {
    console.log('=== initDeviceRequests START ===');
    await loadDeviceRequests();
    console.log('=== initDeviceRequests END ===');

    const searchInput = document.getElementById('searchRequest');
    if (searchInput) {
        searchInput.addEventListener('keypress', function(e) {
            if (e.key === 'Enter') deviceRequestsSearch();
        });
    }
};

async function loadDeviceRequests() {
    const tbody = document.getElementById('device-requests-table-body');
    if (!tbody) return;

    tbody.innerHTML = '<td colspan="8" class="loading-text">Loading...</tr>';

    try {
        let url = `/admin/api/device-change-requests?page=${deviceRequestsCurrentPage}&pageSize=${deviceRequestsPageSize}`;
        if (deviceRequestsCurrentSearch) url += `&search=${encodeURIComponent(deviceRequestsCurrentSearch)}`;
        if (deviceRequestsCurrentStatus) url += `&status=${deviceRequestsCurrentStatus}`;

        const response = await fetchWithAuth(url);
        if (!response.ok) throw new Error('Failed to load device change requests');

        const data = await response.json();
        console.log('Device change requests loaded:', data);

        renderDeviceRequests(data.requests);
        renderDeviceRequestsPagination(data);

    } catch (err) {
        console.error('Failed to load device change requests:', err);
        tbody.innerHTML = `发展<td colspan="8" class="error-text">Failed to load: ${err.message}<br><button onclick="loadDeviceRequests()">Retry</button>发展</tr>`;
    }
}

function renderDeviceRequests(requests) {
    const tbody = document.getElementById('device-requests-table-body');
    if (!tbody) return;

    if (!requests || requests.length === 0) {
        tbody.innerHTML = '发展<td colspan="8" class="empty-text">No device change requests found发展</tr>';
        return;
    }

    tbody.innerHTML = requests.map(request => {
        const statusClass = getStatusClass(request.status);
        const statusText = getStatusText(request.status);

        return `
            <tr>
                <td><strong>${escapeHtml(request.studentName)}</strong><br><small>${escapeHtml(request.studentEmail || '')}</small></td>
                <td>${escapeHtml(request.studentRegNo)}</td>
                <td><code class="device-id">${escapeHtml(request.oldDeviceId.substring(0, 8))}...</code></td>
                <td>
                    <div><strong>${escapeHtml(request.newDeviceInfo.model)}</strong></div>
                    <div><small>${escapeHtml(request.newDeviceInfo.os)}</small></div>
                    <div><code class="device-id">${escapeHtml(request.newDeviceInfo.deviceId.substring(0, 8))}...</code></div>
                </td>
                <td class="reason-cell">${escapeHtml(request.reason || 'No reason provided')}</td>
                <td><span class="status-badge ${statusClass}">${statusText}</span></td>
                <td>${new Date(request.requestedAt).toLocaleString()}</td>
                <td>
                    <button class="btn-info" onclick="viewDeviceRequest('${request.id}')">View</button>
                    ${request.status === 'PENDING' ? `
                        <button class="btn-success" onclick="approveDeviceRequest('${request.id}')">Approve</button>
                        <button class="btn-danger" onclick="rejectDeviceRequest('${request.id}')">Reject</button>
                    ` : ''}
                </td>
            </tr>
        `;
    }).join('');
}

function getStatusClass(status) {
    switch(status) {
        case 'PENDING': return 'status-pending';
        case 'APPROVED': return 'status-active';
        case 'REJECTED': return 'status-inactive';
        case 'CANCELLED': return 'status-cancelled';
        default: return '';
    }
}

function getStatusText(status) {
    switch(status) {
        case 'PENDING': return '⏳ Pending';
        case 'APPROVED': return '✅ Approved';
        case 'REJECTED': return '❌ Rejected';
        case 'CANCELLED': return '🚫 Cancelled';
        default: return status;
    }
}

function renderDeviceRequestsPagination(data) {
    const paginationDiv = document.getElementById('device-requests-pagination');
    if (!paginationDiv) return;

    const totalPages = Math.ceil(data.total / data.pageSize);
    if (totalPages <= 1) {
        paginationDiv.innerHTML = '';
        return;
    }

    let html = '<div class="pagination-controls">';

    if (deviceRequestsCurrentPage > 1) {
        html += `<button onclick="goToDeviceRequestPage(${deviceRequestsCurrentPage - 1})" class="page-btn">← Previous</button>`;
    }

    const startPage = Math.max(1, deviceRequestsCurrentPage - 2);
    const endPage = Math.min(totalPages, deviceRequestsCurrentPage + 2);

    if (startPage > 1) {
        html += `<button onclick="goToDeviceRequestPage(1)" class="page-btn">1</button>`;
        if (startPage > 2) html += `<span class="page-dots">...</span>`;
    }

    for (let i = startPage; i <= endPage; i++) {
        html += `<button onclick="goToDeviceRequestPage(${i})" class="page-btn ${i === deviceRequestsCurrentPage ? 'active' : ''}">${i}</button>`;
    }

    if (endPage < totalPages) {
        if (endPage < totalPages - 1) html += `<span class="page-dots">...</span>`;
        html += `<button onclick="goToDeviceRequestPage(${totalPages})" class="page-btn">${totalPages}</button>`;
    }

    if (deviceRequestsCurrentPage < totalPages) {
        html += `<button onclick="goToDeviceRequestPage(${deviceRequestsCurrentPage + 1})" class="page-btn">Next →</button>`;
    }

    html += '</div>';
    paginationDiv.innerHTML = html;
}

function goToDeviceRequestPage(page) {
    deviceRequestsCurrentPage = page;
    loadDeviceRequests();
}

function deviceRequestsSearch() {
    deviceRequestsCurrentSearch = document.getElementById('searchRequest').value;
    deviceRequestsCurrentPage = 1;
    loadDeviceRequests();
}

function deviceRequestsFilterByStatus() {
    deviceRequestsCurrentStatus = document.getElementById('statusFilterRequest').value;
    deviceRequestsCurrentPage = 1;
    loadDeviceRequests();
}

async function viewDeviceRequest(requestId) {
    try {
        const response = await fetchWithAuth(`/admin/api/device-change-requests/${requestId}`);
        const request = await response.json();

        const modalContent = `
            <div class="modal-header">Device Change Request Details</div>
            <div class="request-details">
                <div class="detail-section">
                    <h4>Student Information</h4>
                    <div class="detail-row"><strong>Name:</strong> ${escapeHtml(request.studentName)}</div>
                    <div class="detail-row"><strong>Registration No:</strong> ${escapeHtml(request.studentRegNo)}</div>
                    <div class="detail-row"><strong>Email:</strong> ${escapeHtml(request.studentEmail || 'N/A')}</div>
                </div>

                <div class="detail-section">
                    <h4>Old Device</h4>
                    <div class="detail-row"><strong>Device ID:</strong> <code>${escapeHtml(request.oldDeviceId)}</code></div>
                </div>

                <div class="detail-section">
                    <h4>New Device</h4>
                    <div class="detail-row"><strong>Device ID:</strong> <code>${escapeHtml(request.newDeviceInfo.deviceId)}</code></div>
                    <div class="detail-row"><strong>Model:</strong> ${escapeHtml(request.newDeviceInfo.model)}</div>
                    <div class="detail-row"><strong>OS:</strong> ${escapeHtml(request.newDeviceInfo.os)}</div>
                    ${request.newDeviceInfo.fcmToken ? `<div class="detail-row"><strong>FCM Token:</strong> <code>${escapeHtml(request.newDeviceInfo.fcmToken)}</code></div>` : ''}
                </div>

                <div class="detail-section">
                    <h4>Request Information</h4>
                    <div class="detail-row"><strong>Reason:</strong> ${escapeHtml(request.reason || 'No reason provided')}</div>
                    <div class="detail-row"><strong>Status:</strong> <span class="status-badge ${getStatusClass(request.status)}">${getStatusText(request.status)}</span></div>
                    <div class="detail-row"><strong>Requested At:</strong> ${new Date(request.requestedAt).toLocaleString()}</div>
                    ${request.reviewedAt ? `<div class="detail-row"><strong>Reviewed At:</strong> ${new Date(request.reviewedAt).toLocaleString()}</div>` : ''}
                    ${request.reviewedBy ? `<div class="detail-row"><strong>Reviewed By:</strong> ${escapeHtml(request.reviewedBy)}</div>` : ''}
                    ${request.rejectionReason ? `<div class="detail-row"><strong>Rejection Reason:</strong> ${escapeHtml(request.rejectionReason)}</div>` : ''}
                </div>
            </div>
            <div style="display: flex; gap: 10px; justify-content: flex-end; margin-top: 20px;">
                <button class="btn-primary" onclick="closeModal()">Close</button>
            </div>
        `;
        showModal(modalContent);
    } catch (err) {
        showNotification('Failed to load request details', 'error');
    }
}

async function approveDeviceRequest(requestId) {
    if (!confirm('Are you sure you want to approve this device change request? The student will be able to use the new device.')) {
        return;
    }

    try {
        const response = await fetchWithAuth(`/admin/api/device-change-requests/${requestId}/review`, {
            method: 'POST',
            body: JSON.stringify({ approve: true })
        });

        if (response.ok) {
            showNotification('Device change request approved successfully', 'success');
            await loadDeviceRequests();
        } else {
            const error = await response.text();
            showNotification(error, 'error');
        }
    } catch (err) {
        showNotification('Failed to approve request', 'error');
    }
}

async function rejectDeviceRequest(requestId) {
    const rejectionReason = prompt('Please enter a reason for rejection:');
    if (rejectionReason === null) return;

    if (!rejectionReason.trim()) {
        showNotification('Rejection reason is required', 'error');
        return;
    }

    if (!confirm('Are you sure you want to reject this device change request?')) {
        return;
    }

    try {
        const response = await fetchWithAuth(`/admin/api/device-change-requests/${requestId}/review`, {
            method: 'POST',
            body: JSON.stringify({ approve: false, rejectionReason })
        });

        if (response.ok) {
            showNotification('Device change request rejected successfully', 'success');
            await loadDeviceRequests();
        } else {
            const error = await response.text();
            showNotification(error, 'error');
        }
    } catch (err) {
        showNotification('Failed to reject request', 'error');
    }
}

// Make functions globally accessible
window.deviceRequestsSearch = deviceRequestsSearch;
window.deviceRequestsFilterByStatus = deviceRequestsFilterByStatus;
window.viewDeviceRequest = viewDeviceRequest;
window.approveDeviceRequest = approveDeviceRequest;
window.rejectDeviceRequest = rejectDeviceRequest;
window.goToDeviceRequestPage = goToDeviceRequestPage;

console.log('device-requests.js loaded successfully');
console.log('window.initDeviceRequests exists:', typeof window.initDeviceRequests);