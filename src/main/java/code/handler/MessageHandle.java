package code.handler;

import code.util.ExceptionUtil;
import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
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
public class MessageHandle {

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

    public static Message sendMessage(String chatId, String text, boolean webPagePreview) {
        return sendMessage(chatId, null, text, webPagePreview, true);
    }
    public static Message sendMessage(String chatId, String text, boolean webPagePreview, boolean notification) {
        return sendMessage(chatId, null, text, webPagePreview, notification);
    }
    public static Message sendMessage(String chatId, Integer replyToMessageId, String text, boolean webPagePreview) {
        return sendMessage(chatId, replyToMessageId, text, webPagePreview, true);
    }
    public static Message sendMessage(String chatId, Integer replyToMessageId, String text, boolean webPagePreview, boolean notification) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setReplyToMessageId(replyToMessageId);
        sendMessage.setText(text);
        sendMessage.setParseMode(ParseMode.HTML);
        if (!notification) {
            sendMessage.disableNotification();
        }
        if (!webPagePreview) {
            sendMessage.disableWebPagePreview();
        }
        return sendMessage(sendMessage);
    }

    public static Message sendMessage(SendMessage sendMessage) {
        try {
            Message execute = Bot.execute(sendMessage);
            return execute;
        } catch (Exception e) {
            log.error(ExceptionUtil.getStackTraceWithCustomInfoToStr(JSON.toJSONString(sendMessage), e));
        }
        return null;
    }

    public static Boolean deleteMessage(DeleteMessage deleteMessage) {
        try {
            Boolean execute = Bot.execute(deleteMessage);
            return execute;
        } catch (Exception e) {
            log.error(ExceptionUtil.getStackTraceWithCustomInfoToStr(JSON.toJSONString(deleteMessage), e));
        }
        return null;
    }

}
