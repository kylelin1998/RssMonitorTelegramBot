package code;

import code.config.Config;
import code.config.ConfigSettings;
import code.config.I18nEnum;
import code.config.RequestProxyConfig;
import code.handler.CommandsHandler;
import code.handler.Handler;
import code.handler.I18nHandle;
import code.handler.MessageHandle;
import code.handler.store.Store;
import code.repository.*;
import code.util.ExceptionUtil;
import code.util.Snowflake;
import com.alibaba.fastjson2.JSON;
import kong.unirest.Unirest;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

/**
 *
 *    ,---.  ,--.                                   ,--.          ,--.,--.
 *   /  O  \ |  |,--.   ,--. ,--,--.,--. ,--.,---.  |  |-.  ,---. |  |`--' ,---.,--.  ,--.,---.
 *  |  .-.  ||  ||  |.'.|  |' ,-.  | \  '  /(  .-'  | .-. '| .-. :|  |,--.| .-. :\  `'  /| .-. :
 *  |  | |  ||  ||   .'.   |\ '-'  |  \   ' .-'  `) | `-' |\   --.|  ||  |\   --. \    / \   --.
 *  `--' `--'`--''--'   '--' `--`--'.-'  /  `----'   `---'  `----'`--'`--' `----'  `--'   `----'
 *                                  `---'
 *  Always believe that something wonderful is about to happen
 *
 *  If you have any additional features you'd like to suggest or if you have any feedback,
 *  you can reach me at my email address: email@kylelin1998.com
 */
@Slf4j
public class Main {
    public static CommandsHandler Bot = null;
    public static volatile ConfigSettings GlobalConfig = Config.initConfig();
    public final static code.repository.SentRecordTableRepository SentRecordTableRepository = new SentRecordTableRepository();
    public final static code.repository.I18nTableRepository I18nTableRepository = new I18nTableRepository();
    public final static code.repository.MonitorTableRepository MonitorTableRepository = new MonitorTableRepository();
    public final static code.repository.WebhookTableRepository WebhookTableRepository = new WebhookTableRepository();
    public final static Snowflake Snowflake = new Snowflake(997);

    public static void main(String[] args) {
        log.info(String.format("Main args: %s", JSON.toJSONString(args)));
        log.info(String.format("System properties: %s", System.getProperties()));
        log.info(String.format("Config: %s", JSON.toJSONString(GlobalConfig)));

        Unirest
                .config()
                .enableCookieManagement(false)
                .verifySsl(GlobalConfig.getVerifySsl())
        ;

        new Thread(() -> {
            while (true) {
                try {
                    GlobalConfig = Config.readConfig();

                    Thread.sleep(5000);
                } catch (InterruptedException e) {}
            }
        }).start();

        Store.init();
        Handler.init();

        log.info("Program is running");

        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);

            if (GlobalConfig.getOnProxy()) {
                Bot = new CommandsHandler(RequestProxyConfig.create().buildDefaultBotOptions());
            } else {
                Bot = new CommandsHandler();
            }

            botsApi.registerBot(Bot);

            MessageHandle.sendMessage(GlobalConfig.getBotAdminId(), I18nHandle.getText(GlobalConfig.getBotAdminId(), I18nEnum.BotStartSucceed) + I18nHandle.getText(GlobalConfig.getBotAdminId(), I18nEnum.CurrentVersion) + ": " + Config.MetaData.CurrentVersion, false);

            Config.oldDataConvert();

        } catch (TelegramApiException e) {
            log.error(ExceptionUtil.getStackTraceWithCustomInfoToStr(e));
        }
    }
}
