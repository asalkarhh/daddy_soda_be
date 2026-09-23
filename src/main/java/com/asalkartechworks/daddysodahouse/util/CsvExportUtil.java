package com.asalkartechworks.daddysodahouse.util;

import java.util.List;

/**
 * Utility for generating RFC 4180-compliant CSV text with formula injection protection.
 */
public final class CsvExportUtil {

    private static final String FORMULA_TRIGGERS = "=+-@\t\r";

    private CsvExportUtil() {
    }

    /**
     * Escapes and formats a single value for safe CSV output.
     * Prevents CSV spreadsheet formula injection and safely quotes characters.
     */
    public static String escapeCell(Object value) {
        if (value == null) {
            return "";
        }
        String str = value.toString();

        // Prevent formula injection in spreadsheet applications (Excel, Calc, Sheets)
        if (!str.isEmpty() && FORMULA_TRIGGERS.indexOf(str.charAt(0)) >= 0) {
            str = "'" + str;
        }

        // Check if quoting is required (contains quote, comma, or newline)
        if (str.contains("\"") || str.contains(",") || str.contains("\n") || str.contains("\r")) {
            return "\"" + str.replace("\"", "\"\"") + "\"";
        }

        return str;
    }

    /**
     * Formats an entire CSV row from a list of cell values.
     */
    public static String formatRow(List<?> cells) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cells.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(escapeCell(cells.get(i)));
        }
        sb.append("\r\n");
        return sb.toString();
    }
}
