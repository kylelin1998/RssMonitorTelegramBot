package code.eneity;

import code.repository.mapper.TableEntity;
import code.repository.mapper.TableField;
import code.repository.mapper.TableName;
import lombok.Data;

@TableName(name = "sent_record_202306_table")
@Data
public class SentRecordTableEntity implements TableEntity {

    @TableField(name = "id", sql = "id varchar(100) primary key")
    private String id;

    @TableField(name = "chat_id", sql = "chat_id varchar(100)")
    private String chatId;

    @TableField(name = "name", sql = "name varchar(50)")
    private String name;

    @TableField(name = "link", sql = "link text")
    private String link;

    @TableField(name = "create_time", sql = "create_time timestamp")
    private Long createTime;

}
