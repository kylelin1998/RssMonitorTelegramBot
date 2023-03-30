package code.config;

import code.util.ExceptionUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
public class Config {

    public static final String CurrentDir = System.getProperty("user.dir") + "/config";
    public static final String MonitorDir = System.getProperty("user.dir") + "/config/monitor";

    public static final String SettingsPath = CurrentDir + "/config.json";

    public static final String DBPath = CurrentDir + "/db.db";

    public static String TelegraphHtml;

    private static Map<String, MonitorConfigSettings> monitorConfigSettingsCaches = new ConcurrentHashMap<>();

    public final static class MetaData {
        public final static String CurrentVersion = "1.0.20";
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

        readMonitorConfigList();
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

    private static List<MonitorConfigSettings> readMonitorConfigListByCaches() {
        if (monitorConfigSettingsCaches.size() == 0) {
            return null;
        }
        return new ArrayList<>(monitorConfigSettingsCaches.values());
    }

    public synchronized static List<MonitorConfigSettings> readMonitorConfigList() {
        List<MonitorConfigSettings> monitorConfigSettings = readMonitorConfigListByCaches();
        if (null != monitorConfigSettings) return monitorConfigSettings;

        List<MonitorConfigSettings> list = new ArrayList<>();
        File file = new File(MonitorDir);
        file.list((File dir, String name) -> {
            File monitorFile = new File(dir, name);
            try {
                if (monitorFile.isFile()) {
                    String text = FileUtils.readFileToString(monitorFile, StandardCharsets.UTF_8);
                    MonitorConfigSettings configSettings = JSON.parseObject(text, MonitorConfigSettings.class, JSONReader.Feature.SupportSmartMatch);
                    configSettings.setFilename(name);
                    if (null == configSettings.getNotification()) {
                        configSettings.setNotification(true);
                    }
                    if (null == configSettings.getZeroDelay()) {
                        configSettings.setZeroDelay(false);
                    }
                    list.add(configSettings);
                    monitorConfigSettingsCaches.put(configSettings.getFileBasename(), configSettings);
                }
            } catch (IOException e) {
                log.error(ExceptionUtil.getStackTraceWithCustomInfoToStr(e));
            }
            return true;
        });
        return list;
    }

    public static boolean hasMonitorConfig(String fileBasename) {
        if (monitorConfigSettingsCaches.size() > 0) {
            return monitorConfigSettingsCaches.containsKey(fileBasename);
        }

        return null != readMonitorConfig(fileBasename);
    }

    public synchronized static MonitorConfigSettings readMonitorConfig(String fileBasename) {
        try {
            MonitorConfigSettings monitorConfigSettings = monitorConfigSettingsCaches.get(fileBasename);
            if (null == monitorConfigSettings) {
                List<MonitorConfigSettings> monitorConfigSettingsList = readMonitorConfigList();
                for (MonitorConfigSettings configSettings : monitorConfigSettingsList) {
                    if (configSettings.getFileBasename().equals(fileBasename)) {
                        return configSettings;
                    }
                }
            }
            return monitorConfigSettings;
        } catch (Exception e) {
            log.error(ExceptionUtil.getStackTraceWithCustomInfoToStr(e));
        }
        return null;
    }

    public synchronized static boolean saveMonitorConfig(MonitorConfigSettings configSettings) {
        try {
            File file = new File(MonitorDir + "/" + configSettings.getFilename());
            FileUtils.write(file, JSON.toJSONString(configSettings, JSONWriter.Feature.PrettyFormat), StandardCharsets.UTF_8);
            monitorConfigSettingsCaches.put(configSettings.getFileBasename(), configSettings);
            return true;
        } catch (IOException e) {
            log.error(ExceptionUtil.getStackTraceWithCustomInfoToStr(e));
        }
        return false;
    }

    public synchronized static void deleteMonitorConfig(MonitorConfigSettings configSettings) {
        File file = new File(MonitorDir + "/" + configSettings.getFilename());
        if (file.exists()) {
            file.delete();
        }
        monitorConfigSettingsCaches.remove(configSettings.getFileBasename());
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
