package code;

import code.commands.CmdCommand;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.extensions.bots.commandbot.TelegramLongPollingCommandBot;
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
