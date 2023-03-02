package code.handler.steps;

public interface StepErrorApi {

    void callback(Exception e, String chatId, Integer replyToMessageId);

}
