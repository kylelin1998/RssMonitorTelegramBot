package code.handler.steps;

import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class StepsHandler {

    private String id = UUID.randomUUID().toString();
    private boolean debug;
    private StepHandleApi initStep;
    private StepHandleApi[] stepHandleApis;

    private static Map<String, List<String>> message = new ConcurrentHashMap<>();

    public static StepsHandler build(boolean debug, StepHandleApi initStep, StepHandleApi... steps) {
        StepsHandler handler = new StepsHandler();

        handler.debug = debug;
        handler.initStep = initStep;
        handler.stepHandleApis = steps;

        return handler;
    }

    public void init(String chatId, String text) {
        List<String> list = new ArrayList<>();
        boolean execute = initStep.execute(chatId, text, list);
        if (execute) {
            message.remove(chatId);
            list.add(text);
            message.put(chatId, list);
            if (debug) {
                log.info("Steps init, id: {}, chat id: {}, text: {}, list: {}", this.id, chatId, text, JSON.toJSONString(list));
            }
        }
    }

    public void step(String chatId, String text) {
        if (!message.containsKey(chatId)) {
            return;
        }

        String key = this.id + chatId;
        synchronized (key.intern()) {
            List<String> list = message.get(chatId);
            boolean execute = this.stepHandleApis[list.size() - 1].execute(chatId, text, list);
            if (execute) {
                list.add(text);
            }
            if (debug) {
                log.info("Step, id: {}, chat id: {}, text: {}, list: {}", this.id, chatId, text, JSON.toJSONString(list));
            }
            if ((list.size() - 1) >= this.stepHandleApis.length) {
                if (debug) {
                    log.info("Steps finish, id: {}, chat id: {}, text: {}, list: {}", this.id, chatId, text, JSON.toJSONString(list));
                }
                exit(chatId);
            }
        }
    }

    public void exit(String chatId) {
        message.remove(chatId);
    }

}
