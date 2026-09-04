package com.company.bl.application.service;

import java.util.ArrayList;
import java.util.List;

final class OfdTextLayout {

    private static final int MAX_PARAGRAPH_LENGTH = 2_000;
    private static final int MAX_UNBROKEN_RUN = 64;

    private OfdTextLayout() {
    }

    static List<String> paragraphs(String value) {
        String normalized = value == null ? "" : value.replace("\r\n", "\n").replace('\r', '\n');
        List<String> result = new ArrayList<>();
        for (String line : normalized.split("\n", -1)) {
            if (line.isEmpty()) {
                result.add(" ");
                continue;
            }
            splitLine(line, result);
        }
        return result.isEmpty() ? List.of(" ") : List.copyOf(result);
    }

    static String inline(String value) {
        return addBreakOpportunities(value == null ? "" : value, ' ');
    }

    private static void splitLine(String value, List<String> result) {
        StringBuilder paragraph = new StringBuilder(Math.min(value.length(), MAX_PARAGRAPH_LENGTH));
        int runLength = 0;
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            paragraph.append(character);
            runLength = Character.isWhitespace(character) ? 0 : runLength + 1;
            boolean unbrokenLimitReached = runLength == MAX_UNBROKEN_RUN
                && index + 1 < value.length()
                && !Character.isWhitespace(value.charAt(index + 1));
            if (paragraph.length() == MAX_PARAGRAPH_LENGTH || unbrokenLimitReached) {
                result.add(paragraph.toString());
                paragraph.setLength(0);
                runLength = 0;
            }
        }
        if (!paragraph.isEmpty()) {
            result.add(paragraph.toString());
        }
    }

    private static String addBreakOpportunities(String value, char breakCharacter) {
        StringBuilder result = new StringBuilder(value.length() + value.length() / MAX_UNBROKEN_RUN);
        int runLength = 0;
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            result.append(character);
            if (Character.isWhitespace(character)) {
                runLength = 0;
                continue;
            }
            runLength++;
            if (runLength == MAX_UNBROKEN_RUN && index + 1 < value.length()
                && !Character.isWhitespace(value.charAt(index + 1))) {
                result.append(breakCharacter);
                runLength = 0;
            }
        }
        return result.toString();
    }
}
