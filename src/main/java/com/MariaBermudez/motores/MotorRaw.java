package com.MariaBermudez.motores;

import com.MariaBermudez.modelos.Ajustes;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MotorRaw implements EstrategiaConexion {
    private final Ajustes ajustes;

    public MotorRaw(Ajustes ajustes) {
        this.ajustes = ajustes;
    }

    @Override
    public Connection obtenerConexion() throws SQLException {
        return DriverManager.getConnection(ajustes.url(), ajustes.usuario(), ajustes.clave());
    }

    @Override
    public void cerrar() {
        // No requiere cerrar nada global
    }
}