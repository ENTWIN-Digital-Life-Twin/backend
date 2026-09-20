package com.digitallifetwin.auth.google;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.digitallifetwin.auth.config.GoogleProperties;
import com.digitallifetwin.auth.exception.GoogleLoginNotConfiguredException;
import org.junit.jupiter.api.Test;

class GoogleIdTokenVerifierAdapterTest {

    @Test
    void blankClientId_throwsNotConfiguredWithoutCallingGoogle() {
        GoogleIdTokenVerifierAdapter adapter = new GoogleIdTokenVerifierAdapter(new GoogleProperties(""));

        assertThatThrownBy(() -> adapter.verify("any-token"))
                .isInstanceOf(GoogleLoginNotConfiguredException.class);
    }
}
