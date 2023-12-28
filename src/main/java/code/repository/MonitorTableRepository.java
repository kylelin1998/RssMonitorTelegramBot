package code.repository;

import code.config.Config;
import code.eneity.MonitorTableEntity;
import code.repository.base.TableRepository;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class MonitorTableRepository extends TableRepository<MonitorTableEntity> {

    public MonitorTableRepository() {
        super(Config.DBPath, true);
    }

    public MonitorTableEntity selectOne(String id, String chatId) {
        MonitorTableEntity where = new MonitorTableEntity();
        where.setId(id);
        where.setChatId(chatId);
        return super.selectOne(where);
    }

    public Integer selectCountByName(String chatId, String name) {
        MonitorTableEntity where = new MonitorTableEntity();
        where.setName(name);
        where.setChatId(chatId);
        return super.selectCount(where);
    }
    public Boolean delete(String id) {
        MonitorTableEntity where = new MonitorTableEntity();
        where.setId(id);
        return super.delete(where);
    }

    public Boolean update(MonitorTableEntity entity) {
        MonitorTableEntity where = new MonitorTableEntity();
        where.setId(entity.getId());
        return super.update(entity, where);
    }

    public List<MonitorTableEntity> selectList(String chatId) {
        MonitorTableEntity where = new MonitorTableEntity();
        where.setChatId(chatId);
        return super.selectList(where);
    }

}
