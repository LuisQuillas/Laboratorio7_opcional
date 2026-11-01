// Solo gestiona las filas dinámicas del formulario de pedido (agregar / quitar).
// Los subtotales y el total se calculan en Java (OrderService), nunca aquí.
(function () {
    'use strict';

    var body = document.getElementById('items-body');
    var addButton = document.getElementById('add-row');
    if (!body || !addButton) {
        return;
    }

    function rows() {
        return body.querySelectorAll('tr.item-row');
    }

    // Mantiene índices consecutivos (items[0], items[1], ...) para que Spring enlace la lista
    function reindex() {
        rows().forEach(function (row, i) {
            row.querySelectorAll('select, input').forEach(function (el) {
                if (el.name) {
                    el.name = el.name.replace(/items\[\d+\]/, 'items[' + i + ']');
                }
                if (el.id) {
                    el.id = el.id.replace(/items\d+/, 'items' + i);
                }
            });
        });
    }

    function clearRow(row) {
        row.querySelectorAll('select').forEach(function (s) {
            s.selectedIndex = 0;
        });
        row.querySelectorAll('input').forEach(function (i) {
            i.value = '';
        });
        row.querySelectorAll('.error').forEach(function (e) {
            e.remove();
        });
    }

    addButton.addEventListener('click', function () {
        var all = rows();
        var clone = all[all.length - 1].cloneNode(true);
        clearRow(clone);
        body.appendChild(clone);
        reindex();
    });

    body.addEventListener('click', function (event) {
        if (!event.target.classList.contains('remove-row')) {
            return;
        }
        var all = rows();
        if (all.length > 1) {
            event.target.closest('tr').remove();
        } else {
            clearRow(all[0]);
        }
        reindex();
    });
})();
