package com.MariaBermudez.GUI;

import com.MariaBermudez.configuracion.CargadorConfig;
import com.MariaBermudez.modelos.Ajustes;
import com.MariaBermudez.motores.MotorPool;
import java.awt.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.*;

public class VentanaGrafica extends JFrame {
    private JProgressBar barra;
    private JLabel lblPorc, lblTiempo;
    private DefaultListModel<String> modelLista;
    private JList<String> listaQueries;
    private JTextField txtQ;
    private JButton btnIn, btnSt;
    private AtomicBoolean corriendo = new AtomicBoolean(false);

    public VentanaGrafica() {
        setTitle("Dashboard Premium - Maria Bermúdez");
        setSize(1000, 650);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(Estilos.FONDO);
        setLayout(new BorderLayout(15, 15));


        JPanel pIzq = new JPanel(new BorderLayout(10, 10));
        pIzq.setPreferredSize(new Dimension(300, 0));
        pIzq.setBackground(Estilos.PANEL);
        pIzq.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        txtQ = new JTextField("10000");
        btnIn = new JButton("LANZAR TEST");
        btnIn.setBackground(Estilos.ACCENT); btnIn.setForeground(Color.WHITE);
        btnSt = new JButton("STOP");

        JPanel pInputs = new JPanel(new GridLayout(4, 1, 5, 5));
        pInputs.setOpaque(false);
        pInputs.add(new JLabel("Nº QUERIES:", SwingConstants.CENTER));
        pInputs.add(txtQ); pInputs.add(btnIn); pInputs.add(btnSt);
        pIzq.add(pInputs, BorderLayout.NORTH);

        modelLista = new DefaultListModel<>();
        listaQueries = new JList<>(modelLista);
        listaQueries.setBackground(new Color(25, 15, 25));
        listaQueries.setForeground(Estilos.SUCCESS);
        listaQueries.setFont(Estilos.MINI);
        pIzq.add(new JScrollPane(listaQueries), BorderLayout.CENTER);

        add(pIzq, BorderLayout.WEST);

        // --- PANEL CENTRAL: Métricas y Gráficas ---
        JPanel pCen = new JPanel(new GridLayout(2, 1, 15, 15));
        pCen.setOpaque(false);

        // Arriba: Porcentaje Grande
        JPanel pMetrica = new JPanel(new BorderLayout());
        pMetrica.setBackground(Estilos.PANEL);
        lblPorc = new JLabel("0%", SwingConstants.CENTER);
        lblPorc.setFont(Estilos.NUMERO);
        lblPorc.setForeground(Estilos.ACCENT);
        pMetrica.add(new JLabel("EFICACIA ACTUAL", SwingConstants.CENTER), BorderLayout.NORTH);
        pMetrica.add(lblPorc, BorderLayout.CENTER);

        // Abajo: Barra y Tiempo
        JPanel pStats = new JPanel(new GridLayout(2, 1, 10, 10));
        pStats.setOpaque(false);

        barra = new JProgressBar(0, 100);
        barra.setPreferredSize(new Dimension(0, 40));
        barra.setForeground(Estilos.ACCENT);

        lblTiempo = new JLabel("Tiempo Promedio: 0ms", SwingConstants.CENTER);
        lblTiempo.setFont(Estilos.TITULO); lblTiempo.setForeground(Color.WHITE);

        pStats.add(barra);
        pStats.add(lblTiempo);

        pCen.add(pMetrica);
        pCen.add(pStats);
        add(pCen, BorderLayout.CENTER);

        // Eventos
        btnIn.addActionListener(e -> ejecutar());
        btnSt.addActionListener(e -> corriendo.set(false));
    }

    private void ejecutar() {
        corriendo.set(true); modelLista.clear();
        btnIn.setEnabled(false); btnSt.setEnabled(true);
        int total = Integer.parseInt(txtQ.getText());
        AtomicInteger e = new AtomicInteger(0);

        Thread.ofVirtual().start(() -> {
            Ajustes conf = CargadorConfig.cargar();
            Ajustes actual = new Ajustes(conf.url(), conf.usuario(), conf.clave(), conf.query(), total, 0, 0, 100);
            MotorPool motor = new MotorPool(actual);
            long inicioGlobal = System.currentTimeMillis();

            for (int i = 0; i < total && corriendo.get(); i++) {
                final int id = i + 1;
                Thread.ofVirtual().start(() -> {
                    long t1 = System.currentTimeMillis();
                    try (var c = motor.obtenerConexion()) {
                        c.createStatement().execute(actual.query());
                        int valExito = e.incrementAndGet();
                        long t2 = System.currentTimeMillis() - t1;

                        if (id % 100 == 0 || total < 100) { // Actualizar lista cada 100 para no trabar
                            SwingUtilities.invokeLater(() -> {
                                modelLista.insertElementAt("ID: " + id + " | OK | " + t2 + "ms", 0);
                                if (modelLista.size() > 50) modelLista.remove(50); // Mantener solo las últimas 50
                            });
                        }
                    } catch (Exception ex) {}

                    actualizarUI(id, total, e.get(), inicioGlobal);
                });
                if (i % 500 == 0) try { Thread.sleep(20); } catch (Exception ex) {}
            }
            motor.cerrar();
            SwingUtilities.invokeLater(() -> { btnIn.setEnabled(true); btnSt.setEnabled(false); });
        });
    }

    private void actualizarUI(int actual, int total, int exitos, long inicio) {
        SwingUtilities.invokeLater(() -> {
            double porc = (exitos * 100.0) / actual;
            long tiempoTranscurrido = System.currentTimeMillis() - inicio;
            double promedio = exitos > 0 ? (double) tiempoTranscurrido / exitos : 0;

            lblPorc.setText(String.format("%.1f%%", porc));
            barra.setValue((actual * 100) / total);
            lblTiempo.setText(String.format("Promedio por Query: %.2f ms", promedio));

            if (porc < 80) lblPorc.setForeground(Color.RED);
            else lblPorc.setForeground(Estilos.ACCENT);
        });
    }

    public static void main(String[] args) {
        new VentanaGrafica().setVisible(true);
    }
}