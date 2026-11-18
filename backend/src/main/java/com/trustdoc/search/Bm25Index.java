package com.trustdoc.search;

import com.trustdoc.common.AccessLevel;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import org.springframework.stereotype.Component;

/**
 * Moteur de recherche BM25 en mémoire (la formule utilisée par Elasticsearch et Lucene).
 * Le filtre d'accès est appliqué AVANT le classement : un passage interdit n'est jamais renvoyé, ni envoyé au LLM.
 */
@Component
public class Bm25Index {

    static final double K1 = 1.2;
    static final double B = 0.75;

    public record Passage(long chunkId, long documentId, String documentName, AccessLevel level, int page, String text) {}

    /** coverage : part des mots-clés de la question présents dans le passage (0 à 1), indépendante de la taille de la base. */
    public record Hit(Passage passage, double score, double coverage) {}

    private record Entry(Passage passage, Map<String, Integer> tf, int length) {}

    private record Snapshot(List<Entry> entries, Map<String, Integer> df, double avgLength) {}

    private volatile Snapshot snapshot = new Snapshot(List.of(), Map.of(), 0);

    /** Reconstruit tout l'index (appelé au démarrage et après chaque import ou suppression). */
    public void rebuild(List<Passage> passages) {
        List<Entry> entries = new ArrayList<>(passages.size());
        Map<String, Integer> df = new HashMap<>();
        long total = 0;
        for (Passage p : passages) {
            List<String> terms = TextAnalyzer.terms(p.documentName() + " " + p.text());
            Map<String, Integer> tf = new HashMap<>();
            for (String t : terms) {
                tf.merge(t, 1, Integer::sum);
            }
            for (String t : tf.keySet()) {
                df.merge(t, 1, Integer::sum);
            }
            entries.add(new Entry(p, tf, terms.size()));
            total += terms.size();
        }
        snapshot = new Snapshot(List.copyOf(entries), Map.copyOf(df), entries.isEmpty() ? 0 : (double) total / entries.size());
    }

    public int size() {
        return snapshot.entries().size();
    }

    public List<Hit> search(String query, Set<AccessLevel> allowed, Predicate<Long> documentAllowed, int k) {
        Snapshot s = snapshot;
        Set<String> q = new LinkedHashSet<>(TextAnalyzer.terms(query));
        if (q.isEmpty() || s.entries().isEmpty()) {
            return List.of();
        }
        int n = s.entries().size();
        List<Hit> hits = new ArrayList<>();
        for (Entry e : s.entries()) {
            Passage p = e.passage();
            if (!allowed.contains(p.level()) || !documentAllowed.test(p.documentId())) {
                continue;
            }
            double score = 0;
            int matched = 0;
            for (String term : q) {
                Integer f = e.tf().get(term);
                if (f == null) {
                    continue;
                }
                matched++;
                int df = s.df().getOrDefault(term, 0);
                double idf = Math.log(1 + (n - df + 0.5) / (df + 0.5));
                score += idf * (f * (K1 + 1)) / (f + K1 * (1 - B + B * e.length() / s.avgLength()));
            }
            if (score > 0) {
                hits.add(new Hit(p, score, (double) matched / q.size()));
            }
        }
        hits.sort(Comparator.comparingDouble(Hit::score).reversed());
        return hits.size() > k ? List.copyOf(hits.subList(0, k)) : hits;
    }
}
