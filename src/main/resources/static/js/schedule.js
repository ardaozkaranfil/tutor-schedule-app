/**
 * Sets up the teacher selector on the schedule grid page: switching the
 * dropdown reloads the page for the newly selected teacher.
 */
document.addEventListener("DOMContentLoaded", function () {
    const teacherSelect = document.getElementById("teacherSelect");
    if (teacherSelect) {
        teacherSelect.addEventListener("change", () => {
            window.location.href = "/schedule?teacherId=" + teacherSelect.value;
        });
    }

    /**
     * Sets up click-to-edit toggling for each schedule cell: clicking the
     * displayed status swaps it for an inline edit form, and clicking
     * anywhere outside the open cell reverts it back to display.
     */
    document.querySelectorAll(".schedule-cell").forEach(cell => {
        const display = cell.querySelector(".cell-display");
        const editForm = cell.querySelector(".cell-edit-form");
        const statusSelect = editForm.querySelector('select[name="status"]');
        const classNameInput = editForm.querySelector('input[name="className"]');

        toggleClassNameField(statusSelect, classNameInput);
        statusSelect.addEventListener("change", () => {
            toggleClassNameField(statusSelect, classNameInput);
        });

        /**
         * Stops the click from reaching the document-level listener below,
         * which would otherwise immediately revert the cell it just opened.
         */
        display.addEventListener("click", (e) => {
            e.stopPropagation();
            display.style.display = "none";
            editForm.style.display = "flex";
        });

        /**
         * Keeps clicks inside the open form (e.g. the status dropdown) from
         * being treated as an "outside click" that closes the form.
         */
        editForm.addEventListener("click", (e) => {
            e.stopPropagation();
        });
    });

    /**
     * Reverts every cell back to display mode. Runs on any click that
     * wasn't stopped by a cell's own listeners above, i.e. a click outside
     * all open edit forms.
     */
    document.addEventListener("click", () => {
        document.querySelectorAll(".schedule-cell").forEach(cell => {
            cell.querySelector(".cell-display").style.display = "";
            cell.querySelector(".cell-edit-form").style.display = "none";
        });
    });
});

/**
 * Shows the className input only when the cell is being marked BUSY,
 * matching the backend rule that className is required for BUSY and
 * cleared for any other status.
 */
function toggleClassNameField(statusSelect, classNameInput) {
    const isBusy = statusSelect.value === "BUSY";
    classNameInput.style.display = isBusy ? "" : "none";
    classNameInput.disabled = !isBusy;
}