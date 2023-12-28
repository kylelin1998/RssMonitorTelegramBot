package code.config;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.Data;

import java.util.List;

@Data
public class ConfigSettings {

    @ConfigField(isNotNull = true)
    public Boolean debug;

    @JSONField(name = "on_proxy")
    @ConfigField(isNotNull = true)
    public Boolean onProxy;
    @JSONField(name = "proxy_host")
    public String proxyHost;
    @JSONField(name = "proxy_port")
    public Integer proxyPort;

    @JSONField(name = "bot_admin_id")
    @ConfigField(isNotNull = true)
    public String botAdminId;
    @JSONField(name = "permission_chat_id_array")
    @ConfigField(isNotNull = true)
    private String[] permissionChatIdArray;

    @JSONField(name = "bot_name")
    @ConfigField(isNotNull = true)
    public String botName;
    @JSONField(name = "bot_token")
    @ConfigField(isNotNull = true)
    public String botToken;

    @JSONField(name = "interval_minute")
    @ConfigField(isNotNull = true)
    private Integer intervalMinute;

    @JSONField(name = "chatId_array")
    @ConfigField(isNotNull = true)
    private String[] chatIdArray;

    @JSONField(name = "chat_buttons")
    private String chatButtons;

    @JSONField(name = "hide_copyright_tips")
    private Boolean hideCopyrightTips;

    @JSONField(name = "verify_ssl")
    private Boolean verifySsl;

    @JSONField(name = "exclude_keywords")
    private List<String> excludeKeywords;
    @JSONField(name = "exclude_keywords_regex")
    private List<String> excludeKeywordsRegex;

    @JSONField(name = "include_keywords")
    private List<String> includeKeywords;
    @JSONField(name = "include_keywords_regex")
    private List<String> includeKeywordsRegex;

    @JSONField(name = "translate_youdao_key")
    public String translateYoudaoKey;
    @JSONField(name = "translate_youdao_secret")
    public String translateYoudaoSecret;

}
