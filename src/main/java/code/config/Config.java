package code.config;

import code.util.ExceptionUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class Config {

    public static String CurrentDir = System.getProperty("user.dir") + "/config";
    public static String MonitorDir = System.getProperty("user.dir") + "/config/monitor";

    public static String SettingsPath = CurrentDir + "/config.json";

    public static String DBPath = CurrentDir + "/db.db";

    static {
        File file = new File(CurrentDir);
        if (!file.exists()) {
            file.mkdirs();
        }
        File mf = new File(MonitorDir);
        if (!mf.exists()) {
            mf.mkdirs();
        }
    }

    public synchronized static ConfigSettings readConfig() {
        try {
            File file = new File(SettingsPath);
            boolean exists = file.exists();
            if (exists) {
                String text = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
                ConfigSettings configSettings = JSON.parseObject(text, ConfigSettings.class, JSONReader.Feature.SupportSmartMatch);
                return configSettings;
            }
        } catch (IOException e) {
            log.error(ExceptionUtil.getStackTraceWithCustomInfoToStr(e));
        }
        return new ConfigSettings();
    }

    public synchronized static List<MonitorConfigSettings> readMonitorConfigList() {
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
                    list.add(configSettings);
                }
            } catch (IOException e) {
                log.error(ExceptionUtil.getStackTraceWithCustomInfoToStr(e));
            }
            return true;
        });
        return list;
    }

    public synchronized static MonitorConfigSettings readMonitorConfig(String fileBasename) {
        try {
            File file = new File(MonitorDir + "/" + fileBasename + ".json");
            if (!file.exists() || !file.isFile()) return null;

            String text = FileUtils.readFileToString(file, StandardCharsets.UTF_8);

            MonitorConfigSettings configSettings = JSON.parseObject(text, MonitorConfigSettings.class, JSONReader.Feature.SupportSmartMatch);
            configSettings.setFilename(fileBasename + ".json");
            if (null == configSettings.getNotification()) {
                configSettings.setNotification(true);
            }

            return configSettings;
        } catch (Exception e) {
            log.error(ExceptionUtil.getStackTraceWithCustomInfoToStr(e));
        }
        return null;
    }

    public synchronized static boolean saveMonitorConfig(MonitorConfigSettings configSettings) {
        try {
            File file = new File(MonitorDir + "/" + configSettings.getFilename());
            FileUtils.write(file, JSON.toJSONString(configSettings, JSONWriter.Feature.PrettyFormat), StandardCharsets.UTF_8);
            return true;
        } catch (IOException e) {
            log.error(ExceptionUtil.getStackTraceWithCustomInfoToStr(e));
        }
        return false;
    }

    public synchronized static boolean saveConfig(ConfigSettings configSettings) {
        try {
            File file = new File(SettingsPath);
            FileUtils.write(file, JSON.toJSONString(configSettings, JSONWriter.Feature.PrettyFormat), StandardCharsets.UTF_8);
            return true;
        } catch (IOException e) {
            log.error(ExceptionUtil.getStackTraceWithCustomInfoToStr(e));
        }
        return false;
    }

}
