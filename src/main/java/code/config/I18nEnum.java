package code.config;

import lombok.Getter;

@Getter
public enum I18nEnum {

    BotStartSucceed("bot_start_succeed"),

    InvalidCommand("invalid_command"),
    MonitorList("monitor_list"),
    NothingHere("nothing_here"),
    On("on"),
    Off("off"),
    Test("test"),
    Update("update"),
    NotFound("not_found"),
    NotFoundMonitor("not_found_monitor"),
    OnMonitor("on_monitor"),
    OffMonitor("off_monitor"),
    ExitEditMode("exit_edit_mode"),
    UnknownError("unknown_error"),
    NothingAtAll("nothing_at_all"),
    CancelSucceed("cancel_succeed"),
    Confirm("confirm"),
    Cancel("cancel"),
    Delete("delete"),
    Finish("finish"),

    LanguageList("language_list"),
    ChangeLanguageFinish("change_language_finish"),

    MonitorExists("monitor_exists"),

    CreateMonitor1("create_monitor_1"),
    CreateMonitor2("create_monitor_2"),
    CreateMonitor3("create_monitor_3"),
    CreateMonitor4("create_monitor_4"),
    CreateMonitor5("create_monitor_5"),
    CreateMonitor6("create_monitor_6"),
    CreateMonitor7("create_monitor_7"),
    CreateMonitor8("create_monitor_8"),
    CreateMonitorFinish("create_monitor_finish"),


    TestMonitor("test_monitor"),


    UpdateMonitor1("update_monitor_1"),
    UpdateMonitor2("update_monitor_2"),
    UpdateMonitor3("update_monitor_3"),
    UpdateMonitor4("update_monitor_4"),
    UpdateFieldError("update_field_error"),
    UpdateMonitorFinish("update_monitor_finish"),

    DeleteMonitorConfirm("delete_monitor_confirm"),
    DeleteMonitorFinish("delete_monitor_finish"),


    ConfigDisplayOn("config_display_on"),
    ConfigDisplayWebPagePreview("config_display_web_page_preview"),
    ConfigDisplayNotification("config_display_notification"),
    ConfigDisplayZeroDelay("config_display_zero_delay"),
    ConfigDisplayUrl("config_display_url"),
    ConfigDisplayTemplate("config_display_template"),
    ConfigDisplayChatIdArray("config_display_chat_id_array"),
    ConfigDisplayFileBasename("config_display_file_basename"),

    ;

    private String key;

    I18nEnum(String key) {
        this.key = key;
    }

}
