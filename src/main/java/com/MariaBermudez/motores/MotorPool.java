package com.MariaBermudez.motores;

import com.MariaBermudez.modelos.Ajustes;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class MotorPool implements EstrategiaConexion {
    private static volatile MotorPool instancia;
    private final HikariDataSource dataSource;

    // Constructor singleton
    private MotorPool(Ajustes ajustes) {
        HikariConfig config = new HikariConfig();
        config.setDriverClassName("org.postgresql.Driver");
        config.setJdbcUrl(ajustes.url());
        config.setUsername(ajustes.usuario());
        config.setPassword(ajustes.clave());
        config.setMaximumPoolSize(ajustes.limitePool());
        config.setConnectionTimeout(10000);

        this.dataSource = new HikariDataSource(config);
    }

    public static MotorPool getInstance(Ajustes ajustes) {
        if (instancia == null) {
            synchronized (MotorPool.class) {
                if (instancia == null) {
                    instancia = new MotorPool(ajustes);
                }
            }
        }
        return instancia;
    }

    @Override
    public Connection obtenerConexion() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void cerrar() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
        synchronized (MotorPool.class) {
            instancia = null;
        }
    }
}