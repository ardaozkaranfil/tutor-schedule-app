document.addEventListener('DOMContentLoaded', function (){
    const nameFilter = document.getElementById('nameFilter');
    const tableBody = document.getElementById('studentTableBody');
    const form = document.getElementById('studentForm');
    const formTitle = document.getElementById('formTitle');
    const idInput = document.getElementById('studentId');
    const fullNameInput = document.getElementById('studentFullName');
    const classNameSelect = document.getElementById('studentClassName');
    const cancelBtn = document.getElementById('cancelEditBtn');
    let debounceTimer;

    function fetchStudents() {
        const name = nameFilter.value.trim();
        const params = new URLSearchParams();
        if(name) params.set('name', name);
        fetch('/students/search?' + params.toString())
            .then(r => r.json())
            .then(renderStudents)
            .catch(err => console.error('Öğrenci araması başarısız:', err));
    }

    function renderStudents(students) {
        tableBody.innerHTML = '';

        if(students.length === 0) {
            tableBody.innerHTML = '<tr><td colspan="4">Sonuç bulunamadı.</td></tr>';
            return;
        }

        students.forEach(student => {
            const row = document.createElement('tr');
            row.setAttribute('data-id', student.id);
            row.innerHTML = `
                <td></td><td></td><td></td>
                <td class="actions">
                    <a href="#" class="edit-link">Düzenle</a>
                    <form method="post" action="/students/delete/${student.id}">
                        <button type="submit" class="link-danger">Sil</button>
                    </form>
                </td>`;
            row.children[0].textContent = student.id;
            row.children[1].textContent = student.fullName;
            row.children[2].textContent = student.className;
            row.querySelector('form').addEventListener('submit', e => {
                if (!confirm('Bu öğrenciyi silmek istediğinize emin misiniz?'))
                    e.preventDefault();
            });
            row.querySelector('.edit-link').addEventListener('click', e => {
                e.preventDefault();
                enterEditMode(student.id);
            });
            tableBody.appendChild(row);
        });
    }
    function enterEditMode(id) {
        fetch('/students/' + id)
            .then(r => r.json())
            .then(student => {
                idInput.value = student.id;
                idInput.readOnly = true;
                fullNameInput.value = student.fullName;
                classNameSelect.value = student.className;

                form.action = '/students/edit/' + student.id;
                formTitle.textContent = 'Öğrenci Düzenle';
                cancelBtn.style.display = 'inline-block';
            })
            .catch(err => console.error('Öğrenci getirilemedi:', err));
    }

    function exitEditMode() {
        form.reset();
        idInput.readOnly = false;
        form.action = '/students/add';
        formTitle.textContent = 'Öğrenci Ekle';
        cancelBtn.style.display = 'none';
    }

    cancelBtn.addEventListener('click', exitEditMode);

    document.querySelectorAll('#studentTableBody .edit-link').forEach(f => {
        f.addEventListener('click', e => {
            e.preventDefault();
            enterEditMode(f.closest('tr').dataset.id);
        });
    });

    document.querySelectorAll('#studentTableBody form').forEach(f => {
        f.addEventListener('submit', e => {
            if (!confirm('Bu öğrenciyi silmek istediğinize emin misiniz?'))
                e.preventDefault();
        });
    });

    nameFilter.addEventListener('input', () => {
        clearTimeout(debounceTimer);
        debounceTimer = setTimeout(fetchStudents, 250);
    });
})