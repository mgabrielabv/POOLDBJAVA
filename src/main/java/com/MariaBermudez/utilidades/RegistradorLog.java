package com.MariaBermudez.utilidades;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class RegistradorLog {
    private static final String ARCHIVO = "simulador.log";
    private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    public static synchronized void escribir(int id, String estado, long latencia, int reintentos) {
        try (FileWriter fw = new FileWriter(ARCHIVO, true);
             PrintWriter pw = new PrintWriter(fw)) {

            String linea = String.format("%s | ID: %d | ESTADO: %s | LATENCIA: %dms | REINTENTOS: %d",
                    LocalDateTime.now().format(dtf), id, estado, latencia, reintentos);
            pw.println(linea);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}