package code.handler;

import code.config.*;
import code.handler.steps.StepResult;
import code.handler.steps.StepsBuilder;
import code.handler.steps.StepsChatSession;
import code.util.*;
import com.rometools.rome.feed.synd.SyndContent;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.feed.synd.SyndPerson;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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
                    for (MonitorConfigSettings configSettings : Config.readMonitorConfigList()) {
                        if (!configSettings.getZeroDelay()) {
                            rssMessageHandle(configSettings, false);
                        }
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
                    boolean isWait = true;
                    for (MonitorConfigSettings configSettings : Config.readMonitorConfigList()) {
                        if (configSettings.getZeroDelay()) {
                            isWait = false;
                            rssMessageHandle(configSettings, false);
                        }
                    }

                    if (isWait) {
                        TimeUnit.MINUTES.sleep(2);
                    } else {
                        TimeUnit.SECONDS.sleep(2);
                    }
                } catch (Exception e) {
                    log.error(ExceptionUtil.getStackTraceWithCustomInfoToStr(e));
                }
            }
        }).start();

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
                    if (Config.hasMonitorConfig(session.getText())) {
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

                    // save to file
                    MonitorConfigSettings settings = new MonitorConfigSettings();
                    settings.setFileBasename(name);
                    settings.setFilename(name + ".json");
                    settings.setTemplate(session.getText());
                    settings.setOn(false);
                    settings.setUrl(url);
                    settings.setWebPagePreview(true);
                    settings.setChatIdArray(new String[]{});
                    settings.setNotification(true);
                    settings.setZeroDelay(false);
                    Config.saveMonitorConfig(settings);

                    showMonitorHandle(session, name);
                    MessageHandle.sendMessage(session.getChatId(), I18nHandle.getText(session.getFromId(), I18nEnum.CreateMonitorFinish), false);
                    rssMessageHandle(settings, true);

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
                    MonitorConfigSettings settings = Config.readMonitorConfig(session.getText());
                    if (null == settings) {
                        MessageHandle.sendMessage(session.getChatId(), session.getReplyToMessageId(), I18nHandle.getText(session.getFromId(), I18nEnum.NotFound, session.getText()), false);
                        return StepResult.end();
                    }

                    List<InlineKeyboardButton> inlineKeyboardButtonArrayList = new ArrayList<>();
                    for (Field field : settings.getClass().getDeclaredFields()) {
                        DisplayConfigAnnotation annotation = field.getAnnotation(DisplayConfigAnnotation.class);
                        if (null != annotation && annotation.set()) {
                            InlineKeyboardButton row = new InlineKeyboardButton();
                            row.setText(I18nHandle.getText(session.getFromId(), annotation.i18n()));
                            row.setCallbackData(StepsCenter.buildCallbackData(false, session, Command.Update, field.getName()));

                            inlineKeyboardButtonArrayList.add(row);
                        }
                    }

                    MessageHandle.sendInlineKeyboard(session.getChatId(), I18nHandle.getText(session.getFromId(), I18nEnum.UpdateMonitor1), inlineKeyboardButtonArrayList);

                    context.put("name", session.getText());

                    return StepResult.ok();
                })
                .steps((StepsChatSession session, int index, List<String> list, Map<String, Object> context) -> {
                    String name = (String) context.get("name");

                    MonitorConfigSettings settings = Config.readMonitorConfig(name);
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

                    Class<?> type = field.getType();
                    if (type == Boolean.class) {
                        InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton();
                        inlineKeyboardButton.setText(I18nHandle.getText(session.getFromId(), I18nEnum.On));
                        inlineKeyboardButton.setCallbackData(StepsCenter.buildCallbackData(false, session, Command.Update, "true"));

                        InlineKeyboardButton inlineKeyboardButton2 = new InlineKeyboardButton();
                        inlineKeyboardButton2.setText(I18nHandle.getText(session.getFromId(), I18nEnum.Off));
                        inlineKeyboardButton2.setCallbackData(StepsCenter.buildCallbackData(false, session, Command.Update, "false"));

                        MessageHandle.sendInlineKeyboard(session.getChatId(), I18nHandle.getText(session.getFromId(), I18nEnum.UpdateMonitor4), inlineKeyboardButton, inlineKeyboardButton2);
                        return StepResult.ok();
                    }

                    MessageHandle.sendMessage(session.getChatId(), I18nHandle.getText(session.getFromId(), I18nEnum.UpdateMonitor4), false);

                    return StepResult.ok();
                }, (StepsChatSession session, int index, List<String> list, Map<String, Object> context) -> {
                    String name = (String) context.get("name");
                    String fieldName = (String) context.get("field");

                    try {
                        MonitorConfigSettings settings = Config.readMonitorConfig(name);
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

                        field.setAccessible(true);
                        if (field.getType() == String[].class) {
                            ArrayList<String> arrayList = new ArrayList<>();
                            for (String s : session.getText().split(" ")) {
                                if (StringUtils.isNotBlank(s)) {
                                    arrayList.add(s);
                                }
                            }


                            field.set(settings, arrayList.toArray(new String[0]));
                        } else {
                            if (session.getText().equals("true") || session.getText().equals("false")) {
                                field.set(settings, Boolean.valueOf(session.getText()));
                            } else {
                                field.set(settings, session.getText());
                            }
                        }

                        Config.saveMonitorConfig(settings);

                        showMonitorHandle(session, name);

                        MessageHandle.sendMessage(session.getChatId(), I18nHandle.getText(session.getFromId(), I18nEnum.UpdateMonitorFinish), false);
                    } catch (IllegalAccessException | IllegalArgumentException e) {
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
                    inlineKeyboardButton.setCallbackData(StepsCenter.buildCallbackData(false, session, Command.Delete, session.getText()));

                    InlineKeyboardButton inlineKeyboardButton2 = new InlineKeyboardButton();
                    inlineKeyboardButton2.setText(I18nHandle.getText(session.getFromId(), I18nEnum.Cancel));
                    inlineKeyboardButton2.setCallbackData(StepsCenter.buildCallbackData(false, session, Command.Delete, ""));

                    MessageHandle.sendInlineKeyboard(session.getChatId(), I18nHandle.getText(session.getFromId(), I18nEnum.DeleteMonitorConfirm), inlineKeyboardButton, inlineKeyboardButton2);

                    return StepResult.ok();
                })
                .steps((StepsChatSession session, int index, List<String> list, Map<String, Object> context) -> {
                    if (StringUtils.isNotBlank(session.getText())) {
                        MonitorConfigSettings monitorConfigSettings = Config.readMonitorConfig(list.get(0));
                        if (null != monitorConfigSettings) {
                            Config.deleteMonitorConfig(monitorConfigSettings);
                        }
                        MessageHandle.sendMessage(session.getChatId(), I18nHandle.getText(session.getFromId(), I18nEnum.DeleteMonitorFinish), false);

                        showMonitorListHandle(session);
                    } else {
                        MessageHandle.sendMessage(session.getChatId(), I18nHandle.getText(session.getFromId(), I18nEnum.CancelSucceeded), false);
                    }

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

                    MonitorConfigSettings settings = Config.readMonitorConfig(session.getText());
                    if (null != settings) {
                        settings.setOn(true);
                        Config.saveMonitorConfig(settings);
                        MessageHandle.sendMessage(session.getChatId(), session.getReplyToMessageId(), I18nHandle.getText(session.getFromId(), I18nEnum.OnMonitor), false);
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

                    MonitorConfigSettings settings = Config.readMonitorConfig(session.getText());
                    if (null != settings) {
                        settings.setOn(false);
                        Config.saveMonitorConfig(settings);
                        MessageHandle.sendMessage(session.getChatId(), session.getReplyToMessageId(), I18nHandle.getText(session.getFromId(), I18nEnum.OffMonitor), false);
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

                    MonitorConfigSettings settings = Config.readMonitorConfig(session.getText());
                    if (null == settings) {
                        MessageHandle.sendMessage(session.getChatId(), session.getReplyToMessageId(), I18nHandle.getText(session.getFromId(), I18nEnum.TestMonitor, session.getText()), false);
                        return StepResult.end();
                    }
                    rssMessageHandle(settings, true);

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
                        inlineKeyboardButton.setCallbackData(StepsCenter.buildCallbackData(false, session, Command.Language, value.getAlias()));

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
                    inlineKeyboardButton.setCallbackData(StepsCenter.buildCallbackData(false, session, Command.Restart, "true"));

                    InlineKeyboardButton inlineKeyboardButton2 = new InlineKeyboardButton();
                    inlineKeyboardButton2.setText(I18nHandle.getText(session.getFromId(), I18nEnum.Cancel));
                    inlineKeyboardButton2.setCallbackData(StepsCenter.buildCallbackData(false, session, Command.Restart, "false"));

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
                        inlineKeyboardButton.setCallbackData(StepsCenter.buildCallbackData(false, session, Command.Upgrade, "true"));

                        InlineKeyboardButton inlineKeyboardButton2 = new InlineKeyboardButton();
                        inlineKeyboardButton2.setText(I18nHandle.getText(session.getFromId(), I18nEnum.Cancel));
                        inlineKeyboardButton2.setCallbackData(StepsCenter.buildCallbackData(false, session, Command.Upgrade, "false"));

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

    private static void showMonitorHandle(StepsChatSession session, String fileBasename) {
        MonitorConfigSettings settings = Config.readMonitorConfig(fileBasename);
        if (null != settings) {
            InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton();
            inlineKeyboardButton.setText(I18nHandle.getText(session.getFromId(), I18nEnum.On));
            inlineKeyboardButton.setCallbackData(StepsCenter.buildCallbackData(true, session, Command.On, settings.getFileBasename()));

            InlineKeyboardButton inlineKeyboardButton2 = new InlineKeyboardButton();
            inlineKeyboardButton2.setText(I18nHandle.getText(session.getFromId(), I18nEnum.Off));
            inlineKeyboardButton2.setCallbackData(StepsCenter.buildCallbackData(true, session, Command.Off, settings.getFileBasename()));

            InlineKeyboardButton inlineKeyboardButton3 = new InlineKeyboardButton();
            inlineKeyboardButton3.setText(I18nHandle.getText(session.getFromId(), I18nEnum.Test));
            inlineKeyboardButton3.setCallbackData(StepsCenter.buildCallbackData(true, session, Command.Test, settings.getFileBasename()));

            InlineKeyboardButton inlineKeyboardButton4 = new InlineKeyboardButton();
            inlineKeyboardButton4.setText(I18nHandle.getText(session.getFromId(), I18nEnum.Update));
            inlineKeyboardButton4.setCallbackData(StepsCenter.buildCallbackData(true, session, Command.Update, settings.getFileBasename()));

            InlineKeyboardButton inlineKeyboardButton5 = new InlineKeyboardButton();
            inlineKeyboardButton5.setText(I18nHandle.getText(session.getFromId(), I18nEnum.Delete));
            inlineKeyboardButton5.setCallbackData(StepsCenter.buildCallbackData(true, session, Command.Delete, settings.getFileBasename()));

            MessageHandle.sendInlineKeyboard(session.getChatId(), getMonitorData(session, settings), inlineKeyboardButton, inlineKeyboardButton2, inlineKeyboardButton3, inlineKeyboardButton4, inlineKeyboardButton5);
        } else {
            MessageHandle.sendMessage(session.getChatId(), session.getReplyToMessageId(), I18nHandle.getText(session.getFromId(), I18nEnum.NotFound), false);
        }
    }

    private static void showMonitorListHandle(StepsChatSession session) {
        List<MonitorConfigSettings> monitorConfigSettingsList = Config.readMonitorConfigList();
        if (monitorConfigSettingsList.size() > 0) {
            StringBuilder builder = new StringBuilder();
            ArrayList<InlineKeyboardButton> inlineKeyboardButtons = new ArrayList<>();

            for (MonitorConfigSettings settings : monitorConfigSettingsList) {
                builder.append(I18nHandle.getText(session.getFromId(), I18nEnum.MonitorList, settings.getFileBasename(), settings.getOn()));
                builder.append("\n\n");

                InlineKeyboardButton button = new InlineKeyboardButton();
                button.setText(settings.getFileBasename());
                button.setCallbackData(StepsCenter.buildCallbackData(true, session, Command.Get, settings.getFileBasename()));
                inlineKeyboardButtons.add(button);
            }

            MessageHandle.sendInlineKeyboard(session.getChatId(), builder.toString(), inlineKeyboardButtons);
        } else {
            MessageHandle.sendMessage(session.getChatId(), session.getReplyToMessageId(), I18nHandle.getText(session.getFromId(), I18nEnum.NothingHere), false);
        }
    }

    private static void rssMessageHandle(MonitorConfigSettings configSettings, boolean isTest) {
        try {
            Boolean on = configSettings.getOn();
            String fileBasename = configSettings.getFileBasename();
            if ((null != on && on) || isTest) {
                SyndFeed feed = RssUtil.getFeed(RequestProxyConfig.create(), configSettings.getUrl());
                if (null == feed) {
                    if (isTest) MessageHandle.sendMessage(GlobalConfig.getBotAdminId(), I18nHandle.getText(GlobalConfig.getBotAdminId(), I18nEnum.CreateMonitor5), false);
                    return;
                }
                List<SyndEntry> entries = feed.getEntries();
                if (null == entries || entries.isEmpty()) {
                    if (isTest) MessageHandle.sendMessage(GlobalConfig.getBotAdminId(), I18nHandle.getText(GlobalConfig.getBotAdminId(), I18nEnum.NothingAtAll), false);
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
                                MessageHandle.sendMessage(s, text, configSettings.getWebPagePreview(), configSettings.getNotification());
                            }

                            if (GlobalConfig.getChatIdArray().length > 0) {
                                SentRecordTableRepository.insert(linkMd5, fileBasename);
                                Config.saveMonitorConfig(configSettings);
                            }
                        } else {
                            MessageHandle.sendMessage(GlobalConfig.getBotAdminId(), text, configSettings.getWebPagePreview(), configSettings.getNotification());
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
                    String telegraphHtml = replaceTelegraphHtml(entry.getLink(), entry.getTitle());

                    TelegraphUtil.SaveResponse response = TelegraphUtil.save(RequestProxyConfig.create(), entry.getTitle(), author, html, telegraphHtml);
                    if (response.isOk()) {
                        s = StringUtils.replace(s, "${telegraph}", response.getUrl());
                    } else {
                        s = StringUtils.replace(s, "${telegraph}", "");
                    }
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

    private static String getMonitorData(StepsChatSession session, MonitorConfigSettings monitorConfigSettings) {
        String chatIdArrayStr = "";
        String[] chatIdArray = monitorConfigSettings.getChatIdArray();
        if (ArrayUtils.isEmpty(chatIdArray)) {
            chatIdArrayStr = StringUtils.join(GlobalConfig.getChatIdArray(), " ");
        } else {
            chatIdArrayStr = StringUtils.join(chatIdArray, " ");
        }

        StringBuilder builder = new StringBuilder();

        builder.append(String.format("%s: %s\n", I18nHandle.getText(session.getFromId(), I18nEnum.ConfigDisplayFileBasename), monitorConfigSettings.getFileBasename()));
        builder.append(String.format("%s: %s\n", I18nHandle.getText(session.getFromId(), I18nEnum.ConfigDisplayOn), monitorConfigSettings.getOn()));
        builder.append(String.format("%s: %s\n", I18nHandle.getText(session.getFromId(), I18nEnum.ConfigDisplayWebPagePreview), monitorConfigSettings.getWebPagePreview()));
        builder.append(String.format("%s: %s\n", I18nHandle.getText(session.getFromId(), I18nEnum.ConfigDisplayNotification), monitorConfigSettings.getNotification()));
        builder.append(String.format("%s: %s\n", I18nHandle.getText(session.getFromId(), I18nEnum.ConfigDisplayZeroDelay), monitorConfigSettings.getZeroDelay()));
        builder.append(String.format("%s: %s\n", I18nHandle.getText(session.getFromId(), I18nEnum.ConfigDisplayUrl), monitorConfigSettings.getUrl()));
        builder.append(String.format("%s: %s\n", I18nHandle.getText(session.getFromId(), I18nEnum.ConfigDisplayChatIdArray), chatIdArrayStr));
        builder.append(String.format("%s: \n%s", I18nHandle.getText(session.getFromId(), I18nEnum.ConfigDisplayTemplate), monitorConfigSettings.getTemplate()));

        return builder.toString();
    }

}
