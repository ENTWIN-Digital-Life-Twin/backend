package com.digitallifetwin.auth.google;

public interface GoogleIdTokenVerifierPort {

    GoogleIdentity verify(String credential);
}
