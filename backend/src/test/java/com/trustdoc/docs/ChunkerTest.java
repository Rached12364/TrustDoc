package com.trustdoc.docs;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class ChunkerTest {

    private final Chunker chunker = new Chunker();

    @Test
    void garde_le_numero_de_page_de_chaque_passage() {
        List<Chunker.Piece> pieces = chunker.split(List.of("Page un. Une phrase.", "", "Page trois. Autre phrase."));
        assertThat(pieces).extracting(Chunker.Piece::page).containsExactly(1, 3);
    }

    @Test
    void ne_coupe_jamais_une_phrase_et_respecte_la_taille_cible() {
        String sentence = "Chaque collaborateur doit lire cette règle avec attention avant de commencer. ";
        List<String> chunks = chunker.splitPage(sentence.repeat(20));
        assertThat(chunks).hasSizeGreaterThan(1);
        assertThat(chunks).allSatisfy(c -> {
            assertThat(c).endsWith(".");
            assertThat(c.length()).isLessThanOrEqualTo(Chunker.TARGET + sentence.length());
        });
    }

    @Test
    void separe_les_titres_de_la_phrase_suivante() {
        String raw = "Télétravail\nChaque collaborateur peut faire jusqu'à 2 jours de télétravail par semaine.\n";
        assertThat(Chunker.markHeadings(raw)).startsWith("Télétravail. Chaque collaborateur");
    }

    @Test
    void ne_prend_pas_une_ligne_coupee_pour_un_titre() {
        String raw = "La demande se fait dans l'outil RH et doit être validée par le manager, avec un délai\n"
            + "Raisonnable pour l'équipe.\n";
        assertThat(Chunker.markHeadings(raw)).doesNotContain("délai.");
    }
}
