package com.MariaBermudez.motores;

import com.MariaBermudez.modelos.Ajustes;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class MotorPool implements EstrategiaConexion {
    private final HikariDataSource dataSource;

    public MotorPool(Ajustes ajustes) {
        try {
            HikariConfig config = new HikariConfig();

            // Forzamos el nombre del driver para evitar errores de conexion
            config.setDriverClassName("org.postgresql.Driver");

            config.setJdbcUrl(ajustes.url());
            config.setUsername(ajustes.usuario());
            config.setPassword(ajustes.clave());

            // Configuración de rendimiento del Pool
            config.setMaximumPoolSize(ajustes.limitePool());
            config.setConnectionTimeout(10000); // 10 seg de espera
            config.setPoolName("Pool-Maria-LaptopNueva");

            this.dataSource = new HikariDataSource(config);
            System.out.println("[SISTEMA] Pool de conexiones listo en esta máquina.");

        } catch (Exception e) {
            System.err.println("--- ERROR CRÍTICO AL INICIAR EL POOL ---");
            // marca si hay algun error en la confi
            e.printStackTrace();
            throw new RuntimeException("No se pudo iniciar el Pool: " + e.getMessage());
        }
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
    }
}