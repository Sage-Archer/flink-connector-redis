package org.apache.flink.streaming.connectors.redis.table;

import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.redis.common.mapper.RedisCommand;
import org.apache.flink.table.api.EnvironmentSettings;
import org.apache.flink.table.api.TableResult;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.junit.Test;

import static org.apache.flink.streaming.connectors.redis.descriptor.RedisValidator.*;

/**
 * Created by jeff.zou on 2020/9/10.
 */
public class SQLTest {

    public static final String CLUSTERNODES = "10.11.80.147:7000,10.11.80.147:7001,10.11.80.147:8000,10.11.80.147:8001,10.11.80.147:9000,10.11.80.147:9001";
    public static final String PASSWORD = "********";

    @Test
    public void testNoPrimaryKeyInsertSQL() throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        EnvironmentSettings environmentSettings = EnvironmentSettings.newInstance().useBlinkPlanner().inStreamingMode().build();
        StreamTableEnvironment tEnv = StreamTableEnvironment.create(env, environmentSettings);

        String ddl = "create table sink_redis(username VARCHAR, passport VARCHAR) with ( 'connector'='redis', " +
                "'host'='10.11.80.147','port'='7001', 'redis-mode'='single','password'='"+PASSWORD+"','" +
                REDIS_COMMAND + "'='" + RedisCommand.SET + "')" ;

        tEnv.executeSql(ddl);
        String sql = " insert into sink_redis select * from (values ('test', 'test11'))";
        TableResult tableResult = tEnv.executeSql(sql);
        tableResult.getJobClient().get()
                .getJobExecutionResult()
                .get();
        System.out.println(sql);
    }


    @Test
    public void testSingleInsertHashClusterSQL() throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        EnvironmentSettings environmentSettings = EnvironmentSettings.newInstance().useBlinkPlanner().inStreamingMode().build();
        StreamTableEnvironment tEnv = StreamTableEnvironment.create(env, environmentSettings);
        env.setParallelism(1);

        String ddl = "create table sink_redis(username varchar, level varchar, age varchar) with ( 'connector'='redis', " +
                "'cluster-nodes'='" + CLUSTERNODES + "','redis-mode'='cluster', 'password'='"+PASSWORD+"','" +
                REDIS_COMMAND + "'='" + RedisCommand.HSET + "', 'maxIdle'='2', 'minIdle'='1'  )" ;

        tEnv.executeSql(ddl);
        String sql = " insert into sink_redis select * from (values ('3', '3', '18'))";
        TableResult tableResult = tEnv.executeSql(sql);
        tableResult.getJobClient().get()
                .getJobExecutionResult()
                .get();
        System.out.println(sql);
    }

    @Test
    public void testHgetSQL() throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        EnvironmentSettings environmentSettings = EnvironmentSettings.newInstance().useBlinkPlanner().inStreamingMode().build();
        StreamTableEnvironment tEnv = StreamTableEnvironment.create(env, environmentSettings);

        String dim = "create table dim_table(name varchar, level varchar, age varchar) with ( 'connector'='redis', " +
                "'cluster-nodes'='" + CLUSTERNODES + "','redis-mode'='cluster', 'password'='"+PASSWORD+ "','" +
                REDIS_COMMAND + "'='" + RedisCommand.HGET + "', 'maxIdle'='2', 'minIdle'='1', 'lookup.cache.max-rows'='10', 'lookup.cache.ttl'='10', 'lookup.max-retries'='3'  )" ;

        String source = "create table source_table(username varchar, level varchar, proctime as procTime()) " +
                "with ('connector'='datagen',  'rows-per-second'='1', " +
                "'fields.username.kind'='sequence',  'fields.username.start'='1',  'fields.username.end'='10'," +
                "'fields.level.kind'='sequence',  'fields.level.start'='1',  'fields.level.end'='10'" +
                ")";

        String sink = "create table sink_table(username varchar, level varchar,age varchar) with ('connector'='print')";

        tEnv.executeSql(source);
        tEnv.executeSql(dim);
        tEnv.executeSql(sink);

        String sql = " insert into sink_table " +
                " select s.username, s.level, concat_ws('_', d.name, d.age) from source_table s" +
                "  left join dim_table for system_time as of s.proctime as d " +
                " on d.name = s.username and d.level = s.level";
        TableResult tableResult = tEnv.executeSql(sql);
        tableResult.getJobClient().get()
                .getJobExecutionResult()
                .get();
        System.out.println(sql);
    }

    @Test
    public void testGetSQL() throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        EnvironmentSettings environmentSettings = EnvironmentSettings.newInstance().useBlinkPlanner().inStreamingMode().build();
        StreamTableEnvironment tEnv = StreamTableEnvironment.create(env, environmentSettings);

        String dim = "create table dim_table(name varchar, level varchar) with ( 'connector'='redis', " +
                "'cluster-nodes'='" + CLUSTERNODES + "','redis-mode'='cluster', 'password'='"+PASSWORD+ "','" +
                REDIS_COMMAND + "'='" + RedisCommand.GET + "', 'maxIdle'='2', 'minIdle'='1', 'lookup.cache.max-rows'='10', 'lookup.cache.ttl'='10', 'lookup.max-retries'='3'  )" ;

        String source = "create table source_table(username varchar, level varchar, proctime as procTime()) " +
                "with ('connector'='datagen',  'rows-per-second'='1', " +
                "'fields.username.kind'='sequence',  'fields.username.start'='11',  'fields.username.end'='20'," +
                "'fields.level.kind'='sequence',  'fields.level.start'='11',  'fields.level.end'='20'" +
                ")";

        String sink = "create table sink_table(username varchar, level varchar,age varchar) with ('connector'='print')";

        tEnv.executeSql(source);
        tEnv.executeSql(dim);
        tEnv.executeSql(sink);

        String sql = " insert into sink_table " +
                " select s.username, s.level, concat_ws('_', d.name, d.level) from source_table s" +
                "  left join dim_table for system_time as of s.proctime as d " +
                " on d.name = s.username";
        TableResult tableResult = tEnv.executeSql(sql);
        tableResult.getJobClient().get()
                .getJobExecutionResult()
                .get();
        System.out.println(sql);
    }
}