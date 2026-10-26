document.addEventListener('DOMContentLoaded', function () {
    const KHR_RATE = 4100;
    const depositAmountInput = document.getElementById('depositAmount');
    const depositKhrEstimate = document.getElementById('depositKhrEstimate');
    const quickDepositButtons = document.querySelectorAll('.quick-deposit-btn');

    function updateKhrEstimate(val) {
        if (!depositKhrEstimate) return;
        const num = parseFloat(val);
        if (!isNaN(num) && num > 0) {
            const khr = Math.round(num * KHR_RATE);
            depositKhrEstimate.textContent = '≈ ៛' + khr.toLocaleString('en-US') + ' KHR';
        } else {
            depositKhrEstimate.textContent = '≈ ៛0 KHR';
        }
    }

    if (depositAmountInput) {
        depositAmountInput.addEventListener('input', function () {
            updateKhrEstimate(this.value);
        });
    }

    quickDepositButtons.forEach(function (btn) {
        btn.addEventListener('click', function () {
            const amt = this.getAttribute('data-amount');
            if (depositAmountInput && amt) {
                depositAmountInput.value = amt;
                updateKhrEstimate(amt);
            }
        });
    });

    const withdrawAmountInput = document.getElementById('withdrawAmount');
    const withdrawAllBtn = document.getElementById('withdrawAllBtn');
    if (withdrawAllBtn && withdrawAmountInput) {
        withdrawAllBtn.addEventListener('click', function () {
            const maxVal = withdrawAmountInput.getAttribute('max');
            if (maxVal && parseFloat(maxVal) > 0) {
                withdrawAmountInput.value = parseFloat(maxVal).toFixed(2);
            }
        });
    }
});

function handleDepositMethodChange(val) {
    const customFields = document.getElementById('customPaymentMethodFields');
    if (!customFields) return;
    if (val === 'NEW') {
        customFields.style.display = 'block';
    } else {
        customFields.style.display = 'none';
    }
}

function autofillWithdrawMethod(selectElem) {
    if (!selectElem) return;
    const selectedOption = selectElem.options[selectElem.selectedIndex];
    if (!selectedOption || !selectedOption.value) return;

    const accNum = selectedOption.value;
    const provider = selectedOption.getAttribute('data-provider') || '';
    const holder = selectedOption.getAttribute('data-holder') || '';

    const accInput = document.getElementById('accountNumber');
    const nameInput = document.getElementById('accountName');
    const channelSelect = document.getElementById('withdrawChannel');

    if (accInput) accInput.value = accNum;
    if (nameInput && holder) nameInput.value = holder;

    if (channelSelect && provider) {
        for (let i = 0; i < channelSelect.options.length; i++) {
            if (channelSelect.options[i].text.toLowerCase().includes(provider.toLowerCase()) ||
                provider.toLowerCase().includes(channelSelect.options[i].text.toLowerCase())) {
                channelSelect.selectedIndex = i;
                break;
            }
        }
    }
}

