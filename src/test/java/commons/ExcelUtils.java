package commons;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class ExcelUtils {
	
	 private static String rulesExcelPath = "testData/IngredientsAndComorbidities-ScrapperHackathon.xlsx";

	    public static List<Map<String, String>> getData(String sheetName) throws IOException {
	        List<Map<String, String>> excelData = new ArrayList<>();

	        try (InputStream in = ExcelUtils.class.getClassLoader().getResourceAsStream(rulesExcelPath);
	             Workbook workbook = new XSSFWorkbook(in)) {

	            if (in == null) {
	                throw new IOException("Excel file not found in resources: " + rulesExcelPath);
	            }

	            // Find sheet ignoring case and trimming spaces
	            Sheet sheet = null;
	            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
	                String currentName = workbook.getSheetName(i).trim();
	                if (currentName.equalsIgnoreCase(sheetName.trim())) {
	                    sheet = workbook.getSheetAt(i);
	                    break;
	                }
	            }

	            if (sheet == null) {
	                throw new IllegalArgumentException("Sheet '" + sheetName + "' not found in Excel file.");
	            }

	            Row headerRow = sheet.getRow(0);
	            int colCount = headerRow.getLastCellNum();
	            int rowCount = sheet.getPhysicalNumberOfRows();

	            for (int r = 1; r < rowCount; r++) {
	                Row currentRow = sheet.getRow(r);
	                Map<String, String> rowData = new HashMap<>();

	                for (int c = 0; c < colCount; c++) {
	                    String columnName = headerRow.getCell(c).getStringCellValue().trim();
	                    Cell cell = currentRow.getCell(c);
	                    String cellValue = (cell == null) ? "" : cell.toString().trim();
	                    rowData.put(columnName, cellValue);
	                }
	                excelData.add(rowData);
	            }
	        }

	        return excelData;
	    }

	    public static DietRules loadDietRules(String sheetName) throws IOException {
	        DietRules rules = new DietRules();
	        List<Map<String, String>> rows = getData(sheetName);

	        for (Map<String, String> row : rows) {
	            String eliminate = row.getOrDefault("Eliminate", "").trim().toLowerCase();
	            String add = row.getOrDefault("Add", "").trim().toLowerCase();
	            if (!eliminate.isEmpty()) rules.eliminate.add(eliminate);
	            if (!add.isEmpty()) rules.add.add(add);
	        }

	        return rules;
	    }

	    public static class DietRules {
	        public Set<String> eliminate = new HashSet<>();
	        public Set<String> add = new HashSet<>();
	    }

}
