/**
 * Khmer Market Admin Console - Live Chart.js Analytics
 * Fetches JSON dataset from /admin/api/analytics/charts and renders 5 interactive charts.
 * Strict CSP Compliant - Zero Inline Scripts.
 */
document.addEventListener("DOMContentLoaded", function () {
    const revenueCanvas = document.getElementById("revenueChart");
    if (!revenueCanvas) {
        return; // Not on the dashboard page
    }

    fetch("/admin/api/analytics/charts", {
        headers: { "Accept": "application/json" }
    })
    .then(response => {
        if (!response.ok) {
            throw new Error("Failed to load analytics: " + response.status);
        }
        return response.json();
    })
    .then(data => {
        initRevenueChart(data.revenueLabels, data.revenueData);
        initOrderStatusChart(data.orderStatusLabels, data.orderStatusData);
        initUserRolesChart(data.roleLabels, data.roleData);
        initCategoriesChart(data.categoryLabels, data.categoryData);
        initFarmersChart(data.topFarmerLabels, data.topFarmerData);
    })
    .catch(error => {
        console.error("Error loading admin chart analytics:", error);
    });

    function initRevenueChart(labels, values) {
        const ctx = document.getElementById("revenueChart").getContext("2d");
        new Chart(ctx, {
            type: "line",
            data: {
                labels: labels || [],
                datasets: [{
                    label: "Daily Revenue ($)",
                    data: values || [],
                    borderColor: "#4f46e5",
                    backgroundColor: "rgba(79, 70, 229, 0.08)",
                    borderWidth: 2.5,
                    fill: true,
                    tension: 0.35,
                    pointRadius: 2,
                    pointHoverRadius: 6
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: { display: false },
                    tooltip: {
                        callbacks: {
                            label: function(ctx) {
                                return " Revenue: $" + Number(ctx.raw).toFixed(2);
                            }
                        }
                    }
                },
                scales: {
                    x: {
                        grid: { display: false },
                        ticks: {
                            maxTicksLimit: 8,
                            font: { size: 11 }
                        }
                    },
                    y: {
                        beginAtZero: true,
                        grid: { color: "#f1f5f9" },
                        ticks: {
                            callback: function(val) { return "$" + val; },
                            font: { size: 11 }
                        }
                    }
                }
            }
        });
    }

    function initOrderStatusChart(labels, values) {
        const ctx = document.getElementById("orderStatusChart").getContext("2d");
        const statusColors = {
            "PENDING": "#f59e0b",
            "CONFIRMED": "#4f46e5",
            "SHIPPED": "#0284c7",
            "DELIVERED": "#10b981",
            "CANCELLED": "#ef4444"
        };
        const bgColors = labels.map(l => statusColors[l] || "#94a3b8");

        new Chart(ctx, {
            type: "doughnut",
            data: {
                labels: labels || [],
                datasets: [{
                    data: values || [],
                    backgroundColor: bgColors,
                    borderWidth: 2,
                    borderColor: "#ffffff"
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        position: "bottom",
                        labels: { boxWidth: 12, padding: 12, font: { size: 11 } }
                    }
                },
                cutout: "65%"
            }
        });
    }

    function initUserRolesChart(labels, values) {
        const ctx = document.getElementById("userRolesChart").getContext("2d");
        new Chart(ctx, {
            type: "bar",
            data: {
                labels: labels || [],
                datasets: [{
                    label: "Registered Users",
                    data: values || [],
                    backgroundColor: ["#0284c7", "#10b981", "#4f46e5"],
                    borderRadius: 6
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: { legend: { display: false } },
                scales: {
                    x: { grid: { display: false } },
                    y: {
                        beginAtZero: true,
                        ticks: { precision: 0 }
                    }
                }
            }
        });
    }

    function initCategoriesChart(labels, values) {
        const ctx = document.getElementById("categoriesChart").getContext("2d");
        new Chart(ctx, {
            type: "bar",
            data: {
                labels: labels.length ? labels : ["No Sales Data Yet"],
                datasets: [{
                    label: "Sales ($)",
                    data: values.length ? values : [0],
                    backgroundColor: "#059669",
                    borderRadius: 6
                }]
            },
            options: {
                indexAxis: "y",
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: { display: false },
                    tooltip: {
                        callbacks: {
                            label: function(ctx) { return " Sales: $" + Number(ctx.raw).toFixed(2); }
                        }
                    }
                },
                scales: {
                    x: {
                        beginAtZero: true,
                        ticks: { callback: function(val) { return "$" + val; } }
                    },
                    y: { grid: { display: false } }
                }
            }
        });
    }

    function initFarmersChart(labels, values) {
        const ctx = document.getElementById("farmersChart").getContext("2d");
        new Chart(ctx, {
            type: "bar",
            data: {
                labels: labels.length ? labels : ["No Farmers Sales Yet"],
                datasets: [{
                    label: "Revenue ($)",
                    data: values.length ? values : [0],
                    backgroundColor: "#6366f1",
                    borderRadius: 6
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: { display: false },
                    tooltip: {
                        callbacks: {
                            label: function(ctx) { return " Revenue: $" + Number(ctx.raw).toFixed(2); }
                        }
                    }
                },
                scales: {
                    x: { grid: { display: false } },
                    y: {
                        beginAtZero: true,
                        ticks: { callback: function(val) { return "$" + val; } }
                    }
                }
            }
        });
    }
});
