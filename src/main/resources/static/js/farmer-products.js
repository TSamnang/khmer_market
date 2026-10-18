/**
 * Khmer Marketplace - Farmer Product & Inventory Management
 * Zero inline scripts - Strict CSP Compliant
 */
document.addEventListener('DOMContentLoaded', function () {
    // Dynamic Package Row Addition in Product Create/Edit
    const packageContainer = document.getElementById('packageRowsContainer');
    const addPackageBtn = document.getElementById('btnAddPackageRow');

    if (addPackageBtn && packageContainer) {
        addPackageBtn.addEventListener('click', function () {
            const rows = packageContainer.querySelectorAll('.km-pkg-row');
            const nextIndex = rows.length;

            const newRow = document.createElement('div');
            newRow.className = 'km-pkg-row row g-3 mb-3 p-3 bg-light rounded-3 border';
            newRow.innerHTML = `
                <div class="col-md-4">
                    <label class="form-label small fw-bold">Package Name</label>
                    <input type="text" name="packages[${nextIndex}].packageName" class="form-control form-control-sm" required placeholder="e.g. 5kg Bag">
                </div>
                <div class="col-md-2">
                    <label class="form-label small fw-bold">Weight/Volume</label>
                    <input type="number" step="0.01" name="packages[${nextIndex}].netWeight" class="form-control form-control-sm" required placeholder="5.0">
                </div>
                <div class="col-md-2">
                    <label class="form-label small fw-bold">Unit</label>
                    <select name="packages[${nextIndex}].unit" class="form-select form-select-sm">
                        <option value="kg">kg</option>
                        <option value="g">g</option>
                        <option value="liter">liter</option>
                        <option value="bunch">bunch</option>
                        <option value="box">box</option>
                        <option value="piece">piece</option>
                    </select>
                </div>
                <div class="col-md-2">
                    <label class="form-label small fw-bold">Price ($)</label>
                    <input type="number" step="0.01" name="packages[${nextIndex}].price" class="form-control form-control-sm" required placeholder="10.00">
                </div>
                <div class="col-md-2 d-flex align-items-end">
                    <button type="button" class="btn btn-outline-danger btn-sm w-100 km-btn-remove-pkg">
                        <i class="bi bi-trash"></i> Remove
                    </button>
                </div>
            `;
            packageContainer.appendChild(newRow);
            attachRemoveHandlers();
        });
    }

    function attachRemoveHandlers() {
        document.querySelectorAll('.km-btn-remove-pkg').forEach(btn => {
            btn.onclick = function () {
                const row = this.closest('.km-pkg-row');
                const container = document.getElementById('packageRowsContainer');
                if (container && container.querySelectorAll('.km-pkg-row').length > 1) {
                    row.remove();
                } else {
                    alert('At least one package option is required.');
                }
            };
        });
    }

    attachRemoveHandlers();
});
