// Dynamic role switcher on registration form
document.addEventListener('DOMContentLoaded', function () {
    const roleBuyer = document.getElementById('role-buyer');
    const roleFarmer = document.getElementById('role-farmer');
    const farmerFields = document.getElementById('farmer-extra-fields');
    const roleTabs = document.querySelectorAll('.role-tab-label');

    function updateRoleState() {
        const isFarmer = roleFarmer && roleFarmer.checked;

        roleTabs.forEach(function (tab) {
            const radio = tab.querySelector('input[type="radio"]');
            if (radio && radio.checked) {
                tab.classList.add('active');
            } else {
                tab.classList.remove('active');
            }
        });

        if (farmerFields) {
            if (isFarmer) {
                farmerFields.style.display = 'block';
            } else {
                farmerFields.style.display = 'none';
            }
        }
    }

    if (roleBuyer) {
        roleBuyer.addEventListener('change', updateRoleState);
    }
    if (roleFarmer) {
        roleFarmer.addEventListener('change', updateRoleState);
    }

    // Native label clicking automatically checks the enclosed radio and triggers the change event
    // Initial run to ensure correct visibility on page render or validation errors

    // Run once on initial load to set active state
    updateRoleState();
});
