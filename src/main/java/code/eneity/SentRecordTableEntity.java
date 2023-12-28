package code.eneity;

import code.repository.base.TableEntity;
import code.repository.base.TableField;
import code.repository.base.TableName;
import lombok.Data;

@TableName(name = "sent_record_202312_table")
@Data
public class SentRecordTableEntity implements TableEntity {

    @TableField(name = "id", sql = "id varchar(100) primary key")
    private String id;

    @TableField(name = "chat_id", sql = "chat_id varchar(100)")
    private String chatId;

    @TableField(name = "name", sql = "name varchar(50)")
    private String name;

    @TableField(name = "uri", sql = "uri text")
    private String uri;

    @TableField(name = "create_time", sql = "create_time timestamp")
    private Long createTime;

}
