package silversword.axiom.client.utils;

public class NumberParser {
    public static double parseDouble(String text, double defaultValue) {
        if (text == null || text.isEmpty()) return defaultValue;
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}