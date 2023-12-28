package code.util.translate;

import code.util.translate.base.TranslateAPI;
import code.util.translate.base.TranslateAuth;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static code.Main.GlobalConfig;

@Slf4j
public class MicrosoftTranslateHandle implements TranslateAPI {
    @Override
    public boolean hasAuth() {
        return false;
    }

    @Override
    public TranslateAuth auth() {
        return null;
    }

    @Override
    public String translate(String text, String from, String to) {
        String url = "https://api.microsofttranslator.com/v2/Http.svc/Translate?appId=A4D660A48A6A97CCA791C34935E4C02BBB1BEC1C&from=%s&to=%s&text=%s";
        url = String.format(url, "auto".equals(from) ? "" : from, to, encode(text));

        HttpResponse<String> response = Unirest
                .get(url)
                .connectTimeout((int) TimeUnit.MILLISECONDS.convert(20, TimeUnit.SECONDS))
                .socketTimeout((int) TimeUnit.MILLISECONDS.convert(60, TimeUnit.SECONDS))
                .asString();
        if (response.getStatus() == 200) {
            String body = response.getBody();
            String string = StringUtils.substringBetween(body,"<string xmlns=\"http://schemas.microsoft.com/2003/10/Serialization/\">", "</string>");
            if (StringUtils.isNotBlank(string)) {
                return string;
            }
        }
        return null;
    }

    private String encode(String text) {
        try {
            return URLEncoder.encode(text, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}
