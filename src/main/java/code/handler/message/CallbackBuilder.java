package code.handler.message;

import code.handler.Command;
import code.handler.steps.StepsChatSession;
import code.util.ExceptionUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public class CallbackBuilder {

    @Data
    public static class CallbackData {
        private boolean init;
        private String id;
        private Command command;
        private String text;
    }

    public static String buildCallbackData(boolean init, StepsChatSession session, Command command, String text) {
        StringBuilder builder = new StringBuilder();
        builder.append("f[" + session.getSessionId() + "]");
        builder.append(command.getCmd());
        builder.append(" ");
        builder.append(init);
        builder.append(" ");
        builder.append(text);
        return builder.toString();
    }
    public static CallbackData parseCallbackData(String callbackData) {
        try {
            CallbackData data = new CallbackData();
            data.setId(StringUtils.substringBetween(callbackData, "f[", "]"));

            String s = StringUtils.replace(callbackData, "f[" + data.getId() + "]", "");
            String[] arguments = s.split(" ");

            data.setCommand(Command.toCmd(arguments[0]));
            data.setInit(Boolean.valueOf(arguments[1]));
            data.setText(arguments.length > 2 ? arguments[2] : null);

            return data;
        } catch (Exception e) {
            log.error(ExceptionUtil.getStackTraceWithCustomInfoToStr(e));
        }
        return null;
    }

}
