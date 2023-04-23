package lt.vu.wifidistancecalculator;

public class StringUtils {

    public static boolean isNotEmpty(CharSequence text) {
        return !isEmpty(text);
    }

    public static boolean isEmpty(CharSequence text) {
        return text == null || text.toString().trim().isEmpty();
    }

}
