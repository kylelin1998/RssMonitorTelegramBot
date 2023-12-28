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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static code.Main.GlobalConfig;

@Slf4j
public class YoudaoTranslateHandle implements TranslateAPI {
    @Override
    public boolean hasAuth() {
        return true;
    }
    @Override
    public TranslateAuth auth() {
        String translateYoudaoKey = GlobalConfig.getTranslateYoudaoKey();
        String translateYoudaoSecret = GlobalConfig.getTranslateYoudaoSecret();
        if (StringUtils.isNotBlank(translateYoudaoKey) && StringUtils.isNotBlank(translateYoudaoSecret)) {
            TranslateAuth translateAuth = new TranslateAuth();
            translateAuth.setKey(translateYoudaoKey);
            translateAuth.setSecret(translateYoudaoSecret);
            return translateAuth;
        }
        return null;
    }

    @Override
    public String translate(String text, String from, String to) {
        TranslateAuth auth = auth();
        String salt = UUID.randomUUID().toString();
        Long curtime = System.currentTimeMillis() / 1000;
        String raw = auth.getKey() + getInput(text) + salt + curtime + auth.getSecret();
        String sign = DigestUtils.sha256Hex(raw);

        Map<String, Object> map = new HashMap();
        map.put("q", text);
        map.put("from", from);
        map.put("to", to);
        map.put("appKey", auth.getKey());
        map.put("salt", salt);
        map.put("sign", sign);
        map.put("signType", "v3");
        map.put("curtime", curtime);
        HttpResponse<String> response = Unirest
                .post("https://openapi.youdao.com/api")
                .fields(map)
                .connectTimeout((int) TimeUnit.MILLISECONDS.convert(20, TimeUnit.SECONDS))
                .socketTimeout((int) TimeUnit.MILLISECONDS.convert(60, TimeUnit.SECONDS))
                .asString();
        if (response.getStatus() == 200) {
            String body = response.getBody();
            JSONObject object = JSON.parseObject(body);
            if ("0".equals(object.getString("errorCode"))) {
                JSONArray translation = object.getJSONArray("translation");
                if (null != translation && translation.size() > 0) {
                    return translation.getString(0);
                }
            }
        }
        return null;
    }

    private static String getInput(String input) {
        if (input == null) {
            return null;
        }
        String result;
        int len = input.length();
        if (len <= 20) {
            result = input;
        } else {
            String startStr = input.substring(0, 10);
            String endStr = input.substring(len - 10, len);
            result = startStr + len + endStr;
        }
        return result;
    }
}
