package code.eneity;

import code.config.DisplayConfigAnnotation;
import code.repository.base.TableEntity;
import code.repository.base.TableField;
import code.repository.base.TableName;
import lombok.Data;

@TableName(name = "monitor_table")
@Data
public class MonitorTableEntity implements TableEntity {

    @TableField(name = "id", sql = "id varchar(55) primary key")
    private String id;

    @TableField(name = "chat_id", sql = "chat_id varchar(100)")
    private String chatId;

    @DisplayConfigAnnotation(i18n = "config_display_zero_delay", set = false)
    @TableField(name = "name", sql = "name varchar(50)")
    private String name;

    @DisplayConfigAnnotation(i18n = "config_display_chat_id_array", set = true)
    @TableField(name = "chat_id_array_json", sql = "chat_id_array_json text")
    private String chatIdArrayJson;

    @TableField(name = "create_time", sql = "create_time timestamp")
    private Long createTime;
    @TableField(name = "update_time", sql = "update_time timestamp")
    private Long updateTime;

    @DisplayConfigAnnotation(i18n = "config_display_notification", set = true)
    @TableField(name = "notification", sql = "notification int(2)")
    private Integer notification;

    @DisplayConfigAnnotation(i18n = "config_display_on", set = false)
    @TableField(name = "enable", sql = "enable int(2)")
    private Integer enable;

    @DisplayConfigAnnotation(i18n = "config_display_template", set = true)
    @TableField(name = "template", sql = "template text")
    private String template;

    @DisplayConfigAnnotation(i18n = "config_display_url", set = true)
    @TableField(name = "url", sql = "url text")
    private String url;

    @DisplayConfigAnnotation(i18n = "config_display_web_page_preview", set = true)
    @TableField(name = "web_page_preview", sql = "web_page_preview int(2)")
    private Integer webPagePreview;

    @DisplayConfigAnnotation(i18n = "config_display_zero_delay", set = true)
    @TableField(name = "zero_delay", sql = "zero_delay int(2)")
    private Integer zeroDelay;

    @DisplayConfigAnnotation(i18n = "capture_flag", set = false)
    @TableField(name = "capture_flag", sql = "capture_flag int(2)")
    private Integer captureFlag;

    @DisplayConfigAnnotation(i18n = "translation_language", set = false)
    @TableField(name = "translation_language", sql = "translation_language text")
    private String translationLanguage;

}
