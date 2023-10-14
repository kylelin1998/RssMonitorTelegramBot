package code.handler.message;

import code.eneity.PageEntity;
import code.handler.Command;
import code.handler.steps.StepsChatSession;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;

public class InlineKeyboardButtonListBuilder {
    private List<List<InlineKeyboardButton>> keyboard;
    private InlineKeyboardButtonListBuilder() {}

    public static InlineKeyboardButtonListBuilder create() {
        InlineKeyboardButtonListBuilder builder = new InlineKeyboardButtonListBuilder();
        builder.keyboard = new ArrayList<>();
        return builder;
    }

    public InlineKeyboardButtonListBuilder add(List<InlineKeyboardButton> inlineKeyboardButtonList) {
        this.keyboard.add(inlineKeyboardButtonList);
        return this;
    }

    public InlineKeyboardButtonListBuilder pagination(PageEntity entity, StepsChatSession session, Command command) {
        int count = entity.getCount();
        if (count > 1) {
            InlineKeyboardButtonBuilder builder = InlineKeyboardButtonBuilder
                    .create();
            if (entity.isHasPrev()) {
                builder.add("⬅️", CallbackBuilder.buildCallbackData(true, session, command, "" + (entity.getCurrent() - 1)));
            }
            if (entity.isHasNext()) {
                builder.add("➡️", CallbackBuilder.buildCallbackData(true, session, command, "" + (entity.getCurrent() + 1)));
            }
            this.keyboard.add(builder.build());
        }
        return this;
    }

    public List<List<InlineKeyboardButton>> build() {
        return keyboard;
    }

}
