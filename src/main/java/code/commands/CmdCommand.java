package code.commands;

import code.handler.CmdHandler;
import code.handler.Handler;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.extensions.bots.commandbot.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.bots.AbsSender;

import static code.Main.GlobalConfig;

@Slf4j
public class CmdCommand extends BotCommand {
    public CmdCommand() {
        super("cmd", "");
    }

    @Override
    public void processMessage(AbsSender absSender, Message message, String[] arguments) {
        execute(absSender, message, arguments);
    }

    public void execute(AbsSender absSender, Message message, String[] arguments) {
        String chatId = message.getChat().getId().toString();
        if (!chatId.equals(GlobalConfig.getBotAdminId())) {
            Handler.sendMessageWithTryCatch(chatId, "Invalid command, you are not admin", false);
            return;
        }

        CmdHandler.handle(chatId, message, arguments);
    }

    @Override
    public void execute(AbsSender absSender, User user, Chat chat, String[] strings) {

    }
}
