// api.js - Centralized API calls
async function fetchWithAuth(url, options = {}) {
    let response = await fetch(url, {
        ...options,
        headers: {
            ...options.headers,
            'Authorization': `Bearer ${accessToken}`,
            'Content-Type': 'application/json'
        }
    });

    if (response.status === 401) {
        const refreshed = await refreshAccessToken();
        if (refreshed) {
            response = await fetch(url, {
                ...options,
                headers: {
                    ...options.headers,
                    'Authorization': `Bearer ${accessToken}`,
                    'Content-Type': 'application/json'
                }
            });
        } else {
            window.location.href = '/admin/login';
        }
    }
    return response;
}

async function refreshAccessToken() {
    try {
        const response = await fetch('/admin/api/refresh', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ refreshToken })
        });

        if (response.ok) {
            const data = await response.json();
            accessToken = data.accessToken;
            refreshToken = data.refreshToken;
            localStorage.setItem('accessToken', accessToken);
            localStorage.setItem('refreshToken', refreshToken);
            return true;
        }
    } catch (err) {
        console.error('Refresh failed:', err);
    }
    return false;
}

// Admin API calls - Return the response directly for better error handling
const AdminAPI = {
    getAll: () => fetchWithAuth('/admin/api/admins').then(res => res.json()),
    getById: (id) => fetchWithAuth(`/admin/api/admins/${id}`).then(res => res.json()),
    create: async (data) => {
        const response = await fetchWithAuth('/admin/api/admins', {
            method: 'POST',
            body: JSON.stringify(data)
        });
        // Return both status and data
        if (response.ok) {
            const adminData = await response.json();
            return { ok: true, data: adminData, status: response.status };
        } else {
            const errorText = await response.text();
            return { ok: false, error: errorText, status: response.status };
        }
    },
    update: async (id, data) => {
        const response = await fetchWithAuth(`/admin/api/admins/${id}`, {
            method: 'PUT',
            body: JSON.stringify(data)
        });
        if (response.ok) {
            const result = await response.json();
            return { ok: true, data: result };
        } else {
            const errorText = await response.text();
            return { ok: false, error: errorText };
        }
    },
    delete: async (id) => {
        const response = await fetchWithAuth(`/admin/api/admins/${id}`, {
            method: 'DELETE'
        });
        if (response.ok) {
            const result = await response.json();
            return { ok: true, data: result };
        } else {
            const errorText = await response.text();
            return { ok: false, error: errorText };
        }
    },
    resetPassword: async (id, newPassword) => {
        const response = await fetchWithAuth(`/admin/api/admins/${id}/reset-password`, {
            method: 'POST',
            body: JSON.stringify({ newPassword })
        });
        if (response.ok) {
            const result = await response.json();
            return { ok: true, data: result };
        } else {
            const errorText = await response.text();
            return { ok: false, error: errorText };
        }
    }
};