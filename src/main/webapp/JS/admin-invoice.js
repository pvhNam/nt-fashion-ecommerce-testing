const invoiceForm = document.getElementById('invoice-form');
if (invoiceForm) {
    const kind = invoiceForm.elements.kind;
    const fields = ['buyer', 'tax', 'address'];
    const personal = {buyer: invoiceForm.elements.buyer.value, address: invoiceForm.elements.address.value, tax: ''};
    const company = {buyer: '', address: '', tax: ''};
    let details = personal;

    function renderInvoice() {
        const isCompany = kind.value === 'company';
        document.getElementById('invoice-name-label').textContent = isCompany ? 'Tên công ty' : 'Họ và tên';
        document.getElementById('invoice-tax-field').hidden = !isCompany;
        invoiceForm.elements.tax.disabled = !isCompany;
        invoiceForm.elements.tax.required = isCompany;
        document.querySelector('[data-invoice-tax]').hidden = !isCompany;
        document.querySelector('[data-invoice="kind"]').textContent = isCompany ? 'Công ty' : 'Cá nhân';
        for (const field of fields) {
            document.querySelector(`[data-invoice="${field}"]`).textContent = invoiceForm.elements[field].value.trim() || '—';
        }
    }

    kind.addEventListener('change', () => {
        for (const field of fields) details[field] = invoiceForm.elements[field].value;
        details = kind.value === 'company' ? company : personal;
        for (const field of fields) invoiceForm.elements[field].value = details[field];
        renderInvoice();
    });
    invoiceForm.addEventListener('input', renderInvoice);
    invoiceForm.addEventListener('submit', event => {
        event.preventDefault();
        for (const field of fields) invoiceForm.elements[field].value = invoiceForm.elements[field].value.trim();
        if (invoiceForm.reportValidity()) {
            renderInvoice();
            window.print();
        }
    });
    renderInvoice();
    invoiceForm.querySelector('button[type="submit"]').disabled = false;
}
