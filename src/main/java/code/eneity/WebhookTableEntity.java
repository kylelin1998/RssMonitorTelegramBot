package code.eneity;

import code.repository.base.TableEntity;
import code.repository.base.TableField;
import code.repository.base.TableName;
import lombok.Data;

@TableName(name = "webhook_table")
@Data
public class WebhookTableEntity implements TableEntity {

    @TableField(name = "id", sql = "id varchar(100) primary key")
    private String id;

    @TableField(name = "chat_id", sql = "chat_id varchar(100)")
    private String chatId;

    @TableField(name = "settings_json", sql = "settings_json text")
    private String settingsJson;

}
