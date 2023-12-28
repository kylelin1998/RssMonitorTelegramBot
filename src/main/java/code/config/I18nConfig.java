package code.config;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class I18nConfig {

    private static Map<String, Map<String, String>> cacheMap = new LinkedHashMap<>();

    static {
        ResourceBundle.Control control = new ResourceBundle.Control() {
            @Override
            public ResourceBundle newBundle(String baseName, java.util.Locale locale, String format, ClassLoader loader, boolean reload) throws IOException {
                String resourceName = toResourceName(toBundleName(baseName, locale), "properties");
                InputStream inputStream = null;
                try {
                    inputStream = loader.getResourceAsStream(resourceName);
                    if (inputStream != null) {
                        return new PropertyResourceBundle(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
                    }
                } finally {
                    if (inputStream != null) {
                        inputStream.close();
                    }
                }
                return null;
            }
        };
        for (I18nLocaleEnum value : I18nLocaleEnum.values()) {
            ResourceBundle bundle = ResourceBundle.getBundle("i18n/i18n", value.getLocale(), control);

            HashMap<String, String> hashMap = new HashMap<>();
            for (String s : bundle.keySet()) {
                hashMap.put(s, bundle.getString(s));
            }

            cacheMap.put(value.getAlias(), hashMap);
        }
    }

    public static String getText(String i18nAlias, String key) {
        Map<String, String> map = cacheMap.get(StringUtils.isNotBlank(i18nAlias) ? i18nAlias : I18nLocaleEnum.ZH_CN.getAlias());
        return map.get(key);
    }

    public static String getText(String i18nAlias, I18nEnum i18nEnum) {
        Map<String, String> map = cacheMap.get(StringUtils.isNotBlank(i18nAlias) ? i18nAlias : I18nLocaleEnum.ZH_CN.getAlias());
        return map.get(i18nEnum.getKey());
    }

}
