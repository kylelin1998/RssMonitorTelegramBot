package code.handler;

import lombok.Getter;

@Getter
public enum Command {

    Start("start"),
    Help("help"),
    Create("create"),
    List("list"),
    Get("get"),
    Update("update"),
    Test("test"),
    ForceRecord("fr"),
    On("on"),
    Off("off"),
    Delete("delete"),

    Admin("admin"),
    Exit("exit"),
    Language("language"),
    Restart("restart"),
    UpdateConfig("uc"),
    Upgrade("upgrade"),
    SetChatButtons("set_chat_buttons"),
    HideCopyrightTips("hide_copyright_tips"),

    Webhook("webhook"),

    SetVerifySsl("verify_ssl"),
    SetExcludeKeywords("exclude_keywords"),
    SetExcludeKeywordsRegex("exclude_keywords_regex"),
    SetIncludeKeywords("include_keywords"),
    SetIncludeKeywordsRegex("include_keywords_regex"),
    SetCaptureFlag("set_capture_flag"),

    ;

    private String cmd;

    Command(String cmd) {
        this.cmd = cmd;
    }

    public static Command toCmd(String cmd) {
        for (Command value : Command.values()) {
            if (value.getCmd().equals(cmd)) {
                return value;
            }
        }
        return null;
    }

    public static boolean exist(String cmd) {
        return null != toCmd(cmd);
    }

}
