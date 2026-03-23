// modules/dashboard.js
let attendanceChart = null;

// Define the init function globally with a unique name
window.initDashboard = async function() {
    console.log('=== initDashboard START ===');
    console.log('Current module-content:', document.getElementById('module-content'));
    console.log('statsGrid exists:', !!document.getElementById('statsGrid'));
    console.log('activitiesList exists:', !!document.getElementById('activitiesList'));
    console.log('attendanceChart canvas exists:', !!document.getElementById('attendanceChart'));

    await loadDashboardData();
    console.log('=== initDashboard END ===');
};

async function loadDashboardData() {
    console.log('loadDashboardData called');
    try {
        // Check if we have the fetchWithAuth function
        if (typeof fetchWithAuth === 'undefined') {
            console.error('fetchWithAuth is not defined!');
            throw new Error('fetchWithAuth function not available');
        }

        console.log('Calling /admin/api/dashboard...');
        const response = await fetchWithAuth('/admin/api/dashboard');
        console.log('Dashboard API response status:', response.status);

        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(`Failed to load dashboard data: ${errorText}`);
        }

        const data = await response.json();
        console.log('Dashboard data received:', data);

        // Update admin name in navbar
        const adminNameSpan = document.getElementById('adminName');
        if (adminNameSpan) {
            adminNameSpan.textContent = data.adminName || 'Admin';
        }

        // Update stats grid
        const statsGrid = document.getElementById('statsGrid');
        if (statsGrid) {
            statsGrid.innerHTML = `
                <div class="stat-card">
                    <div class="stat-value">${data.totalStudents}</div>
                    <div class="stat-label">Total Students</div>
                </div>
                <div class="stat-card">
                    <div class="stat-value">${data.totalLecturers}</div>
                    <div class="stat-label">Total Lecturers</div>
                </div>
                <div class="stat-card">
                    <div class="stat-value">${data.totalUniversities}</div>
                    <div class="stat-label">Universities</div>
                </div>
                <div class="stat-card">
                    <div class="stat-value">${data.totalProgrammes}</div>
                    <div class="stat-label">Programmes</div>
                </div>
                <div class="stat-card">
                    <div class="stat-value">${data.totalSessions}</div>
                    <div class="stat-label">Total Sessions</div>
                </div>
                <div class="stat-card">
                    <div class="stat-value">${data.todaySessions}</div>
                    <div class="stat-label">Today's Sessions</div>
                </div>
                <div class="stat-card">
                    <div class="stat-value">${data.totalAttendance}</div>
                    <div class="stat-label">Attendance Records</div>
                </div>
            `;
        } else {
            console.error('statsGrid element not found!');
        }

        // Update activities list
        const activitiesList = document.getElementById('activitiesList');
        if (activitiesList) {
            if (data.recentActivities && data.recentActivities.length > 0) {
                activitiesList.innerHTML = data.recentActivities.map(activity => `
                    <div class="activity-item">
                        <div>
                            <span class="activity-type">${activity.type}</span>
                            <span class="activity-desc">${escapeHtml(activity.description)}</span>
                        </div>
                        <div class="activity-time">${new Date(activity.timestamp).toLocaleString()}</div>
                    </div>
                `).join('');
            } else {
                activitiesList.innerHTML = '<div class="activity-item">No recent activities</div>';
            }
        } else {
            console.error('activitiesList element not found!');
        }

        // Create chart
        const canvas = document.getElementById('attendanceChart');
        if (canvas) {
            createAttendanceChart(canvas, data.attendanceData || [65, 59, 80, 81, 56, 55, 40]);
        } else {
            console.error('attendanceChart canvas not found!');
        }

    } catch (err) {
        console.error('Failed to load dashboard:', err);
        const container = document.getElementById('module-content');
        if (container) {
            container.innerHTML = `
                <div class="error-container">
                    <h3>⚠️ Failed to Load Dashboard</h3>
                    <p>${err.message}</p>
                    <button class="retry-btn" onclick="window.initDashboard()">Retry</button>
                </div>
            `;
        }
    }
}

function createAttendanceChart(canvas, attendanceData) {
    const ctx = canvas.getContext('2d');
    if (attendanceChart) attendanceChart.destroy();

    const labels = [];
    const today = new Date();
    for (let i = 6; i >= 0; i--) {
        const date = new Date(today);
        date.setDate(date.getDate() - i);
        labels.push(date.toLocaleDateString('en-US', { weekday: 'short', month: 'short', day: 'numeric' }));
    }

    attendanceChart = new Chart(ctx, {
        type: 'line',
        data: {
            labels: labels,
            datasets: [{
                label: 'Daily Attendance',
                data: attendanceData,
                borderColor: '#667eea',
                backgroundColor: 'rgba(102, 126, 234, 0.1)',
                borderWidth: 3,
                pointBackgroundColor: '#667eea',
                pointBorderColor: '#fff',
                pointBorderWidth: 2,
                pointRadius: 5,
                tension: 0.3,
                fill: true
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { position: 'top' },
                tooltip: { mode: 'index', intersect: false }
            },
            scales: {
                y: {
                    beginAtZero: true,
                    ticks: { stepSize: 20 },
                    title: {
                        display: true,
                        text: 'Number of Students'
                    }
                },
                x: {
                    title: {
                        display: true,
                        text: 'Date'
                    }
                }
            }
        }
    });
}