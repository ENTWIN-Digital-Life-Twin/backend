package com.digitallifetwin.auth.google;

import com.digitallifetwin.auth.config.GoogleProperties;
import com.digitallifetwin.auth.exception.GoogleLoginNotConfiguredException;
import com.digitallifetwin.auth.exception.InvalidGoogleTokenException;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class GoogleIdTokenVerifierAdapter implements GoogleIdTokenVerifierPort {

    private static final List<String> ISSUERS = List.of("https://accounts.google.com", "accounts.google.com");

    private final GoogleProperties googleProperties;
    private final GoogleIdTokenVerifier verifier;

    public GoogleIdTokenVerifierAdapter(GoogleProperties googleProperties) {
        this.googleProperties = googleProperties;
        this.verifier = googleProperties.configured()
                ? new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                        .setAudience(List.of(googleProperties.clientId().trim()))
                        .setIssuers(ISSUERS)
                        .build()
                : null;
    }

    @Override
    public GoogleIdentity verify(String credential) {
        if (!googleProperties.configured() || verifier == null) {
            throw new GoogleLoginNotConfiguredException();
        }
        if (credential == null || credential.isBlank()) {
            throw new InvalidGoogleTokenException();
        }
        try {
            GoogleIdToken token = verifier.verify(credential);
            if (token == null) {
                throw new InvalidGoogleTokenException();
            }
            GoogleIdToken.Payload payload = token.getPayload();
            String subject = payload.getSubject();
            if (subject == null || subject.isBlank()) {
                throw new InvalidGoogleTokenException();
            }
            Boolean verified = payload.getEmailVerified();
            return new GoogleIdentity(
                    subject,
                    payload.getEmail(),
                    Boolean.TRUE.equals(verified),
                    stringClaim(payload, "given_name"),
                    stringClaim(payload, "family_name"),
                    payload.getHostedDomain()
            );
        } catch (GeneralSecurityException | IOException | IllegalArgumentException ex) {
            throw new InvalidGoogleTokenException();
        }
    }

    private static String stringClaim(GoogleIdToken.Payload payload, String name) {
        Object value = payload.get(name);
        return value instanceof String text ? text : null;
    }
}
