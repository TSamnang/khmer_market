/**
 * Khmer Marketplace - Receipt Printing Utility
 * Strict CSP Compliant (No inline scripts)
 */
document.addEventListener('DOMContentLoaded', function () {
    function printElement(targetId) {
        const el = document.getElementById(targetId);
        if (!el) {
            window.print();
            return;
        }

        // Try popup print window
        try {
            const printWin = window.open('', '_blank', 'width=440,height=750,menubar=no,toolbar=no,location=no,status=no');
            if (printWin) {
                printWin.document.open();
                printWin.document.write(`
                    <!DOCTYPE html>
                    <html lang="en">
                    <head>
                        <meta charset="UTF-8">
                        <title>Receipt Slip</title>
                        <link rel="stylesheet" href="/css/kantumruy-pro.css">
                        <link rel="stylesheet" href="/css/bootstrap.min.css">
                        <link rel="stylesheet" href="/css/bootstrap-icons.min.css">
                        <link rel="stylesheet" href="/css/storefront.css">
                        <style>
                            @page {
                                size: 80mm auto;
                                margin: 4mm 2mm;
                            }
                            body {
                                background: #ffffff !important;
                                margin: 0 !important;
                                padding: 8px !important;
                                font-family: 'Courier New', Courier, 'Kantumruy Pro', monospace;
                            }
                            .receipt-paper {
                                box-shadow: none !important;
                                border: 1px solid #64748b !important;
                                width: 100% !important;
                                max-width: 380px !important;
                                margin: 0 auto !important;
                                padding: 16px 12px !important;
                            }
                            .receipt-jagged-top, .receipt-jagged-bottom {
                                display: none !important;
                            }
                        </style>
                    </head>
                    <body>
                        ${el.innerHTML}
                        <script>
                            window.addEventListener('load', function() {
                                setTimeout(function() {
                                    window.focus();
                                    window.print();
                                    setTimeout(function() { window.close(); }, 500);
                                }, 250);
                            });
                        <\/script>
                    </body>
                    </html>
                `);
                printWin.document.close();
                return;
            }
        } catch (e) {
            console.warn('Popup print fallback to in-page print', e);
        }

        // Fallback to in-page window.print()
        window.print();
    }

    // Attach listeners to all print triggers
    document.querySelectorAll('.btn-print-receipt').forEach(function (btn) {
        btn.addEventListener('click', function (e) {
            e.preventDefault();
            const targetId = this.getAttribute('data-print-target');
            printElement(targetId);
        });
    });
});
