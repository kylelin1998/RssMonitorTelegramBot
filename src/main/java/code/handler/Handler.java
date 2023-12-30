package code.handler;

import code.config.*;
import code.eneity.*;
import code.handler.message.CallbackBuilder;
import code.handler.message.InlineKeyboardButtonBuilder;
import code.handler.message.InlineKeyboardButtonListBuilder;
import code.handler.steps.StepResult;
import code.handler.steps.StepsBuilder;
import code.handler.steps.StepsChatSession;
import code.handler.store.ChatButtonsStore;
import code.handler.store.WebhookStore;
import code.util.*;
import code.util.translate.Translate;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import com.rometools.rome.feed.synd.SyndContent;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.feed.synd.SyndPerson;
import kong.unirest.HttpResponse;
import kong.unirest.RequestBodyEntity;
import kong.unirest.Unirest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.media.InputMedia;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaPhoto;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.io.File;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static code.Main.*;

@Slf4j
public class Handler {

    private static boolean isAdmin(String fromId) {
        return GlobalConfig.getBotAdminId().equals(fromId);
    }

    public static void init() {
        new Thread(() -> {
            while (true) {
                try {
                    MonitorTableEntity where = new MonitorTableEntity();
                    where.setZeroDelay(YesOrNoEnum.No.getNum());
                    where.setEnable(YesOrNoEnum.Yes.getNum());
                    List<MonitorTableEntity> list = MonitorTableRepository.selectList(where);
                    for (MonitorTableEntity entity : list) {
                        rssMessageHandle(null, entity, false, false);
                    }

                    TimeUnit.MINUTES.sleep(GlobalConfig.getIntervalMinute());
                } catch (Exception e) {
                    log.error(ExceptionUtil.getStackTraceWithCustomInfoToStr(e));
                }
            }
        }).start();

        new Thread(() -> {
            while (true) {
                try {
                    long startMillis = System.currentTimeMillis();
                    if (GlobalConfig.getDebug()) {
                        log.info("Zero delay, start timestamp: {}", startMillis);
                    }

                    MonitorTableEntity where = new MonitorTableEntity();
                    where.setZeroDelay(YesOrNoEnum.Yes.getNum());
                    where.setEnable(YesOrNoEnum.Yes.getNum());
                    List<MonitorTableEntity> list = MonitorTableRepository.selectList(where);
                    if (list.isEmpty()) {
                        log.info("Zero delay, monitor list is empty!");
                        TimeUnit.MINUTES.sleep(2);
                    } else {
                        CountDownLatch countDownLatch = new CountDownLatch(list.size());

                        for (MonitorTableEntity entity : list) {
                            MonitorExecutorsConfig.submit(() -> {
                                try {
                                    rssMessageHandle(null, entity, false, false);
                                } finally {
                                    countDownLatch.countDown();
                                }
                            });
                        }
                        countDownLatch.await();

                        long endMillis = System.currentTimeMillis();
                        if (GlobalConfig.getDebug()) {
                            log.info("Zero delay, end timestamp: {}, total time: {}", endMillis, endMillis - startMillis);
                        }
                        TimeUnit.SECONDS.sleep(5);
                    }
                } catch (Exception e) {
                    log.error(ExceptionUtil.getStackTraceWithCustomInfoToStr(e));
                }
            }
        }).start();

        StepsBuilder
                .create()
                .bindCommand(Command.Start, Command.Help)
                .debug(GlobalConfig.getDebug())
                .error((Exception e, StepsChatSession session) -> {
                    log.error(ExceptionUtil.getStackTraceWithCustomInfoToStr(e));
                    MessageHandle.sendMessage(session.getChatId(), I18nHandle.getText(session.getFromId(), I18nEnum.UnknownError), false);
                })
                .steps((StepsChatSession session, int index, List<String> list, Map<String, Object> context) -> {
                    MessageHandle.sendMessage(session.getChatId(), I18nHandle.getText(session.getFromId(), I18nEnum.HelpText), false);
                    return StepResult.end();
                })
                .build();

        // Create
        StepsBuilder
                .create()
                .bindCommand(Command.Create)
                .debug(GlobalConfig.getDebug())
                .error((Exception e, StepsChatSession session) -> {
                    log.error(ExceptionUtil.getStackTraceWithCustomInfoToStr(e));
                    MessageHandle.sendMessage(session.getChatId(), I18nHandle.getText(session.getFromId(), I18nEnum.UnknownError), false);
                })
                .init((StepsChatSession session, int index, List<String> list, Map<String, Object> context) -> {
                    MessageHandle.sendMessage(session.getChatId(), I18nHandle.getText(session.getFromId(), I18nEnum.CreateMonitor1), false);

                    return StepResult.ok();
                })
                .steps((StepsChatSession session, int index, List<String> list, Map<String, Object> context) -> {
                    if (session.getText().length() > 50) {
                        MessageHandle.sendMessage(session.getChatId(), I18nHandle.getText(session.getFromId(), I18nEnum.CreateNameTooLong), false);
                        return StepResult.reject();
                    }
                    Integer count = MonitorTableRepository.selectCountByName(session.getFromId(), session.getText());
                    if (count > 0) {
                        MessageHandle.sendMessage(session.getChatId(), I18nHandle.getText(session.getFromId(), I18nEnum.MonitorExists), false);
                        return StepResult.reject();
                    }

                    MessageHandle.sendMessage(session.getChatId(), I18nHandle.getText(session.getFromId(), I18nEnum.CreateMonitor2, session.getText()), false);
                    MessageHandle.sendMessage(session.getChatId(), I18nHandle.getText(session.getFromId(), I18nEnum.CreateMonitor3), false);

                    context.put("name", session.getText());

                    return StepResult.ok();
                }, (StepsChatSession session, int index, List<String> list, Map<String, Object> context) -> {
                    MessageHandle.sendMessage(session.getChatId(), I18nHandle.getText(session.getFromId(), I18nEnum.CreateMonitor4), false);
                    SyndFeed feed = RssUtil.getFeed(RequestProxyConfig.create(), session.getText());
                    if (null == feed) {
                        MessageHandle.sendMessage(session.getChatId(), I18nHandle.getText(session.getFromId(), I18nEnum.CreateMonitor5), false);
                        return StepResult.reject();
                    }

                    MessageHandle.sendMessage(session.getChatId(), I18nHandle.getText(session.getFromId(), I18nEnum.CreateMonitor6, session.getText()), false);
                    MessageHandle.sendMessage(session.getChatId(), I18nHandle.getText(session.getFromId(), I18nEnum.CreateMonitor7), false);

                    context.put("url", session.getText());

                    return StepResult.ok();
                }, (StepsChatSession session, int index, List<String> list, Map<String, Object> context) -> {
                    MessageHandle.sendMessage(session.getChatId(), I18nHandle.getText(session.getFromId(), I18nEnum.CreateMonitor8, session.getText()), false);

                    String name = (String) context.get("name");
                    String url = (String) context.get("url");

                    String id = Snowflake.nextIdToStr();

                    MonitorTableEntity settings = new MonitorTableEntity();
                    settings.setId(id);
                    settings.setCreateTime(System.currentTimeMillis());
                    settings.setChatId(session.getFromId());
                    settings.setName(name);
                    settings.setTemplate(session.getText());
                    settings.setEnable(YesOrNoEnum.No.getNum());
                    settings.setUrl(url);
                    settings.setWebPagePreview(YesOrNoEnum.Yes.getNum());
                    settings.setChatIdArrayJson(JSON.toJSONString(new ArrayList<String>()));
                    settings.setNotification(YesOrNoEnum.Yes.getNum());
                    settings.setZeroDelay(YesOrNoEnum.No.getNum());

                    MonitorTableRepository.insert(settings);

                    showMonitorHandle(session, id);
//                    MessageHandle.sendMessage(session.getChatId(), I18nHandle.getText(session.getFromId(), I18nEnum.CreateMonitorFinish), false);
//                    rssMessageHandle(session, settings, true, false);

                    return StepResult.ok();
                })
                .build();

        // Update
        StepsBuilder
                .create()
                .bindCommand(Command.Update)
                .debug(GlobalConfig.getDebug())
                .error((Exception e, StepsChatSession session) -> {
                    log.error(ExceptionUtil.getStackTraceWithCustomInfoToStr(e));
                    MessageHandle.sendMessage(session.getChatId(), I18nHandle.getText(session.getFromId(), I18nEnum.UnknownError), false);
                })
                .init((StepsChatSession session, int index, List<String> list, Map<String, Object> context) -> {
                    MonitorTableEntity settings = MonitorTableRepository.selectOne(session.getText(), session.getFromId());
                    if (null == settings) {
                        MessageHandle.sendMessage(session.getChatId(), session.getReplyToMessageId(), I18nHandle.getText(session.getFromId(), I18nEnum.NotFound, session.getText()), false);
                        return StepResult.end();
                    }

                    InlineKeyboardButtonListBuilder listBuilder = InlineKeyboardButtonListBuilder.create();
                    List<InlineKeyboardButton> inlineKeyboardButtonArrayList = new ArrayList<>();
                    for (Field field : settings.getClass().getDeclaredFields()) {
                        DisplayConfigAnnotation annotation = field.getAnnotation(DisplayConfigAnnotation.class);
                        if (null != annotation && annotation.set()) {
                            listBuilder.add(
                                    InlineKeyboardButtonBuilder
                                            .create()
                                            .add(I18nHandle.getText(session.getFromId(), annotation.i18n()), CallbackBuilder.buildCallbackData(false, session, Command.Update, field.getName()))
                                            .build()
                            );
                        }
                    }
                    listBuilder.add(
                            InlineKeyboardButtonBuilder
                                    .create()
                                    .add(I18nHandle.getText(session.getFromId(), I18nEnum.Back), CallbackBuilder.buildCallbackData(true, session, Command.Get, settings.getId()))
                                    .add(I18nHandle.getText(session.getFromId(), I18nEnum.Refresh), CallbackBuilder.buildCallbackData(true, session, Command.Get, settings.getId()))
                                    .build()
                    );


                    MessageHandle.updateInlineKeyboardList(session.getCallbackQuery().getMessage(), session.getChatId(), session.getCallbackQuery().getMessage().getText(), listBuilder.build());

                    context.put("id", session.getText());
                    context.put("session", session);

                    return StepResult.ok();
                })
                .steps((StepsChatSession session, int index, List<String> list, Map<String, Object> context) -> {
                    String id = (String) context.get("id");

                    MonitorTableEntity settings = MonitorTableRepository.selectOne(id, session.getFromId());
                    Field[] declaredFields = settings.getClass().getDeclaredFields();
                    Field field = null;
                    for (Field declaredField : declaredFields) {
                        DisplayConfigAnnotation annotation = declaredField.getAnnotation(DisplayConfigAnnotation.class);
                        if (null != annotation && annotation.set()) {
                            if (declaredField.getName().equals(session.getText())) {
                                field = declaredField;
                                break;
                            }
                        }
                    }
                    if (null == field) {
                        MessageHandle.sendMessage(session.getChatId(), session.getReplyToMessageId(), I18nHandle.getText(session.getFromId(), I18nEnum.UpdateMonitor2, session.getText()), false);
                        return StepResult.reject();
                    }

                    context.put("field", session.getText());

                    MessageHandle.sendMessage(session.getChatId(), session.getReplyToMessageId(), I18nHandle.getText(session.getFromId(), I18nEnum.UpdateMonitor3, session.getText()), false);

                    if (session.getText().equals("notification") || session.getText().equals("enable")
                            || session.getText().equals("webPagePreview") || session.getText().equals("zeroDelay")) {
                        InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton();
                        inlineKeyboardButton.setText(I18nHandle.getText(session.getFromId(), I18nEnum.On));
                        inlineKeyboardButton.setCallbackData(CallbackBuilder.buildCallbackData(false, session, Command.Update, String.valueOf(YesOrNoEnum.Yes.getNum())));

                        InlineKeyboardButton inlineKeyboardButton2 = new InlineKeyboardButton();
                        inlineKeyboardButton2.setText(I18nHandle.getText(session.getFromId(), I18nEnum.Off));
                        inlineKeyboardButton2.setCallbackData(CallbackBuilder.buildCallbackData(false, session, Command.Update, String.valueOf(YesOrNoEnum.No.getNum())));

                        Message message = MessageHandle.sendInlineKeyboard(session.getChatId(), I18nHandle.getText(session.getFromId(), I18nEnum.UpdateMonitor4), inlineKeyboardButton, inlineKeyboardButton2);
                        putDeleteMessage(context, message);
                        return StepResult.ok();
                    }

                    if (session.getText().equals("template")) {
                        MessageHandle.sendMessage(session.getChatId(), I18nHandle.getText(session.getFromId(), I18nEnum.CreateMonitor7), false);
                    } else {
                        MessageHandle.sendMessage(session.getChatId(), I18nHandle.getText(session.getFromId(), I18nEnum.UpdateMonitor4), false);
                    }
                    return StepResult.ok();
                }, (StepsChatSession session, int index, List<String> list, Map<String, Object> context) -> {
                    String id = (String) context.get("id");
                    String fieldName = (String) context.get("field");

                    try {
                        MonitorTableEntity settings = MonitorTableRepository.selectOne(id, session.getFromId());
                        Field[] declaredFields = settings.getClass().getDeclaredFields();
                        Field field = null;
                        for (Field declaredField : declaredFields) {
                            DisplayConfigAnnotation annotation = declaredField.getAnnotation(DisplayConfigAnnotation.class);
                            if (null != annotation && annotation.set()) {
                                if (declaredField.getName().equals(fieldName)) {
                                    field = declaredField;
                                    break;
                                }
                            }
                        }
                        if (null == field) {
                            MessageHandle.sendMessage(session.getChatId(), session.getReplyToMessageId(), I18nHandle.getText(session.getFromId(), I18nEnum.UpdateMonitor2, session.getText()), false);
                            return StepResult.reject();
                        }

                        if (fieldName.equals("zeroDelay")) {
                            settings.setZeroDelay(YesOrNoEnum.get(Integer.valueOf(session.getText())).get().getNum());
                        } else if (fieldName.equals("webPagePreview")) {
                            settings.setWebPagePreview(YesOrNoEnum.get(Integer.valueOf(session.getText())).get().getNum());
                        } else if (fieldName.equals("enable")) {
                            settings.setEnable(YesOrNoEnum.get(Integer.valueOf(session.getText())).get().getNum());
                        } else if (fieldName.equals("notification")) {
                            settings.setNotification(YesOrNoEnum.get(Integer.valueOf(session.getText())).get().getNum());
                        } else if (fieldName.equals("chatIdArrayJson")) {
                            String[] s = StringUtils.split(session.getText(), " ");
                            if (s.length == 0) {
                                MessageHandle.sendMessage(session.getChatId(), I18nHandle.getText(session.getFromId(), I18nEnum.UpdateFieldError), false);
                                return StepResult.reject();
                            }
                            settings.setChatIdArrayJson(JSON.toJSONString(s));
                        } else if (fieldName.equals("template")) {
                            settings.setTemplate(session.getText());
                        } else if (fieldName.equals("url")) {
                            settings.setUrl(session.getText());
                        }

                        settings.setUpdateTime(System.currentTimeMillis());
                        MonitorTableRepository.update(settings);

                        MessageHandle.sendMessage(session.getChatId(), I18nHandle.getText(session.getFromId(), I18nEnum.UpdateMonitorFinish), false);
                        showMonitorHandle((StepsChatSession) context.get("session"), id);
                        deleteMessage(context);

                    } catch (IllegalArgumentException e) {
                        MessageHandle.sendMessage(session.getChatId(), session.getReplyToMessageId(), I18nHandle.getText(session.getFromId(), I18nEnum.UpdateFieldError), false);
                        return StepResult.reject();
                    }

                    return StepResult.end();
                })
                .build();

        // Delete
        StepsBuilder
                .create()
                .bindCommand(Command.Delete)
                .debug(GlobalConfig.getDebug())
                .error((Exception e, StepsChatSession session) -> {
                    log.error(ExceptionUtil.getStackTraceWithCustomInfoToStr(e));
                    MessageHandle.sendMessage(session.getChatId(), I18nHandle.getText(session.getFromId(), I18nEnum.UnknownError), false);
                })
                .init((StepsChatSession session, int index, List<String> list, Map<String, Object> context) -> {
                    InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton();
                    inlineKeyboardButton.setText(I18nHandle.getText(session.getFromId(), I18nEnum.Confirm));
                    inlineKeyboardButton.setCallbackData(CallbackBuilder.buildCallbackData(false, session, Command.Delete, session.getText()));

                    InlineKeyboardButton inlineKeyboardButton2 = new InlineKeyboardButton();
                    inlineKeyboardButton2.setText(I18nHandle.getText(session.getFromId(), I18nEnum.Cancel));
                    inlineKeyboardButton2.setCallbackData(CallbackBuilder.buildCallbackData(false, session, Command.Delete, ""));

                    Message message = MessageHandle.sendInlineKeyboard(session.getChatId(), I18nHandle.getText(session.getFromId(), I18nEnum.DeleteMonitorConfirm), inlineKeyboardButton, inlineKeyboardButton2);
                    putDeleteMessage(context, message);
                    context.put("session", session);

                    return StepResult.ok();
                })
                .steps((StepsChatSession session, int index, List<String> list, Map<String, Object> context) -> {
                    if (StringUtils.isNotBlank(session.getText())) {
                        MonitorTableEntity entity = MonitorTableRepository.selectOne(list.get(0), session.getFromId());
                        if (null != entity) {
                            SentRecordTableRepository.delete(entity.getName(), entity.getChatId());
                            MonitorTableRepository.delete(entity.getId());
                        }
                        MessageHandle.sendMessage(session.getChatId(), I18nHandle.getText(session.getFromId(), I18nEnum.DeleteMonitorFinish), false);
                        showMonitorListHandle((StepsChatSession) context.get("session"));
                    } else {
                        MessageHandle.sendMessage(session.getChatId(), I18nHandle.getText(session.getFromId(), I18nEnum.CancelSucceeded), false);
                    }
                    deleteMessage(context);
                    return StepResult.end();
                })
                .build();

        // Exit
        StepsBuilder
                .create()
                .bindCommand(Command.Exit)
                .debug(GlobalConfig.getDebug())
                .error((Exception e, StepsChatSession session) -> {
                    log.error(ExceptionUtil.getStackTraceWithCustomInfoToStr(e));
                    MessageHandle.sendMessage(session.getChatId(), I18nHandle.getText(session.getFromId(), I18nEnum.UnknownError), false);
                })
                .steps((StepsChatSession session, int index, List<String> list, Map<String, Object> context) -> {
                    StepsCenter.exit(session);

                    MessageHandle.sendMessage(session.getChatId(), session.getReplyToMessageId(), I18nHandle.getText(session.getFromId(), I18nEnum.ExitSucceeded), false);
                    return StepResult.end();
                })
                .build();

        // List
        StepsBuilder
                .create()
                .bindCommand(Command.List)
                .debug(GlobalConfig.getDebug())
                .error((Exception e, StepsChatSession session) -> {
                    log.error(ExceptionUtil.getStackTraceWithCustomInfoToStr(e));
                    MessageHandle.sendMessage(session.getChatId(), I18nHandle.getText(session.getFromId(), I18nEnum.UnknownError), false);
                })
                .steps((StepsChatSession session, int index, List<String> list, Map<String, Object> context) -> {

                    showMonitorListHandle(session);

                    return StepResult.end();
                })
                .build();

        // Get
        StepsBuilder
                .create()
                .bindCommand(Command.Get)
                .debug(GlobalConfig.getDebug())
                .error((Exception e, StepsChatSession session) -> {
                    log.error(ExceptionUtil.getStackTraceWithCustomInfoToStr(e));
                    MessageHandle.sendMessage(session.getChatId(), I18nHandle.getText(session.getFromId(), I18nEnum.UnknownError), false);
                })
                .steps((StepsChatSession session, int index, List<String> list, Map<String, Object> context) -> {

                    showMonitorHandle(session, session.getText());

                    return StepResult.end();
                })
                .build();

        // On
        StepsBuilder
                .create()
                .bindCommand(Command.On)
                .debug(GlobalConfig.getDebug())
                .error((Exception e, StepsChatSession session) -> {
                    log.error(ExceptionUtil.getStackTraceWithCustomInfoToStr(e));
                    MessageHandle.sendMessage(session.getChatId(), I18nHandle.getText(session.getFromId(), I18nEnum.UnknownError), false);
                })
                .steps((StepsChatSession session, int index, List<String> list, Map<String, Object> context) -> {

                    MonitorTableEntity settings = MonitorTableRepository.selectOne(session.getText(), session.getFromId());
                    if (null != settings) {
                        settings.setEnable(YesOrNoEnum.Yes.getNum());
                        MonitorTableRepository.update(settings);
                        showMonitorHandle(session, session.getText());
                    } else {
                        MessageHandle.sendMessage(session.getChatId(), session.getReplyToMessageId(), I18nHandle.getText(session.getFromId(), I18nEnum.NotFound), false);
                    }

                    return StepResult.end();
                })
                .build();

        // Off
        StepsBuilder
                .create()
                .bindCommand(Command.Off)
                .debug(GlobalConfig.getDebug())
                .error((Exception e, StepsChatSession session) -> {
                    log.error(ExceptionUtil.getStackTraceWithCustomInfoToStr(e));
                    MessageHandle.sendMessage(session.getChatId(), I18nHandle.getText(session.getFromId(), I18nEnum.UnknownError), false);
                })
                .steps((StepsChatSession session, int index, List<String> list, Map<String, Object> context) -> {

                    MonitorTableEntity settings = MonitorTableRepository.selectOne(session.getText(), session.getFromId());
                    if (null != settings) {
                        settings.setEnable(YesOrNoEnum.No.getNum());
                        MonitorTableRepository.update(settings);
                        showMonitorHandle(session, session.getText());
                    } else {
                        MessageHandle.sendMessage(session.getChatId(), session.getReplyToMessageId(), I18nHandle.getText(session.getFromId(), I18nEnum.NotFound), false);
                    }

                    return StepResult.end();
                })
                .build();

        StepsBuilder
                .create()
                .bindCommand(Command.SetCaptureFlag)
                .debug(GlobalConfig.getDebug())
                .error((Exception e, StepsChatSession session) -> {
                    log.error(ExceptionUtil.getStackTraceWithCustomInfoToStr(e));
                    MessageHandle.sendMessage(session.getChatId(), I18nHandle.getText(session.getFromId(), I18nEnum.UnknownError), false);
                })
                .steps((StepsChatSession session, int index, List<String> list, Map<String, Object> context) -> {
                    MonitorTableEntity settings = MonitorTableRepository.selectOne(session.getText(), session.getFromId());
                    if (null != settings) {
                        if (null == settings.getCaptureFlag()) {
                            settings.setCaptureFlag(YesOrNoEnum.No.getNum());
                        }
                        boolean captureFlag = !YesOrNoEnum.get(settings.getCaptureFlag()).get().isBool();
                        if (captureFlag) {
                            settings.setCaptureFlag(YesOrNoEnum.Yes.getNum());
                            MessageHandle.sendMessage(session.getChatId(), I18nHandle.getText(session.getFromId(), I18nEnum.SetCaptureFlagOnNote), false);
                        } else {
                            settings.setCaptureFlag(YesOrNoEnum.No.getNum());
                            MessageHandle.sendMessage(session.getChatId(), I18nHandle.getText(session.getFromId(), I18nEnum.SetCaptureFlagOffNote), false);
                        }
                        MonitorTableRepository.update(settings);
                        showMonitorHandle(session, session.getText());
                    } else {
                        MessageHandle.sendMessage(session.getChatId(), session.getReplyToMessageId(), I18nHandle.getText(session.getFromId(), I18nEnum.NotFound), false);
                    }
                    return StepResult.end();
                })
                .build();

        // Test
        StepsBuilder
                .create()
                .bindCommand(Command.Test)
                .debug(GlobalConfig.getDebug())
                .error((Exception e, StepsChatSession session) -> {
                    log.error(ExceptionUtil.getStackTraceWithCustomInfoToStr(e));
                    MessageHandle.sendMessage(session.getChatId(), I18nHandle.getText(session.getFromId(), I18nEnum.UnknownError), false);
                })
                .steps((StepsChatSession session, int index, List<String> list, Map<String, Object> context) -> {

                    MonitorTableEntity settings = MonitorTableRepository.selectOne(session.getText(), session.getFromId());
                    if (null == settings) {
                        MessageHandle.sendMessage(session.getChatId(), session.getReplyToMessageId(), I18nHandle.getText(session.getFromId(), I18nEnum.TestMonitor, session.getText()), false);
                        return StepResult.end();
                    }
                    rssMessageHandle(session, settings, true, false);

                    return StepResult.end();
                })
                .build();

        // Force Record
        StepsBuilder
                .create()
                .bindCommand(Command.ForceRecord)
                .debug(GlobalConfig.getDebug())
                .error((Exception e, StepsChatSession session) -> {
                    log.error(ExceptionUtil.getStackTraceWithCustomInfoToStr(e));
                    MessageHandle.sendMessage(session.getChatId(), I18nHandle.getText(session.getFromId(), I18nEnum.UnknownError), false);
                })
                .steps((StepsChatSession session, int index, List<String> list, Map<String, Object> context) -> {

                    MonitorTableEntity settings = MonitorTableRepository.selectOne(session.getText(), session.getFromId());
                    if (null == settings) {
                        MessageHandle.sendMessage(session.getChatId(), session.getReplyToMessageId(), I18nHandle.getText(session.getFromId(), I18nEnum.TestMonitor, session.getText()), false);
                        return StepResult.end();
                    }
                    rssMessageHandle(session, settings, false, true);

                    MessageHandle.sendMessage(session.getChatId(), session.getReplyToMessageId(), I18nHandle.getText(session.getFromId(), I18nEnum.ForceRecordSucceeded, session.getText()), false);

                    return StepResult.end();
                })
                .build();

        // Language
        StepsBuilder
                .create()
                .bindCommand(Command.Language)
                .debug(GlobalConfig.getDebug())
                .error((Exception e, StepsChatSession session) -> {
                    log.error(ExceptionUtil.getStackTraceWithCustomInfoToStr(e));
                    MessageHandle.sendMessage(session.getChatId(), I18nHandle.getText(session.getFromId(), I18nEnum.UnknownError), false);
                })
                .init((StepsChatSession session, int index, List<String> list, Map<String, Object> context) -> {
                    ArrayList<InlineKeyboardButton> inlineKeyboardButtons = new ArrayList<>();
                    for (I18nLocaleEnum value : I18nLocaleEnum.values()) {
                        InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton();
                        inlineKeyboardButton.setText(value.getDisplayText());
                        inlineKeyboardButton.setCallbackData(CallbackBuilder.buildCallbackData(false, session, Command.Language, value.getAlias()));

                        inlineKeyboardButtons.add(inlineKeyboardButton);
                    }

                    MessageHandle.sendInlineKeyboard(session.getChatId(), I18nHandle.getText(session.getFromId(), I18nEnum.LanguageList), inlineKeyboardButtons);

                    return StepResult.ok();
                })
                .steps((StepsChatSession session, int index, List<String> list, Map<String, Object> context) -> {
                    I18nLocaleEnum alias = I18nLocaleEnum.getI18nLocaleEnumByAlias(session.getText());

                    I18nHandle.save(session.getFromId(), alias);

                    MessageHandle.sendMessage(session.getChatId(), I18nHandle.getText(session.getFromId(), I18nEnum.ChangeLanguageFinish), false);

                    return StepResult.end();
                })
                .build();

        // Admin
        StepsBuilder
                .create()
                .bindCommand(Command.Admin)
                .debug(GlobalConfig.getDebug())
                .error((Exception e, StepsChatSession session) -> {
                    log.error(ExceptionUtil.getStackTraceWithCustomInfoToStr(e));
                    code.handler.message.MessageHandle.sendMessage(session.getChatId(), I18nHandle.getText(session.getFromId(), I18nEnum.UnknownError), false);
                })
                .steps((StepsChatSession session, int index, List<String> list, Map<String, Object> context) -> {
                    if (!isAdmin(session.getFromId())) {
                        code.handler.message.MessageHandle.sendMessage(session.getChatId(), session.getReplyToMessageId(), I18nHandle.getText(session.getFromId(), I18nEnum.YouAreNotAnAdmin), false);
                        return StepResult.end();
                    }

                    ConfigSettings config = Config.readConfig();
                    List<List<InlineKeyboardButton>> keyboardButton = InlineKeyboardButtonListBuilder
                            .create()
                            .add(InlineKeyboardButtonBuilder
                                    .create()
                                    .add(I18nHandle.getText(session.getFromId(), I18nEnum.SetChatButtons), CallbackBuilder.buildCallbackData(true, session, Command.SetChatButtons, null))
                                    .add(I18nHandle.getText(session.getFromId(), I18nEnum.SettingWebhook), CallbackBuilder.buildCallbackData(true, session, Command.Webhook, null))
                                    .build()
                            )
                            .add(InlineKeyboardButtonBuilder
                                    .create()
                                    .add(I18nHandle.getText(session.getFromId(), I18nEnum.UpdateConfig), CallbackBuilder.buildCallbackData(true, session, Command.UpdateConfig, null))
                                    .add(I18nHandle.getText(session.getFromId(), I18nEnum.HideCopyrightTips), CallbackBuilder.buildCallbackData(true, session, Command.HideCopyrightTips, null))
                                    .build()
                            )
                            .add(InlineKeyboardButtonBuilder
                                    .create()
                                    .add(I18nHandle.getText(session.getFromId(), I18nEnum.VerifySsl) + String.format("(%s)", config.getVerifySsl() ? I18nHandle.getText(session.getFromId(), I18nEnum.Enable) : I18nHandle.getText(session.getFromId(), I18nEnum.Disable)), CallbackBuilder.buildCallbackData(true, session, Command.SetVerifySsl, null))
                                    .build()
                            )
                            .add(InlineKeyboardButtonBuilder
                                    .create()
                                    .add(I18nHandle.getText(session.getFromId(), I18nEnum.ExcludeKeywords), CallbackBuilder.buildCallbackData(true, session, Command.SetExcludeKeywords, null))
                                    .add(I18nHandle.getText(session.getFromId(), I18nEnum.ExcludeKeywordsRegex), CallbackBuilder.buildCallbackData(true, session, Command.SetExcludeKeywordsRegex, null))
                                    .build()
                            )
                            .add(InlineKeyboardButtonBuilder
                                    .create()
                                    .add(I18nHandle.getText(session.getFromId(), I18nEnum.IncludeKeywords), CallbackBuilder.buildCallbackData(true, session, Command.SetIncludeKeywords, null))
                                    .add(I18nHandle.getText(session.getFromId(), I18nEnum.IncludeKeywordsRegex), CallbackBuilder.buildCallbackData(true, session, Command.SetIncludeKeywordsRegex, null))
                                    .build()
                            )
                            .add(InlineKeyboardButtonBuilder
                                    .create()
                                    .add(I18nHandle.getText(session.getFromId(), I18nEnum.Restart), CallbackBuilder.buildCallbackData(true, session, Command.Restart, null))
                                    .add(I18nHandle.getText(session.getFromId(), I18nEnum.Upgrade), CallbackBuilder.buildCallbackData(true, session, Command.Upgrade, null))
                                    .build()
                            )
                            .build();

                    Properties properties = System.getProperties();

                    StringBuilder builder = new StringBuilder();
                    builder.append("os.name: ");
                    builder.append(properties.getProperty("os.name"));
                    builder.append("\n");
                    builder.append("os.arch: ");
                    builder.append(properties.getProperty("os.arch"));

                    code.handler.message.MessageHandle.sendInlineKeyboardList(session.getFromId(), builder.toString(),  keyboardButton);

                    return StepResult.end();
                })
                .build();

        StepsBuilder
                .create()
                .bindCommand(Command.SetExcludeKeywordsRegex)
                .debug(GlobalConfig.getDebug())
                .error((Exception e, StepsChatSession session) -> {
                    log.error(ExceptionUtil.getStackTraceWithCustomInfoToStr(e));
                    code.handler.message.MessageHandle.sendMessage(session.getChatId(), I18nHandle.getText(session.getFromId(), I18nEnum.UnknownError), false);
                })
                .init((StepsChatSession session, int index, List<String> list, Map<String, Object> context) -> {
                    if (!isAdmin(session.getFromId())) {
                        MessageHandle.sendMessage(session.getChatId(), session.getReplyToMessageId(), I18nHandle.getText(session.getFromId(), I18nEnum.YouAreNotAnAdmin), false);
                        return StepResult.end();
                    }

                    ConfigSettings config = Config.readConfig();
                    List<String> excludeKeywordsRegex = config.getExcludeKeywordsRegex();
                    if (null != excludeKeywordsRegex && !excludeKeywordsRegex.isEmpty()) {
                        MessageHandle.sendMessage(session.getChatId(), session.getReplyToMessageId(), excludeKeywordsRegex.stream().collect(Collectors.joining("\n")), false);
                    }

                    MessageHandle.sendMessage(session.getChatId(), session.getReplyToMessageId(), I18nHandle.getText(session.getFromId(), I18nEnum.PleaseSendMeExcludeKeywordsRegex), false);
                    return StepResult.ok();
                })
                .steps((StepsChatSession session, int index, List<String> list, Map<String, Object> context) -> {
                    String text = session.getText();
                    if (StringUtils.isBlank(text)) {
                        MessageHandle.sendMessage(session.getChatId(), session.getReplyToMessageId(), I18nHandle.getText(session.getFromId(), I18nEnum.FormatError), false);
                        return StepResult.reject();
                    }
                    List<String> excludeKeywordsRegex = new ArrayList<>();
                    if (!text.equals("-1")) {
                        String[] split = StringUtils.split(text, "\n");
                        for (String s : split) {
                            if (StringUtils.isNotBlank(s)) {
                                excludeKeywordsRegex.add(s);
                            }
                        }
                    }
                    ConfigSettings config = Config.readConfig();
                    config.setExcludeKeywordsRegex(excludeKeywordsRegex);
                    Config.saveConfig(config);

                    MessageHandle.sendMessage(session.getChatId(), session.getReplyToMessageId(), I18nHandle.getText(session.getFromId(), I18nEnum.UpdateSucceeded) + "\n\n" + excludeKeywordsRegex.stream().collect(Collectors.joining("\n")), false);
                    return StepResult.ok();
                })
                .build();

        // Set Exclude Keywords
        StepsBuilder
                .create()
                .bindCommand(Command.SetExcludeKeywords)
                .debug(GlobalConfig.getDebug())
                .error((Exception e, StepsChatSession session) -> {
                    log.error(ExceptionUtil.getStackTraceWithCustomInfoToStr(e));
                    code.handler.message.MessageHandle.sendMessage(session.getChatId(), I18nHandle.getText(session.getFromId(), I18nEnum.UnknownError), false);
                })
                .init((StepsChatSession session, int index, List<String> list, Map<String, Object> context) -> {
                    if (!isAdmin(session.getFromId())) {
                        MessageHandle.sendMessage(session.getChatId(), session.getReplyToMessageId(), I18nHandle.getText(session.getFromId(), I18nEnum.YouAreNotAnAdmin), false);
                        return StepResult.end();
                    }

                    ConfigSettings config = Config.readConfig();
                    List<String> excludeKeywords = config.getExcludeKeywords();
                    if (null != excludeKeywords && !excludeKeywords.isEmpty()) {
                        MessageHandle.sendMessage(session.getChatId(), session.getReplyToMessageId(), excludeKeywords.stream().collect(Collectors.joining("\n")), false);
                    }

                    MessageHandle.sendMessage(session.getChatId(), session.getReplyToMessageId(), I18nHandle.getText(session.getFromId(), I18nEnum.PleaseSendMeExcludeKeywords), false);
                    return StepResult.ok();
                })
                .steps((StepsChatSession session, int index, List<String> list, Map<String, Object> context) -> {
                    String text = session.getText();
                    if (StringUtils.isBlank(text)) {
                        MessageHandle.sendMessage(session.getChatId(), session.getReplyToMessageId(), I18nHandle.getText(session.getFromId(), I18nEnum.FormatError), false);
                        return StepResult.reject();
                    }
                    List<String> excludeKeywords = new ArrayList<>();
                    if (!text.equals("-1")) {
                        String[] split = StringUtils.split(text, "\n");
                        for (String s : split) {
                            if (StringUtils.isNotBlank(s)) {
                                excludeKeywords.add(s);
                            }
                        }
                    }
                    ConfigSettings config = Config.readConfig();
                    config.setExcludeKeywords(excludeKeywords);
                    Config.saveConfig(config);

                    MessageHandle.sendMessage(session.getChatId(), session.getReplyToMessageId(), I18nHandle.getText(session.getFromId(), I18nEnum.UpdateSucceeded) + "\n\n" + excludeKeywords.stream().collect(Collectors.joining("\n")), false);
                    return StepResult.ok();
                })
                .build();

        StepsBuilder
                .create()
                .bindCommand(Command.SetIncludeKeywordsRegex)
                .debug(GlobalConfig.getDebug())
                .error((Exception e, StepsChatSession session) -> {
                    log.error(ExceptionUtil.getStackTraceWithCustomInfoToStr(e));
                    code.handler.message.MessageHandle.sendMessage(session.getChatId(), I18nHandle.getText(session.getFromId(), I18nEnum.UnknownError), false);
                })
                .init((StepsChatSession session, int index, List<String> list, Map<String, Object> context) -> {
                    if (!isAdmin(session.getFromId())) {
                        MessageHandle.sendMessage(session.getChatId(), session.getReplyToMessageId(), I18nHandle.getText(session.getFromId(), I18nEnum.YouAreNotAnAdmin), false);
                        return StepResult.end();
                    }

                    ConfigSettings config = Config.readConfig();
                    List<String> includeKeywordsRegex = config.getIncludeKeywordsRegex();
                    if (null != includeKeywordsRegex && !includeKeywordsRegex.isEmpty()) {
                        MessageHandle.sendMessage(session.getChatId(), session.getReplyToMessageId(), includeKeywordsRegex.stream().collect(Collectors.joining("\n")), false);
                    }

                    MessageHandle.sendMessage(session.getChatId(), session.getReplyToMessageId(), I18nHandle.getText(session.getFromId(), I18nEnum.PleaseSendMeIncludeKeywordsRegex), false);
                    return StepResult.ok();
                })
                .steps((StepsChatSession session, int index, List<String> list, Map<String, Object> context) -> {
                    String text = session.getText();
                    if (StringUtils.isBlank(text)) {
                        MessageHandle.sendMessage(session.getChatId(), session.getReplyToMessageId(), I18nHandle.getText(session.getFromId(), I18nEnum.FormatError), false);
                        return StepResult.reject();
                    }
                    List<String> includeKeywordsRegex = new ArrayList<>();
                    if (!text.equals("-1")) {
                        String[] split = StringUtils.split(text, "\n");
                        for (String s : split) {
                            if (StringUtils.isNotBlank(s)) {
                                includeKeywordsRegex.add(s);
                            }
                        }
                    }
                    ConfigSettings config = Config.readConfig();
                    config.setIncludeKeywordsRegex(includeKeywordsRegex);
                    Config.saveConfig(config);

                    MessageHandle.sendMessage(session.getChatId(), session.getReplyToMessageId(), I18nHandle.getText(session.getFromId(), I18nEnum.UpdateSucceeded) + "\n\n" + includeKeywordsRegex.stream().collect(Collectors.joining("\n")), false);
                    return StepResult.ok();
                })
                .build();

        StepsBuilder
                .create()
                .bindCommand(Command.SetIncludeKeywords)
                .debug(GlobalConfig.getDebug())
                .error((Exception e, StepsChatSession session) -> {
                    log.error(ExceptionUtil.getStackTraceWithCustomInfoToStr(e));
                    code.handler.message.MessageHandle.sendMessage(session.getChatId(), I18nHandle.getText(session.getFromId(), I18nEnum.UnknownError), false);
                })
                .init((StepsChatSession session, int index, List<String> list, Map<String, Object> context) -> {
                    if (!isAdmin(session.getFromId())) {
                        MessageHandle.sendMessage(session.getChatId(), session.getReplyToMessageId(), I18nHandle.getText(session.getFromId(), I18nEnum.YouAreNotAnAdmin), false);
                        return StepResult.end();
                    }

                    ConfigSettings config = Config.readConfig();
                    List<String> includeKeywords = config.getIncludeKeywords();
                    if (null != includeKeywords && !includeKeywords.isEmpty()) {
                        MessageHandle.sendMessage(session.getChatId(), session.getReplyToMessageId(), includeKeywords.stream().collect(Collectors.joining("\n")), false);
                    }

                    MessageHandle.sendMessage(session.getChatId(), session.getReplyToMessageId(), I18nHandle.getText(session.getFromId(), I18nEnum.PleaseSendMeIncludeKeywords), false);
                    return StepResult.ok();
                })
                .steps((StepsChatSession session, int index, List<String> list, Map<String, Object> context) -> {
                    String text = session.getText();
                    if (StringUtils.isBlank(text)) {
                        MessageHandle.sendMessage(session.getChatId(), session.getReplyToMessageId(), I18nHandle.getText(session.getFromId(), I18nEnum.FormatError), false);
                        return StepResult.reject();
                    }
                    List<String> includeKeywords = new ArrayList<>();
                    if (!text.equals("-1")) {
                        String[] split = StringUtils.split(text, "\n");
                        for (String s : split) {
                            if (StringUtils.isNotBlank(s)) {
                                includeKeywords.add(s);
                            }
                        }
                    }
                    ConfigSettings config = Config.readConfig();
                    config.setIncludeKeywords(includeKeywords);
                    Config.saveConfig(config);

                    MessageHandle.sendMessage(session.getChatId(), session.getReplyToMessageId(), I18nHandle.getText(session.getFromId(), I18nEnum.UpdateSucceeded) + "\n\n" + includeKeywords.stream().collect(Collectors.joining("\n")), false);
                    return StepResult.ok();
                })
                .build();

        // Set Chat Buttons
        StepsBuilder
                .create()
                .bindCommand(Command.SetChatButtons)
                .debug(GlobalConfig.getDebug())
                .error((Exception e, StepsChatSession session) -> {
                    log.error(ExceptionUtil.getStackTraceWithCustomInfoToStr(e));
                    code.handler.message.MessageHandle.sendMessage(session.getChatId(), I18nHandle.getText(session.getFromId(), I18nEnum.UnknownError), false);
                })
                .init((StepsChatSession session, int index, List<String> list, Map<String, Object> context) -> {
                    if (!isAdmin(session.getFromId())) {
                        code.handler.message.MessageHandle.sendMessage(session.getChatId(), session.getReplyToMessageId(), I18nHandle.getText(session.getFromId(), I18nEnum.YouAreNotAnAdmin), false);
                        return StepResult.end();
                    }

                    ConfigSettings config = Config.readConfig();
                    String chatButtons = config.getChatButtons();
                    if (StringUtils.isNotBlank(chatButtons)) {
                        code.handler.message.MessageHandle.sendMessage(session.getChatId(), session.getReplyToMessageId(), chatButtons, false);
                    }

                    code.handler.message.MessageHandle.sendMessage(session.getChatId(), session.getReplyToMessageId(), I18nHandle.getText(session.getFromId(), I18nEnum.PleaseSendMeChatButtons), false);
                    return StepResult.ok();
                })
                .steps((StepsChatSession session, int index, List<String> list, Map<String, Object> context) -> {
                    String text = session.getText();
                    if (StringUtils.isBlank(text)) {
                        code.handler.message.MessageHandle.sendMessage(session.getChatId(), session.getReplyToMessageId(), I18nHandle.getText(session.getFromId(), I18nEnum.FormatError), false);
                        return StepResult.reject();
                    }
                    if (!text.equals("-1")) {
                        Optional<ChatButtonsStore.ChatButtonsToInlineKeyboardButtons> buttons = ChatButtonsStore.verify(text);
                        if (!buttons.isPresent()) {
                            code.handler.message.MessageHandle.sendMessage(session.getChatId(), session.getReplyToMessageId(), I18nHandle.getText(session.getFromId(), I18nEnum.FormatError), false);
                            return StepResult.reject();
                        }
                        ChatButtonsStore.ChatButtonsToInlineKeyboardButtons keyboardButtons = buttons.get();

                        for (Map.Entry<String, List<InlineKeyboardButton>> entry : keyboardButtons.getMap().entrySet()) {
                            List<List<InlineKeyboardButton>> build = InlineKeyboardButtonListBuilder
                                    .create()
                                    .add(entry.getValue())
                                    .build();
                            code.handler.message.MessageHandle.sendInlineKeyboardList(session.getChatId(), session.getReplyToMessageId(), entry.getKey(), build);
                        }
                    }
                    ChatButtonsStore.set(text);

                    ConfigSettings config = Config.readConfig();
                    config.setChatButtons(text);
                    Config.saveConfig(config);
                    code.handler.message.MessageHandle.sendMessage(session.getChatId(), session.getReplyToMessageId(), I18nHandle.getText(session.getFromId(), I18nEnum.UpdateSucceeded), false);
                    return StepResult.ok();
                })
                .build();

        // Hide Copyright Tips
        StepsBuilder
                .create()
                .bindCommand(Command.HideCopyrightTips)
                .debug(GlobalConfig.getDebug())
                .error((Exception e, StepsChatSession session) -> {
                    log.error(ExceptionUtil.getStackTraceWithCustomInfoToStr(e));
                    MessageHandle.sendMessage(session.getChatId(), I18nHandle.getText(session.getFromId(), I18nEnum.UnknownError), false);
                })
                .init((StepsChatSession session, int index, List<String> list, Map<String, Object> context) -> {
                    if (!isAdmin(session.getFromId())) {
                        MessageHandle.sendMessage(session.getChatId(), session.getReplyToMessageId(), I18nHandle.getText(session.getFromId(), I18nEnum.YouAreNotAnAdmin), false);
                        return StepResult.end();
                    }

                    InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton();
                    inlineKeyboardButton.setText(I18nHandle.getText(session.getFromId(), I18nEnum.Confirm));
                    inlineKeyboardButton.setCallbackData(CallbackBuilder.buildCallbackData(false, session, Command.HideCopyrightTips, "true"));

                    InlineKeyboardButton inlineKeyboardButton2 = new InlineKeyboardButton();
                    inlineKeyboardButton2.setText(I18nHandle.getText(session.getFromId(), I18nEnum.Cancel));
                    inlineKeyboardButton2.setCallbackData(CallbackBuilder.buildCallbackData(false, session, Command.HideCopyrightTips, "false"));

                    Message message = MessageHandle.sendInlineKeyboard(session.getChatId(), I18nHandle.getText(session.getFromId(), I18nEnum.AreYouSureYouWantToHideCopyrightTips), inlineKeyboardButton, inlineKeyboardButton2);
                    putDeleteMessage(context, message);

                    return StepResult.ok();
                })
                .steps((StepsChatSession session, int index, List<String> list, Map<String, Object> context) -> {
                    Boolean of = Boolean.valueOf(session.getText());
                    ConfigSettings configSettings = Config.readConfig();
                    configSettings.setHideCopyrightTips(of);
                    Config.saveConfig(configSettings);

                    MessageHandle.sendMessage(session.getChatId(), session.getReplyToMessageId(), I18nHandle.getText(session.getFromId(), I18nEnum.UpdateSucceeded), false);
                    deleteMessage(context);

                    return StepResult.end();
                })
                .build();

        StepsBuilder
                .create()
                .bindCommand(Command.SetVerifySsl)
                .debug(GlobalConfig.getDebug())
                .error((Exception e, StepsChatSession session) -> {
                    log.error(ExceptionUtil.getStackTraceWithCustomInfoToStr(e));
                    MessageHandle.sendMessage(session.getChatId(), I18nHandle.getText(session.getFromId(), I18nEnum.UnknownError), false);
                })
                .init((StepsChatSession session, int index, List<String> list, Map<String, Object> context) -> {
                    if (!isAdmin(session.getFromId())) {
                        MessageHandle.sendMessage(session.getChatId(), session.getReplyToMessageId(), I18nHandle.getText(session.getFromId(), I18nEnum.YouAreNotAnAdmin), false);
                        return StepResult.end();
                    }

                    InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton();
                    inlineKeyboardButton.setText(I18nHandle.getText(session.getFromId(), I18nEnum.Enable));
                    inlineKeyboardButton.setCallbackData(CallbackBuilder.buildCallbackData(false, session, Command.SetVerifySsl, "true"));

                    InlineKeyboardButton inlineKeyboardButton2 = new InlineKeyboardButton();
                    inlineKeyboardButton2.setText(I18nHandle.getText(session.getFromId(), I18nEnum.Disable));
                    inlineKeyboardButton2.setCallbackData(CallbackBuilder.buildCallbackData(false, session, Command.SetVerifySsl, "false"));

                    Message message = MessageHandle.sendInlineKeyboard(session.getChatId(), I18nHandle.getText(session.getFromId(), I18nEnum.AreYouSureYouWantToSetVerifySsl), inlineKeyboardButton, inlineKeyboardButton2);
                    putDeleteMessage(context, message);

                    return StepResult.ok();
                })
                .steps((StepsChatSession session, int index, List<String> list, Map<String, Object> context) -> {
                    Boolean of = Boolean.valueOf(session.getText());
                    ConfigSettings configSettings = Config.readConfig();
                    configSettings.setVerifySsl(of);
                    Config.saveConfig(configSettings);

                    MessageHandle.sendMessage(session.getChatId(), session.getReplyToMessageId(), I18nHandle.getText(session.getFromId(), I18nEnum.UpdateSucceeded) + ", " + I18nHandle.getText(session.getFromId(), I18nEnum.NeedToRestartBot), false);
                    deleteMessage(context);

                    return StepResult.end();
                })
                .build();

        // Webhook
        StepsBuilder
                .create()
                .bindCommand(Command.Webhook)
                .debug(GlobalConfig.getDebug())
                .error((Exception e, StepsChatSession session) -> {
                    log.error(ExceptionUtil.getStackTraceWithCustomInfoToStr(e));
                    MessageHandle.sendMessage(session.getChatId(), I18nHandle.getText(session.getFromId(), I18nEnum.UnknownError), false);
                })
                .init((StepsChatSession session, int index, List<String> list, Map<String, Object> context) -> {
                    if (!isAdmin(session.getFromId())) {
                        MessageHandle.sendMessage(session.getChatId(), session.getReplyToMessageId(), I18nHandle.getText(session.getFromId(), I18nEnum.YouAreNotAnAdmin), false);
                        return StepResult.end();
                    }

                    StringBuilder builder = new StringBuilder();
                    builder.append(I18nHandle.getText(session.getFromId(), I18nEnum.CurrentSetting) + ": \n\n");
                    WebhookTableEntity webhookTableEntity = WebhookTableRepository.selectOne(session.getFromId());
                    if (null == webhookTableEntity) {
                        builder.append(Config.WebhookJson);
                    } else {
                        builder.append(webhookTableEntity.getSettingsJson());
                    }
                    builder.append("\n\n");
                    builder.append(I18nHandle.getText(session.getFromId(), I18nEnum.AreYouSureToUpdateTheConfig));

                    InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton();
                    inlineKeyboardButton.setText(I18nHandle.getText(session.getFromId(), I18nEnum.Confirm));
                    inlineKeyboardButton.setCallbackData(CallbackBuilder.buildCallbackData(false, session, Command.Webhook, "true"));

                    InlineKeyboardButton inlineKeyboardButton2 = new InlineKeyboardButton();
                    inlineKeyboardButton2.setText(I18nHandle.getText(session.getFromId(), I18nEnum.Cancel));
                    inlineKeyboardButton2.setCallbackData(CallbackBuilder.buildCallbackData(false, session, Command.Webhook, "false"));

                    Message message = MessageHandle.sendInlineKeyboard(session.getChatId(), builder.toString(), inlineKeyboardButton, inlineKeyboardButton2);
                    putDeleteMessage(context, message);

                    return StepResult.ok();
                })
                .steps((StepsChatSession session, int index, List<String> list, Map<String, Object> context) -> {
                    Boolean of = Boolean.valueOf(session.getText());
                    if (of) {
                        MessageHandle.sendMessage(session.getChatId(), session.getReplyToMessageId(), I18nHandle.getText(session.getFromId(), I18nEnum.PleaseSendMeWebhookSettings), false);
                        return StepResult.ok();
                    } else {
                        MessageHandle.sendMessage(session.getChatId(), session.getReplyToMessageId(), I18nHandle.getText(session.getFromId(), I18nEnum.CancelSucceeded), false);
                        deleteMessage(context);
                        return StepResult.end();
                    }
                }, (StepsChatSession session, int index, List<String> list, Map<String, Object> context) -> {
                    String text = session.getText();
                    boolean verify = WebhookStore.verify(text);
                    if (!verify) {
                        MessageHandle.sendMessage(session.getChatId(), session.getReplyToMessageId(), I18nHandle.getText(session.getFromId(), I18nEnum.UpdateConfigFail), false);
                        return StepResult.reject();
                    }
                    WebhookStore.Webhook webhook = WebhookStore.get(text).get();

                    WebhookTableEntity webhookTableEntity = new WebhookTableEntity();
                    webhookTableEntity.setChatId(session.getFromId());
                    webhookTableEntity.setSettingsJson(JSON.toJSONString(webhook, JSONWriter.Feature.PrettyFormat));
                    WebhookTableRepository.save(webhookTableEntity);

                    MessageHandle.sendMessage(session.getChatId(), session.getReplyToMessageId(), I18nHandle.getText(session.getFromId(), I18nEnum.UpdateSucceeded), false);
                    deleteMessage(context);

                    return StepResult.end();
                })
                .build();

        // Restart
        StepsBuilder
                .create()
                .bindCommand(Command.Restart)
                .debug(GlobalConfig.getDebug())
                .error((Exception e, StepsChatSession session) -> {
                    log.error(ExceptionUtil.getStackTraceWithCustomInfoToStr(e));
                    MessageHandle.sendMessage(session.getChatId(), I18nHandle.getText(session.getFromId(), I18nEnum.UnknownError), false);
                })
                .init((StepsChatSession session, int index, List<String> list, Map<String, Object> context) -> {
                    if (!isAdmin(session.getFromId())) {
                        MessageHandle.sendMessage(session.getChatId(), session.getReplyToMessageId(), I18nHandle.getText(session.getFromId(), I18nEnum.YouAreNotAnAdmin), false);
                        return StepResult.end();
                    }

                    InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton();
                    inlineKeyboardButton.setText(I18nHandle.getText(session.getFromId(), I18nEnum.Confirm));
                    inlineKeyboardButton.setCallbackData(CallbackBuilder.buildCallbackData(false, session, Command.Restart, "true"));

                    InlineKeyboardButton inlineKeyboardButton2 = new InlineKeyboardButton();
                    inlineKeyboardButton2.setText(I18nHandle.getText(session.getFromId(), I18nEnum.Cancel));
                    inlineKeyboardButton2.setCallbackData(CallbackBuilder.buildCallbackData(false, session, Command.Restart, "false"));

                    MessageHandle.sendInlineKeyboard(session.getChatId(), I18nHandle.getText(session.getFromId(), I18nEnum.AreYouSureToRestartRightNow), inlineKeyboardButton, inlineKeyboardButton2);

                    return StepResult.ok();
                })
                .steps((StepsChatSession session, int index, List<String> list, Map<String, Object> context) -> {
                    Boolean of = Boolean.valueOf(session.getText());
                    if (of) {
                        MessageHandle.sendMessage(session.getChatId(), session.getReplyToMessageId(), I18nHandle.getText(session.getFromId(), I18nEnum.Restarting), false);
                        ProgramUtil.restart(Config.MetaData.ProcessName);
                    } else {
                        MessageHandle.sendMessage(session.getChatId(), session.getReplyToMessageId(), I18nHandle.getText(session.getFromId(), I18nEnum.CancelSucceeded), false);
                    }
                    return StepResult.end();
                })
                .build();

        // Update config
        StepsBuilder
                .create()
                .bindCommand(Command.UpdateConfig)
                .debug(GlobalConfig.getDebug())
                .error((Exception e, StepsChatSession session) -> {
                    log.error(ExceptionUtil.getStackTraceWithCustomInfoToStr(e));
                    code.handler.message.MessageHandle.sendMessage(session.getChatId(), I18nHandle.getText(session.getFromId(), I18nEnum.UnknownError), false);
                })
                .init((StepsChatSession session, int index, List<String> list, Map<String, Object> context) -> {
                    if (!isAdmin(session.getFromId())) {
                        code.handler.message.MessageHandle.sendMessage(session.getChatId(), session.getReplyToMessageId(), I18nHandle.getText(session.getFromId(), I18nEnum.YouAreNotAnAdmin), false);
                        return StepResult.end();
                    }

                    List<InlineKeyboardButton> buttons = InlineKeyboardButtonBuilder
                            .create()
                            .add(I18nHandle.getText(session.getFromId(), I18nEnum.Confirm), CallbackBuilder.buildCallbackData(false, session, Command.UpdateConfig, "confirm"))
                            .add(I18nHandle.getText(session.getFromId(), I18nEnum.Cancel), CallbackBuilder.buildCallbackData(false, session, Command.UpdateConfig, "cancel"))
                            .build();
                    ConfigSettings config = Config.readConfig();

                    MessageHandle.sendMessage(session.getFromId(), JSON.toJSONString(config, JSONWriter.Feature.PrettyFormat), false);
                    Message message = MessageHandle.sendInlineKeyboard(session.getChatId(), I18nHandle.getText(session.getFromId(), I18nEnum.AreYouSureToUpdateTheConfig), buttons);
                    putDeleteMessage(context, message);
                    return StepResult.ok();
                })
                .steps((StepsChatSession session, int index, List<String> list, Map<String, Object> context) -> {
                    String text = session.getText();
                    if (text.equals("confirm")) {
                        MessageHandle.sendMessage(session.getChatId(), session.getReplyToMessageId(), I18nHandle.getText(session.getFromId(), I18nEnum.PleaseSendMeConfigContent), false);
                        return StepResult.ok();
                    } else {
                        MessageHandle.sendMessage(session.getChatId(), session.getReplyToMessageId(), I18nHandle.getText(session.getFromId(), I18nEnum.CancelSucceeded), false);
                        deleteMessage(context);
                        return StepResult.end();
                    }
                }, (StepsChatSession session, int index, List<String> list, Map<String, Object> context) -> {
                    String text = session.getText();
                    ConfigSettings configSettings = Config.verifyConfig(text);
                    if (null == configSettings) {
                        code.handler.message.MessageHandle.sendMessage(session.getChatId(), session.getReplyToMessageId(), I18nHandle.getText(session.getFromId(), I18nEnum.UpdateConfigFail), false);
                        return StepResult.reject();
                    }

                    boolean b = Config.saveConfig(configSettings);
                    if (b) {
                        MessageHandle.sendMessage(session.getChatId(), session.getReplyToMessageId(), I18nHandle.getText(session.getFromId(), I18nEnum.UpdateSucceeded), false);
                    } else {
                        MessageHandle.sendMessage(session.getChatId(), session.getReplyToMessageId(), I18nHandle.getText(session.getFromId(), I18nEnum.UpdateFailed), false);
                    }
                    deleteMessage(context);
                    return StepResult.end();
                })
                .build();

        // Upgrade
        StepsBuilder
                .create()
                .bindCommand(Command.Upgrade)
                .debug(GlobalConfig.getDebug())
                .error((Exception e, StepsChatSession stepsChatSession) -> {
                    log.error(ExceptionUtil.getStackTraceWithCustomInfoToStr(e));
                    MessageHandle.sendMessage(stepsChatSession.getChatId(), I18nHandle.getText(stepsChatSession.getFromId(), I18nEnum.UnknownError), false);
                })
                .init((StepsChatSession session, int index, List<String> list, Map<String, Object> context) -> {
                    if (!isAdmin(session.getFromId())) {
                        MessageHandle.sendMessage(session.getChatId(), session.getReplyToMessageId(), I18nHandle.getText(session.getFromId(), I18nEnum.YouAreNotAnAdmin), false);
                        return StepResult.end();
                    }

                    Message message = MessageHandle.sendMessage(session.getChatId(), session.getReplyToMessageId(), I18nHandle.getText(session.getFromId(), I18nEnum.GettingUpdateData), false);
                    GithubUtil.LatestReleaseResponse release = GithubUtil.getLatestRelease(RequestProxyConfig.create(), Config.MetaData.GitOwner, Config.MetaData.GitRepo);
                    if (release.isOk()) {
                        StringBuilder builder = new StringBuilder();
                        builder.append(I18nHandle.getText(session.getFromId(), I18nEnum.AreYouSureToUpgradeThisBotRightNow));
                        builder.append("\n");
                        builder.append(I18nHandle.getText(session.getFromId(), I18nEnum.TargetVersion) + ": ");
                        builder.append(release.getTagName());
                        builder.append("\n");
                        builder.append(I18nHandle.getText(session.getFromId(), I18nEnum.CurrentVersion) + ": ");
                        builder.append(Config.MetaData.CurrentVersion);
                        builder.append("\n");
                        builder.append(I18nHandle.getText(session.getFromId(), I18nEnum.UpdateLogs) + ": ");
                        builder.append("\n");
                        builder.append(release.getBody());

                        InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton();
                        inlineKeyboardButton.setText(I18nHandle.getText(session.getFromId(), I18nEnum.Confirm));
                        inlineKeyboardButton.setCallbackData(CallbackBuilder.buildCallbackData(false, session, Command.Upgrade, "true"));

                        InlineKeyboardButton inlineKeyboardButton2 = new InlineKeyboardButton();
                        inlineKeyboardButton2.setText(I18nHandle.getText(session.getFromId(), I18nEnum.Cancel));
                        inlineKeyboardButton2.setCallbackData(CallbackBuilder.buildCallbackData(false, session, Command.Upgrade, "false"));

                        MessageHandle.sendInlineKeyboard(session.getChatId(), builder.toString(), inlineKeyboardButton, inlineKeyboardButton2);

                        String url = "";
                        for (GithubUtil.LatestReleaseAsset asset : release.getAssets()) {
                            if (Config.MetaData.JarName.equals(asset.getName())) {
                                url = asset.getBrowserDownloadUrl();
                                break;
                            }
                        }

                        context.put("url", url);

                        return StepResult.ok();
                    } else {
                        MessageHandle.editMessage(message, I18nHandle.getText(session.getFromId(), I18nEnum.UnknownError));
                        return StepResult.end();
                    }
                })
                .steps((StepsChatSession session, int index, List<String> list, Map<String, Object> context) -> {
                    Boolean of = Boolean.valueOf(session.getText());
                    if (of) {
                        Message message = MessageHandle.sendMessage(session.getChatId(), session.getReplyToMessageId(), I18nHandle.getText(session.getFromId(), I18nEnum.Updating), false);
                        String url = (String) context.get("url");

                        AtomicInteger count = new AtomicInteger();
                        String temp = System.getProperty("user.dir") + "/temp.jar";
                        log.info("temp: " + temp);
                        boolean b = DownloadUtil.download(
                                RequestProxyConfig.create(),
                                url,
                                temp,
                                (String var1, String var2, Long var3, Long var4) -> {
                                    if ((var4 - var3) > 0) {
                                        count.incrementAndGet();
                                        if (count.get() == 100) {
                                            MessageHandle.editMessage(message, I18nHandle.getText(session.getFromId(), I18nEnum.Downloaded, BytesUtil.toDisplayStr(var3), BytesUtil.toDisplayStr(var4)));
                                            count.set(0);
                                        }
                                    }
                                }
                        );

                        if (b) {
                            System.exit(1);
                        } else {
                            MessageHandle.sendMessage(session.getChatId(), session.getReplyToMessageId(), I18nHandle.getText(session.getFromId(), I18nEnum.UnknownError), false);
                        }

                    } else {
                        MessageHandle.sendMessage(session.getChatId(), session.getReplyToMessageId(), I18nHandle.getText(session.getFromId(), I18nEnum.CancelSucceeded), false);
                    }
                    return StepResult.end();
                })
                .build();
    }

    private static void putDeleteMessage(Map<String, Object> context, Message message) {
        context.put("delete", message);
    }
    private static void deleteMessage(Map<String, Object> context) {
        try {
            if (context.containsKey("delete")) {
                MessageHandle.deleteMessage((Message) context.get("delete"));
            }
        } catch (Exception e) {
            log.error(ExceptionUtil.getStackTraceWithCustomInfoToStr(e));
        }
    }

    private static void showMonitorHandle(StepsChatSession session, String id) {
        MonitorTableEntity settings = MonitorTableRepository.selectOne(id, session.getFromId());
        if (null != settings) {
            if (null == settings.getCaptureFlag()) {
                settings.setCaptureFlag(YesOrNoEnum.No.getNum());
            }
            boolean captureFlag = YesOrNoEnum.get(settings.getCaptureFlag()).get().isBool();

            List<List<InlineKeyboardButton>> build = InlineKeyboardButtonListBuilder
                    .create()
                    .add(
                            InlineKeyboardButtonBuilder
                                    .create()
                                    .add((YesOrNoEnum.get(settings.getEnable()).get().isBool() ? "✅ " : "") + I18nHandle.getText(session.getFromId(), I18nEnum.On), CallbackBuilder.buildCallbackData(true, session, Command.On, settings.getId()))
                                    .add((!YesOrNoEnum.get(settings.getEnable()).get().isBool() ? "❌ " : "") + I18nHandle.getText(session.getFromId(), I18nEnum.Off), CallbackBuilder.buildCallbackData(true, session, Command.Off, settings.getId()))
                                    .build()
                    )
                    .add(
                            InlineKeyboardButtonBuilder
                                    .create()
                                    .add((captureFlag ? "✅ " : "❌ ") + I18nHandle.getText(session.getFromId(), I18nEnum.SetCaptureFlag), CallbackBuilder.buildCallbackData(true, session, Command.SetCaptureFlag, settings.getId()))
                                    .build()
                    )
                    .add(
                            InlineKeyboardButtonBuilder
                                    .create()
                                    .add(I18nHandle.getText(session.getFromId(), I18nEnum.Test), CallbackBuilder.buildCallbackData(true, session, Command.Test, settings.getId()))
                                    .add(I18nHandle.getText(session.getFromId(), I18nEnum.ForceRecord), CallbackBuilder.buildCallbackData(true, session, Command.ForceRecord, settings.getId()))
                                    .build()
                    )
                    .add(
                            InlineKeyboardButtonBuilder
                                    .create()
                                    .add(I18nHandle.getText(session.getFromId(), I18nEnum.Update), CallbackBuilder.buildCallbackData(true, session, Command.Update, settings.getId()))
                                    .add(I18nHandle.getText(session.getFromId(), I18nEnum.Delete), CallbackBuilder.buildCallbackData(true, session, Command.Delete, settings.getId()))
                                    .build()
                    )
                    .add(
                            InlineKeyboardButtonBuilder
                                    .create()
                                    .add(I18nHandle.getText(session.getFromId(), I18nEnum.Refresh), CallbackBuilder.buildCallbackData(true, session, Command.Get, settings.getId()))
                                    .build()
                    )
                    .add(
                            InlineKeyboardButtonBuilder
                                    .create()
                                    .add(I18nHandle.getText(session.getFromId(), I18nEnum.Back), CallbackBuilder.buildCallbackData(true, session, Command.List, ""))
                                    .build()
                    )
                    .build();

            if (null == session.getCallbackQuery()) {
                MessageHandle.sendInlineKeyboardList(session.getChatId(), getMonitorData(session, settings), build);
            } else {
                MessageHandle.updateInlineKeyboardList(session.getCallbackQuery().getMessage(), session.getChatId(), getMonitorData(session, settings), build);
            }
        } else {
            MessageHandle.sendMessage(session.getChatId(), session.getReplyToMessageId(), I18nHandle.getText(session.getFromId(), I18nEnum.NotFound), false);
        }
    }

    private static void showMonitorListHandle(StepsChatSession session) {
        MonitorTableEntity where = new MonitorTableEntity();
        where.setChatId(session.getFromId());
        PageEntity page = MonitorTableRepository.page(where, 5, NumberUtils.toInt(session.getText(), 1), "order by create_time desc");
        if (!page.isHasNext() && page.getList().isEmpty()) {
            page = MonitorTableRepository.page(where, 5, 1, "order by create_time desc");
        }

        List<MonitorTableEntity> entityList = page.getList();
        if (entityList.size() > 0) {
            StringBuilder builder = new StringBuilder();
            ArrayList<InlineKeyboardButton> inlineKeyboardButtons = new ArrayList<>();

            for (MonitorTableEntity settings : entityList) {
                builder.append(I18nHandle.getText(session.getFromId(), I18nEnum.MonitorList, settings.getName(), getEnableDisplayI18nText(session.getFromId(), YesOrNoEnum.get(settings.getEnable()).get())));
                builder.append("\n\n");

                InlineKeyboardButton button = new InlineKeyboardButton();
                button.setText(settings.getName());
                button.setCallbackData(CallbackBuilder.buildCallbackData(true, session, Command.Get, settings.getId()));
                inlineKeyboardButtons.add(button);
            }

            List<List<InlineKeyboardButton>> build = InlineKeyboardButtonListBuilder
                    .create()
                    .add(inlineKeyboardButtons)
                    .pagination(page, session, Command.List)
                    .build();
            if (null == session.getCallbackQuery()) {
                MessageHandle.sendInlineKeyboardList(session.getChatId(), builder.toString(), build);
            } else {
                MessageHandle.updateInlineKeyboardList(session.getCallbackQuery().getMessage(), session.getChatId(), builder.toString(), build);
            }
        } else {
            if (null != session.getCallbackQuery()) {
                MessageHandle.deleteMessage(session.getCallbackQuery().getMessage());
            }
            MessageHandle.sendMessage(session.getChatId(), session.getReplyToMessageId(), I18nHandle.getText(session.getFromId(), I18nEnum.NothingHere), false);
        }
    }

    private static void rssMessageHandle(StepsChatSession session, MonitorTableEntity entity, boolean isTest, boolean forceRecord) {
        try {
            Boolean on = YesOrNoEnum.toBoolean(entity.getEnable()).get();
            String name = entity.getName();
            if ((null != on && on) || isTest || forceRecord) {
                SyndFeed feed = RssUtil.getFeed(RequestProxyConfig.create(), entity.getUrl());
                if (null == feed) {
                    if (isTest) MessageHandle.sendMessage(session.getChatId(), I18nHandle.getText(session.getFromId(), I18nEnum.CreateMonitor5), false);
                    return;
                }
                List<SyndEntry> entries = feed.getEntries();
                if (null == entries || entries.isEmpty()) {
                    if (isTest) MessageHandle.sendMessage(session.getChatId(), I18nHandle.getText(session.getFromId(), I18nEnum.NothingAtAll), false);
                    return;
                }
                if (!SentRecordTableRepository.exists(name, entity.getChatId()) || forceRecord) {
                    for (int i = 0; i < entries.size(); i++) {
                        SyndEntry entry = entries.get(i);

                        String uri = entry.getUri();
                        if (StringUtils.isBlank(uri)) {
                            uri = entry.getLink();
                        }

                        SentRecordTableEntity sentRecordTableEntity = new SentRecordTableEntity();
                        sentRecordTableEntity.setId(Snowflake.nextIdToStr());
                        sentRecordTableEntity.setCreateTime(System.currentTimeMillis());
                        sentRecordTableEntity.setName(name);
                        sentRecordTableEntity.setChatId(entity.getChatId());
                        sentRecordTableEntity.setUri(uri);
                        SentRecordTableRepository.save(sentRecordTableEntity);
                    }
                }

                String template = entity.getTemplate();
                for (int i = 0; i < entries.size(); i++) {
                    SyndEntry entry = entries.get(i);

                    String uri = entry.getUri();
                    if (StringUtils.isBlank(uri)) {
                        uri = entry.getLink();
                    }

                    if (SentRecordTableRepository.exists(uri, name, entity.getChatId()) && !isTest) {
                        continue;
                    }

                    String text = replaceTemplate(template, feed, entry);
                    if (StringUtils.isNotBlank(text)) {
                        List<String> images = null;
                        Integer captureFlag = (null == entity.getCaptureFlag() ? YesOrNoEnum.No.getNum() : entity.getCaptureFlag());
                        Optional<Boolean> captureFlagBoolean = YesOrNoEnum.toBoolean(captureFlag);
                        if (captureFlagBoolean.isPresent() && captureFlagBoolean.get()) {
                            images = getImages(entry);
                        }

                        if (!isTest) {
                            List<String> chatIdArray = JSON.parseArray(entity.getChatIdArrayJson(), String.class);
                            if (null == chatIdArray || chatIdArray.isEmpty()) {
                                chatIdArray = Arrays.asList(GlobalConfig.getChatIdArray());
                            }
                            for (String s : chatIdArray) {
                                if (!containsExcludeKeywords(text)) {
                                    if (isEnableIncludeKeywords()) {
                                        if (containsIncludeKeywords(text)) {
                                            sendRss(s, session, entity, text, images);
                                        }
                                    } else {
                                        sendRss(s, session, entity, text, images);
                                    }
                                }
                            }

                            if (chatIdArray.size() > 0) {
                                SentRecordTableEntity sentRecordTableEntity = new SentRecordTableEntity();
                                sentRecordTableEntity.setId(Snowflake.nextIdToStr());
                                sentRecordTableEntity.setCreateTime(System.currentTimeMillis());
                                sentRecordTableEntity.setName(name);
                                sentRecordTableEntity.setChatId(entity.getChatId());
                                sentRecordTableEntity.setUri(uri);
                                SentRecordTableRepository.save(sentRecordTableEntity);
                            }
                        } else {
                            sendRss(session.getChatId(), session, entity, text, images);
                            if (i >= 2) {
                                break;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error(ExceptionUtil.getStackTraceWithCustomInfoToStr(e));
            if (isTest) MessageHandle.sendMessage(GlobalConfig.getBotAdminId(), e.getMessage(), false);
        }
    }

    private static boolean isEnableIncludeKeywords() {
        ConfigSettings configSettings = Config.readConfig();
        if (configSettings.getIncludeKeywords().isEmpty() && configSettings.getIncludeKeywordsRegex().isEmpty()) {
            return false;
        }
        return true;
    }
    private static boolean containsIncludeKeywords(String text) {
        try {
            if (StringUtils.isNotBlank(text)) {
                ConfigSettings configSettings = Config.readConfig();
                for (String includeKeywords : configSettings.getIncludeKeywords()) {
                    if (StringUtils.containsIgnoreCase(text, includeKeywords)) {
                        return true;
                    }
                }
                for (String includeKeywordsRegex : configSettings.getIncludeKeywordsRegex()) {
                    Pattern pattern = Pattern.compile(includeKeywordsRegex);
                    Matcher matcher = pattern.matcher(text);
                    if (matcher.find()) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            log.error(ExceptionUtil.getStackTraceWithCustomInfoToStr(e));
        }
        return false;
    }
    private static boolean containsExcludeKeywords(String text) {
        try {
            if (StringUtils.isNotBlank(text)) {
                ConfigSettings configSettings = Config.readConfig();
                for (String excludeKeyword : configSettings.getExcludeKeywords()) {
                    if (StringUtils.containsIgnoreCase(text, excludeKeyword)) {
                        return true;
                    }
                }
                for (String excludeKeywordsRegex : configSettings.getExcludeKeywordsRegex()) {
                    Pattern pattern = Pattern.compile(excludeKeywordsRegex);
                    Matcher matcher = pattern.matcher(text);
                    if (matcher.find()) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            log.error(ExceptionUtil.getStackTraceWithCustomInfoToStr(e));
        }
        return false;
    }

    private static void sendRss(String chatId, StepsChatSession session, MonitorTableEntity entity, String text, List<String> images) {
        List<List<InlineKeyboardButton>> build = null;
        Optional<ChatButtonsStore.ChatButtonsToInlineKeyboardButtons> buttons = ChatButtonsStore.get();
        if (buttons.isPresent()) {
            Optional<List<InlineKeyboardButton>> inlineKeyboardButtonList = buttons.get().getButtons(session.getChatId());
            if (inlineKeyboardButtonList.isPresent()) {
                build = InlineKeyboardButtonListBuilder
                        .create()
                        .add(inlineKeyboardButtonList.get())
                        .build();
            }
        }

        boolean sendText = true;
        if (null != images && !images.isEmpty()) {
            try {
                boolean sendSingleImage = true;
                if (images.size() > 1) {
                    List<InputMedia> inputMedia = new ArrayList<>();
                    AtomicInteger countAtomic = new AtomicInteger(0);
                    for (String image : images) {
                        if (inputMedia.size() >= 10) {
                            break;
                        }
                        String name = UUID.randomUUID().toString();
                        String temp = Config.TempDir + File.separator + name + ".png";
                        boolean download = DownloadUtil.download(RequestProxyConfig.create(), image, temp);
                        if (download) {
                            int count = countAtomic.addAndGet(1);

                            InputMediaPhoto inputMediaPhoto = new InputMediaPhoto();
                            inputMediaPhoto.setMedia(new File(temp), name);
                            if (count == 1) {
                                inputMediaPhoto.setCaption(text);
                            }
                            inputMedia.add(inputMediaPhoto);
                        }
                    }
                    if (inputMedia.size() >= 2) {
                        List<Message> messages = MessageHandle.sendMediaGroup(chatId, inputMedia, YesOrNoEnum.toBoolean(entity.getNotification()).get());
                        if (null != messages && !messages.isEmpty()) {
                            sendSingleImage = false;
                            sendText = false;
                        }
                    }
                }
                if (sendSingleImage) {
                    String image = images.get(0);
                    String temp = Config.TempDir + File.separator + UUID.randomUUID() + ".png";
                    boolean download = DownloadUtil.download(RequestProxyConfig.create(), image, temp);
                    if (download) {
                        Message message = MessageHandle.sendImage(chatId, null, text, new File(temp), build);
                        if (null != message) {
                            sendText = false;
                        }
                    }
                }
            } catch (Exception e) {
                log.error(ExceptionUtil.getStackTraceWithCustomInfoToStr(e));
            }
        }
        if (sendText) {
            MessageHandle.sendMessage(chatId, null, text, YesOrNoEnum.toBoolean(entity.getWebPagePreview()).get(), YesOrNoEnum.toBoolean(entity.getNotification()).get(), build);
        }

        WebhookTableEntity webhookTableEntity = WebhookTableRepository.selectOne(entity.getChatId());
        if (null != webhookTableEntity) {
            Optional<WebhookStore.Webhook> webhookOptional = WebhookStore.get(webhookTableEntity.getSettingsJson());
            if (webhookOptional.isPresent()) {
                WebhookStore.Webhook webhook = webhookOptional.get();
                if (webhook.isEnable()) {
                    try {
                        for (WebhookStore.WebhookRequest request : webhook.getList()) {
                            String body = JSON.toJSONString(request.getBody());
                            String str = JSON.toJSONString(text);
                            str = StringUtils.removeStart(str, "\"");
                            str = StringUtils.removeEnd(str, "\"");
                            body = StringUtils.replace(body, "${text}", str);

                            RequestBodyEntity requestBody = Unirest
                                    .request(request.getMethod(), request.getUrl())
                                    .headers(request.getHeaders())
                                    .body(body)
                                    .connectTimeout((int) TimeUnit.MILLISECONDS.convert(20, TimeUnit.SECONDS))
                                    .socketTimeout((int) TimeUnit.MILLISECONDS.convert(40, TimeUnit.SECONDS));
                            if (GlobalConfig.getOnProxy()) {
                                requestBody.proxy(GlobalConfig.getProxyHost(), GlobalConfig.getProxyPort());
                            }
                            HttpResponse<String> rsp = requestBody.asString();
                            log.info("Webhook request, url: {}, body: {}, rsp: {}", request.getUrl(), body, rsp.getBody());
                        }
                    } catch (Exception e) {
                        log.error(ExceptionUtil.getStackTraceWithCustomInfoToStr(e));
                    }
                }
            }
        }
    }

    private static List<String> getImages(SyndEntry entry) {
        List<String> list = new ArrayList<>();
        if (null != entry) {
            SyndContent description = entry.getDescription();
            if ("text/html".equals(description.getType())) {
                try {
                    Document document = Jsoup.parse(description.getValue());
                    if (null != document) {
                        Elements images = document.select("img");
                        for (Element image : images) {
                            String imageUrl = image.attr("src");
                            if (StringUtils.isNotBlank(imageUrl)) {
                                list.add(imageUrl);
                            }
                        }
                    }
                } catch (Exception e) {
                    log.error(ExceptionUtil.getStackTraceWithCustomInfoToStr(e));
                }
            }
        }
        return list;
    }
    public static String getDescription(SyndEntry entry) {
        if (null != entry) {
            SyndContent description = entry.getDescription();
            if ("text/html".equals(description.getType())) {
                return getDescription(description.getValue());
            } else {
                return StringUtils.defaultIfBlank(description.getValue(), "");
            }
        }
        return "";
    }
    public static String getDescription(String html) {
        if (null != html) {
            try {
                Document document = Jsoup.parse(html);
                if (null != document) {
                    Elements br = document.select("br");
                    for (Element element : br) {
                        element.html("\n");
                    }
                    String text = document.wholeText();
                    return StringUtils.defaultIfBlank(text, "");
                }
            } catch (Exception e) {
                log.error(ExceptionUtil.getStackTraceWithCustomInfoToStr(e));
            }
        }
        return "";
    }
    private static String replaceTemplate(String template, SyndFeed feed, SyndEntry entry) {
        try {
            if (StringUtils.isBlank(template) || null == entry) {
                return null;
            }

            String s = new String(template);

            if (template.contains("${translate")) {
                try {
                    String pattern = "\\$\\{translate\\|(\\w+-\\w+|\\w+)\\|\\w+\\}";

                    Pattern regex = Pattern.compile(pattern);
                    Matcher matcher = regex.matcher(s);

                    while (matcher.find()) {
                        String variable = matcher.group();
                        if (StringUtils.isNotBlank(variable)) {
                            String variableEdit = StringUtils.removeStart(variable, "${");
                            variableEdit = StringUtils.removeEnd(variableEdit, "}");
                            String[] split = StringUtils.split(variableEdit, "|");
                            if (split.length == 3) {
                                String s1 = split[0];
                                String s2 = split[1];
                                String s3 = split[2];

                                String text = "";
                                if ("title".equals(s3)) {
                                    text = entry.getTitle();
                                } else if ("description".equals(s3)) {
                                    text = getDescription(entry);
                                }
                                String translate = Translate.translate(text, "auto", s2);
                                s = StringUtils.replace(s, variable, translate);
                            }
                        }
                    }
                } catch (Exception e) {
                    log.error(ExceptionUtil.getStackTraceWithCustomInfoToStr(e));
                }
            }

            if (template.contains("${link}")) {
                s = StringUtils.replace(s, "${link}", entry.getLink());
            }
            if (template.contains("${title}")) {
                s = StringUtils.replace(s, "${title}", entry.getTitle());
            }
            if (template.contains("${description}")) {
                s = StringUtils.replace(s, "${description}", getDescription(entry));
            }
            String author = entry.getAuthor();
            if (StringUtils.isBlank(author)) {
                author = feed.getAuthor();
            }
            if (StringUtils.isBlank(author)) {
                List<SyndPerson> authors = feed.getAuthors();
                author = authors.size() > 0 ? authors.get(0).getName() : "";
            }
            if (template.contains("${author}")) {
                s = StringUtils.replace(s, "${author}", author);
            }
            if (template.contains("${telegraph}")) {
                String html = null;

                List<SyndContent> contents = entry.getContents();
                if (contents.size() > 0) {
                    String value = contents.get(0).getValue();
                    if (StringUtils.isNotBlank(value)) {
                        html = value;
                    }
                }

                if (StringUtils.isBlank(html)) {
                    SyndContent description = entry.getDescription();
                    if (null != description) {
                        String value = description.getValue();
                        if (StringUtils.isNotBlank(value)) {
                            html = value;
                        }
                    }
                }

                if (StringUtils.isNotBlank(html)) {
                    String telegraphHtml = null;
                    if (!GlobalConfig.getHideCopyrightTips()) {
                        telegraphHtml = replaceTelegraphHtml(entry.getLink(), entry.getTitle());
                    }

                    TelegraphUtil.SaveResponse response =
                            TelegraphUtil.save(RequestProxyConfig.create(), entry.getTitle(), author, html, telegraphHtml);
                    if (response.isOk()) {
                        s = StringUtils.replace(s, "${telegraph}", response.getUrl());
                    } else {
                        s = StringUtils.replace(s, "${telegraph}", "");
                    }
                } else {
                    s = StringUtils.replace(s, "${telegraph}", "");
                }
            }

            return s;
        } catch (Exception e) {
            log.error(ExceptionUtil.getStackTraceWithCustomInfoToStr(e));
        }
        return null;
    }

    private static String replaceTelegraphHtml(String link, String title) {
        String s = StringUtils.replace(Config.TelegraphHtml, "${link}", link);
        return StringUtils.replace(s, "${title}", title);
    }

    private static String getEnableDisplayI18nText(String fromId, YesOrNoEnum yesOrNoEnum) {
        switch (yesOrNoEnum) {
            case Yes:
                return I18nHandle.getText(fromId, I18nEnum.On);
            case No:
                return I18nHandle.getText(fromId, I18nEnum.Off);
            default:
                return "";
        }
    }

    private static String getMonitorData(StepsChatSession session, MonitorTableEntity entity) {
        String chatIdArrayStr = "";
        List<String> chatIdArray = JSON.parseArray(entity.getChatIdArrayJson(), String.class);
        if (null == chatIdArray || chatIdArray.isEmpty()) {
            chatIdArrayStr = StringUtils.join(GlobalConfig.getChatIdArray(), " ");
        } else {
            chatIdArrayStr = StringUtils.join(chatIdArray, " ");
        }

        StringBuilder builder = new StringBuilder();

        builder.append(String.format("%s: %s\n", I18nHandle.getText(session.getFromId(), I18nEnum.ConfigDisplayFileBasename), entity.getName()));
        builder.append(String.format("%s: %s\n", I18nHandle.getText(session.getFromId(), I18nEnum.ConfigDisplayOn), getEnableDisplayI18nText(session.getFromId(), YesOrNoEnum.get(entity.getEnable()).get())));
        builder.append(String.format("%s: %s\n", I18nHandle.getText(session.getFromId(), I18nEnum.ConfigDisplayWebPagePreview), getEnableDisplayI18nText(session.getFromId(), YesOrNoEnum.get(entity.getWebPagePreview()).get())));
        builder.append(String.format("%s: %s\n", I18nHandle.getText(session.getFromId(), I18nEnum.ConfigDisplayNotification), getEnableDisplayI18nText(session.getFromId(), YesOrNoEnum.get(entity.getNotification()).get())));
        builder.append(String.format("%s: %s\n", I18nHandle.getText(session.getFromId(), I18nEnum.ConfigDisplayZeroDelay), getEnableDisplayI18nText(session.getFromId(), YesOrNoEnum.get(entity.getZeroDelay()).get())));
        builder.append(String.format("%s: %s\n", I18nHandle.getText(session.getFromId(), I18nEnum.ConfigDisplayUrl), entity.getUrl()));
        builder.append(String.format("%s: %s\n", I18nHandle.getText(session.getFromId(), I18nEnum.ConfigDisplayChatIdArray), chatIdArrayStr));
        builder.append(String.format("%s: \n%s", I18nHandle.getText(session.getFromId(), I18nEnum.ConfigDisplayTemplate), entity.getTemplate()));

        return builder.toString();
    }

}
