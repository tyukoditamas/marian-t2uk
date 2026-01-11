package org.app.excel;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.app.model.ExcelData;
import org.app.model.Item;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ExcelReader {
    public ExcelData read(File excelFile) throws Exception {
        try (InputStream inputStream = new FileInputStream(excelFile);
             Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                throw new IllegalArgumentException("Excel file has no sheets.");
            }
            DataFormatter formatter = new DataFormatter(Locale.ROOT);

            Header header = findHeader(sheet, formatter);
            if (header == null) {
                throw new IllegalArgumentException("Unable to find header row with required columns.");
            }

            List<Item> items = new ArrayList<>();
            BigDecimal total = BigDecimal.ZERO;
            int lastRow = sheet.getLastRowNum();
            for (int i = header.rowIndex + 1; i <= lastRow; i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    break;
                }
                String awb = cellString(row, header.awbIndex, formatter);
                if (isBlank(awb)) {
                    break;
                }
                String description = cellString(row, header.descriptionIndex, formatter);
                String code = cellString(row, header.incadrareIndex, formatter);
                BigDecimal kgs = cellDecimal(row, header.weightIndex, formatter);

                if (isBlank(description) || isBlank(code) || kgs == null) {
                    throw new IllegalArgumentException("Randul " + (i + 1)
                            + " lipseste valori pentru descriere bunuri, incadrare sau greutate.");
                }

                items.add(new Item(description, code, awb, kgs));
                total = total.add(kgs);
            }

            return new ExcelData(items, total);
        }
    }

    private Header findHeader(Sheet sheet, DataFormatter formatter) {
        int firstRow = sheet.getFirstRowNum();
        int lastRow = Math.min(sheet.getLastRowNum(), firstRow + 20);
        for (int i = firstRow; i <= lastRow; i++) {
            Row row = sheet.getRow(i);
            if (row == null) {
                continue;
            }
            Map<String, Integer> columns = new HashMap<>();
            for (Cell cell : row) {
                String name = normalizeHeader(formatter.formatCellValue(cell));
                if (!name.isEmpty() && !columns.containsKey(name)) {
                    columns.put(name, cell.getColumnIndex());
                }
            }

            Integer descriptionIndex = columns.get("descriere marfa");
            Integer incadrareIndex = columns.get("incadrare");
            Integer awbIndex = columns.get("awb");
            Integer weightIndex = columns.get("greutate");
            if (weightIndex == null) {
                weightIndex = columns.get("kgs");
            }

            if (descriptionIndex != null && incadrareIndex != null && awbIndex != null && weightIndex != null) {
                return new Header(i, descriptionIndex, incadrareIndex, awbIndex, weightIndex);
            }
        }
        return null;
    }

    private static String normalizeHeader(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private static String cellString(Row row, int index, DataFormatter formatter) {
        Cell cell = row.getCell(index);
        if (cell == null) {
            return "";
        }
        return formatter.formatCellValue(cell).trim();
    }

    private static BigDecimal cellDecimal(Row row, int index, DataFormatter formatter) {
        Cell cell = row.getCell(index);
        if (cell == null) {
            return null;
        }
        switch (cell.getCellType()) {
            case NUMERIC:
                return BigDecimal.valueOf(cell.getNumericCellValue());
            case STRING:
                String text = formatter.formatCellValue(cell).trim();
                if (text.isEmpty()) {
                    return null;
                }
                text = text.replace(" ", "").replace(",", ".");
                return new BigDecimal(text);
            case FORMULA:
                String formulaValue = formatter.formatCellValue(cell).trim();
                if (formulaValue.isEmpty()) {
                    return null;
                }
                formulaValue = formulaValue.replace(" ", "").replace(",", ".");
                return new BigDecimal(formulaValue);
            default:
                String value = formatter.formatCellValue(cell).trim();
                if (value.isEmpty()) {
                    return null;
                }
                value = value.replace(" ", "").replace(",", ".");
                return new BigDecimal(value);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static final class Header {
        private final int rowIndex;
        private final int descriptionIndex;
        private final int incadrareIndex;
        private final int awbIndex;
        private final int weightIndex;

        private Header(int rowIndex, int descriptionIndex, int incadrareIndex, int awbIndex, int weightIndex) {
            this.rowIndex = rowIndex;
            this.descriptionIndex = descriptionIndex;
            this.incadrareIndex = incadrareIndex;
            this.awbIndex = awbIndex;
            this.weightIndex = weightIndex;
        }
    }
}
