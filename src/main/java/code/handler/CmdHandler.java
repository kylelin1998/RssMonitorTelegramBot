package code.handler;

import org.telegram.telegrambots.meta.api.objects.Message;

public class CmdHandler {
    public static void handle(String chatId, Message message, String[] arguments) {
        if (arguments.length < 1) {
            return;
        }

        String argument = arguments[0].toLowerCase();
        if (argument.equals("create")) {
            Handler.CreateStepsHandler.init(chatId, message.getMessageId(), "");
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
                Handler.UpdateStepsHandler.init(chatId, message.getMessageId(), argument1);
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
        } else if (argument.equals("call")) {
            if (argument.length() > 1) {
                String argument1 = arguments[1].toLowerCase();
                if (argument1.equals("update")) {
                    Handler.UpdateStepsHandler.step(chatId, message.getMessageId(), arguments[2]);
                } else if (argument1.equals("delete")) {
                    Handler.DeleteStepsHandler.step(chatId, message.getMessageId(), arguments[2]);
                }
            }
        } else if (argument.equals("language")) {
            if (argument.length() > 1) {
                String argument1 = arguments[1];
                Handler.changeLanguageHandle(chatId, argument1);
            }
        } else if (argument.equals("delete")) {
            if (argument.length() > 1) {
                String argument1 = arguments[1];
                Handler.DeleteStepsHandler.init(chatId, message.getMessageId(), argument1);
            }
        }

    }

}
