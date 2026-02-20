package com.MariaBermudez;

import com.MariaBermudez.configuracion.CargadorConfig;
import com.MariaBermudez.modelos.Ajustes; // Verifica si es 'modelo' o 'modelos'
import com.MariaBermudez.motores.MotorPool;
import com.MariaBermudez.motores.MotorRaw;
import com.MariaBermudez.motores.Simulador;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        System.out.println("=== SIMULADOR DE CONEXIONES DB - JAVA 25 ===");

        // 1. se carga la configuracion desde el JSON
        Ajustes misAjustes = CargadorConfig.cargar();
        System.out.println("Configuración cargada: " + misAjustes.url());

        // 2. Hilo de Freno Manual, sirve para esccribir el stop
        Thread.ofPlatform().start(() -> {
            System.out.println("[INFO] Escribe 'STOP' y presiona Enter para detener la simulación.");
            Scanner sc = new Scanner(System.in);
            while (true) {
                if (sc.next().equalsIgnoreCase("STOP")) {
                    System.err.println("\n!!! SIMULACIÓN INTERRUMPIDA POR EL USUARIO !!!");
                    System.exit(0);
                }
            }
        });

        // 3. Simulacion RAW
        System.out.println("\n>>> INICIANDO FASE 1: MODO RAW (CONEXIÓN DIRECTA)");
        MotorRaw motorRaw = new MotorRaw(misAjustes);
        Simulador simRaw = new Simulador(misAjustes, motorRaw);
        simRaw.ejecutar();
        motorRaw.cerrar();

        System.out.println("\nEsperando 2 segundos para liberar recursos de la red...");
        try { Thread.sleep(2000); } catch (InterruptedException _) {}

        // 4. Simulacion POOLED
        System.out.println("\n>>> INICIANDO FASE 2: MODO POOLED (HIKARICP)");
        MotorPool motorPool = new MotorPool(misAjustes);
        Simulador simPool = new Simulador(misAjustes, motorPool);
        simPool.ejecutar();
        motorPool.cerrar();

        System.out.println("\n=== PROCESO TERMINADO CON ÉXITO ===");
        System.out.println("Analiza los resultados detallados en el archivo: 'simulacion.log'");
    }
}