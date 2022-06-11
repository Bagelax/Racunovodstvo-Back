package raf.si.racunovodstvo.user.config;

import com.wix.mysql.EmbeddedMysql;
import com.wix.mysql.config.MysqldConfig;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;

import java.io.IOException;
import java.net.ServerSocket;

import javax.sql.DataSource;

import static com.wix.mysql.EmbeddedMysql.anEmbeddedMysql;
import static com.wix.mysql.config.Charset.UTF8;
import static com.wix.mysql.config.MysqldConfig.aMysqldConfig;
import static com.wix.mysql.distribution.Version.v8_latest;
import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_SINGLETON;

@TestConfiguration
public class EmbeddedMysqlServerConfig {

    private static final String DATABASE_USERNAME = "root_test";
    private static final String DATABASE_PASSWORD = "root_test";
    private static final String DATABASE_NAME = "test_db";

    @Bean
    @Scope(value = SCOPE_SINGLETON)
    public MysqldConfig config() throws IOException {
        return aMysqldConfig(v8_latest).withCharset(UTF8).withPort(getRandomFreePort())
                                         .withUser(DATABASE_USERNAME, DATABASE_PASSWORD).build();
    }

    @Bean(name = "embeddedMysql", destroyMethod = "stop")
    public EmbeddedMysql embeddedMysql(MysqldConfig config) {
        return anEmbeddedMysql(config)
            .addSchema(DATABASE_NAME)
            .start();
    }

    @Bean
    @Primary
    @DependsOn("embeddedMysql") // Because mySQLServer must be present before jdbc/hibernate
    public DataSource dataSource(MysqldConfig mysqldConfig) {
        return DataSourceBuilder.create().username(mysqldConfig.getUsername()).password(mysqldConfig.getPassword())
                                .url("jdbc:mysql://localhost:" + mysqldConfig.getPort() + "/" + DATABASE_NAME
                                         + "?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC&allowMultiQueries=true")
                                .driverClassName("com.mysql.cj.jdbc.Driver").build();
    }

    private int getRandomFreePort() throws IOException {
        // obtain random port for Mysql
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            return serverSocket.getLocalPort();
        }
    }
}
