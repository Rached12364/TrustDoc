package com.trustdoc.common;

import java.util.EnumSet;
import java.util.Set;

/** Rôle d'un utilisateur et niveaux de documents qu'il a le droit de lire. */
public enum Role {
    EMPLOYE(EnumSet.of(AccessLevel.PUBLIC)),
    RH(EnumSet.of(AccessLevel.PUBLIC, AccessLevel.RH)),
    ADMIN(EnumSet.allOf(AccessLevel.class));

    private final Set<AccessLevel> readable;

    Role(Set<AccessLevel> readable) {
        this.readable = readable;
    }

    public Set<AccessLevel> readableLevels() {
        return readable;
    }

    public boolean canRead(AccessLevel level) {
        return readable.contains(level);
    }

    /** ADMIN peut importer à tous les niveaux, RH seulement PUBLIC et RH, EMPLOYE rien. */
    public boolean canUpload(AccessLevel level) {
        return switch (this) {
            case ADMIN -> true;
            case RH -> level != AccessLevel.CONFIDENTIEL;
            case EMPLOYE -> false;
        };
    }
}
