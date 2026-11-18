package com.trustdoc.common;

/** État d'intégrité d'un document : l'empreinte SHA-256 du fichier stocké correspond-elle à celle de l'import ? */
public enum Integrity {
    OK,
    ALTERE,
    NON_VERIFIE
}
