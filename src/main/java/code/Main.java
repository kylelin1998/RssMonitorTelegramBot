package code;

import code.config.Config;
import code.config.ConfigSettings;
import code.config.I18nEnum;
import code.config.RequestProxyConfig;
import code.handler.CommandsHandler;
import code.handler.Handler;
import code.handler.I18nHandle;
import code.handler.MessageHandle;
import code.handler.message.InlineKeyboardButtonBuilder;
import code.repository.*;
import code.util.ExceptionUtil;
import code.util.Snowflake;
import kong.unirest.Unirest;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.api.objects.webapp.WebAppInfo;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.List;

@Slf4j
public class Main {
    public static CommandsHandler Bot = null;
    public static ConfigSettings GlobalConfig = Config.readConfig();
    public static code.repository.SentRecordTableRepository SentRecordTableRepository = new SentRecordTableRepository();
    public static code.repository.I18nTableRepository I18nTableRepository = new I18nTableRepository();
    public static code.repository.MonitorTableRepository MonitorTableRepository = new MonitorTableRepository();
    public static Snowflake Snowflake = new Snowflake(997);

    public static void main(String[] args) {
        Unirest
                .config()
                .enableCookieManagement(false);

        new Thread(() -> {
            while (true) {
                try {
                    GlobalConfig = Config.readConfig();

                    Thread.sleep(5000);
                } catch (InterruptedException e) {}
            }
        }).start();

        log.info("Program is running");

        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);

            new Thread(() -> {
                while (true) {
                    try {
                        if (null != Bot) {
//                            WebAppInfo webAppInfo = new WebAppInfo();
//                            webAppInfo.setUrl("https://d20c-104-28-249-49.ngrok-free.app");
//
//                            KeyboardRow keyboardButtons = new KeyboardRow();
//                            KeyboardButton keyboardButton = new KeyboardButton();
//                            keyboardButton.setText("test web");
//                            keyboardButton.setWebApp(webAppInfo);
//                            keyboardButtons.add(keyboardButton);
//                            MessageHandle.sendCustomKeyboard(GlobalConfig.getBotAdminId(), "web", keyboardButtons);
                            MessageHandle.sendMessage(GlobalConfig.getBotAdminId(), I18nHandle.getText(GlobalConfig.getBotAdminId(), I18nEnum.BotStartSucceed) + I18nHandle.getText(GlobalConfig.getBotAdminId(), I18nEnum.CurrentVersion) + ": " + Config.MetaData.CurrentVersion, false);

                            Config.oldDataConvert();

                            break;
                        }

                        Thread.sleep(100);
                    } catch (InterruptedException e) {}
                }
                Handler.init();
            }).start();

            if (GlobalConfig.getOnProxy()) {
                Bot = new CommandsHandler(RequestProxyConfig.create().buildDefaultBotOptions());
            } else {
                Bot = new CommandsHandler();
            }

            botsApi.registerBot(Bot);
        } catch (TelegramApiException e) {
            log.error(ExceptionUtil.getStackTraceWithCustomInfoToStr(e));
        }
    }
}
