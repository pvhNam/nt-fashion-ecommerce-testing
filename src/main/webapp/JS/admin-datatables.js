(() => {
    const DataTable = window.DataTable;
    if (!DataTable) return;

    const jumpForms = new WeakMap();
    const pagingButton = DataTable.ext.renderer.pagingButton;
    pagingButton.admin = (settings, type, content, active, disabled) => {
        const button = pagingButton._(settings, type, content, active, disabled);
        if (type === 'ellipsis') {
            const table = new DataTable.Api(settings).table().node();
            button.display = jumpForms.get(table).cloneNode(true);
        } else if (disabled) {
            button.clicker.disabled = true;
        }
        return button;
    };

    let dataTable;
    function enhanceAdminTable() {
        dataTable?.destroy();
        dataTable = null;

        const main = document.querySelector('main.main-content');
        const table = main?.querySelector('table[data-admin-datatable]');
        const pagination = main?.querySelector('.admin-pagination[data-total-rows]');
        if (!table || !pagination || table.tBodies[0]?.querySelector('td[colspan], td[rowspan]')) return;

        const fallbackLinks = pagination.querySelector('ul');
        const form = pagination.querySelector('template[data-page-jump]').content.firstElementChild;
        const pageUrl = new URL(form.action, window.location.href);
        pageUrl.search = new URLSearchParams(new FormData(form)).toString();
        jumpForms.set(table, form);

        const { pageStart, pageLength, totalRows } = pagination.dataset;
        dataTable = new DataTable(table, {
            serverSide: true,
            deferLoading: Number(totalRows),
            displayStart: Number(pageStart),
            pageLength: Number(pageLength),
            ordering: false,
            searching: false,
            info: false,
            lengthChange: false,
            autoWidth: false,
            renderer: { pagingButton: 'admin' },
            layout: {
                topStart: null,
                topEnd: null,
                bottomStart: null,
                bottomEnd: { paging: { firstLast: false } }
            },
            language: {
                paginate: { previous: 'Trước', next: 'Sau' },
                aria: { paginate: { previous: 'Trang trước', next: 'Trang sau', number: 'Trang ' } }
            },
            ajax({ start, length }) {
                // Each page is rendered by the existing servlet and carries its row actions.
                pageUrl.searchParams.set('page', String(start / length + 1));
                window.location.assign(pageUrl.href);
            },
            initComplete() {
                const pager = this.api().table().container().querySelector('.dt-paging');
                const controls = pager.querySelector('nav');
                controls.removeAttribute('aria-label');
                controls.setAttribute('role', 'presentation');
                fallbackLinks.replaceWith(pager);
            }
        });
    }

    enhanceAdminTable();
    document.addEventListener('admin:content-updated', enhanceAdminTable);
})();