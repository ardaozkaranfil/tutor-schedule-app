package com.tutorschedule.app.service;

import com.tutorschedule.app.entity.BackupTrigger;
import com.tutorschedule.app.entity.ClassGroup;
import com.tutorschedule.app.entity.Student;
import com.tutorschedule.app.repository.ClassGroupRepository;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Bulk-imports students from an Excel file. Rows are processed
 * independently — one bad row doesn't stop the rest from being imported,
 * failures are collected and reported back in the summary message.
 */
@Service
public class ExcelImportService {

    private final StudentService studentService;
    private final ClassGroupRepository classGroupRepository;
    private final BackupService backupService;

    public ExcelImportService(StudentService studentService, ClassGroupRepository classGroupRepository, BackupService backupService){
        this.studentService = studentService;
        this.classGroupRepository = classGroupRepository;
        this.backupService = backupService;
    }

    /**
     * Reads the uploaded Excel file row by row and saves each as a student.
     * The first row is treated as a header and skipped. Returns a summary
     * message stating how many students were added, including how many rows
     * failed and why. A backup is taken automatically once the import finishes.
     */
    public String importFromExcel(MultipartFile file){
        int successCount = 0;
        List<String> errors = new ArrayList<>();

        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(inputStream)){

            Sheet sheet = workbook.getSheetAt(0);

            int rowIndex = 0;
            for(Row row : sheet){
                if(rowIndex == 0){
                    rowIndex++;
                    continue;
                }

                try {
                    Student student = mapRowToStudent(row);
                    if (student != null) {
                        successCount++;
                    }
                } catch (Exception e) {
                    errors.add("Satır " + (rowIndex + 1) + ": " + e.getMessage());
                }

                rowIndex++;
            }
        }
        catch (IOException e){
            throw new RuntimeException("Excel dosyası okunamadı: " + e.getMessage(), e);
        }

        backupService.performBackup(BackupTrigger.SCHEDULE_SAVE);

        if (errors.isEmpty()) {
            return successCount + " öğrenci başarıyla içe aktarıldı.";
        }
        return successCount + " öğrenci içe aktarıldı, " + errors.size() + " satır başarısız: " + String.join("; ", errors);
    }

    /**
     * Converts a single Excel row into a Student entity. The three columns
     * are read as name, school number, and class, in that order. Returns
     * null if the row is entirely empty (meaning it should be skipped).
     * Throws NumberFormatException if the school number isn't a valid number.
     * If the row's class name isn't already registered, a ClassGroup is
     * created for it here on the spot.
     */
    public Student mapRowToStudent(Row row){
        String name = getCellValueAsString(row.getCell(0));
        String studentNo = getCellValueAsString(row.getCell(1));
        String studentClass = getCellValueAsString(row.getCell(2));

        if (name.isBlank() && studentNo.isBlank() && studentClass.isBlank()) {
            return null;
        }

        Long id;
        try {
            id = Long.parseLong(studentNo);
        } catch (NumberFormatException e) {
            throw new NumberFormatException("Geçersiz öğrenci numarası '" + studentNo + "'");
        }

        if (!classGroupRepository.existsById(studentClass)) {
            ClassGroup classGroup = new ClassGroup(studentClass);
            classGroupRepository.save(classGroup);
        }

        return studentService.createStudent(id, studentClass, name);
    }

    /**
     * Safely converts an Excel cell to a String regardless of its underlying
     * type (text, number, boolean, formula). Whole numbers are written
     * without a decimal point (e.g. "1023", not "1023.0").
     */
    public String getCellValueAsString(Cell cell){
        if (cell == null) {
            return "";
        }

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                double numericValue = cell.getNumericCellValue();
                if (numericValue == Math.floor(numericValue)) {
                    return String.valueOf((long) numericValue);
                }
                return String.valueOf(numericValue);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case BLANK:
                return "";
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }
}