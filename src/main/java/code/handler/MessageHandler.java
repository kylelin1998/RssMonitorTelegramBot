package code.handler;

import code.util.ExceptionUtil;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static code.Main.Bot;

@Slf4j
public class MessageHandler {

    public static Message sendInlineKeyboard(String chatId, String text, InlineKeyboardButton... inlineKeyboardButtonList) {
        return sendInlineKeyboard(chatId, text, Arrays.asList(inlineKeyboardButtonList));
    }

    public static Message sendInlineKeyboard(String chatId, String text, List<InlineKeyboardButton> inlineKeyboardButtonList) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        keyboard.add(inlineKeyboardButtonList);

        inlineKeyboardMarkup.setKeyboard(keyboard);
        message.setReplyMarkup(inlineKeyboardMarkup);

        try {
            return Bot.execute(message);
        } catch (TelegramApiException e) {
            log.error(ExceptionUtil.getStackTraceWithCustomInfoToStr(e));
        }
        return null;
    }

    public static Message sendCustomKeyboard(String chatId, String text, KeyboardRow row) {
        List<KeyboardRow> list = new ArrayList<>();
        list.add(row);

        return sendCustomKeyboard(chatId, text, list);
    }

    public static Message sendCustomKeyboard(String chatId, String text, List<KeyboardRow> keyboard) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();

        keyboardMarkup.setKeyboard(keyboard);
        message.setReplyMarkup(keyboardMarkup);

        try {
            return Bot.execute(message);
        } catch (TelegramApiException e) {
            log.error(ExceptionUtil.getStackTraceWithCustomInfoToStr(e));
        }
        return null;
    }

}
