package com.MariaBermudez.configuracion;

import com.MariaBermudez.modelos.Ajustes;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;

public class CargadorConfig {
    public static Ajustes cargar() {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream is = CargadorConfig.class.getClassLoader().getResourceAsStream("config.json")) {
            return mapper.readValue(is, Ajustes.class);
        } catch (Exception e) {
            throw new RuntimeException("Error leyendo el JSON: " + e.getMessage());
        }
    }
}