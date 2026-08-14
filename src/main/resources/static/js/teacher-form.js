document.addEventListener('DOMContentLoaded', function () {
    /**
     * Wires every schedule-grid cell so its className input is only
     * editable when the cell's status is BUSY. className stays readonly
     * (never disabled) so it's still submitted with the form and keeps
     * the timeSlotId/dayOfWeek/status/className arrays aligned by index
     * on the server side (see TeacherController#buildScheduleEntries).
     */
    document.querySelectorAll('.schedule-cell').forEach(function (cell) {
        const statusSelect = cell.querySelector('select[name="status"]');
        const classNameInput = cell.querySelector('input[name="className"]');

        function syncClassNameField() {
            const isBusy = statusSelect.value === 'BUSY';
            classNameInput.readOnly = !isBusy;
            if (!isBusy) {
                classNameInput.value = '';
            } else {
                classNameInput.focus();
            }
        }

        statusSelect.addEventListener('change', syncClassNameField);
        syncClassNameField();
    });
});