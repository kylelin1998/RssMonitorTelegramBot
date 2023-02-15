package code.config;

import lombok.Getter;

@Getter
public enum TableEnum {

    SentRecordTable("sent_record_table"),

    ;

    private String name;

    TableEnum(String name) {
        this.name = name;
    }

}
