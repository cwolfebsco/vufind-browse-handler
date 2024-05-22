package org.vufind.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.logging.Logger;

public class NumPadNormalizer implements Normalizer
{

    @Override
    public byte[] normalize(String s)
    {
        String n = padNumbersInString(s.toLowerCase().replaceAll("[,;\\.:\\-_]+$", ""));
        // for debugging; if needed
        //log.info("Normalized: " + s + " to: " + n);
        byte[] key = (n == null) ? null : n.getBytes();
        return key;
    }

    private static final Logger log = Logger.getLogger(NumPadNormalizer.class.getName());


    public static String padNumbersInString(String input) {
        Pattern pattern = Pattern.compile("\\d+");
        Matcher matcher = pattern.matcher(input);
        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            String number = matcher.group();
            String paddedNumber = String.format("%08d", Integer.parseInt(number));
            matcher.appendReplacement(buffer, paddedNumber);
        }
        matcher.appendTail(buffer);

        return buffer.toString();
    }
}
