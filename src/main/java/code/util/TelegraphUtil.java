package code.util;

import code.config.RequestProxyConfig;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import kong.unirest.ContentType;
import kong.unirest.HttpResponse;
import kong.unirest.MultipartBody;
import kong.unirest.Unirest;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.*;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
public class TelegraphUtil {

    @Data
    private static class TelegraphNode {
        private String tag;
        private Map<String, String> attrs;
        private List<Object> children;
    }

    @Data
    public static class SaveResponse {
        private boolean ok;
        private String pageId;
        private String url;
    }

    public static SaveResponse save(RequestProxyConfig proxyConfig, String title, String author, String html, String appendHtml) {
        SaveResponse saveResponse = new SaveResponse();
        saveResponse.setOk(false);
        try {
            String telegraphContent = getTelegraphContent(html, appendHtml);
            if (StringUtils.isBlank(telegraphContent)) {
                log.warn("Telegraph content is null, title: {}", title);
                return saveResponse;
            }

            for (int i = 0;  i < 3; i++) {
                MultipartBody multipartBody = Unirest.post("https://edit.telegra.ph/save")
                        .header("origin", " https://telegra.ph")
                        .header("referer", " https://telegra.ph/")
                        .header("user-agent", " Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/110.0.0.0 Safari/537.36")
                        .field("Data", new ByteArrayInputStream(telegraphContent.getBytes(StandardCharsets.UTF_8)), ContentType.create("text/html", StandardCharsets.UTF_8), "content.html")
                        .field("title", title)
                        .field("author", StringUtils.isBlank(author) ? "Unknown" : author)
                        .field("page_id", "0")
                        .connectTimeout((int) TimeUnit.MILLISECONDS.convert(20, TimeUnit.SECONDS))
                        .socketTimeout((int) TimeUnit.MILLISECONDS.convert(40, TimeUnit.SECONDS))
                        ;
                proxyConfig.viaProxy(multipartBody);
                HttpResponse<String> response = multipartBody.asString();

                String body = response.getBody();
                log.info("title: {}, response: {}", title, body);
                JSONObject jsonObject = JSON.parseObject(body);
                String path = jsonObject.getString("path");
                if (StringUtils.isNotBlank(path)) {
                    saveResponse.setOk(true);
                    saveResponse.setPageId(path);
                    saveResponse.setUrl("https://telegra.ph/" + path);
                    return saveResponse;
                }
                String errorDetails = jsonObject.getString("error_details");
                if (StringUtils.isNotBlank(errorDetails) && "PATH_NUM_NOT_FOUND".equals(errorDetails)) {
                    title = title + "-r" + RandomUtils.nextInt(1, 999);
                }
            }
        } catch (Exception e) {
            log.error(ExceptionUtil.getStackTraceWithCustomInfoToStr(e));
        }
        return saveResponse;
    }

    private static String getTelegraphContent(String html, String appendHtml) {
        Document doc = Jsoup.parse(html);

        Element body = doc.body();
        if (StringUtils.isNotBlank(appendHtml)) {
            body.append(appendHtml);
        }

        TelegraphNode telegraphNode = (TelegraphNode) buildNote(body);
        return JSON.toJSONString(telegraphNode.getChildren());
    }

    private static Object buildNote(Node node) {
        if (node instanceof TextNode) {
            return ((TextNode) node).text();
        }
        if (!(node instanceof Element)) {
            return false;
        }

        TelegraphNode telegraphNode = new TelegraphNode();

        String nodeName = node.nodeName();
        if ("figure".equals(nodeName) || "div".equals(nodeName)) {
            nodeName = "p";
        }
        if ("td".equals(nodeName)) {
            String aClass = node.attr("class");
            if ("gutter".equals(aClass)) {
                return false;
            }
        }

        telegraphNode.setTag(nodeName);
        HashMap<String, String> attributeMap = new HashMap<>();
        for (Attribute attribute : node.attributes()) {
            String key = attribute.getKey();
            if ("href".equals(key) || "src".equals(key)) {
                attributeMap.put(key, attribute.getValue());
            }
        }
        telegraphNode.setAttrs(attributeMap);
        if (node.childNodeSize() > 0) {
            telegraphNode.setChildren(new ArrayList<>());
            for (Node childNode : node.childNodes()) {
                telegraphNode.getChildren().add(buildNote(childNode));
            }
        }

        return telegraphNode;
    }

}
