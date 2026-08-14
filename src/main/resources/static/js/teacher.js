document.addEventListener('DOMContentLoaded', function () {
    const nameFilter = document.getElementById('nameFilter');
    const branchFilter = document.getElementById('branchFilter');
    const tableBody = document.getElementById('teacherTableBody');

    let debounceTimer;

    function fetchTeachers() {
        const name = nameFilter.value.trim();
        const branch = branchFilter.value;

        const params = new URLSearchParams();
        if (name) params.set('name', name);
        if (branch) params.set('branch', branch);

        fetch('/teachers/search?' + params.toString())
            .then(response => response.json())
            .then(teachers => renderTeachers(teachers))
            .catch(err => console.error('Öğretmen araması başarısız:', err));
    }

    function renderTeachers(teachers) {
        tableBody.innerHTML = '';

        if (teachers.length === 0) {
            tableBody.innerHTML = '<tr><td colspan="3">Sonuç bulunamadı.</td></tr>';
            return;
        }

        teachers.forEach(teacher => {
            const row = document.createElement('tr');
            row.setAttribute('data-id', teacher.id);
            row.innerHTML = `
                <td></td>
                <td></td>
                <td class="actions">
                    <a href="/teachers/edit/${teacher.id}">Düzenle</a>
                    <form action="/teachers/delete/${teacher.id}" method="post">
                        <button type="submit" class="link-danger">Sil</button>
                    </form>
                </td>
            `;
            row.children[0].textContent = teacher.fullName;
            row.children[1].textContent = teacher.branch || '-';
            row.querySelector('form').addEventListener('submit', function (e) {
                if (!confirm('Bu öğretmeni silmek istediğinize emin misiniz?')) {
                    e.preventDefault();
                }
            });
            tableBody.appendChild(row);
        });
    }

    nameFilter.addEventListener('input', function () {
        clearTimeout(debounceTimer);
        debounceTimer = setTimeout(fetchTeachers, 250);
    });

    branchFilter.addEventListener('change', fetchTeachers);
});