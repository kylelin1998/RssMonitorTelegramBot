package code.handler;

import code.config.ExecutorsConfig;
import code.handler.message.CallbackBuilder;
import code.handler.steps.StepsChatSession;
import code.handler.steps.StepsHandler;
import code.handler.steps.StepsRegisterCenter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static code.Main.GlobalConfig;

@Slf4j
public class StepsCenter {

    public static boolean cmdHandle(StepsChatSession session) {
        if (StringUtils.isNotBlank(session.getText()) && session.getText().startsWith("/")) {
            String s = StringUtils.remove(session.getText(), "/");
            String[] split = s.split(" ");
            if (split.length > 0) {
                String cmd = split[0];
                cmd = StringUtils.replace(cmd, "@" + GlobalConfig.getBotName(), "");
                if (Command.exist(cmd)) {
                    split[0] = cmd;
                    session.setText(Stream.of(split).skip(1).collect(Collectors.joining(" ")));
                    cmdHandle(
                            Command.toCmd(cmd),
                            false,
                            session,
                            null
                    );
                    return true;
                }
            }
        }
        return false;
    }

    public static void cmdHandle(CallbackBuilder.CallbackData callbackData, StepsChatSession stepsChatSession) {
        cmdHandle(callbackData.getCommand(), true, stepsChatSession, callbackData);
    }

    public static void cmdHandle(Command command, StepsChatSession stepsChatSession) {
        cmdHandle(command, false, stepsChatSession, null);
    }

    private static void cmdHandle(Command command, boolean isCall, StepsChatSession stepsChatSession, CallbackBuilder.CallbackData callbackData) {
        boolean permission = false;

        String botAdminId = GlobalConfig.getBotAdminId();
        if (botAdminId.equals(stepsChatSession.getChatId()) || botAdminId.equals(stepsChatSession.getFromId())) {
            permission = true;
        }
        for (String s : GlobalConfig.getPermissionChatIdArray()) {
            if (s.equals(stepsChatSession.getChatId()) || s.equals(stepsChatSession.getFromId())) {
                permission = true;
                break;
            }
        }

        if (!permission) {
            MessageHandle.sendMessage(stepsChatSession.getChatId(), stepsChatSession.getReplyToMessageId(), "你没有使用权限， 不过你可以自己搭建一个\nhttps://github.com/kylelin1998/RssMonitorTelegramBot", false);
            return;
        }

        if (null != callbackData){
            StepsHandler handler = StepsRegisterCenter.getRegister(command.getCmd());
            if (!callbackData.isInit() && !handler.hasInit(stepsChatSession)) {
                return;
            }
        }

        ExecutorsConfig.submit(() -> {
            StepsHandler handler = StepsRegisterCenter.getRegister(command.getCmd());
            if (null != handler.getInitStep() && (!handler.hasInit(stepsChatSession) || !isCall)) {
                handler.init(stepsChatSession);
            } else {
                handler.step(stepsChatSession);
            }
        });
    }

    public static void textHandle(StepsChatSession stepsChatSession) {
        StepsHandler handler = StepsRegisterCenter.getPriority(stepsChatSession);
        if (null == handler) {
            return;
        }
        ExecutorsConfig.submit(() -> {
            handler.step(stepsChatSession);
        });
    }

    public static void exit(StepsChatSession stepsChatSession) {
        Collection<StepsHandler> list = StepsRegisterCenter.getRegisterList();
        for (StepsHandler handler : list) {
            handler.exit(stepsChatSession);
        }
    }

}
