package code.config;

import lombok.Data;

@Data
public class ConfigSettings {

    public Boolean onProxy;
    public String proxyHost;
    public Integer proxyPort;

    public String botAdminUsername;
    public String botAdminId;

    public String botName;
    public String botToken;

    private Integer intervalMinute;

    private String[] chatIdArray;

}
