
import java.util.ArrayList;
import java.util.List;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import gr.uom.java.xmi.decomposition.AbstractCodeFragment.*;
import java.util.HashMap;
import java.util.Map;


public class Mainn {

    public static void main(String[] args) {

        String inp = "System.out.println(certs(abbas));";
        Map<String,String> myMap = new HashMap<>();
        myMap.put("certs","local");
        String s = replaceParametersWithArguments(inp, myMap);
        System.out.println(s);

    }

    private static boolean quoteBefore(String beforeMatch) {
        if(beforeMatch.contains("\"")) {
            if(beforeMatch.contains("+")) {
                int indexOfQuote = beforeMatch.lastIndexOf("\"");
                int indexOfPlus = beforeMatch.lastIndexOf("+");
                if(indexOfPlus > indexOfQuote) {
                    return false;
                }
                else {
                    return true;
                }
            }
            else {
                return true;
            }
        }
        return false;
    }

    private static boolean quoteAfter(String afterMatch) {
        if(afterMatch.contains("\"")) {
            if(afterMatch.contains("+")) {
                int indexOfQuote = afterMatch.indexOf("\"");
                int indexOfPlus = afterMatch.indexOf("+");
                if(indexOfPlus < indexOfQuote) {
                    return false;
                }
                else {
                    return true;
                }
            }
            else {
                return true;
            }
        }
        return false;
    }
    public static String replaceParametersWithArguments(String input, Map<String, String> parameterToArgumentMap) {
        String afterReplacements = input;
        for(String parameter : parameterToArgumentMap.keySet()) {
            String argument = parameterToArgumentMap.get(parameter);
            if(!parameter.equals(argument)) {
                StringBuffer sb = new StringBuffer();
                Pattern p = Pattern.compile(Pattern.quote(parameter));
                Matcher m = p.matcher(afterReplacements);
                while(m.find()) {
                    //check if the matched string is an argument
                    //previous character should be "(" or "," or " " or there is no previous character
                    int start = m.start();
                    boolean isArgument = false;
                    boolean isInsideStringLiteral = false;
                    if(start >= 1) {
                        String previousChar = afterReplacements.substring(start-1, start);
                        if(previousChar.equals("(") || previousChar.equals(",") || previousChar.equals(" ") || previousChar.equals("=")) {
                            int indexOfNextChar = start + parameter.length();
                            if(afterReplacements.length() > indexOfNextChar) {
                                char nextChar = afterReplacements.charAt(indexOfNextChar);
                                if(!Character.isLetterOrDigit(nextChar)) {
                                    isArgument = true;
                                }
                            }
                            if(parameter.endsWith(".")) {
                                isArgument = true;
                            }
                        }
                        String beforeMatch = afterReplacements.substring(0, start);
                        String afterMatch = afterReplacements.substring(start+parameter.length(), afterReplacements.length());
                        if(quoteBefore(beforeMatch) && quoteAfter(afterMatch)) {
                            isInsideStringLiteral = true;
                        }
                    }
                    else if(start == 0 && !afterReplacements.startsWith("return ")) {
                        int indexOfNextChar = start + parameter.length();
                        if(afterReplacements.length() > indexOfNextChar) {
                            char nextChar = afterReplacements.charAt(indexOfNextChar);
                            if(!Character.isLetterOrDigit(nextChar)) {
                                isArgument = true;
                            }
                        }
                        if(parameter.endsWith(".")) {
                            isArgument = true;
                        }
                    }
                    if(isArgument && !isInsideStringLiteral) {
                        m.appendReplacement(sb, Matcher.quoteReplacement(argument));
                    }
                }
                m.appendTail(sb);
                afterReplacements = sb.toString();
            }
        }
        return afterReplacements;
    }
}
