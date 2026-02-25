package com.MariaBermudez.motores;

import com.MariaBermudez.modelos.Ajustes;
import com.MariaBermudez.utilidades.RegistradorLog;
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
        int totalConsultas = ajustes.muestras();
        CountDownLatch pestillo = new CountDownLatch(1);
        AtomicInteger exitos = new AtomicInteger(0);
        AtomicInteger fallos = new AtomicInteger(0);

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 1; i <= totalConsultas; i++) {
                int id = i;
                executor.submit(() -> {
                    try {
                        pestillo.await();
                        realizarTarea(id, exitos, fallos);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            }

            long inicio = System.currentTimeMillis();
            pestillo.countDown(); // Arrancan todos los hilos
            executor.close();    // Espera a que todos terminen
            long fin = System.currentTimeMillis();

            // --- CÁLCULOS DE EFICACIA ---
            double porcentajeExito = (exitos.get() * 100.0) / totalConsultas;
            boolean esEficaz = porcentajeExito >= 80.0;

            System.out.println("\n--- RESULTADOS DE ESTA RÁFAGA ---");
            System.out.println("Consultas totales: " + totalConsultas);
            System.out.println("Éxitos: " + exitos.get() + " | Fallos: " + fallos.get());
            System.out.printf("Porcentaje de Éxito: %.2f%%\n", porcentajeExito);
            System.out.println("¿Es eficaz? (min 80%): " + (esEficaz ? "Si" : "NO "));
            System.out.println("Tiempo total: " + (fin - inicio) + "ms");
        }
    }

    private void realizarTarea(int id, AtomicInteger exitos, AtomicInteger fallos) {
        try (var conn = estrategia.obtenerConexion();
             var stmt = conn.prepareStatement(ajustes.query())) {
            stmt.executeQuery();
            exitos.incrementAndGet();
            RegistradorLog.escribir(id, "EXITO", 0, 0);
        } catch (Exception e) {
            fallos.incrementAndGet();
            RegistradorLog.escribir(id, "FALLO: " + e.getMessage(), 0, 0);
        }
    }
}