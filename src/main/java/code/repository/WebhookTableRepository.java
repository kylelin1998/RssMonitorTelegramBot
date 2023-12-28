package code.repository;

import code.config.Config;
import code.eneity.WebhookTableEntity;
import code.repository.base.TableRepository;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class WebhookTableRepository extends TableRepository<WebhookTableEntity> {

    public WebhookTableRepository() {
        super(Config.DBPath, false);
    }

    public WebhookTableEntity selectOne(String chatId) {
        WebhookTableEntity where = new WebhookTableEntity();
        where.setChatId(chatId);
        return super.selectOne(where);
    }

    public List<WebhookTableEntity> selectList(String chatId) {
        WebhookTableEntity where = new WebhookTableEntity();
        where.setChatId(chatId);
        return super.selectList(where);
    }

    public synchronized Boolean save(WebhookTableEntity entity) {
        WebhookTableEntity rsp = selectOne(entity.getChatId());
        if (null == rsp) {
            return super.insert(entity);
        } else {
            WebhookTableEntity where = new WebhookTableEntity();
            where.setChatId(entity.getChatId());
            return super.update(entity, where);
        }
    }

    public Boolean delete(String chatId) {
        WebhookTableEntity where = new WebhookTableEntity();
        where.setChatId(chatId);
        return super.delete(where);
    }

}
