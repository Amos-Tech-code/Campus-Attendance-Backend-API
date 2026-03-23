// static/admin/js/modules/storage-management.js
let storageCurrentPage = 1;
let storageCurrentSearch = '';
let storageCurrentFilter = 'all';
let storagePageSize = 20;
let allFiles = [];

window.initStorageManagement = async function() {
    console.log('=== initStorageManagement START ===');
    await loadStats();
    await loadFiles();
    console.log('=== initStorageManagement END ===');

    const searchInput = document.getElementById('searchFile');
    if (searchInput) {
        searchInput.addEventListener('keypress', function(e) {
            if (e.key === 'Enter') storageSearch();
        });
    }
};

async function loadStats() {
    try {
        const response = await fetchWithAuth('/admin/api/storage/stats');
        const stats = await response.json();

        document.getElementById('totalFiles').textContent = stats.totalFiles;
        document.getElementById('storageUsed').textContent = stats.storageUsedMB.toFixed(2) + ' MB';
        document.getElementById('orphanedFiles').textContent = stats.orphanedFiles;
        document.getElementById('expiredFiles').textContent = stats.expiredFiles;
    } catch (err) {
        console.error('Failed to load stats:', err);
    }
}

async function loadFiles() {
    const tbody = document.getElementById('storage-table-body');
    if (!tbody) return;

    tbody.innerHTML = '<tr><td colspan="7" class="loading-text">Loading...</td></tr>';

    try {
        let url = `/admin/api/storage/files`;
        if (storageCurrentFilter === 'orphaned') url += '?type=orphaned';
        else if (storageCurrentFilter === 'expired') url += '?type=expired';

        const response = await fetchWithAuth(url);
        if (!response.ok) throw new Error('Failed to load files');

        allFiles = await response.json();
        console.log('Files loaded:', allFiles);

        renderFiles();
        renderPagination();

    } catch (err) {
        console.error('Failed to load files:', err);
        tbody.innerHTML = '<tr><td colspan="7" class="error-text">Failed to load. <button onclick="loadFiles()">Retry</button></td></tr>';
    }
}

function renderFiles() {
    const tbody = document.getElementById('storage-table-body');
    if (!tbody) return;

    let filteredFiles = allFiles;

    // Apply search filter
    if (storageCurrentSearch) {
        filteredFiles = filteredFiles.filter(file =>
            file.fileName.toLowerCase().includes(storageCurrentSearch.toLowerCase())
        );
    }

    // Apply type filter
    if (storageCurrentFilter === 'qr') {
        filteredFiles = filteredFiles.filter(file => file.fileType === 'QR_CODE');
    } else if (storageCurrentFilter === 'pdf') {
        filteredFiles = filteredFiles.filter(file => file.fileType === 'PDF');
    } else if (storageCurrentFilter === 'csv') {
        filteredFiles = filteredFiles.filter(file => file.fileType === 'CSV');
    }

    // Pagination
    const start = (storageCurrentPage - 1) * storagePageSize;
    const end = start + storagePageSize;
    const paginatedFiles = filteredFiles.slice(start, end);

    if (paginatedFiles.length === 0) {
        tbody.innerHTML = '<tr><td colspan="7" class="empty-text">No files found</td></tr>';
        return;
    }

    tbody.innerHTML = paginatedFiles.map(file => {
        const fileTypeIcon = getFileTypeIcon(file.fileType);
        const fileSize = formatFileSize(file.fileSize);
        const statusClass = file.isActive ? 'status-active' : 'status-inactive';
        const statusText = file.isActive ? 'Active' : 'Orphaned';

        return `
            <tr>
                <td>
                    <div><strong>${escapeHtml(file.fileName)}</strong></div>
                    <div class="file-url"><small>${escapeHtml(file.fileUrl.substring(0, 50))}...</small></div>
                </td>
                <td><span class="file-type-badge">${fileTypeIcon} ${file.fileType}</span></td>
                <td>${fileSize}</td>
                <td>${file.createdAt ? new Date(file.createdAt).toLocaleString() : 'N/A'}</td>
                <td>${file.expiresAt ? new Date(file.expiresAt).toLocaleString() : 'Never'}</td>
                <td><span class="status-badge ${statusClass}">${statusText}</span></td>
                <td>
                    <button class="btn-info" onclick="viewFileDetails('${file.id}')">Details</button>
                    <button class="btn-danger" onclick="deleteFile('${file.id}')">Delete</button>
                </td>
            </tr>
        `;
    }).join('');
}

function renderPagination() {
    const paginationDiv = document.getElementById('storage-pagination');
    if (!paginationDiv) return;

    let filteredFiles = allFiles;
    if (storageCurrentSearch) {
        filteredFiles = filteredFiles.filter(file =>
            file.fileName.toLowerCase().includes(storageCurrentSearch.toLowerCase())
        );
    }
    if (storageCurrentFilter === 'qr') {
        filteredFiles = filteredFiles.filter(file => file.fileType === 'QR_CODE');
    } else if (storageCurrentFilter === 'pdf') {
        filteredFiles = filteredFiles.filter(file => file.fileType === 'PDF');
    } else if (storageCurrentFilter === 'csv') {
        filteredFiles = filteredFiles.filter(file => file.fileType === 'CSV');
    }

    const totalPages = Math.ceil(filteredFiles.length / storagePageSize);
    if (totalPages <= 1) {
        paginationDiv.innerHTML = '';
        return;
    }

    let html = '<div class="pagination-controls">';

    if (storageCurrentPage > 1) {
        html += `<button onclick="goToStoragePage(${storageCurrentPage - 1})" class="page-btn">← Previous</button>`;
    }

    const startPage = Math.max(1, storageCurrentPage - 2);
    const endPage = Math.min(totalPages, storageCurrentPage + 2);

    if (startPage > 1) {
        html += `<button onclick="goToStoragePage(1)" class="page-btn">1</button>`;
        if (startPage > 2) html += `<span class="page-dots">...</span>`;
    }

    for (let i = startPage; i <= endPage; i++) {
        html += `<button onclick="goToStoragePage(${i})" class="page-btn ${i === storageCurrentPage ? 'active' : ''}">${i}</button>`;
    }

    if (endPage < totalPages) {
        if (endPage < totalPages - 1) html += `<span class="page-dots">...</span>`;
        html += `<button onclick="goToStoragePage(${totalPages})" class="page-btn">${totalPages}</button>`;
    }

    if (storageCurrentPage < totalPages) {
        html += `<button onclick="goToStoragePage(${storageCurrentPage + 1})" class="page-btn">Next →</button>`;
    }

    html += '</div>';
    paginationDiv.innerHTML = html;
}

function goToStoragePage(page) {
    storageCurrentPage = page;
    renderFiles();
    renderPagination();
}

function storageSearch() {
    storageCurrentSearch = document.getElementById('searchFile').value;
    storageCurrentPage = 1;
    renderFiles();
    renderPagination();
}

function storageFilterByType() {
    storageCurrentFilter = document.getElementById('fileTypeFilter').value;
    storageCurrentPage = 1;
    if (storageCurrentFilter === 'orphaned' || storageCurrentFilter === 'expired') {
        loadFiles(); // Reload from server for these filters
    } else {
        renderFiles();
        renderPagination();
    }
}

function getFileTypeIcon(type) {
    switch(type) {
        case 'QR_CODE': return '📱';
        case 'PDF': return '📄';
        case 'CSV': return '📊';
        case 'EXCEL': return '📈';
        default: return '📁';
    }
}

function formatFileSize(bytes) {
    if (!bytes) return 'N/A';
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
}

async function viewFileDetails(fileId) {
    const file = allFiles.find(f => f.id === fileId);
    if (!file) return;

    const modalContent = `
        <div class="modal-header">File Details</div>
        <div class="file-details">
            <div class="detail-row"><strong>File Name:</strong> ${escapeHtml(file.fileName)}</div>
            <div class="detail-row"><strong>Type:</strong> ${file.fileType}</div>
            <div class="detail-row"><strong>Size:</strong> ${formatFileSize(file.fileSize)}</div>
            <div class="detail-row"><strong>URL:</strong> <code class="file-url-full">${escapeHtml(file.fileUrl)}</code></div>
            <div class="detail-row"><strong>Created:</strong> ${file.createdAt ? new Date(file.createdAt).toLocaleString() : 'N/A'}</div>
            <div class="detail-row"><strong>Expires:</strong> ${file.expiresAt ? new Date(file.expiresAt).toLocaleString() : 'Never'}</div>
            <div class="detail-row"><strong>Associated With:</strong> ${file.associatedWith || 'N/A'}</div>
            <div class="detail-row"><strong>Status:</strong> <span class="status-badge ${file.isActive ? 'status-active' : 'status-inactive'}">${file.isActive ? 'Active' : 'Orphaned'}</span></div>
        </div>
        <div style="display: flex; gap: 10px; justify-content: flex-end; margin-top: 20px;">
            <a href="${file.fileUrl}" target="_blank" class="btn-primary">Open File</a>
            <button class="btn-danger" onclick="deleteFile('${file.id}')">Delete File</button>
            <button class="btn-warning" onclick="closeModal()">Close</button>
        </div>
    `;
    showModal(modalContent);
}

function showCleanupModal() {
    const modalContent = `
        <div class="modal-header">Clean Up Files</div>
        <form id="cleanupForm">
            <div class="form-group">
                <label>Cleanup Type</label>
                <select id="cleanupType" required>
                    <option value="orphaned">Orphaned Files (from ended sessions)</option>
                    <option value="expired">Expired Files</option>
                    <option value="all">All Inactive Files</option>
                </select>
            </div>
            <div class="form-group">
                <label>Older Than (days)</label>
                <input type="number" id="olderThanDays" placeholder="Leave empty for all" min="1">
                <small>Only delete files older than this many days</small>
            </div>
            <div class="warning-box">
                ⚠️ Warning: This action cannot be undone. Deleted files will be permanently removed from storage.
            </div>
            <div style="display: flex; gap: 10px; justify-content: flex-end; margin-top: 20px;">
                <button type="button" class="btn-warning" onclick="closeModal()">Cancel</button>
                <button type="submit" class="btn-danger">Start Cleanup</button>
            </div>
        </form>
    `;
    showModal(modalContent);

    document.getElementById('cleanupForm').addEventListener('submit', async (e) => {
        e.preventDefault();
        await performCleanup();
    });
}

async function performCleanup() {
    const cleanupType = document.getElementById('cleanupType').value;
    const olderThanDays = document.getElementById('olderThanDays').value;

    if (!confirm(`Are you sure you want to delete ${cleanupType} files? This action cannot be undone.`)) {
        return;
    }

    closeModal();
    showNotification('Starting cleanup... This may take a moment.', 'info');

    try {
        const request = {
            cleanupType: cleanupType,
            olderThanDays: olderThanDays ? parseInt(olderThanDays) : null
        };

        const response = await fetchWithAuth('/admin/api/storage/cleanup', {
            method: 'POST',
            body: JSON.stringify(request)
        });

        if (response.ok) {
            const result = await response.json();
            showCleanupResult(result);
            await loadStats();
            await loadFiles();
        } else {
            throw new Error('Cleanup failed');
        }
    } catch (err) {
        showNotification('Failed to clean up files: ' + err.message, 'error');
    }
}

function showCleanupResult(result) {
    const modalContent = `
        <div class="modal-header">Cleanup Results</div>
        <div class="cleanup-results">
            <div class="result-summary">
                <div class="result-stat">✅ Deleted: ${result.deletedCount} files</div>
                <div class="result-stat">❌ Failed: ${result.failedCount} files</div>
                <div class="result-stat">💾 Freed Space: ${result.freedSpaceMB.toFixed(2)} MB</div>
            </div>
            ${result.details.length > 0 ? `
                <details class="result-details">
                    <summary>View Details (${result.details.length} items)</summary>
                    <div class="details-list">
                        ${result.details.map(detail => `
                            <div class="detail-item ${detail.success ? 'success' : 'error'}">
                                <div><strong>${escapeHtml(detail.fileName)}</strong></div>
                                <div>${detail.success ? '✅ Deleted' : '❌ Failed: ' + escapeHtml(detail.error || 'Unknown error')}</div>
                            </div>
                        `).join('')}
                    </div>
                </details>
            ` : ''}
        </div>
        <div style="display: flex; gap: 10px; justify-content: flex-end; margin-top: 20px;">
            <button class="btn-primary" onclick="closeModal()">Close</button>
        </div>
    `;
    showModal(modalContent);
}

async function deleteFile(fileId) {
    if (!confirm('Are you sure you want to delete this file?')) return;

    try {
        const response = await fetchWithAuth(`/admin/api/storage/files/${fileId}`, {
            method: 'DELETE'
        });

        if (response.ok) {
            showNotification('File deleted successfully', 'success');
            await loadStats();
            await loadFiles();
        } else {
            throw new Error('Delete failed');
        }
    } catch (err) {
        showNotification('Failed to delete file', 'error');
    }
}

// Make functions globally accessible
window.storageSearch = storageSearch;
window.storageFilterByType = storageFilterByType;
window.viewFileDetails = viewFileDetails;
window.deleteFile = deleteFile;
window.showCleanupModal = showCleanupModal;
window.goToStoragePage = goToStoragePage;

console.log('storage-management.js loaded successfully');
console.log('window.initStorageManagement exists:', typeof window.initStorageManagement);