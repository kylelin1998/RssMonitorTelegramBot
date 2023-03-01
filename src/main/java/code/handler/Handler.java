package code.handler;

import code.config.*;
import code.util.ExceptionUtil;
import code.util.RssUtil;
import com.alibaba.fastjson2.JSON;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.feed.synd.SyndPerson;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static code.Main.*;
import static code.Main.Bot;

@Slf4j
public class Handler {

    private static Map<String, List<String>> addMonitorMap = new HashMap<>();
    private static Map<String, List<String>> updateMonitorMap = new HashMap<>();

    public static void init() {
        new Thread(() -> {
            while (true) {
                try {
                    for (MonitorConfigSettings configSettings : Config.readMonitorConfigList()) {
                        rssMessageHandle(configSettings, false);
                    }

                    TimeUnit.MINUTES.sleep(GlobalConfig.getIntervalMinute());
                } catch (Exception e) {
                    log.error(ExceptionUtil.getStackTraceWithCustomInfoToStr(e));
                }
            }
        }).start();
    }

    public static void showMonitorListHandle(String chatId, Integer replyToMessageId) {
        List<MonitorConfigSettings> list = Config.readMonitorConfigList();
        if (list.size() > 0) {
            StringBuilder builder = new StringBuilder();
            ArrayList<InlineKeyboardButton> inlineKeyboardButtons = new ArrayList<>();

            for (MonitorConfigSettings settings : list) {
                builder.append(I18nHandle.getText(chatId, I18nEnum.MonitorList, settings.getFileBasename(), settings.getOn()));
                builder.append("\n\n");

                InlineKeyboardButton button = new InlineKeyboardButton();
                button.setText(settings.getFileBasename());
                button.setCallbackData("get " + settings.getFileBasename());
                inlineKeyboardButtons.add(button);
            }

            MessageHandler.sendInlineKeyboard(chatId, builder.toString(), inlineKeyboardButtons);
        } else {
            sendMessageWithTryCatch(chatId, replyToMessageId, I18nHandle.getText(chatId, I18nEnum.NothingHere), false);
        }
    }

    public static void showMonitorHandle(String chatId, Integer replyToMessageId, String text) {
        MonitorConfigSettings settings = Config.readMonitorConfig(text);
        if (null != settings) {
            InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton();
            inlineKeyboardButton.setText(I18nHandle.getText(chatId, I18nEnum.On));
            inlineKeyboardButton.setCallbackData("on " + settings.getFileBasename());

            InlineKeyboardButton inlineKeyboardButton2 = new InlineKeyboardButton();
            inlineKeyboardButton2.setText(I18nHandle.getText(chatId, I18nEnum.Off));
            inlineKeyboardButton2.setCallbackData("off " + settings.getFileBasename());

            InlineKeyboardButton inlineKeyboardButton3 = new InlineKeyboardButton();
            inlineKeyboardButton3.setText(I18nHandle.getText(chatId, I18nEnum.Test));
            inlineKeyboardButton3.setCallbackData("test " + settings.getFileBasename());

            InlineKeyboardButton inlineKeyboardButton4 = new InlineKeyboardButton();
            inlineKeyboardButton4.setText(I18nHandle.getText(chatId, I18nEnum.Update));
            inlineKeyboardButton4.setCallbackData("update " + settings.getFileBasename());

            MessageHandler.sendInlineKeyboard(chatId, settings.toString(), inlineKeyboardButton, inlineKeyboardButton2, inlineKeyboardButton3, inlineKeyboardButton4);
        } else {
            sendMessageWithTryCatch(chatId, replyToMessageId, I18nHandle.getText(chatId, I18nEnum.NotFound), false);
        }
    }

    public static void onMonitorHandle(String chatId, Integer replyToMessageId, String text) {
        MonitorConfigSettings settings = Config.readMonitorConfig(text);
        if (null != settings) {
            settings.setOn(true);
            Config.saveMonitorConfig(settings);
            sendMessageWithTryCatch(chatId, replyToMessageId, I18nHandle.getText(chatId, I18nEnum.OnMonitor), false);
        } else {
            sendMessageWithTryCatch(chatId, replyToMessageId, I18nHandle.getText(chatId, I18nEnum.NotFound), false);
        }
    }
    public static void offMonitorHandle(String chatId, Integer replyToMessageId, String text) {
        MonitorConfigSettings settings = Config.readMonitorConfig(text);
        if (null != settings) {
            settings.setOn(false);
            Config.saveMonitorConfig(settings);
            sendMessageWithTryCatch(chatId, replyToMessageId, I18nHandle.getText(chatId, I18nEnum.OffMonitor), false);
        } else {
            sendMessageWithTryCatch(chatId, replyToMessageId, I18nHandle.getText(chatId, I18nEnum.NotFound), false);
        }
    }

    public static void exitEditModeHandle(String chatId, Integer replyToMessageId) {
        addMonitorMap.remove(chatId);
        updateMonitorMap.remove(chatId);

        sendMessageWithTryCatch(chatId, replyToMessageId, I18nHandle.getText(chatId, I18nEnum.ExitEditMode), false);
    }

    public static void createMonitorHandle(boolean first, String chatId, Integer replyToMessageId, String text) {
        if (!first && !addMonitorMap.containsKey(chatId)) {
            return;
        }

        String key = "create" + chatId;
        synchronized (key.intern()) {
            try {
                if (first && !addMonitorMap.containsKey(chatId)) {
                    addMonitorMap.put(chatId, new ArrayList<String>());

                    sendMessageWithTryCatch(chatId, I18nHandle.getText(chatId, I18nEnum.CreateMonitor1), false);
                    return;
                }
                if (addMonitorMap.containsKey(chatId)) {
                    List<String> list = addMonitorMap.get(chatId);
                    if (list.size() == 0) {
                        list.add(text);
                        sendMessageWithTryCatch(chatId, replyToMessageId, I18nHandle.getText(chatId, I18nEnum.CreateMonitor2, text), false);
                        sendMessageWithTryCatch(chatId, I18nHandle.getText(chatId, I18nEnum.CreateMonitor3), false);
                        return;
                    }
                    if (list.size() == 1) {
                        sendMessageWithTryCatch(chatId, replyToMessageId, I18nHandle.getText(chatId, I18nEnum.CreateMonitor4), false);
                        SyndFeed feed = RssUtil.getFeed(RequestProxyConfig.create(), text);
                        if (null == feed) {
                            sendMessageWithTryCatch(chatId, replyToMessageId, I18nHandle.getText(chatId, I18nEnum.CreateMonitor5), false);
                            return;
                        }

                        list.add(text);
                        sendMessageWithTryCatch(chatId, replyToMessageId, I18nHandle.getText(chatId, I18nEnum.CreateMonitor6, text), false);
                        sendMessageWithTryCatch(chatId, I18nHandle.getText(chatId, I18nEnum.CreateMonitor7), false);
                        return;
                    }
                    if (list.size() == 2) {
                        list.add(text);
                        sendMessageWithTryCatch(chatId, replyToMessageId, I18nHandle.getText(chatId, I18nEnum.CreateMonitor8, text), false);
                        // save to file
                        MonitorConfigSettings settings = new MonitorConfigSettings();
                        settings.setFileBasename(list.get(0));
                        settings.setFilename(list.get(0) + ".json");
                        settings.setTemplate(list.get(2));
                        settings.setOn(false);
                        settings.setUrl(list.get(1));
                        settings.setWebPagePreview(true);
                        settings.setChatIdArray(new String[]{});
                        settings.setNotification(true);
                        Config.saveMonitorConfig(settings);
                        addMonitorMap.remove(chatId);

                        showMonitorHandle(chatId, replyToMessageId, list.get(0));
                        sendMessageWithTryCatch(chatId, I18nHandle.getText(chatId, I18nEnum.CreateMonitorFinish), false);
                        rssMessageHandle(settings, true);

                        return;
                    }
                }
            } catch (Exception e) {
                log.error(ExceptionUtil.getStackTraceWithCustomInfoToStr(e));
                sendMessageWithTryCatch(chatId, replyToMessageId, I18nHandle.getText(chatId, I18nEnum.UnknownError), false);
            }
        }
    }

    public static void testMonitorHandle(String chatId, Integer replyToMessageId, String text) {
        MonitorConfigSettings settings = Config.readMonitorConfig(text);
        if (null == settings) {
            sendMessageWithTryCatch(chatId, replyToMessageId, I18nHandle.getText(chatId, I18nEnum.TestMonitor, text), false);
            return;
        }
        rssMessageHandle(settings, true);
    }

    public static void updateMonitorHandle(boolean first, String chatId, Integer replyToMessageId, String text) {
        if (!first && !updateMonitorMap.containsKey(chatId)) {
            return;
        }

        String key = "update" + chatId;
        synchronized (key.intern()) {
            if (first && !updateMonitorMap.containsKey(chatId)) {
                MonitorConfigSettings settings = Config.readMonitorConfig(text);
                if (null == settings) {
                    sendMessageWithTryCatch(chatId, replyToMessageId, I18nHandle.getText(chatId, I18nEnum.NotFoundMonitor, text), false);
                    return;
                }

                updateMonitorMap.put(chatId, new ArrayList<>());
                List<String> list = updateMonitorMap.get(chatId);
                list.add(text);

                ArrayList<KeyboardRow> keyboardRows = new ArrayList<>();
                for (Field field : settings.getClass().getFields()) {
                    KeyboardRow row = new KeyboardRow();
                    row.add(field.getName());
                    keyboardRows.add(row);
                }

                MessageHandler.sendCustomKeyboard(chatId, I18nHandle.getText(chatId, I18nEnum.UpdateMonitor1), keyboardRows);
                return;
            }
            if (updateMonitorMap.containsKey(chatId)) {
                List<String> list = updateMonitorMap.get(chatId);
                if (list.size() == 1) {
                    try {
                        MonitorConfigSettings settings = Config.readMonitorConfig(list.get(0));
                        Field field = settings.getClass().getDeclaredField(text);
                        if (null == field) {
                            sendMessageWithTryCatch(chatId, replyToMessageId, I18nHandle.getText(chatId, I18nEnum.UpdateMonitor2, text), false);
                            return;
                        }

                        list.add(text);
                        sendMessageWithTryCatch(chatId, replyToMessageId, I18nHandle.getText(chatId, I18nEnum.UpdateMonitor3, text), false);

                        Class<?> type = field.getType();
                        if (type == Boolean.class) {
                            KeyboardRow row = new KeyboardRow();
                            row.add("true");
                            row.add("false");

                            MessageHandler.sendCustomKeyboard(chatId, I18nHandle.getText(chatId, I18nEnum.UpdateMonitor4), row);
                            return;
                        }

                        sendMessageWithTryCatch(chatId, I18nHandle.getText(chatId, I18nEnum.UpdateMonitor4), false);
                    } catch (NoSuchFieldException e) {
                        log.error(ExceptionUtil.getStackTraceWithCustomInfoToStr(e));
                        sendMessageWithTryCatch(chatId, replyToMessageId, I18nHandle.getText(chatId, I18nEnum.UnknownError), false);
                    }

                    return;
                }
                if (list.size() == 2) {
//                    list.add(text);

                    try {
                        MonitorConfigSettings settings = Config.readMonitorConfig(list.get(0));
                        Field field = settings.getClass().getDeclaredField(list.get(1));

                        if (field.getType() == String[].class) {
                            ArrayList<String> arrayList = new ArrayList<>();
                            for (String s : text.split(" ")) {
                                if (StringUtils.isNotBlank(s)) {
                                    arrayList.add(s);
                                }
                            }


                            field.set(settings, arrayList.toArray(new String[0]));
                        } else {
                            if (text.equals("true") || text.equals("false")) {
                                field.set(settings, Boolean.valueOf(text));
                            } else {
                                field.set(settings, text);
                            }
                        }

                        Config.saveMonitorConfig(settings);
                        updateMonitorMap.remove(chatId);

                        showMonitorHandle(chatId, replyToMessageId, list.get(0));
                        sendMessageWithTryCatch(chatId, I18nHandle.getText(chatId, I18nEnum.UpdateMonitorFinish), false);
                    } catch (IllegalAccessException | NoSuchFieldException | IllegalArgumentException e) {
                        sendMessageWithTryCatch(chatId, replyToMessageId, I18nHandle.getText(chatId, I18nEnum.UpdateFieldError), false);
                    }

                    return;
                }
            }

        }
    }

    public static void rssMessageHandle(MonitorConfigSettings configSettings, boolean isTest) {
        try {
            Boolean on = configSettings.getOn();
            String fileBasename = configSettings.getFileBasename();
            if ((null != on && on) || isTest) {
                SyndFeed feed = RssUtil.getFeed(RequestProxyConfig.create(), configSettings.getUrl());
                if (null == feed) {
                    if (isTest) sendMessageWithTryCatch(GlobalConfig.getBotAdminId(), I18nHandle.getText(GlobalConfig.getBotAdminId(), I18nEnum.CreateMonitor5), false);
                    return;
                }
                List<SyndEntry> entries = feed.getEntries();
                if (null == entries || entries.isEmpty()) {
                    if (isTest) sendMessageWithTryCatch(GlobalConfig.getBotAdminId(), I18nHandle.getText(GlobalConfig.getBotAdminId(), I18nEnum.NothingAtAll), false);
                    return;
                }
                if (!SentRecordTableRepository.selectExistByMonitorName(fileBasename)) {
                    for (int i = 0; i < entries.size(); i++) {
                        SyndEntry entry = entries.get(i);
                        String linkMd5 = DigestUtils.md5Hex(entry.getLink());
                        SentRecordTableRepository.insert(linkMd5, fileBasename);
                    }
                    Config.saveMonitorConfig(configSettings);
                }

                String template = configSettings.getTemplate();
                for (int i = 0; i < entries.size(); i++) {
                    SyndEntry entry = entries.get(i);
                    String linkMd5 = DigestUtils.md5Hex(entry.getLink());

                    if (SentRecordTableRepository.selectExistByIdAndMonitorName(linkMd5, fileBasename) && !isTest) continue;

                    String text = replaceTemplate(template, feed, entry);
                    if (StringUtils.isNotBlank(text)) {
                        if (!isTest) {
                            String[] chatIdArray = configSettings.getChatIdArray();
                            if (null == chatIdArray || chatIdArray.length == 0) {
                                chatIdArray = GlobalConfig.getChatIdArray();
                            }
                            for (String s : chatIdArray) {
                                sendMessageWithTryCatch(s, text, configSettings.getWebPagePreview(), configSettings.getNotification());
                            }

                            if (GlobalConfig.getChatIdArray().length > 0) {
                                SentRecordTableRepository.insert(linkMd5, fileBasename);
                                Config.saveMonitorConfig(configSettings);
                            }
                        } else {
                            sendMessageWithTryCatch(GlobalConfig.getBotAdminId(), text, configSettings.getWebPagePreview(), configSettings.getNotification());
                            if (i >= 4) {
                                break;
                            }
                        }
                    }
                }

            }
        } catch (Exception e) {
            log.error(ExceptionUtil.getStackTraceWithCustomInfoToStr(e));
            if (isTest) sendMessageWithTryCatch(GlobalConfig.getBotAdminId(), e.getMessage(), false);
        }
    }

    private static String replaceTemplate(String template, SyndFeed feed, SyndEntry entry) {
        try {
            if (StringUtils.isBlank(template) || null == entry) {
                return null;
            }

            String s = template;
            if (template.contains("${link}")) {
                s = StringUtils.replace(s, "${link}", entry.getLink());
            }
            if (template.contains("${title}")) {
                System.out.println(entry.getTitle());
                s = StringUtils.replace(s, "${title}", entry.getTitle());
            }
            if (template.contains("${author}")) {
                String author = entry.getAuthor();
                if (StringUtils.isBlank(author)) {
                    author = feed.getAuthor();
                }
                if (StringUtils.isBlank(author)) {
                    List<SyndPerson> authors = feed.getAuthors();
                    author = authors.size() > 0 ? authors.get(0).getName() : "";
                }
                s = StringUtils.replace(s, "${author}", author);
            }

            return s;
        } catch (Exception e) {
            log.error(ExceptionUtil.getStackTraceWithCustomInfoToStr(e));
        }
        return null;
    }

    public static void showLanguageListHandle(String chatId) {
        ArrayList<InlineKeyboardButton> inlineKeyboardButtons = new ArrayList<>();
        for (I18nLocaleEnum value : I18nLocaleEnum.values()) {
            InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton();
            inlineKeyboardButton.setText(value.getDisplayText());
            inlineKeyboardButton.setCallbackData("language " + value.getAlias());

            inlineKeyboardButtons.add(inlineKeyboardButton);
        }

        MessageHandler.sendInlineKeyboard(chatId, I18nHandle.getText(chatId, I18nEnum.LanguageList), inlineKeyboardButtons);
    }

    public static void changeLanguageHandle(String chatId, String text) {
        I18nLocaleEnum alias = I18nLocaleEnum.getI18nLocaleEnumByAlias(text);
        if (null == alias) {
            sendMessageWithTryCatch(chatId, I18nHandle.getText(chatId, I18nEnum.UnknownError), false);
            return;
        }

        I18nHandle.save(chatId, alias);

        sendMessageWithTryCatch(chatId, I18nHandle.getText(chatId, I18nEnum.ChangeLanguageFinish), false);
    }

    public static Message sendMessageWithTryCatch(String chatId, String text, boolean webPagePreview) {
        return sendMessageWithTryCatch(chatId, null, text, webPagePreview, true);
    }
    public static Message sendMessageWithTryCatch(String chatId, String text, boolean webPagePreview, boolean notification) {
        return sendMessageWithTryCatch(chatId, null, text, webPagePreview, notification);
    }
    public static Message sendMessageWithTryCatch(String chatId, Integer replyToMessageId, String text, boolean webPagePreview) {
        return sendMessageWithTryCatch(chatId, replyToMessageId, text, webPagePreview, true);
    }
    public static Message sendMessageWithTryCatch(String chatId, Integer replyToMessageId, String text, boolean webPagePreview, boolean notification) {
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
        return sendMessageWithTryCatch(sendMessage);
    }

    public static Message sendMessageWithTryCatch(SendMessage sendMessage) {
        try {
            Message execute = Bot.execute(sendMessage);
            return execute;
        } catch (Exception e) {
            log.error(ExceptionUtil.getStackTraceWithCustomInfoToStr(JSON.toJSONString(sendMessage), e));
        }
        return null;
    }

    public static Boolean deleteMessageWithTryCatch(DeleteMessage deleteMessage) {
        try {
            Boolean execute = Bot.execute(deleteMessage);
            return execute;
        } catch (Exception e) {
            log.error(ExceptionUtil.getStackTraceWithCustomInfoToStr(JSON.toJSONString(deleteMessage), e));
        }
        return null;
    }

}
