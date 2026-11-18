package com.trustdoc.search;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Transforme un texte français en mots-clés comparables : minuscules, sans accents, sans mots vides, pluriel simplifié. */
public final class TextAnalyzer {

    private static final Set<String> STOP = Set.of(
        "a", "au", "aux", "avec", "ce", "ces", "cet", "cette", "dans", "de", "des", "du", "elle", "elles", "en", "et",
        "est", "etre", "il", "ils", "je", "la", "le", "les", "leur", "leurs", "lui", "ma", "mais", "me", "mes", "moi",
        "mon", "ne", "nos", "notre", "nous", "on", "ou", "par", "pas", "pour", "qu", "que", "qui", "quoi", "sa", "se",
        "ses", "son", "sont", "sur", "ta", "te", "tes", "toi", "ton", "tu", "un", "une", "vos", "votre", "vous", "y",
        "d", "l", "j", "m", "n", "s", "t", "c", "quel", "quelle", "quels", "quelles", "combien", "comment", "quand",
        "doit", "doivent", "faut", "peut", "peuvent", "dois", "puis", "fait", "faire", "si", "ai", "as", "avons",
        "avez", "ont", "suis", "es", "etes", "sommes", "ceux", "celle", "celui", "plus", "tout", "tous", "toute",
        "toutes", "chaque", "entre", "apres", "avant", "sans", "sous", "lors", "afin", "ainsi", "donc", "car", "dont");

    private TextAnalyzer() {}

    public static List<String> terms(String text) {
        List<String> out = new ArrayList<>();
        if (text == null) {
            return out;
        }
        String plain = Normalizer.normalize(text.toLowerCase(Locale.FRENCH), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        for (String w : plain.split("[^a-z0-9]+")) {
            if (w.isEmpty() || STOP.contains(w)) {
                continue;
            }
            out.add(stem(w));
        }
        return out;
    }

    /** Racinisation très légère : « jours » → « jour », « congés » → « conge ». */
    static String stem(String w) {
        if (w.length() > 4 && (w.endsWith("s") || w.endsWith("x"))) {
            w = w.substring(0, w.length() - 1);
        }
        if (w.length() > 5 && w.endsWith("e")) {
            w = w.substring(0, w.length() - 1);
        }
        return w;
    }
}
