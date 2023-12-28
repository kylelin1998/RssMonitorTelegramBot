package code.config;

import lombok.Getter;

@Getter
public enum I18nEnum {

    BotStartSucceed("bot_start_succeed"),
    HelpText("help_text"),

    InvalidCommand("invalid_command"),
    MonitorList("monitor_list"),
    NothingHere("nothing_here"),

    On("on"),
    Off("off"),
    Test("test"),
    Update("update"),
    NotFound("not_found"),
    UnknownError("unknown_error"),
    NothingAtAll("nothing_at_all"),
    CancelSucceeded("cancel_succeeded"),
    Confirm("confirm"),
    Cancel("cancel"),
    Delete("delete"),
    Finish("finish"),
    ExitSucceeded("exit_succeeded"),
    Getting("getting"),
    Downloading("downloading"),
    ForceRecord("force_record"),

    OnMonitor("on_monitor"),
    OffMonitor("off_monitor"),

    LanguageList("language_list"),
    ChangeLanguageFinish("change_language_finish"),

    MonitorExists("monitor_exists"),

    CreateNameTooLong("create_name_too_long"),
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
    ForceRecordSucceeded("force_record_succeeded"),


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

    YouAreNotAnAdmin("you_are_not_an_admin"),
    AreYouSureToRestartRightNow("are_you_sure_to_restart_right_now"),
    Restarting("restarting"),
    GettingUpdateData("getting_update_data"),
    AreYouSureToUpgradeThisBotRightNow("are_you_sure_to_upgrade_this_bot_right_now"),
    TargetVersion("target_version"),
    CurrentVersion("current_version"),
    UpdateLogs("update_logs"),
    Updating("updating"),
    Downloaded("downloaded"),

    AreYouSureToUpdateTheConfig("are_you_sure_to_update_the_config"),
    PleaseSendMeConfigContent("please_send_me_config_content"),
    UpdateConfigFail("update_config_fail"),

    UpdateSucceeded("update_succeeded"),
    UpdateFailed("update_failed"),

    SetChatButtons("set_chat_buttons"),
    UpdateConfig("update_config"),
    Restart("restart"),
    Upgrade("upgrade"),

    PleaseSendMeChatButtons("please_send_me_chat_buttons"),
    FormatError("format_error"),

    SettingWebhook("setting_webhook"),
    HideCopyrightTips("hide_copyright_tips"),
    AreYouSureYouWantToHideCopyrightTips("are_you_sure_you_want_to_hide_copyright_tips"),
    CurrentSetting("current_setting"),
    PleaseSendMeWebhookSettings("please_send_me_webhook_settings"),

    Back("back"),
    Refresh("refresh"),
    ExcludeKeywords("exclude_keywords"),
    ExcludeKeywordsRegex("exclude_keywords_regex"),
    PleaseSendMeExcludeKeywords("please_send_me_exclude_keywords"),
    PleaseSendMeExcludeKeywordsRegex("please_send_me_exclude_keywords_regex"),

    VerifySsl("verify_ssl"),
    AreYouSureYouWantToSetVerifySsl("are_you_sure_you_want_to_set_verify_ssl"),
    Enable("enable"),
    Disable("disable"),
    NeedToRestartBot("need_to_restart_bot"),

    IncludeKeywords("include_keywords"),
    IncludeKeywordsRegex("include_keywords_regex"),
    PleaseSendMeIncludeKeywords("please_send_me_include_keywords"),
    PleaseSendMeIncludeKeywordsRegex("please_send_me_include_keywords_regex"),

    CaptureFlag("capture_flag"),
    TranslationLanguage("translation_language"),
    SetCaptureFlag("set_capture_flag"),
    SetCaptureFlagOnNote("set_capture_flag_on_note"),
    SetCaptureFlagOffNote("set_capture_flag_off_note"),

    ;

    private String key;

    I18nEnum(String key) {
        this.key = key;
    }

}
