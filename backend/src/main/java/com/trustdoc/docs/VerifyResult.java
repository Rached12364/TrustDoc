package com.trustdoc.docs;

import com.trustdoc.common.Integrity;

/** Résultat d'une vérification : empreinte attendue (à l'import) et empreinte recalculée maintenant. */
public record VerifyResult(Long id, Integrity integrity, String expected, String actual) {}
