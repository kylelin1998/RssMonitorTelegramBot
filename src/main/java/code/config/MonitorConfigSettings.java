package code.config;

import lombok.Data;

@Data
public class MonitorConfigSettings {

    @DisplayConfigAnnotation(i18n = "config_display_on", set = false)
    private Boolean on;

    private String filename;
    @DisplayConfigAnnotation(i18n = "config_display_zero_delay", set = false)
    private String fileBasename;

    @DisplayConfigAnnotation(i18n = "config_display_web_page_preview", set = true)
    private Boolean webPagePreview;
    @DisplayConfigAnnotation(i18n = "config_display_notification", set = true)
    private Boolean notification;
    @DisplayConfigAnnotation(i18n = "config_display_zero_delay", set = true)
    private Boolean zeroDelay;

    @DisplayConfigAnnotation(i18n = "config_display_url", set = true)
    private String url;
    @DisplayConfigAnnotation(i18n = "config_display_template", set = true)
    private String template;

    @DisplayConfigAnnotation(i18n = "config_display_chat_id_array", set = true)
    private String[] chatIdArray;

}
