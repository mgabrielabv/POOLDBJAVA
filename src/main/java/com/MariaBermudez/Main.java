package com.MariaBermudez;

import com.MariaBermudez.GUI.VentanaGrafica;
import com.formdev.flatlaf.FlatDarkLaf;
import javax.swing.UIManager;

public class Main {
    public static void main(String[] args) {

        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (Exception e) {
            System.err.println("No se pudo cargar el estilo");
        }

        javax.swing.SwingUtilities.invokeLater(() -> {
            new VentanaGrafica().setVisible(true);
        });
    }
}