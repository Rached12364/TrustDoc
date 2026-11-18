package com.trustdoc.auth;

import com.trustdoc.common.Role;
import java.util.Map;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Annuaire de démonstration : trois comptes, mots de passe hachés avec BCrypt.
 * En entreprise, on le remplacerait par l'annuaire de l'entreprise (LDAP / Active Directory / SSO).
 */
@Component
public class UserDirectory {

    private record Account(AppUser user, String passwordHash) {}

    private final Map<String, Account> accounts;
    private final PasswordEncoder encoder;

    public UserDirectory(PasswordEncoder encoder) {
        this.encoder = encoder;
        this.accounts = Map.of(
            "employe", new Account(new AppUser("employe", "Sami Ben Ali", Role.EMPLOYE), encoder.encode("employe123")),
            "rh", new Account(new AppUser("rh", "Leila Trabelsi", Role.RH), encoder.encode("rh123")),
            "admin", new Account(new AppUser("admin", "Admin TrustDoc", Role.ADMIN), encoder.encode("admin123"))
        );
    }

    public Optional<AppUser> authenticate(String username, String password) {
        if (username == null || password == null) {
            return Optional.empty();
        }
        Account a = accounts.get(username.trim().toLowerCase());
        return a != null && encoder.matches(password, a.passwordHash()) ? Optional.of(a.user()) : Optional.empty();
    }
}
