package com.example.yy25gbs35_goods.Resovel;

import org.springframework.web.servlet.LocaleResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Locale;

public class MyLocalResovel implements LocaleResolver {
    
    @Override
    public Locale resolveLocale(HttpServletRequest request) {
        String language = request.getParameter("lang");
        if (language == null || language.isEmpty()) {
            language = request.getHeader("Accept-Language");
            if (language == null || language.isEmpty()) {
                return Locale.getDefault();
            }
        }
        return Locale.forLanguageTag(language);
    }
    
    @Override
    public void setLocale(HttpServletRequest request, HttpServletResponse response, Locale locale) {
        throw new UnsupportedOperationException("Cannot set locale - this resolver is read-only");
    }
}
