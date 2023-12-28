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

    Webhook("webhook"),

    SetVerifySsl("rssb01"),
    SetExcludeKeywords("rssb02"),
    SetExcludeKeywordsRegex("rssb03"),
    SetIncludeKeywords("rssb04"),
    SetIncludeKeywordsRegex("rssb05"),
    SetCaptureFlag("rssb06"),
    SetTranslationLanguage("rssb07"),
    SetChatButtons("rssb11"),
    HideCopyrightTips("rssb10"),

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
