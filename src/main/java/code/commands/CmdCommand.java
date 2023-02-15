package code.commands;

import code.Handler;
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

        if (arguments.length < 1) {
            return;
        }

        String argument = arguments[0].toLowerCase();
        if (argument.equals("create")) {
            Handler.createMonitorHandle(true, chatId, message.getMessageId(), null);
        } else if (argument.equals("list")) {
            Handler.showMonitorListHandle(chatId, message.getMessageId());
        } else if (argument.equals("get")) {
            if (argument.length() > 1) {
                String argument1 = arguments[1];
                Handler.showMonitorHandle(chatId, message.getMessageId(), argument1);
            }
        } else if (argument.equals("exit")) {
            Handler.exitEditModeHandle(chatId, message.getMessageId());
        } else if (argument.equals("update")) {
            if (argument.length() > 1) {
                String argument1 = arguments[1];
                Handler.updateMonitorHandle(true, chatId, message.getMessageId(), argument1);
            }
        } else if (argument.equals("test")) {
            if (argument.length() > 1) {
                String argument1 = arguments[1];
                Handler.testMonitorHandle(chatId, message.getMessageId(), argument1);
            }
        } else if (argument.equals("on")) {
            if (argument.length() > 1) {
                String argument1 = arguments[1];
                Handler.onMonitorHandle(chatId, message.getMessageId(), argument1);
            }
        } else if (argument.equals("off")) {
            if (argument.length() > 1) {
                String argument1 = arguments[1];
                Handler.offMonitorHandle(chatId, message.getMessageId(), argument1);
            }
        }
    }

    @Override
    public void execute(AbsSender absSender, User user, Chat chat, String[] strings) {

    }
}
