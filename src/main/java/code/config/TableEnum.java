package code.config;

import lombok.Getter;

@Getter
public enum TableEnum {

    SentRecordTable("sent_record_table"),
    I18nTable("i18n_table"),

    ;

    private String name;

    TableEnum(String name) {
        this.name = name;
    }

}
