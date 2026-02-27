package com.MariaBermudez.utilidades;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class RegistradorLog {
    private static final String ARCHIVO = "simulador.log";
    private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    // Nuevo metodo: registra id, estado, latencia, reintentos, tipo de motor (POOL/RAW) y la query ejecutada
    public static synchronized void escribir(int id, String estado, long latencia, int reintentos, String motor, String query) {
        try (FileWriter fw = new FileWriter(ARCHIVO, true);
             PrintWriter pw = new PrintWriter(fw)) {

            String linea = String.format("%s | MOTOR: %s | QUERY: %s | ID: %d | ESTADO: %s | LATENCIA: %dms | REINTENTOS: %d",
                    LocalDateTime.now().format(dtf), motor == null ? "UNKNOWN" : motor, query == null ? "" : query.replaceAll("\n", " "), id, estado, latencia, reintentos);
            pw.println(linea);
        } catch (Exception e) {
            // No usar printStackTrace directamente; escribir un mensaje simple en stderr
            System.err.println("ERROR al escribir en el log: " + e.getMessage());
        }
    }

    // Sobrecarga para compatibilidad en caso de llamadas antiguas
    public static void escribir(int id, String estado, long latencia, int reintentos) {
        escribir(id, estado, latencia, reintentos, "UNKNOWN", "");
    }
}