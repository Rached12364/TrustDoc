package com.trustdoc.llm;

/** Un modèle de langage qui reçoit des consignes (system) et une demande (user), et renvoie du texte. */
public interface LlmClient {

    String name();

    String model();

    String complete(String system, String user);

    /** Le modèle répond-il en ce moment ? (utilisé pour afficher le vrai état dans l'interface) */
    default boolean available() {
        return true;
    }
}
