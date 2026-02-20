package com.MariaBermudez.utilidades;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.LocalDateTime;

public class RegistradorLog {
    public static synchronized void escribir(int id, String estado, long tiempo, int reintento) {
        try (PrintWriter out = new PrintWriter(new FileWriter("simulacion.log", true))) {
            out.printf("[%s] Muestra: %d | Estado: %s | Tiempo: %dms | Reintentos: %d%n",
                    LocalDateTime.now(), id, estado, tiempo, reintento);
        } catch (Exception e) {
            System.err.println("Error en log: " + e.getMessage());
        }
    }
}