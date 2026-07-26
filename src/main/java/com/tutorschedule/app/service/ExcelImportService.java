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
                    errors.add("Row " + (rowIndex + 1) + ": " + e.getMessage());
                }

                rowIndex++;
            }
        }
        catch (IOException e){
            throw new RuntimeException("Failed to read Excel file: " + e.getMessage(), e);
        }

        backupService.performBackup(BackupTrigger.SCHEDULE_SAVE);

        if (errors.isEmpty()) {
            return successCount + " students imported successfully.";
        }
        return successCount + " students imported, " + errors.size() + " row(s) failed: " + String.join("; ", errors);
    }

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
            throw new NumberFormatException("Invalid student number '" + studentNo + "'");
        }

        if (!classGroupRepository.existsById(studentClass)) {
            ClassGroup classGroup = new ClassGroup(studentClass);
            classGroupRepository.save(classGroup);
        }

        return studentService.createStudent(id, studentClass, name);
    }

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