package com.siffmember.info.utils;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ExcelUtil {
    private static final String TAG = ExcelUtil.class.getSimpleName();

    /*public static List<Map<Integer, Object>> readExcelNew(Context context, Uri uri, String filePath) {
        List<Map<Integer, Object>> list = new ArrayList<>();
        Workbook wb = null;
        InputStream is = null;
        if (filePath == null) {
            return null;
        }
        try {
            is = context.getContentResolver().openInputStream(uri);
            wb = new XSSFWorkbook(is);
            if (wb != null) {
                Sheet sheet = wb.getSheetAt(0);
                Row rowHeader = sheet.getRow(0);
                int cellsCount = rowHeader.getPhysicalNumberOfCells();
                // Store header
                Map<Integer, Object> headerMap = new HashMap<>();
                for (int c = 0; c < cellsCount; c++) {
                    Object value = getCellFormatValue(rowHeader.getCell(c));
                    if (value != null && !value.toString().isEmpty()) {
                        headerMap.put(c, value);
                    }
                }
                list.add(headerMap);
                int rownum = sheet.getPhysicalNumberOfRows();
                int colnum = headerMap.size();
                for (int i = 1; i < rownum; i++) {
                    Row row = sheet.getRow(i);
                    Map<Integer, Object> itemMap = new HashMap<>();
                    if (row != null) {
                        for (int j = 0; j < colnum; j++) {
                            Object value = getCellFormatValue(row.getCell(j));
                            if (value != null && !value.toString().isEmpty()) {
                                itemMap.put(j, value);
                            }
                        }
                        list.add(itemMap);
                    }
                }
            }
        } catch (final Exception e) {
            Log.e(TAG, "readExcelNew: import error " + e.getMessage(), e);
            if (Looper.myLooper() == null) {
                Looper.prepare();  // Prepare the Looper if it's not already prepared
            }
            new Handler(Looper.getMainLooper()).post(() ->
                    Toast.makeText(context, "Import error: " + e.getMessage(), Toast.LENGTH_SHORT).show()
            );
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
                if (wb != null) {
                    wb.close();
                }
            } catch (IOException e) {
                Log.e(TAG, "Error closing resources: " + e.getMessage(), e);
            }
        }
        return list;
    }*/

    public static List<Map<Integer, Object>> readExcelNew(Context context, Uri uri, String filePath) {
        List<Map<Integer, Object>> list = new ArrayList<>();
        Workbook wb = null;
        InputStream is = null;
        if (filePath == null) {
            return null;
        }
        try {
            is = context.getContentResolver().openInputStream(uri);
            wb = new XSSFWorkbook(is);
            if (wb != null) {
                Sheet sheet = wb.getSheetAt(0);
                Row rowHeader = sheet.getRow(0);
                int cellsCount = rowHeader.getPhysicalNumberOfCells();

                // Store header
                Map<Integer, Object> headerMap = new HashMap<>();
                for (int c = 0; c < cellsCount; c++) {
                    Object value = getCellFormatValue(rowHeader.getCell(c));
                    if (value != null && !value.toString().isEmpty()) {
                        headerMap.put(c, value);
                    }
                }
                list.add(headerMap);

                // FIXED: use getLastRowNum instead of getPhysicalNumberOfRows
                int lastRowNum = sheet.getLastRowNum();
                int colnum = headerMap.size();

                for (int i = 1; i <= lastRowNum; i++) {
                    Row row = sheet.getRow(i);
                    Map<Integer, Object> itemMap = new HashMap<>();
                    if (row != null) {
                        for (int j = 0; j < colnum; j++) {
                            Object value = getCellFormatValue(row.getCell(j));
                            if (value != null && !value.toString().isEmpty()) {
                                itemMap.put(j, value);
                            }
                        }
                        if (!itemMap.isEmpty()) {
                            list.add(itemMap);
                        }
                    }
                }
            }
        } catch (final Exception e) {
            Log.e(TAG, "readExcelNew: import error " + e.getMessage(), e);
            if (Looper.myLooper() == null) {
                Looper.prepare();
            }
            new Handler(Looper.getMainLooper()).post(() ->
                    Toast.makeText(context, "Import error: " + e.getMessage(), Toast.LENGTH_SHORT).show()
            );
        } finally {
            try {
                if (is != null) is.close();
                if (wb != null) wb.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing resources: " + e.getMessage(), e);
            }
        }
        return list;
    }

    public static List<Map<Integer, Object>> readMembershipExcel(Context context, Uri uri) {

        List<Map<Integer, Object>> list = new ArrayList<>();
        Workbook wb = null;
        InputStream is = null;

        try {
            is = context.getContentResolver().openInputStream(uri);
            wb = new XSSFWorkbook(is);

            Sheet sheet = wb.getSheetAt(0);
            int lastRowNum = sheet.getLastRowNum();

            for (int i = 0; i <= lastRowNum; i++) {

                Row row = sheet.getRow(i);
                Map<Integer, Object> rowMap = new HashMap<>();

                if (row != null) {

                    int lastColumn = row.getLastCellNum();
                    if (lastColumn < 0) lastColumn = 0;

                    for (int j = 0; j < lastColumn; j++) {

                        Cell cell = row.getCell(j, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                        String value = getCellFormatValueNew(cell);

                        rowMap.put(j, value == null ? "" : value.trim());
                    }
                }

                list.add(rowMap);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (is != null) is.close();
                if (wb != null) wb.close();
            } catch (Exception ignored) {}
        }

        return list;
    }

    private static String getCellFormatValue(Cell cell) {
        if (cell == null) {
            return "";
        }
        try {
            switch (cell.getCellType()) {
                case STRING:
                    return cell.getStringCellValue().trim();

                case NUMERIC:
                    if (DateUtil.isCellDateFormatted(cell)) {
                        // Format date as string
                        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                .format(cell.getDateCellValue());
                    } else {
                        // Convert numeric to string without .0 for integers
                        double num = cell.getNumericCellValue();
                        if (num == (long) num) {
                            return String.valueOf((long) num);
                        } else {
                            return String.valueOf(num);
                        }
                    }

                case BOOLEAN:
                    return String.valueOf(cell.getBooleanCellValue());

                case FORMULA:
                    try {
                        // Try evaluating the formula
                        FormulaEvaluator evaluator = cell.getSheet().getWorkbook()
                                .getCreationHelper().createFormulaEvaluator();
                        CellValue cellValue = evaluator.evaluate(cell);
                        if (cellValue != null) {
                            switch (cellValue.getCellType()) {
                                case STRING:
                                    return cellValue.getStringValue();
                                case NUMERIC:
                                    return String.valueOf(cellValue.getNumberValue());
                                case BOOLEAN:
                                    return String.valueOf(cellValue.getBooleanValue());
                                default:
                                    return "";
                            }
                        }
                    } catch (Exception e) {
                        return ""; // fallback if evaluation fails
                    }
                    return "";

                case BLANK:
                case ERROR:
                default:
                    return "";
            }
        } catch (Exception e) {
            // Last-resort catch so loop never breaks
            return "";
        }
    }

    private static String getCellFormatValueNew(Cell cell) {

        if (cell == null) return "";

        try {

            DataFormatter formatter = new DataFormatter(Locale.getDefault());
            FormulaEvaluator evaluator = cell.getSheet()
                    .getWorkbook()
                    .getCreationHelper()
                    .createFormulaEvaluator();

            // 🔥 This handles numeric, string, formula, date — everything properly
            String formattedValue = formatter.formatCellValue(cell, evaluator);

            return formattedValue == null ? "" : formattedValue.trim();

        } catch (Exception e) {
            return "";
        }
    }

    /*private static Object getCellFormatValue(Cell cell) {
        if (cell == null) {
            return "";
        }
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue();
                } else {
                    return cell.getNumericCellValue();
                }
            case BOOLEAN:
                return cell.getBooleanCellValue();
            case FORMULA:
                return cell.getCellFormula();
            case BLANK:
                return "";
            case ERROR:
                return cell.getErrorCellValue();
            default:
                return cell.toString();
        }
    }*/

    public static void writeExcelNew(Context context, List<Map<Integer, Object>> exportExcel, Uri uri) {
        try {
            XSSFWorkbook workbook = new XSSFWorkbook();
            XSSFSheet sheet = workbook.createSheet(WorkbookUtil.createSafeSheetName("Sheet1"));

            int colums = exportExcel.get(0).size();
            for (int i = 0; i < colums; i++) {
                //set the cell default width to 15 characters
                sheet.setColumnWidth(i, 15 * 256);
            }

            for (int i = 0; i < exportExcel.size(); i++) {
                Row row = sheet.createRow(i);
                Map<Integer, Object> integerObjectMap = exportExcel.get(i);
                for (int j = 0; j < colums; j++) {
                    Cell cell = row.createCell(j);
                    cell.setCellValue(String.valueOf(integerObjectMap.get(j)));
                }
            }

            OutputStream outputStream = context.getContentResolver().openOutputStream(uri);
            workbook.write(outputStream);
            outputStream.flush();
            outputStream.close();
            Log.i(TAG, "writeExcel: export successful");
            Toast.makeText(context, "Download successful", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "writeExcel: error" + e);
            Toast.makeText(context, "export error" + e, Toast.LENGTH_SHORT).show();
        }
    }
}
