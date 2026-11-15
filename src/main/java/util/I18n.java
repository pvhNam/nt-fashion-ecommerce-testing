package util;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/** Lấy câu thông báo trong resources/messages_vi|en theo ngôn ngữ đang chọn. */
public final class I18n {
    private static final String BUNDLE = "resources.messages";

    private I18n() {
    }

    public static Locale locale(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        Object lang = session == null ? null : session.getAttribute("lang");
        return "en".equals(lang) ? Locale.ENGLISH : Locale.forLanguageTag("vi");
    }

    public static String t(HttpServletRequest request, String key, Object... args) {
        Locale locale = locale(request);
        try {
            String text = ResourceBundle.getBundle(BUNDLE, locale).getString(key);
            return args.length == 0 ? text : new MessageFormat(text, locale).format(args);
        } catch (MissingResourceException e) {
            return key;
        }
    }
}
