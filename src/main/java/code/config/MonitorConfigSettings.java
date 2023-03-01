package code.config;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;

@Data
public class MonitorConfigSettings {

    private Boolean on;
    public Boolean webPagePreview;
    public Boolean notification;

    private String filename;
    private String fileBasename;


    public String url;

    public String template;

    public String[] chatIdArray;

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(String.format("on: %s\n", on));
        builder.append(String.format("webPagePreview: %s\n", webPagePreview));
        builder.append(String.format("notification: %s\n", notification));
        builder.append(String.format("url: %s\n", url));
        builder.append(String.format("chatIdArray: %s\n", StringUtils.join(chatIdArray, " ")));
        builder.append(String.format("template: \n%s", template));
        return builder.toString();
    }

}
