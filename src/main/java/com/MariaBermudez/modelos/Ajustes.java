package com.MariaBermudez.modelos; // O el paquete que estés usando

public record Ajustes(
        String url,
        String usuario,
        String clave,
        String query,
        int muestras,
        int reintentos,
        int salto,
        int limitePool
) {}