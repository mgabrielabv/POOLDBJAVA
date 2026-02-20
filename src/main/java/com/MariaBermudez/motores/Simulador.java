package com.MariaBermudez.motores;

import com.MariaBermudez.modelos.Ajustes;
import com.MariaBermudez.utilidades.RegistradorLog;
import com.MariaBermudez.motores.EstrategiaConexion;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class Simulador {
    private final Ajustes ajustes;
    private final EstrategiaConexion estrategia;

    public Simulador(Ajustes ajustes, EstrategiaConexion estrategia) {
        this.ajustes = ajustes;
        this.estrategia = estrategia;
    }

    public void ejecutar() {
        CountDownLatch puertaDeSalida = new CountDownLatch(1);
        AtomicInteger exitosas = new AtomicInteger(0);
        AtomicInteger fallidas = new AtomicInteger(0);

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 1; i <= ajustes.muestras(); i++) {
                int idMuestra = i;
                executor.submit(() -> {
                    try {
                        puertaDeSalida.await(); // Esperan la señal
                        realizarPrueba(idMuestra, exitosas, fallidas);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            }

            long inicioGlobal = System.currentTimeMillis();
            puertaDeSalida.countDown(); // ¡Arrancan todos!

            executor.close();
            long finGlobal = System.currentTimeMillis();

            System.out.println("\n--- RESULTADOS ---");
            System.out.println("Tiempo total: " + (finGlobal - inicioGlobal) + "ms");
            System.out.println("Éxitos: " + exitosas.get());
            System.out.println("Fallos: " + fallidas.get());
        }
    }

    private void realizarPrueba(int id, AtomicInteger exitos, AtomicInteger fallos) {
        long inicioMuestra = System.currentTimeMillis();
        int intentosRealizados = 0;
        boolean logrado = false;

        while (intentosRealizados <= ajustes.reintentos() && !logrado) {
            try (Connection conn = estrategia.obtenerConexion();
                 PreparedStatement stmt = conn.prepareStatement(ajustes.query())) {

                stmt.executeQuery();
                logrado = true;
                exitos.incrementAndGet();
                RegistradorLog.escribir(id, "EXITOSA", System.currentTimeMillis() - inicioMuestra, intentosRealizados);

            } catch (SQLException e) {
                intentosRealizados++;
                if (intentosRealizados > ajustes.reintentos()) {
                    fallos.incrementAndGet();
                    RegistradorLog.escribir(id, "FALLIDA: " + e.getMessage(), System.currentTimeMillis() - inicioMuestra, intentosRealizados - 1);
                }
            }
        }
    }
}