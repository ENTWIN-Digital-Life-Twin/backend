package com.digitallifetwin.auth.google;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class GoogleIdentityTest {

    @Test
    void gmailIsGoogleAuthoritativeWhenVerified() {
        GoogleIdentity identity = new GoogleIdentity(
                "sub", "user@gmail.com", true, "Ada", "Lovelace", null);
        assertThat(identity.googleAuthoritativeEmail()).isTrue();
    }

    @Test
    void googlemailIsGoogleAuthoritativeWhenVerified() {
        GoogleIdentity identity = new GoogleIdentity(
                "sub", "user@googlemail.com", true, null, null, null);
        assertThat(identity.googleAuthoritativeEmail()).isTrue();
    }

    @Test
    void hostedDomainIsGoogleAuthoritativeWhenVerified() {
        GoogleIdentity identity = new GoogleIdentity(
                "sub", "ada@entwin.test", true, "Ada", "Lovelace", "entwin.test");
        assertThat(identity.googleAuthoritativeEmail()).isTrue();
    }

    @Test
    void unverifiedGmailIsNotAuthoritative() {
        GoogleIdentity identity = new GoogleIdentity(
                "sub", "user@gmail.com", false, null, null, null);
        assertThat(identity.googleAuthoritativeEmail()).isFalse();
    }

    @Test
    void thirdPartyEmailWithoutHostedDomainIsNotAuthoritative() {
        GoogleIdentity identity = new GoogleIdentity(
                "sub", "ada@company.com", true, "Ada", "Lovelace", null);
        assertThat(identity.googleAuthoritativeEmail()).isFalse();
    }
}
