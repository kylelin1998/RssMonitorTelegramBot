package code.config;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class MonitorConfigSettings {

    private Boolean on;
    private Boolean webPagePreview;

    private String filename;
    private String fileBasename;


    private String url;

    private String template;

    private String[] chatIdArray;

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(String.format("on: %s\n", on));
        builder.append(String.format("webPagePreview: %s\n", webPagePreview));
        builder.append(String.format("url: %s\n", url));
        builder.append(String.format("template: \n%s", template));
        return builder.toString();
    }

}
