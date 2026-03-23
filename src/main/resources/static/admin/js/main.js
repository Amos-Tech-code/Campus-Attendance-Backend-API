// main.js - Core application logic
let accessToken = localStorage.getItem('accessToken');
let refreshToken = localStorage.getItem('refreshToken');
let currentModule = 'dashboard';

// Navigation
document.querySelectorAll('.menu-item').forEach(item => {
    item.addEventListener('click', async () => {
        const module = item.getAttribute('data-module');
        if (module === currentModule) return;

        currentModule = module;

        // Update active state
        document.querySelectorAll('.menu-item').forEach(m => m.classList.remove('active'));
        item.classList.add('active');

        // Load module
        await loadModule(module);
    });
});

async function loadModule(moduleName) {
    const container = document.getElementById('module-content');

    // Show loading
    container.innerHTML = `
        <div class="loading-container">
            <div class="spinner">
                <div class="double-bounce1"></div>
                <div class="double-bounce2"></div>
            </div>
            <div class="loading-text">Loading ${moduleName.replace(/-/g, ' ')}...</div>
        </div>
    `;

    try {
        // Fetch module HTML
        const response = await fetch(`/templates/admin/modules/${moduleName}.html`);
        if (!response.ok) throw new Error('Module not found');

        const html = await response.text();
        container.innerHTML = html;

        console.log(`HTML loaded for ${moduleName}`);

        // Wait a bit for DOM to be ready
        await new Promise(resolve => setTimeout(resolve, 100));

        // Map module names to their init function names
        const moduleInitMap = {
            'dashboard': 'initDashboard',
            'admins': 'initAdmins',
            'lecturers': 'initLecturers',
            'students': 'initStudents',
            'universities': 'initUniversities',
            'departments': 'initDepartments',
            'programmes': 'initProgrammes',
            'units': 'initUnits',
            'academic-terms': 'initAcademicTerms',
            'device-requests': 'initDeviceRequests',
            'suspicious-activity': 'initSuspiciousActivity',
            'attendance-reports': 'initAttendanceReports',
            'system-settings': 'initSystemSettings',
            'notification-templates': 'initNotificationTemplates'
        };

        // Get the correct init function name
        const initFunctionName = moduleInitMap[moduleName];
        const initFunction = window[initFunctionName];

        console.log(`Looking for ${initFunctionName}:`, initFunction);

        if (initFunction && typeof initFunction === 'function') {
            console.log(`Calling ${initFunctionName}...`);
            await initFunction();
            console.log(`${initFunctionName} completed`);
        } else {
            console.warn(`No init function found for ${moduleName}`);
            // Try direct call as fallback
            const fallbackFunctions = {
                'dashboard': window.initDashboard,
                'admins': window.initAdmins,
                'lecturers': window.initLecturers,
                'students': window.initStudents
            };

            const fallbackFn = fallbackFunctions[moduleName];
            if (fallbackFn && typeof fallbackFn === 'function') {
                console.log(`Calling fallback function for ${moduleName}`);
                await fallbackFn();
            } else {
                console.error(`No fallback function found for ${moduleName}`);
                // Show a message that the module is under development
                container.innerHTML = `
                    <div class="info-container">
                        <div style="text-align: center; padding: 60px;">
                            <div style="font-size: 48px; margin-bottom: 20px;">🚧</div>
                            <h3>${moduleName.replace(/-/g, ' ')} Module</h3>
                            <p style="color: #666; margin-top: 10px;">This feature is under development</p>
                        </div>
                    </div>
                `;
            }
        }
    } catch (error) {
        console.error('Failed to load module:', error);
        container.innerHTML = `
            <div class="error-container">
                <h3>⚠️ Failed to Load Module</h3>
                <p>${error.message}</p>
                <button class="retry-btn" onclick="loadModule('${moduleName}')">Retry</button>
            </div>
        `;
    }
}

function capitalize(str) {
    return str.replace(/-([a-z])/g, (g) => g[1].toUpperCase());
}

// Utility functions
function escapeHtml(str) {
    if (!str) return '';
    return str
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

function showNotification(message, type = 'info') {
    const notification = document.createElement('div');
    notification.className = `notification ${type}`;
    notification.innerHTML = `
        <div class="notification-content" style="background: ${type === 'success' ? '#4caf50' : type === 'error' ? '#f44336' : '#2196f3'};
                    color: white; padding: 12px 20px; border-radius: 4px; margin-bottom: 10px;
                    box-shadow: 0 2px 5px rgba(0,0,0,0.2);">
            ${message}
        </div>
    `;

    const mainContent = document.querySelector('.main-content');
    mainContent.insertBefore(notification, mainContent.firstChild);

    setTimeout(() => {
        notification.remove();
    }, 3000);
}

function closeModal() {
    const modal = document.getElementById('dynamic-modal');
    if (modal) {
        modal.remove();
    }
}

function showModal(content) {
    closeModal();
    const modal = document.createElement('div');
    modal.id = 'dynamic-modal';
    modal.className = 'modal active';
    modal.innerHTML = `
        <div class="modal-content">
            ${content}
        </div>
    `;
    document.body.appendChild(modal);
}

async function logout() {
    try {
        await fetch('/admin/api/logout', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ refreshToken })
        });
    } catch (err) {}
    finally {
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        window.location.href = '/admin/login';
    }
}

// Load initial module
loadModule('dashboard');