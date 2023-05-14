package code.config;

import code.eneity.MonitorTableEntity;
import code.eneity.YesOrNoEnum;
import code.util.ExceptionUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONReader;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;

import java.util.stream.Collectors;

import static code.Main.*;

@Slf4j
public class Config {

    public static final String CurrentDir = System.getProperty("user.dir") + "/config";
    public static final String MonitorDir = System.getProperty("user.dir") + "/config/monitor";

    public static final String SettingsPath = CurrentDir + "/config.json";

    public static final String DBPath = CurrentDir + "/db.db";

    public static String TelegraphHtml;

    public final static class MetaData {
        public final static String CurrentVersion = "1.0.30";
        public final static String GitOwner = "kylelin1998";
        public final static String GitRepo = "RssMonitorTelegramBot";
        public final static String ProcessName = "rss-monitor-for-telegram-universal.jar";
        public final static String JarName = "rss-monitor-for-telegram-universal.jar";
    }

    static {
        InputStream is = Config.class.getResourceAsStream("telegraph.html");

        TelegraphHtml = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))
                .lines()
                .collect(Collectors.joining("\n"));

        File file = new File(CurrentDir);
        if (!file.exists()) {
            file.mkdirs();
        }
        File mf = new File(MonitorDir);
        if (!mf.exists()) {
            mf.mkdirs();
        }
    }

    public static ConfigSettings readConfig() {
        try {
            File file = new File(SettingsPath);
            boolean exists = file.exists();
            if (exists) {
                String text = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
                ConfigSettings configSettings = JSON.parseObject(text, ConfigSettings.class, JSONReader.Feature.SupportSmartMatch);
                Boolean debug = configSettings.getDebug();
                if (null == debug) {
                    configSettings.setDebug(false);
                }
                if (null == configSettings.getPermissionChatIdArray()) {
                    configSettings.setPermissionChatIdArray(new String[] {});
                }
                return configSettings;
            }
        } catch (IOException e) {
            log.error(ExceptionUtil.getStackTraceWithCustomInfoToStr(e));
        }
        return new ConfigSettings();
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
                    MonitorTableRepository.insert(monitorTableEntity);

                    monitorFile.delete();
                }
            } catch (IOException e) {
                log.error(ExceptionUtil.getStackTraceWithCustomInfoToStr(e));
            }
            return true;
        });
    }

//    public synchronized static boolean saveConfig(ConfigSettings configSettings) {
//        try {
//            File file = new File(SettingsPath);
//            FileUtils.write(file, JSON.toJSONString(configSettings, JSONWriter.Feature.PrettyFormat), StandardCharsets.UTF_8);
//            return true;
//        } catch (IOException e) {
//            log.error(ExceptionUtil.getStackTraceWithCustomInfoToStr(e));
//        }
//        return false;
//    }

}
