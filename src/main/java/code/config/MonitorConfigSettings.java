package code.config;

import lombok.Data;

@Data
public class MonitorConfigSettings {

    private Boolean on;
    public Boolean webPagePreview;
    public Boolean notification;

    public Boolean zeroDelay;

    private String filename;
    private String fileBasename;


    public String url;

    public String template;

    public String[] chatIdArray;

}
