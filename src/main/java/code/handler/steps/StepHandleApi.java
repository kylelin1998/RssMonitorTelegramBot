package code.handler.steps;

import java.util.List;

public interface StepHandleApi {

    boolean execute(String chatId, Integer replyToMessageId, String text, int index, List<String> list);

}
