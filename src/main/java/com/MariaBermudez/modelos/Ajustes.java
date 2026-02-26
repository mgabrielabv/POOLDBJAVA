package com.MariaBermudez.modelos; // O el paquete que estes usando

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