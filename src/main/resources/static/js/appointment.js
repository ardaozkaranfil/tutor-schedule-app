/**
 * Sets up the "Randevular" filter bar (date/branch/teacher) and the
 * teacher×slot appointment grid: reloading on filter change, opening a
 * booking form on a free cell, and live student search inside that form.
 */
document.addEventListener("DOMContentLoaded", function () {
    const dateFilter = document.getElementById("dateFilter");
    const branchFilter = document.getElementById("branchFilter");
    const teacherFilter = document.getElementById("teacherFilter");

    /**
     * Reloads the page with the current filter values applied as query
     * params, so the server re-renders the grid for the new date/branch/
     * teacher selection.
     */
    function doReload() {
        window.location.href = reload(dateFilter, branchFilter, teacherFilter);
    }

    dateFilter.addEventListener("change", doReload);
    teacherFilter.addEventListener("change", doReload);
    branchFilter.addEventListener("change", () => {
        teacherFilter.value = "";
        doReload();
    });

    /**
     * Sets up click-to-book behavior for each FREE cell: clicking the
     * displayed status swaps it for an inline booking form, wires up the
     * student search box inside that form, and blocks submission until a
     * student has actually been picked from the suggestions (submitting
     * with an empty studentId fails binding on the server before the
     * controller's own validation ever runs).
     */
    document.querySelectorAll(".schedule-cell").forEach(cell => {
        const display = cell.querySelector(".cell-display");
        const bookingForm = cell.querySelector(".cell-booking-form");

        if(!bookingForm){
            return;
        } else {

            /**
             * Stops the click from reaching the document-level listener below,
             * which would otherwise immediately revert the cell it just opened.
             */
            display.addEventListener("click", (e) => {
                e.stopPropagation();
                display.style.display = "none";
                bookingForm.style.display = "flex";
            });

            /**
             * Keeps clicks inside the open form from being treated as an
             * "outside click" that closes the form.
             */
            bookingForm.addEventListener("click", (e) => {
                e.stopPropagation();
            });

            const studentSearch = bookingForm.querySelector(".student-search");
            const studentIdInput = bookingForm.querySelector('input[name="studentId"]');
            const suggestionsBox = bookingForm.querySelector(".student-suggestions");

            /**
             * Live-searches students via GET /students/search on every
             * keystroke and renders the matches as clickable suggestions.
             * Clears studentId on each keystroke so a stale selection can't
             * be submitted if the user keeps typing after picking one.
             */
            studentSearch.addEventListener("input", () => {
                const query = studentSearch.value.trim();
                studentIdInput.value = "";
                suggestionsBox.innerHTML = "";

                if (query.length === 0) {
                    return;
                }

                fetch("/students/search?name=" + encodeURIComponent(query))
                    .then(response => response.json())
                    .then(students => {
                        suggestionsBox.innerHTML = "";
                        students.forEach(student => {
                            const option = document.createElement("div");
                            option.textContent = student.fullName + " — " + student.className;

                            /**
                             * Selects this student: fills the visible search box
                             * with their name and the hidden studentId with their
                             * id, then clears the suggestion list.
                             */
                            option.addEventListener("click", (e) => {
                                e.stopPropagation();
                                studentSearch.value = student.fullName;
                                studentIdInput.value = student.id;
                                suggestionsBox.innerHTML = "";
                            });
                            suggestionsBox.appendChild(option);
                        });
                    });
            });

            /**
             * Guards against submitting with no student selected — without
             * this, an empty studentId reaches Spring as a raw binding
             * failure instead of the graceful errorMessage flash that
             * AppointmentService's own validation produces.
             */
            bookingForm.addEventListener("submit", (e) => {
                if (!studentIdInput.value) {
                    e.preventDefault();
                    alert("Lütfen önce listeden bir öğrenci seçin.");
                    studentSearch.focus();
                }
            });
        }
    });

    /**
     * Reverts every cell back to display mode. Runs on any click that
     * wasn't stopped by a cell's own listeners above, i.e. a click outside
     * all open booking forms.
     */
    document.addEventListener("click", () => {
        document.querySelectorAll(".schedule-cell").forEach(cell => {
            const display = cell.querySelector(".cell-display");
            const bookingForm = cell.querySelector(".cell-booking-form");
            if (bookingForm) {
                display.style.display = "";
                bookingForm.style.display = "none";
            }
        });
    });
});

/**
 * Builds the current page URL with date/branch/teacherId query params set
 * from the given filter elements (empty branch/teacher removes the param
 * entirely instead of sending an empty one).
 */
function reload(dateFilter, branchFilter, teacherFilter){
    const url = new URL(window.location.href);
    url.searchParams.set('date', dateFilter.value);

    if (branchFilter.value) {
        url.searchParams.set('branch', branchFilter.value);
    } else {
        url.searchParams.delete('branch');
    }

    if (teacherFilter.value) {
        url.searchParams.set('teacherId', teacherFilter.value);
    } else {
        url.searchParams.delete('teacherId');
    }

    return url.toString();
}