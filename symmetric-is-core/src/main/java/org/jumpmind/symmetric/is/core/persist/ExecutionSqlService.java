package org.jumpmind.symmetric.is.core.persist;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.jumpmind.db.model.Table;
import org.jumpmind.db.platform.IDatabasePlatform;
import org.jumpmind.db.sql.ISqlRowMapper;
import org.jumpmind.db.sql.ISqlTemplate;
import org.jumpmind.db.sql.Row;
import org.jumpmind.persist.IPersistenceManager;
import org.jumpmind.symmetric.is.core.model.Execution;
import org.jumpmind.symmetric.is.core.model.ExecutionStepLog;
import org.springframework.core.env.Environment;

public class ExecutionSqlService extends AbstractExecutionService implements IExecutionService {

    IDatabasePlatform databasePlatform;

    public ExecutionSqlService(IDatabasePlatform databasePlatform,
            IPersistenceManager persistenceManager, String tablePrefix, Environment env) {
        super(persistenceManager, tablePrefix, env);
        this.databasePlatform = databasePlatform;
    }

    public List<ExecutionStepLog> findExecutionStepLog(Set<String> executionStepIds) {
        ISqlTemplate template = databasePlatform.getSqlTemplate();
        StringBuilder inClause = new StringBuilder("(");
        int i = executionStepIds.size();
        for (String executionStepId : executionStepIds) {
            inClause.append("'").append(executionStepId).append("'");
            if (--i > 0) {
                inClause.append(",");
            }
        }
        inClause.append(")");
        return template.query(
                String.format("select id, execution_step_id, level, log_text, create_time "
                        + "from %1$s_execution_step_log " + "where execution_step_id in "
                        + inClause.toString() + " order by create_time", tablePrefix),
                new ISqlRowMapper<ExecutionStepLog>() {
                    public ExecutionStepLog mapRow(Row row) {
                        ExecutionStepLog e = new ExecutionStepLog();
                        e.setId(row.getString("id"));
                        e.setExecutionStepId(row.getString("execution_step_id"));
                        e.setLevel(row.getString("level"));
                        e.setLogText(row.getString("log_text"));
                        e.setCreateTime(row.getDateTime("create_time"));
                        return e;
                    }
                });
    }

    public List<Execution> findExecutions(Map<String, Object> params, int limit) {
        ISqlTemplate template = databasePlatform.getSqlTemplate();
        StringBuilder whereClause = new StringBuilder();
        int i = params.size();
        for (String columnName : params.keySet()) {
            whereClause.append(
                    StringUtils.join(StringUtils.splitByCharacterTypeCamelCase(columnName), "_"))
                    .append(" = ? ");
            if (--i > 0) {
                whereClause.append("and ");
            }
        }
        return template.query(String.format(
                "select id, agent_id, flow_id, deployment_id, deployment_name, agent_name, "
                + "host_name, flow_name, status, start_time, end_time "
                        + "from %1$s_execution " + "where " + whereClause
                        + "order by create_time desc limit " + limit, tablePrefix),
                new ISqlRowMapper<Execution>() {
                    public Execution mapRow(Row row) {
                        Execution e = new Execution();
                        e.setId(row.getString("id"));
                        e.setAgentId(row.getString("agent_id"));
                        e.setFlowId(row.getString("flow_id"));
                        e.setAgentName(row.getString("agent_name"));
                        e.setHostName(row.getString("host_name"));
                        e.setFlowName(row.getString("flow_name"));
                        e.setDeploymentId(row.getString("deployment_id"));
                        e.setDeploymentName(row.getString("deployment_name"));
                        e.setStatus(row.getString("status"));
                        e.setStartTime(row.getDateTime("start_time"));
                        e.setEndTime(row.getDateTime("end_time"));
                        return e;
                    }
                }, params.values().toArray());
    }

    @Override
    public void purgeExecutions(String status, int retentionTimeInMs) {
        Table table = databasePlatform
                .readTableFromDatabase(null, null, tableName(Execution.class));
        if (table != null) {
            Date purgeBefore = DateUtils.addMilliseconds(new Date(), -retentionTimeInMs);
            log.info("Purging executions with the status of {} before {}", status, purgeBefore);
            ISqlTemplate template = databasePlatform.getSqlTemplate();
            long count = template
                    .update(String
                            .format("delete from %1$s_execution_step_log where execution_step_id in "
                                    + "(select id from %1$s_execution_step where execution_id in "
                                    + "(select id from %1$s_execution where status=? and last_update_time <= ?))",
                                    tablePrefix), status, purgeBefore);
            count += template
                    .update(String
                            .format("delete from %1$s_execution_step where execution_id in "
                                    + "(select id from %1$s_execution where status=? and last_update_time <= ?)",
                                    tablePrefix), status, purgeBefore);
            count += template.update(String.format(
                    "delete from %1$s_execution where status=? and last_update_time <= ?",
                    tablePrefix), status, purgeBefore);
            log.info("Purged {} execution records with the status of {}", new Object[] { count,
                    status });
        } else {
            log.info("Could not run execution purge because table had not been created yet");
        }
    }

}
