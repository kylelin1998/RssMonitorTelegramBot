package code.handler;

import code.commands.CmdCommand;
import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.extensions.bots.commandbot.TelegramLongPollingCommandBot;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import static code.Main.GlobalConfig;

@Slf4j
public class CommandsHandler extends TelegramLongPollingCommandBot {

    public CommandsHandler() {
        super();
        start();
    }

    public CommandsHandler(DefaultBotOptions botOptions) {
        super(botOptions);
        start();
    }

    public void start() {
        register(new CmdCommand());
    }

    @Override
    public String getBotUsername() {
        return GlobalConfig.getBotName();
    }

    @Override
    public String getBotToken() {
        return GlobalConfig.getBotToken();
    }

    @Override
    public void processNonCommandUpdate(Update update) {
        if (GlobalConfig.getDebug()) {
            log.info(JSON.toJSONString(update));
        }

        CallbackQuery callbackQuery = update.getCallbackQuery();
        if (null != callbackQuery) {
            String chatId = String.valueOf(callbackQuery.getFrom().getId());
            if (!chatId.equals(GlobalConfig.getBotAdminId())) {
                return;
            }

            String data = callbackQuery.getData();
            if (StringUtils.isNotBlank(data)) {
                String[] arguments = data.split(" ");
                CmdHandler.handle(chatId, callbackQuery.getMessage(), arguments);
                return;
            }
        }

        Message message = update.getMessage();
        if (null == message) {
            return;
        }
        String chatId = message.getChat().getId().toString();
        if (!chatId.equals(GlobalConfig.getBotAdminId())) {
            return;
        }

        String text = message.getText();
        Handler.createMonitorHandle(false, chatId, message.getMessageId(), text);
        Handler.updateMonitorHandle(false, chatId, message.getMessageId(), text);
    }

}
