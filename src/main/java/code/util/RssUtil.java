package code.util;

import code.config.RequestProxyConfig;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONReader;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.client5.http.fluent.Response;
import org.apache.hc.core5.util.Timeout;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;

@Slf4j
public class RssUtil {

    public static SyndFeed getFeed(RequestProxyConfig proxyConfig, String url) {
        try {
            Request request = Request
                    .get(url)
                    .connectTimeout(Timeout.of(20, TimeUnit.SECONDS))
                    .responseTimeout(Timeout.of(60, TimeUnit.SECONDS));
            proxyConfig.viaProxy(request);
            Response execute = request.execute();
            InputStream inputStream = execute.returnContent().asStream();

            SyndFeed syndFeed = new SyndFeedInput().build(new XmlReader(inputStream));
            return syndFeed;
        } catch (Exception e) {
            log.error(ExceptionUtil.getStackTraceWithCustomInfoToStr(e));
        }
        return null;
    }

}
