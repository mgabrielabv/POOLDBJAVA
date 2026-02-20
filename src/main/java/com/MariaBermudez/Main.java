package com.MariaBermudez;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
import com.MariaBermudez.configuracion.CargadorConfig;
import com.MariaBermudez.modelos.Ajustes;

public class Main {
    public static void main(String[] args) {
        System.out.println("Iniciando simulador en Java 25...");

        Ajustes misAjustes = CargadorConfig.cargar();
        System.out.println("Configuración cargada: " + misAjustes.url());
        System.out.println("Consulta a usar: " + misAjustes.query());


    }
}