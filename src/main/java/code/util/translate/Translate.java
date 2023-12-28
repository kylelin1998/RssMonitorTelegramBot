package code.util.translate;

import code.util.ExceptionUtil;
import code.util.translate.base.TranslateAPI;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class Translate {

    private static List<TranslateAPI> handleList = new ArrayList<>();
    static {
        handleList.add(new YoudaoTranslateHandle());
        handleList.add(new MicrosoftTranslateHandle());
    }

    public static List<String> translateAll(String text, String from, String to) {
        ArrayList<String> list = new ArrayList<>();
        for (TranslateAPI api : handleList) {
            try {
                if (api.hasAuth()) {
                    if (null == api.auth()) {
                        continue;
                    }
                }
                String translate = api.translate(text, from, to);
                if (StringUtils.isNotBlank(translate)) {
                    list.add(translate);
                }
            } catch (Exception e) {
                log.error(ExceptionUtil.getStackTraceWithCustomInfoToStr(e));
            }
        }
        return list;
    }

    public static String translate(String text, String from, String to) {
        for (TranslateAPI api : handleList) {
            try {
                if (api.hasAuth()) {
                    if (null == api.auth()) {
                        continue;
                    }
                }
                String translate = api.translate(text, from, to);
                if (StringUtils.isNotBlank(translate)) {
                    return translate;
                }
            } catch (Exception e) {
                log.error(ExceptionUtil.getStackTraceWithCustomInfoToStr(e));
            }
        }
        return "";
    }
}
