package code.handler.store;

import com.alibaba.fastjson2.JSON;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class WebhookStore {

    @Data
    public static class Webhook {
        private boolean enable;
        private List<WebhookRequest> list;
    }
    @Data
    public static class WebhookRequest {
        private String url;
        private String method;
        private Map<String, String> headers;
        private Map<String, Object> body;
    }

    public static boolean verify(String webhookJson) {
        if (StringUtils.isBlank(webhookJson)) {
            return false;
        }

        Optional<Webhook> webhookOptional = get(webhookJson);
        if (!webhookOptional.isPresent()) {
            return false;
        }
        Webhook webhook = webhookOptional.get();
        if (!webhook.isEnable()) {
            return true;
        } else {
            List<WebhookRequest> list = webhook.getList();
            if (null == list || list.isEmpty()) {
                return false;
            }
        }
        for (WebhookRequest request : webhook.list) {
            String url = request.getUrl();
            if (!StringUtils.startsWith(url, "http")) {
                return false;
            } else if (StringUtils.isBlank(request.getMethod())) {
                return false;
            }
        }
        return true;
    }

    public static Optional<Webhook> get(String webhookJson) {
        try {
            Webhook webhook = JSON.parseObject(webhookJson, Webhook.class);
            return Optional.ofNullable(webhook);
        } catch (Exception e) {
        }
        return Optional.empty();
    }

}
