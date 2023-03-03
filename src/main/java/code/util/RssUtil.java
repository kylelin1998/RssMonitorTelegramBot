package code.util;

import code.config.RequestProxyConfig;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import kong.unirest.GetRequest;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Slf4j
public class RssUtil {

    public static SyndFeed getFeed(RequestProxyConfig proxyConfig, String url) {
        try {
            GetRequest request = Unirest
                    .get(url)
                    .connectTimeout((int) TimeUnit.MILLISECONDS.convert(20, TimeUnit.SECONDS))
                    .socketTimeout((int) TimeUnit.MILLISECONDS.convert(40, TimeUnit.SECONDS))
                    ;
            proxyConfig.viaProxy(request);
            HttpResponse<String> response = request.asString();
            String body = response.getBody();

            InputStream inputStream = new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8));

            SyndFeed syndFeed = new SyndFeedInput().build(new XmlReader(inputStream));
            return syndFeed;
        } catch (Exception e) {
            log.error(ExceptionUtil.getStackTraceWithCustomInfoToStr(e));
        }
        return null;
    }

}
