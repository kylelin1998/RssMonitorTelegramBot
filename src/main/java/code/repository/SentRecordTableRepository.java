package code.repository;

import code.config.Config;
import code.config.TableEnum;
import code.repository.mapper.TableRepository;
import code.util.ExceptionUtil;
import lombok.extern.slf4j.Slf4j;

import java.sql.ResultSet;

@Slf4j
public class SentRecordTableRepository extends TableRepository {

    public SentRecordTableRepository() {
        super(Config.DBPath, TableEnum.SentRecordTable.getName());
    }

    @Override
    public String getCreateTableSql() {
        return String.format("create table if not exists %s (id varchar(255), monitor_name varchar(88), create_time timestamp)", super.getTableName());
    }

    public Boolean selectExistByMonitorName(String monitorName) {
        return super.exist("monitor_name", monitorName);
    }
    public Boolean selectExistByIdAndMonitorName(String id, String monitorName) {
        try {
            Integer total = (int) execute((statement) -> {
                String sql = String.format("select count(*) as total from %s where id = '%s' and monitor_name = '%s'", super.getTableName(), id, monitorName);
                ResultSet query = statement.executeQuery(sql);
                return query.getInt("total");
            });
            if (null == total) return null;
            return total > 0;
        } catch (Exception e) {
            log.error(ExceptionUtil.getStackTraceWithCustomInfoToStr(e));
        }
        return null;
    }

    public boolean insert(String id, String monitorName) {
        String sql = String.format("insert into %s values('%s', '%s', %s)", super.getTableName(), id, monitorName, System.currentTimeMillis());
        try {
            execute((statement) -> {
                statement.executeUpdate(sql);
                return null;
            });
            return true;
        } catch (Exception e) {
            log.error(ExceptionUtil.getStackTraceWithCustomInfoToStr(e));
            return false;
        }
    }

}
