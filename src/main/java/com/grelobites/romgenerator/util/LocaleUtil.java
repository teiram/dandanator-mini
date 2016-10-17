package com.grelobites.romgenerator.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class LocaleUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(LocaleUtil.class);

    private static final String BUNDLE_NAME = "romgenerator";
    private static final Locale locale = Locale.getDefault();
    private static final EncodingControl encodingControl = new EncodingControl("UTF-8");
    private static ResourceBundle localeBundle;

    private static ResourceBundle defaultLocaleBundle = ResourceBundle.getBundle(BUNDLE_NAME, Locale.ENGLISH, getControl());

    public static ResourceBundle.Control getControl() {
        return encodingControl;
    }

    public static Locale getLocale() {
        return locale;
    }

    public static ResourceBundle getBundle() {
        if (localeBundle == null) {
            try {
                localeBundle = ResourceBundle.getBundle(BUNDLE_NAME, getLocale(), getControl());
            } catch (Exception e) {
                LOGGER.warn("Loading language bundle for locale " + getLocale(), e);
                LOGGER.warn("Defaulting to English");
                localeBundle = defaultLocaleBundle;
            }
        }
        return localeBundle;
    }

    public static String i18n(String key) {
        try {
            return getBundle().getString(key);
        } catch (MissingResourceException mre) {
            return defaultLocaleBundle.getString(key);
        }
    }
}
