package com.yammer.collections.guava.azure;

import com.google.common.base.Throwables;
import com.microsoft.windowsazure.services.core.storage.utils.Base64;

import java.io.UnsupportedEncodingException;

final class StringEntityUtil {
    private static final String ENCODING = "UTF-8";

    static String encode(String stringToBeEncoded) {
        try {
            return Base64.encode(stringToBeEncoded.getBytes(ENCODING));
        } catch (UnsupportedEncodingException e) {
            // shouldn't happen but
            throw Throwables.propagate(e);
        }
    }

    static String decode(String stringToBeDecoded) {
        try {
            return new String(Base64.decode(stringToBeDecoded), ENCODING);
        } catch (UnsupportedEncodingException e) {
            // shouldn't happen but
            throw Throwables.propagate(e);
        }
    }

}
