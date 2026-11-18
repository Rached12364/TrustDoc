package com.trustdoc.docs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Découpe le texte d'une page en passages d'environ {@link #TARGET} caractères, sans couper une phrase.
 * La dernière phrase d'un passage est répétée au début du suivant (chevauchement) pour ne pas perdre le contexte.
 */
@Component
public class Chunker {

    public static final int TARGET = 450;

    public record Piece(int page, String text) {}

    public List<Piece> split(List<String> pages) {
        List<Piece> out = new ArrayList<>();
        for (int i = 0; i < pages.size(); i++) {
            int page = i + 1;
            for (String text : splitPage(pages.get(i))) {
                out.add(new Piece(page, text));
            }
        }
        return out;
    }

    List<String> splitPage(String raw) {
        String text = markHeadings(raw == null ? "" : raw).replaceAll("\\s+", " ").trim();
        if (text.isEmpty()) {
            return List.of();
        }
        List<String> sentences = Arrays.stream(text.split("(?<=[.!?])\\s+(?=[\\p{Lu}«\"(])"))
            .map(String::trim).filter(s -> !s.isEmpty()).toList();

        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        String last = null;
        for (String s : sentences) {
            if (current.length() > 0 && current.length() + s.length() + 1 > TARGET) {
                chunks.add(current.toString());
                current.setLength(0);
                if (last != null && last.length() < TARGET / 2) {
                    current.append(last);
                }
            }
            if (current.length() > 0) {
                current.append(' ');
            }
            current.append(s);
            last = s;
        }
        if (current.length() > 0) {
            chunks.add(current.toString());
        }
        return chunks;
    }

    /**
     * Dans un PDF, un titre est une ligne courte, sans ponctuation finale, après une phrase terminée.
     * On lui ajoute un point pour qu'il devienne une « phrase » séparée au lieu d'être collé à la suivante.
     */
    static String markHeadings(String raw) {
        StringBuilder out = new StringBuilder();
        boolean previousEnded = true;
        for (String line : raw.split("\\R")) {
            String l = line.trim();
            if (l.isEmpty()) {
                previousEnded = true;
                continue;
            }
            boolean ends = l.matches(".*[.!?:;,]$");
            boolean heading = previousEnded && !ends && l.length() <= 60 && Character.isUpperCase(l.charAt(0));
            out.append(l).append(heading ? ". " : " ");
            previousEnded = ends || heading;
        }
        return out.toString();
    }
}
