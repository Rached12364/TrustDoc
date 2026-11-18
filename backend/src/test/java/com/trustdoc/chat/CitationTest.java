package com.trustdoc.chat;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class CitationTest {

    @Test
    void accepte_une_reponse_qui_cite_une_vraie_source() {
        assertThat(ChatService.checkCitations("Deux jours par semaine [1].", 2)).isEqualTo("Deux jours par semaine [1].");
    }

    @Test
    void retire_les_numeros_de_source_inventes() {
        assertThat(ChatService.checkCitations("Deux jours [1] et trois jours [7].", 2)).isEqualTo("Deux jours [1] et trois jours .");
    }

    @Test
    void refuse_une_reponse_sans_aucune_citation() {
        assertThat(ChatService.checkCitations("Deux jours par semaine.", 2)).isNull();
        assertThat(ChatService.checkCitations("Selon [9], c'est deux jours.", 2)).isNull();
    }

    @Test
    void reconnait_le_refus_du_modele() {
        assertThat(ChatService.checkCitations("JE_NE_SAIS_PAS", 3)).isEqualTo(ChatService.UNKNOWN_TOKEN);
    }

    @Test
    void reponse_extractive_reprend_une_phrase_exacte_avec_sa_source() {
        var sources = List.of(
            new ChatService.SourceDto(1, 1, "guide.pdf", 1,
                "Télétravail. Chaque collaborateur peut faire jusqu'à 2 jours de télétravail par semaine. Le lundi est exclu.", 5),
            new ChatService.SourceDto(2, 2, "autre.pdf", 2, "Le badge est remis le premier jour par l'accueil.", 1));
        String answer = ChatService.extractive("Combien de jours de télétravail par semaine ?", sources);
        assertThat(answer).contains("jusqu'à 2 jours de télétravail par semaine. [1]");
        assertThat(answer).doesNotContain("[2]");
    }
}
