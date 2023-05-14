package code.eneity;

import code.config.DisplayConfigAnnotation;
import code.repository.mapper.TableEntity;
import code.repository.mapper.TableField;
import code.repository.mapper.TableName;
import lombok.Data;

@TableName(name = "sent_record_table_202305")
@Data
public class SentRecordTableEntity implements TableEntity {

    @TableField(name = "id", sql = "id varchar(255) primary key")
    private String id;

    @TableField(name = "chat_id", sql = "chat_id varchar(100)")
    private String chatId;

    @TableField(name = "name", sql = "name varchar(50)")
    private String name;

    @TableField(name = "create_time", sql = "create_time timestamp")
    private Long createTime;

}
