package code.config;

import code.eneity.MonitorTableEntity;
import code.eneity.YesOrNoEnum;
import code.util.ExceptionUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;

import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import static code.Main.*;

@Slf4j
public class Config {

    public static final String CurrentDir = System.getProperty("user.dir") + File.separator + "config";
    public static final String MonitorDir = CurrentDir + File.separator + "monitor";

    public static final String SettingsPath = CurrentDir + File.separator + "config.json";

    public static final String DBPath = CurrentDir + File.separator + "db.db";

    public final static String TempDir = CurrentDir + File.separator + "temp";

    public static String TelegraphHtml = new BufferedReader(new InputStreamReader(Config.class.getResourceAsStream("telegraph.html"), StandardCharsets.UTF_8))
            .lines()
            .collect(Collectors.joining("\n"));
    public static String WebhookJson = new BufferedReader(new InputStreamReader(Config.class.getResourceAsStream("webhook.json"), StandardCharsets.UTF_8))
            .lines()
            .collect(Collectors.joining("\n"));

    private static ReentrantReadWriteLock reentrantReadWriteLock = new ReentrantReadWriteLock();

    public final static class MetaData {
        public final static String CurrentVersion = "1.1.5";
        public final static String GitOwner = "kylelin1998";
        public final static String GitRepo = "RssMonitorTelegramBot";
        public final static String ProcessName = "rss-monitor-for-telegram-universal.jar";
        public final static String JarName = "rss-monitor-for-telegram-universal.jar";
    }

    static {
        mkdirs(CurrentDir, MonitorDir, TempDir);

        new Thread(() -> {
            while (true) {
                try {
                    File file = new File(TempDir);
                    ArrayList<File> files = new ArrayList<>();
                    file.list((File dir, String name) -> {
                        File file1 = new File(dir, name);
                        try {
                            BasicFileAttributes attributes = Files.readAttributes(file1.toPath(), BasicFileAttributes.class);
                            FileTime fileTime = attributes.creationTime();
                            long millis = System.currentTimeMillis() - fileTime.toMillis();
                            if (millis > 3600000) {
                                files.add(file1);
                            }
                        } catch (IOException e) {
                            log.error(ExceptionUtil.getStackTraceWithCustomInfoToStr(e));
                        }

                        return true;
                    });

                    for (File df : files) {
                        df.delete();
                    }
                } catch (Exception e) {
                    log.error(ExceptionUtil.getStackTraceWithCustomInfoToStr(e));
                }
                try {
                    TimeUnit.MINUTES.sleep(30);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }

    private static void mkdirs(String... dirs) {
        for (String dir : dirs) {
            File file = new File(dir);
            if (!file.exists()) {
                file.mkdirs();
            }
        }
    }

    public static ConfigSettings initConfig() {
        File file = new File(SettingsPath);
        if (!file.exists()) {
            Properties properties = System.getProperties();

            ConfigSettings configSettings = new ConfigSettings();
            configSettings.setBotAdminId(properties.getProperty("botAdminId", ""));
            configSettings.setBotName(properties.getProperty("botName", ""));
            configSettings.setBotToken(properties.getProperty("botToken", ""));
            configSettings.setOnProxy(Boolean.valueOf(properties.getProperty("botProxy", "false")));
            configSettings.setProxyHost(properties.getProperty("botProxyHost", "127.0.0.1"));
            configSettings.setProxyPort(Integer.valueOf(properties.getProperty("botProxyPort", "7890")));

            saveConfig(handle(configSettings));
        }
        return readConfig();
    }

    private static ConfigSettings handle(ConfigSettings configSettings) {
        Boolean debug = configSettings.getDebug();
        if (null == debug) {
            configSettings.setDebug(false);
        }
        String[] permissionChatIdArray = configSettings.getPermissionChatIdArray();
        if (null == permissionChatIdArray) {
            configSettings.setPermissionChatIdArray(new String[]{ configSettings.getBotAdminId() });
        }
        String[] chatIdArray = configSettings.getChatIdArray();
        if (null == chatIdArray) {
            configSettings.setChatIdArray(new String[]{ configSettings.getBotAdminId() });
        }
        Integer intervalMinute = configSettings.getIntervalMinute();
        if (null == intervalMinute) {
            configSettings.setIntervalMinute(5);
        }
        Boolean hideCopyrightTips = configSettings.getHideCopyrightTips();
        if (null == hideCopyrightTips) {
            configSettings.setHideCopyrightTips(false);
        }
        Boolean verifySsl = configSettings.getVerifySsl();
        if (null == verifySsl) {
            configSettings.setVerifySsl(true);
        }
        List<String> excludeKeywords = configSettings.getExcludeKeywords();
        if (null == excludeKeywords) {
            configSettings.setExcludeKeywords(new ArrayList<>());
        }
        List<String> excludeKeywordsRegex = configSettings.getExcludeKeywordsRegex();
        if (null == excludeKeywordsRegex) {
            configSettings.setExcludeKeywordsRegex(new ArrayList<>());
        }
        List<String> includeKeywords = configSettings.getIncludeKeywords();
        if (null == includeKeywords) {
            configSettings.setIncludeKeywords(new ArrayList<>());
        }
        List<String> includeKeywordsRegex = configSettings.getIncludeKeywordsRegex();
        if (null == includeKeywordsRegex) {
            configSettings.setIncludeKeywordsRegex(new ArrayList<>());
        }
        return configSettings;
    }

    public static ConfigSettings readConfig() {
        ReentrantReadWriteLock.ReadLock readLock = reentrantReadWriteLock.readLock();
        readLock.lock();
        try {
            File file = new File(SettingsPath);
            boolean exists = file.exists();
            if (exists) {
                String text = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
                ConfigSettings configSettings = JSON.parseObject(text, ConfigSettings.class, JSONReader.Feature.SupportSmartMatch);
                return handle(configSettings);
            } else {
                log.warn("Settings file not found, " + SettingsPath);
            }
        } catch (IOException e) {
            log.error(ExceptionUtil.getStackTraceWithCustomInfoToStr(e), SettingsPath);
        } finally {
            readLock.unlock();
        }
        return null;
    }

    public static ConfigSettings verifyConfig(String configJson) {
        if (StringUtils.isBlank(configJson)) {
            return null;
        }
        ConfigSettings configSettings = null;
        try {
            configSettings = JSON.parseObject(configJson, ConfigSettings.class, JSONReader.Feature.SupportSmartMatch);
        } catch (JSONException e) {
        }
        if (null == configSettings) {
            return null;
        }
        for (Field field : configSettings.getClass().getDeclaredFields()) {
            ConfigField configField = field.getAnnotation(ConfigField.class);
            if (null == configField) {
                continue;
            }
            if (configField.isNotNull()) {
                try {
                    field.setAccessible(true);
                    Object o = field.get(configSettings);
                    if (null == o) {
                        return null;
                    }
                } catch (IllegalAccessException e) {
                    log.error(ExceptionUtil.getStackTraceWithCustomInfoToStr(e));
                    return null;
                }
            }
        }

        return configSettings;
    }

    public synchronized static void oldDataConvert() {
        File file = new File(MonitorDir);
        file.list((File dir, String name) -> {
            File monitorFile = new File(dir, name);
            try {
                if (monitorFile.isFile()) {
                    String text = FileUtils.readFileToString(monitorFile, StandardCharsets.UTF_8);
                    MonitorConfigSettings configSettings = JSON.parseObject(text, MonitorConfigSettings.class, JSONReader.Feature.SupportSmartMatch);
                    configSettings.setFilename(name);
                    configSettings.setFileBasename(StringUtils.removeEnd(name, ".json"));
                    if (null == configSettings.getNotification()) {
                        configSettings.setNotification(true);
                    }
                    if (null == configSettings.getZeroDelay()) {
                        configSettings.setZeroDelay(false);
                    }

                    MonitorTableEntity monitorTableEntity = new MonitorTableEntity();
                    monitorTableEntity.setId(Snowflake.nextIdToStr());
                    monitorTableEntity.setChatId(GlobalConfig.botAdminId);
                    monitorTableEntity.setEnable(YesOrNoEnum.toInt(configSettings.getOn()));
                    monitorTableEntity.setName(configSettings.getFileBasename());
                    monitorTableEntity.setNotification(YesOrNoEnum.toInt(configSettings.getNotification()));
                    monitorTableEntity.setUrl(configSettings.getUrl());
                    monitorTableEntity.setTemplate(configSettings.getTemplate());
                    monitorTableEntity.setZeroDelay(YesOrNoEnum.toInt(configSettings.getZeroDelay()));
                    monitorTableEntity.setCreateTime(System.currentTimeMillis());
                    monitorTableEntity.setChatIdArrayJson(JSON.toJSONString(configSettings.getChatIdArray()));
                    monitorTableEntity.setWebPagePreview(YesOrNoEnum.toInt(configSettings.getWebPagePreview()));
                    monitorTableEntity.setCaptureFlag(YesOrNoEnum.No.getNum());
                    MonitorTableRepository.insert(monitorTableEntity);

                    monitorFile.delete();
                }
            } catch (IOException e) {
                log.error(ExceptionUtil.getStackTraceWithCustomInfoToStr(e));
            }
            return true;
        });
    }

    public static boolean saveConfig(ConfigSettings configSettings) {
        ReentrantReadWriteLock.WriteLock writeLock = reentrantReadWriteLock.writeLock();
        writeLock.lock();
        try {
            File file = new File(SettingsPath);
            FileUtils.write(file, JSON.toJSONString(configSettings, JSONWriter.Feature.PrettyFormat), StandardCharsets.UTF_8);
            return true;
        } catch (IOException e) {
            log.error(ExceptionUtil.getStackTraceWithCustomInfoToStr(e));
        } finally {
            writeLock.unlock();
        }
        return false;
    }

}
