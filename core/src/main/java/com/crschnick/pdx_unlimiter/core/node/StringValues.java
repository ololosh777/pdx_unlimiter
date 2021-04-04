package com.crschnick.pdx_unlimiter.core.node;

import java.util.regex.Pattern;

public class StringValues {

    private static final byte DOUBLE_QUOTE_CHAR = 34;

    public static String unescapeScalarValue(NodeContext context, int index) {
        var b = context.getLiteralsBegin()[index];
        var l = context.getLiteralsLength()[index];
        boolean quoted = context.getData()[b] == DOUBLE_QUOTE_CHAR &&
                context.getData()[b + l - 1] == DOUBLE_QUOTE_CHAR;

        var s = new String(context.getData(), b, l, context.getCharset());
        var matcher = UNESCAPE_PATTERN.matcher(s);
        if (quoted) {
            matcher.region(1, s.length() - 1);
        }
        return matcher.replaceAll(r -> "$1");
    }

    private static final Pattern UNESCAPE_PATTERN = Pattern.compile("\\\\([\"\\\\])");

    public static String unescapeStringContent(String val) {
        return UNESCAPE_PATTERN.matcher(val).replaceAll(r -> "$1");
    }

    public static String escapeStringContent(String val) {
        return val.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
