/**
 * Sets up live search, row rendering, and inline add/edit form
 * transitions for the student list page.
 */
document.addEventListener('DOMContentLoaded', function (){
    const nameFilter = document.getElementById('nameFilter');
    const tableBody = document.getElementById('studentTableBody');
    const form = document.getElementById('studentForm');
    const formTitle = document.getElementById('formTitle');
    const fullNameInput = document.getElementById('studentFullName');
    const classNameInput = document.getElementById('studentClassName');
    const cancelBtn = document.getElementById('cancelEditBtn');
    let debounceTimer;

    /**
     * Calls the /students/search endpoint with the current name filter
     * and re-renders the table with the returned results.
     */
    function fetchStudents() {
        const name = nameFilter.value.trim();
        const params = new URLSearchParams();
        if(name) params.set('name', name);
        fetch('/students/search?' + params.toString())
            .then(r => r.json())
            .then(renderStudents)
            .catch(err => console.error('Öğrenci araması başarısız:', err));
    }

    /**
     * Converts an array of students into table rows; each row gets
     * an edit link and a delete form with a confirmation prompt.
     * @param {Array<{id: number, fullName: string, className: string}>} students
     */
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

    /**
     * Fetches the student with the given id and switches the form into
     * edit mode (id field becomes readonly, form action points to the
     * edit endpoint).
     * @param {number|string} id
     */
    function enterEditMode(id) {
        fetch('/students/' + id)
            .then(r => r.json())
            .then(student => {
                fullNameInput.value = student.fullName;
                classNameInput.value = student.className;

                form.action = '/students/edit/' + student.id;
                formTitle.textContent = 'Öğrenci Düzenle';
                cancelBtn.style.display = 'inline-block';
            })
            .catch(err => console.error('Öğrenci getirilemedi:', err));
    }

    /**
     * Resets the form back to add mode
     * (resets action, title, and hides the cancel button).
     */
    function exitEditMode() {
        form.reset();
        form.action = '/students/add';
        formTitle.textContent = 'Öğrenci Ekle';
        cancelBtn.style.display = 'none';
    }

    cancelBtn.addEventListener('click', exitEditMode);

    const importFile = document.getElementById('importFile');
    const importFileName = document.getElementById('importFileName');
    if (importFile) {
        importFile.addEventListener('change', () => {
            importFileName.textContent = importFile.files.length
                ? importFile.files[0].name
                : 'Buradan dosya seçiniz';
        });
    }

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