package code.config;

import lombok.Data;

@Data
public class ConfigSettings {

    public Boolean debug;

    public Boolean onProxy;
    public String proxyHost;
    public Integer proxyPort;

    public String botAdminId;
    private String[] permissionChatIdArray;

    public String botName;
    public String botToken;

    private Integer intervalMinute;

    private String[] chatIdArray;

}
