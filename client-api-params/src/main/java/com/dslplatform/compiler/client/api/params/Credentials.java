/**
 * Copyright (C) 2013 Nova Generacija Softvera d.o.o. (HR), <https://dsl-platform.com/>
 */
package com.dslplatform.compiler.client.api.params;

import java.nio.charset.Charset;

import org.apache.commons.codec.binary.Base64;

public class Credentials implements Auth {
    public final String user;
    public final byte[] password;

    public Credentials(
            final String user,
            final String password) {
        this.user = user;
        this.password = password.getBytes(Charset.forName("UTF-8"));
    }

    // -------------------------------------------------------------------------

    @Override
    public boolean allowMultiple() { return false; }

    @Override
    public void addToPayload(final XMLOut xO) {
        xO.start("auth")
            .start("credentials")
                .node("user", user)
                .node("password", Base64.encodeBase64String(password))
            .end()
        .end();
    }
}