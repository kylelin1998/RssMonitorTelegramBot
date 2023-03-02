package code.handler.steps;

import java.util.List;

public interface StepHandleApi {

    boolean execute(String chatId, String text, List<String> list);

}
