package com.MariaBermudez.motores;

import java.sql.Connection;
import java.sql.SQLException;

public interface EstrategiaConexion {
    /**
     * Este metodo será implementado por MotorRaw y MotorPool
     */
    Connection obtenerConexion() throws SQLException;

    /**
     * Para limpiar recursos al finalizar
     */
    void cerrar();
}