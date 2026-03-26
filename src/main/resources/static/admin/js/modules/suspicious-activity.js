// static/admin/js/modules/suspicious-activity.js
let suspiciousCurrentPage = 1;
let suspiciousCurrentSearch = '';
let suspiciousCurrentUnit = '';
let suspiciousPageSize = 20;
let suspiciousUnitsList = [];

window.initSuspiciousActivity = async function() {
    console.log('=== initSuspiciousActivity START ===');
    await suspiciousLoadUnits();
    await suspiciousLoadStats();
    await suspiciousLoadActivities();
    console.log('=== initSuspiciousActivity END ===');

    const searchInput = document.getElementById('searchSuspicious');
    if (searchInput) {
        searchInput.addEventListener('keypress', function(e) {
            if (e.key === 'Enter') suspiciousSearch();
        });
    }
};

async function suspiciousLoadUnits() {
    try {
        const response = await fetchWithAuth('/admin/api/units?pageSize=100&activeOnly=true');
        if (!response.ok) throw new Error('Failed to load units');

        const data = await response.json();
        suspiciousUnitsList = data.units || [];

        const filter = document.getElementById('unitFilter');
        if (filter) {
            filter.innerHTML = '<option value="">All Units</option>' +
                suspiciousUnitsList.map(unit => `<option value="${unit.id}">${escapeHtml(unit.code)} - ${escapeHtml(unit.name)}</option>`).join('');
        }
    } catch (err) {
        console.error('Failed to load units:', err);
    }
}

async function suspiciousLoadStats() {
    try {
        const response = await fetchWithAuth('/admin/api/suspicious-activity/stats');
        if (!response.ok) throw new Error('Failed to load stats');

        const stats = await response.json();

        // Check if elements exist before setting textContent
        const totalSuspiciousEl = document.getElementById('totalSuspicious');
        const pendingReviewEl = document.getElementById('pendingReview');

        if (totalSuspiciousEl) {
            totalSuspiciousEl.textContent = stats.totalSuspicious || 0;
        }
        if (pendingReviewEl) {
            pendingReviewEl.textContent = stats.pendingReview || 0;
        }
    } catch (err) {
        console.error('Failed to load stats:', err);
        // Set default values on error
        const totalSuspiciousEl = document.getElementById('totalSuspicious');
        const pendingReviewEl = document.getElementById('pendingReview');
        if (totalSuspiciousEl) totalSuspiciousEl.textContent = '0';
        if (pendingReviewEl) pendingReviewEl.textContent = '0';
    }
}

async function suspiciousLoadActivities() {
    const tbody = document.getElementById('suspicious-table-body');
    if (!tbody) return;

    tbody.innerHTML = '<td colspan="9" class="loading-text">Loading...<th';

    try {
        let url = `/admin/api/suspicious-activity?page=${suspiciousCurrentPage}&pageSize=${suspiciousPageSize}`;
        if (suspiciousCurrentSearch) url += `&search=${encodeURIComponent(suspiciousCurrentSearch)}`;
        if (suspiciousCurrentUnit) url += `&unitId=${suspiciousCurrentUnit}`;

        const response = await fetchWithAuth(url);
        if (!response.ok) throw new Error('Failed to load suspicious activities');

        const data = await response.json();
        console.log('Suspicious activities loaded:', data);

        suspiciousRenderActivities(data.activities);
        suspiciousRenderPagination(data);

    } catch (err) {
        console.error('Failed to load suspicious activities:', err);
        tbody.innerHTML = `发展<td colspan="9" class="error-text">Failed to load: ${err.message}<br><button onclick="suspiciousLoadActivities()">Retry</button>发展<th`;
    }
}

function suspiciousRenderActivities(activities) {
    const tbody = document.getElementById('suspicious-table-body');
    if (!tbody) return;

    if (!activities || activities.length === 0) {
        tbody.innerHTML = '<td colspan="9" class="empty-text">No suspicious activities found<th';
        return;
    }

    tbody.innerHTML = activities.map(activity => {
        const locationStatus = suspiciousGetLocationStatus(activity);
        const deviceStatus = suspiciousGetDeviceStatus(activity);

        return `
            <tr class="suspicious-row">
                <td><strong>${escapeHtml(activity.studentName)}</strong></td>
                <td>${escapeHtml(activity.studentRegNo)}</td>
                <td>
                    <div><strong>${escapeHtml(activity.unitCode)}</strong></div>
                    <div><small>${escapeHtml(activity.unitName)}</small></div>
                </td>
                <td><span class="method-badge">${activity.attendanceMethodUsed}</span></td>
                <td class="reason-cell">
                    <span class="reason-badge ${suspiciousGetReasonClass(activity.suspiciousReason)}">
                        ${escapeHtml(activity.suspiciousReason || 'Unknown')}
                    </span>
                </td>
                <td>
                    <div class="status-indicator ${locationStatus.class}">
                        ${locationStatus.icon} ${locationStatus.text}
                    </div>
                    ${activity.distanceFromLecturer ? `<div class="distance-info">${Math.round(activity.distanceFromLecturer)}m away</div>` : ''}
                </td>
                <td>
                    <div class="status-indicator ${deviceStatus.class}">
                        ${deviceStatus.icon} ${deviceStatus.text}
                    </div>
                    <div><small>${activity.deviceId ? activity.deviceId.substring(0, 8) + '...' : 'N/A'}</small></div>
                </td>
                <td>${new Date(activity.attendedAt).toLocaleString()}</td>
                <td>
                    <button class="btn-info" onclick="suspiciousViewActivity('${activity.id}')">View Details</button>
                    <button class="btn-success" onclick="suspiciousClearActivity('${activity.id}')">Clear</button>
                </td>
            </tr>
        `;
    }).join('');
}

function suspiciousGetLocationStatus(activity) {
    if (!activity.isLocationVerified) {
        return {
            class: 'status-warning',
            icon: '⚠️',
            text: 'Location Mismatch'
        };
    }
    if (activity.distanceFromLecturer && activity.distanceFromLecturer > 100) {
        return {
            class: 'status-warning',
            icon: '📍',
            text: 'Far from lecturer'
        };
    }
    return {
        class: 'status-success',
        icon: '✅',
        text: 'Verified'
    };
}

function suspiciousGetDeviceStatus(activity) {
    if (!activity.isDeviceVerified) {
        return {
            class: 'status-warning',
            icon: '⚠️',
            text: 'Device Mismatch'
        };
    }
    return {
        class: 'status-success',
        icon: '✅',
        text: 'Verified'
    };
}

function suspiciousGetReasonClass(reason) {
    if (!reason) return 'reason-unknown';
    if (reason.includes('location')) return 'reason-location';
    if (reason.includes('device')) return 'reason-device';
    if (reason.includes('method')) return 'reason-method';
    return 'reason-other';
}

function suspiciousRenderPagination(data) {
    const paginationDiv = document.getElementById('suspicious-pagination');
    if (!paginationDiv) return;

    const totalPages = Math.ceil(data.total / data.pageSize);
    if (totalPages <= 1) {
        paginationDiv.innerHTML = '';
        return;
    }

    let html = '<div class="pagination-controls">';

    if (suspiciousCurrentPage > 1) {
        html += `<button onclick="suspiciousGoToPage(${suspiciousCurrentPage - 1})" class="page-btn">← Previous</button>`;
    }

    const startPage = Math.max(1, suspiciousCurrentPage - 2);
    const endPage = Math.min(totalPages, suspiciousCurrentPage + 2);

    if (startPage > 1) {
        html += `<button onclick="suspiciousGoToPage(1)" class="page-btn">1</button>`;
        if (startPage > 2) html += `<span class="page-dots">...</span>`;
    }

    for (let i = startPage; i <= endPage; i++) {
        html += `<button onclick="suspiciousGoToPage(${i})" class="page-btn ${i === suspiciousCurrentPage ? 'active' : ''}">${i}</button>`;
    }

    if (endPage < totalPages) {
        if (endPage < totalPages - 1) html += `<span class="page-dots">...</span>`;
        html += `<button onclick="suspiciousGoToPage(${totalPages})" class="page-btn">${totalPages}</button>`;
    }

    if (suspiciousCurrentPage < totalPages) {
        html += `<button onclick="suspiciousGoToPage(${suspiciousCurrentPage + 1})" class="page-btn">Next →</button>`;
    }

    html += '</div>';
    paginationDiv.innerHTML = html;
}

function suspiciousGoToPage(page) {
    suspiciousCurrentPage = page;
    suspiciousLoadActivities();
}

function suspiciousSearch() {
    const searchInput = document.getElementById('searchSuspicious');
    if (searchInput) {
        suspiciousCurrentSearch = searchInput.value;
    }
    suspiciousCurrentPage = 1;
    suspiciousLoadActivities();
}

function suspiciousFilterByUnit() {
    const filterSelect = document.getElementById('unitFilter');
    if (filterSelect) {
        suspiciousCurrentUnit = filterSelect.value;
    }
    suspiciousCurrentPage = 1;
    suspiciousLoadActivities();
}

async function suspiciousViewActivity(activityId) {
    try {
        const response = await fetchWithAuth(`/admin/api/suspicious-activity/${activityId}`);
        if (!response.ok) throw new Error('Failed to load activity details');

        const activity = await response.json();

        const modalContent = `
            <div class="modal-header">Suspicious Activity Details</div>
            <div class="activity-details">
                <div class="detail-section">
                    <h4>Student Information</h4>
                    <div class="detail-row"><strong>Name:</strong> ${escapeHtml(activity.studentName)}</div>
                    <div class="detail-row"><strong>Registration No:</strong> ${escapeHtml(activity.studentRegNo)}</div>
                </div>

                <div class="detail-section">
                    <h4>Session Information</h4>
                    <div class="detail-row"><strong>Unit:</strong> ${escapeHtml(activity.unitCode)} - ${escapeHtml(activity.unitName)}</div>
                    <div class="detail-row"><strong>Session:</strong> ${escapeHtml(activity.sessionTitle || 'N/A')}</div>
                    <div class="detail-row"><strong>Lecturer:</strong> ${escapeHtml(activity.lecturerName || 'N/A')}</div>
                    <div class="detail-row"><strong>Method Used:</strong> ${activity.attendanceMethodUsed}</div>
                    <div class="detail-row"><strong>Time:</strong> ${new Date(activity.attendedAt).toLocaleString()}</div>
                </div>

                <div class="detail-section">
                    <h4>Suspicious Details</h4>
                    <div class="detail-row"><strong>Reason:</strong> <span class="reason-badge ${suspiciousGetReasonClass(activity.suspiciousReason)}">${escapeHtml(activity.suspiciousReason || 'Unknown')}</span></div>
                </div>

                <div class="detail-section">
                    <h4>Location Information</h4>
                    <div class="detail-row"><strong>Location Verified:</strong> ${activity.isLocationVerified ? '✅ Yes' : '❌ No'}</div>
                    ${activity.studentLatitude ? `<div class="detail-row"><strong>Student Location:</strong> ${activity.studentLatitude}, ${activity.studentLongitude}</div>` : ''}
                    ${activity.distanceFromLecturer ? `<div class="detail-row"><strong>Distance from Lecturer:</strong> ${Math.round(activity.distanceFromLecturer)} meters</div>` : ''}
                </div>

                <div class="detail-section">
                    <h4>Device Information</h4>
                    <div class="detail-row"><strong>Device Verified:</strong> ${activity.isDeviceVerified ? '✅ Yes' : '❌ No'}</div>
                    <div class="detail-row"><strong>Device ID:</strong> <code>${escapeHtml(activity.deviceId || 'N/A')}</code></div>
                </div>
            </div>
            <div style="display: flex; gap: 10px; justify-content: flex-end; margin-top: 20px;">
                <button class="btn-success" onclick="suspiciousClearActivity('${activity.id}')">Clear Suspicious Flag</button>
                <button class="btn-primary" onclick="closeModal()">Close</button>
            </div>
        `;
        showModal(modalContent);
    } catch (err) {
        console.error('Failed to load activity details:', err);
        showNotification('Failed to load activity details', 'error');
    }
}

async function suspiciousClearActivity(activityId) {
    if (!confirm('Are you sure you want to clear the suspicious flag for this attendance record?')) {
        return;
    }

    try {
        const response = await fetchWithAuth(`/admin/api/suspicious-activity/${activityId}/review`, {
            method: 'POST',
            body: JSON.stringify({ isSuspicious: false, notes: 'Cleared by admin' })
        });

        if (response.ok) {
            showNotification('Suspicious flag cleared successfully', 'success');
            closeModal();
            await suspiciousLoadStats();
            await suspiciousLoadActivities();
        } else {
            const error = await response.text();
            showNotification(error, 'error');
        }
    } catch (err) {
        console.error('Failed to clear suspicious flag:', err);
        showNotification('Failed to clear suspicious flag', 'error');
    }
}

// Make functions globally accessible with unique names
window.suspiciousSearch = suspiciousSearch;
window.suspiciousFilterByUnit = suspiciousFilterByUnit;
window.suspiciousViewActivity = suspiciousViewActivity;
window.suspiciousClearActivity = suspiciousClearActivity;
window.suspiciousGoToPage = suspiciousGoToPage;
window.suspiciousLoadActivities = suspiciousLoadActivities;

console.log('suspicious-activity.js loaded successfully');
console.log('window.initSuspiciousActivity exists:', typeof window.initSuspiciousActivity);