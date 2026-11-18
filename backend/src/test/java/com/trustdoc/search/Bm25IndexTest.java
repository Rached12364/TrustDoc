package com.trustdoc.search;

import static org.assertj.core.api.Assertions.assertThat;

import com.trustdoc.common.AccessLevel;
import java.util.EnumSet;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class Bm25IndexTest {

    private final Bm25Index index = new Bm25Index();

    @BeforeEach
    void setUp() {
        index.rebuild(List.of(
            new Bm25Index.Passage(1, 10, "guide.pdf", AccessLevel.PUBLIC, 1, "Chaque collaborateur peut faire 2 jours de télétravail par semaine."),
            new Bm25Index.Passage(2, 10, "guide.pdf", AccessLevel.PUBLIC, 2, "Le badge est remis le premier jour par l'accueil."),
            new Bm25Index.Passage(3, 20, "rh.pdf", AccessLevel.RH, 1, "Les notes de frais sont déposées dans un délai de 30 jours."),
            new Bm25Index.Passage(4, 30, "pca.pdf", AccessLevel.CONFIDENTIEL, 1, "Les services critiques redémarrent en moins de 4 heures.")));
    }

    @Test
    void trouve_le_bon_passage_malgre_les_accents_et_le_pluriel() {
        var hits = index.search("Combien de JOURS de teletravail ?", EnumSet.allOf(AccessLevel.class), id -> true, 3);
        assertThat(hits).isNotEmpty();
        assertThat(hits.get(0).passage().chunkId()).isEqualTo(1);
    }

    @Test
    void ne_renvoie_jamais_un_passage_interdit_pour_le_role() {
        var hits = index.search("délai notes de frais", EnumSet.of(AccessLevel.PUBLIC), id -> true, 5);
        assertThat(hits).noneMatch(h -> h.passage().level() != AccessLevel.PUBLIC);
    }

    @Test
    void exclut_les_documents_modifies() {
        var hits = index.search("télétravail semaine", EnumSet.allOf(AccessLevel.class), id -> id != 10, 5);
        assertThat(hits).noneMatch(h -> h.passage().documentId() == 10);
    }

    @Test
    void question_sans_mot_utile_ne_renvoie_rien() {
        assertThat(index.search("Que est-ce que c'est ?", EnumSet.allOf(AccessLevel.class), id -> true, 3)).isEmpty();
    }

    @Test
    void analyseur_supprime_accents_mots_vides_et_pluriel() {
        assertThat(TextAnalyzer.terms("Les congés payés de l'année")).containsExactly("conge", "paye", "annee");
    }
}
