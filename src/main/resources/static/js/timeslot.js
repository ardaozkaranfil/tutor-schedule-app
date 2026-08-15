/**
 * Sets up click-to-edit toggling for each time slot row on the settings
 * page: clicking the displayed time swaps it for an inline edit form,
 * and clicking anywhere outside the open row reverts it back to display.
 */
document.addEventListener("DOMContentLoaded", function () {
    document.querySelectorAll('.timeslot-row').forEach(row => {
        const display = row.querySelector('.slot-display');
        const editForm = row.querySelector('.slot-edit-form');
        /**
         * Stops the click from reaching the document-level listener below,
         * which would otherwise immediately revert the row it just opened.
         */
        display.addEventListener('click', (e) => {
            e.stopPropagation();
            display.style.display = 'none';
            editForm.style.display = 'flex';
        });
        /**
         * Keeps clicks inside the open form (e.g. the time picker) from
         * being treated as an "outside click" that closes the form.
         */
        editForm.addEventListener('click', (e) => {
            e.stopPropagation();
        });
    });

    /**
     * Reverts every row back to display mode. Runs on any click that
     * wasn't stopped by a row's own listeners above, i.e. a click outside
     * all open edit forms.
     */
    document.addEventListener('click', () => {
        document.querySelectorAll('.timeslot-row').forEach(row => {
            row.querySelector('.slot-display').style.display = '';
            row.querySelector('.slot-edit-form').style.display = 'none';
        });
    });
})