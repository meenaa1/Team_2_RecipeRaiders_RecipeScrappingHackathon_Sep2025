package utilities;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExcelReader {
    // Reads the entire sheet into a List of Maps
    public static List<Map<String, String>> getData(String sheetName) {
        List<Map<String, String>> excelData = new ArrayList<>();

        String filepath = ConfigReader.getProperty("testData");
        try (FileInputStream fis = new FileInputStream(filepath);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheet(sheetName);
            Row headerRow = sheet.getRow(0);
            int colCount = headerRow.getLastCellNum();
            int rowCount = sheet.getPhysicalNumberOfRows();

            for (int r = 1; r < rowCount; r++) {
                Row currentRow = sheet.getRow(r);
                Map<String, String> rowData = new HashMap<>();

                for (int c = 0; c < colCount; c++) {
                    String columnName = headerRow.getCell(c).getStringCellValue();
                    Cell cell = currentRow.getCell(c);
                    String cellValue = (cell == null) ? "" : cell.toString().trim();
                    rowData.put(columnName, cellValue);
                }
                excelData.add(rowData);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return excelData;
    }
    // Fetch single row by TestCaseID from a sheet
    public static Map<String, String> getRowByTestCaseId(String sheetName, String testCaseId) {
        List<Map<String, String>> allData = getData(sheetName);
        for (Map<String, String> row : allData) {
            if (row.get("TestCaseID").equalsIgnoreCase(testCaseId)) {
                return row;
            }
        }
        return new HashMap<String, String>();
    }

    // Get default login credentials
    public static Map<String, String> getDefaultLogin() {
        return getRowByTestCaseId("Default_LoginCredentials", "DefaultLogin");
    }




}
