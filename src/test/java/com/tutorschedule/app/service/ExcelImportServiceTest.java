package com.tutorschedule.app.service;

import com.tutorschedule.app.entity.ClassGroup;
import com.tutorschedule.app.entity.Student;
import com.tutorschedule.app.repository.ClassGroupRepository;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ExcelImportServiceTest {

    @Mock
    private StudentService studentService;

    @Mock
    private ClassGroupRepository classGroupRepository;

    @Mock
    private BackupService backupService;

    @InjectMocks
    private ExcelImportService excelImportService;

    @Test
    void getCellValueAsString_whenCellIsNull_returnsEmptyString(){
        assertEquals("", excelImportService.getCellValueAsString(null));
    }

    @Test
    void getCellValueAsString_whenCellIsString_returnsTrimmedValue(){
        Cell cell = createCell(c -> c.setCellValue("  Arda  "));
        assertEquals("Arda", excelImportService.getCellValueAsString(cell));
    }

    @Test
    void getCellValueAsString_whenCellIsWholeNumber_returnsWithoutDecimalPoint(){
        Cell cell = createCell(c -> c.setCellValue(1023.0));
        assertEquals("1023", excelImportService.getCellValueAsString(cell));
    }

    @Test
    void getCellValueAsString_whenCellIsDecimalNumber_returnsWithDecimalPoint(){
        Cell cell = createCell(c -> c.setCellValue(12.5));
        assertEquals("12.5", excelImportService.getCellValueAsString(cell));
    }

    @Test
    void getCellValueAsString_whenCellIsBlank_returnsEmptyString(){
        Cell cell = createCell(c -> c.setCellType(CellType.BLANK));
        assertEquals("", excelImportService.getCellValueAsString(cell));
    }

    @Test
    void mapRowToStudent_whenRowEntirelyEmpty_returnsNull(){
        Row row = createRow("", "", "");

        Student result = excelImportService.mapRowToStudent(row);

        assertNull(result);
    }

    @Test
    void mapRowToStudent_whenStudentNumberInvalid_throwsNumberFormatException(){
        Row row = createRow("Arda", "abc", "12-MF");

        NumberFormatException exception = assertThrows(
                NumberFormatException.class,
                () -> excelImportService.mapRowToStudent(row)
        );

        assertEquals("Invalid student number 'abc'", exception.getMessage());
    }

    @Test
    void mapRowToStudent_whenClassNotRegistered_createsClassGroupThenStudent(){
        Row row = createRow("Arda", "1", "12-MF");
        Student expected = mock(Student.class);

        when(classGroupRepository.existsById("12-MF")).thenReturn(false);
        when(studentService.createStudent(1L, "12-MF", "Arda")).thenReturn(expected);

        Student result = excelImportService.mapRowToStudent(row);

        verify(classGroupRepository).save(new ClassGroup("12-MF"));
        assertEquals(expected, result);
    }

    @Test
    void mapRowToStudent_whenClassAlreadyRegistered_doesNotCreateClassGroup(){
        Row row = createRow("Arda", "1", "12-MF");

        when(classGroupRepository.existsById("12-MF")).thenReturn(true);
        when(studentService.createStudent(1L, "12-MF", "Arda")).thenReturn(mock(Student.class));

        excelImportService.mapRowToStudent(row);

        verify(classGroupRepository, never()).save(any());
    }

    @Test
    void importFromExcel_skipsHeaderAndImportsValidRows() throws IOException {
        when(classGroupRepository.existsById(any())).thenReturn(true);
        when(studentService.createStudent(any(), any(), any())).thenReturn(mock(Student.class));

        MockMultipartFile file = buildExcelFile(
                new String[]{"Name", "No", "Class"},
                new String[]{"Arda", "1", "12-MF"},
                new String[]{"Efe", "2", "12-MF"}
        );

        String summary = excelImportService.importFromExcel(file);

        assertEquals("2 students imported successfully.", summary);
    }

    @Test
    void importFromExcel_collectsErrorForBadRowButContinuesWithOthers() throws IOException {
        when(classGroupRepository.existsById(any())).thenReturn(true);
        when(studentService.createStudent(any(), any(), any())).thenReturn(mock(Student.class));

        MockMultipartFile file = buildExcelFile(
                new String[]{"Name", "No", "Class"},
                new String[]{"Arda", "abc", "12-MF"},
                new String[]{"Efe", "2", "12-MF"}
        );

        String summary = excelImportService.importFromExcel(file);

        assertTrue(summary.startsWith("1 students imported, 1 row(s) failed"));
        assertTrue(summary.contains("Row 2"));
    }

    @Test
    void importFromExcel_performsBackupAfterImport() throws IOException {
        when(classGroupRepository.existsById(any())).thenReturn(true);
        when(studentService.createStudent(any(), any(), any())).thenReturn(mock(Student.class));

        MockMultipartFile file = buildExcelFile(
                new String[]{"Name", "No", "Class"},
                new String[]{"Arda", "1", "12-MF"}
        );

        excelImportService.importFromExcel(file);

        verify(backupService).performBackup(com.tutorschedule.app.entity.BackupTrigger.SCHEDULE_SAVE);
    }

    // ---- test helpers ----

    private Cell createCell(java.util.function.Consumer<Cell> setter){
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet();
        Cell cell = sheet.createRow(0).createCell(0);
        setter.accept(cell);
        return cell;
    }

    private Row createRow(String name, String studentNo, String className){
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet();
        Row row = sheet.createRow(0);
        row.createCell(0).setCellValue(name);
        row.createCell(1).setCellValue(studentNo);
        row.createCell(2).setCellValue(className);
        return row;
    }

    private MockMultipartFile buildExcelFile(String[]... rows) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet();
            for (int r = 0; r < rows.length; r++) {
                Row row = sheet.createRow(r);
                for (int c = 0; c < rows[r].length; c++) {
                    row.createCell(c).setCellValue(rows[r][c]);
                }
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return new MockMultipartFile(
                    "file", "students.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    out.toByteArray()
            );
        }
    }
}