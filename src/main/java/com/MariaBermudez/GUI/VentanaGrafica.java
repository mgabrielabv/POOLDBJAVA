package com.MariaBermudez.GUI;

import com.MariaBermudez.configuracion.CargadorConfig;
import com.MariaBermudez.modelos.Ajustes;
import com.MariaBermudez.motores.*;
import com.MariaBermudez.utilidades.RegistradorLog;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import javax.swing.*;
import javax.swing.border.*;

public class VentanaGrafica extends JFrame {
    private JProgressBar barra;
    private JLabel lblExitos, lblFallos, lblPorcExito, lblPorcFallo, lblReloj, lblReintentos;
    private DefaultListModel<String> logModel;
    private JTextField txtQ;
    private JButton btnIn, btnSt;
    private JComboBox<String> comboMotor, comboQueries;
    private PanelGrafico panelGrafica;

    // Metricas para el analisis y comparacion entre motores
    private double eficaciaPooledFinal = -1;
    private AtomicBoolean running = new AtomicBoolean(false);
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    public VentanaGrafica() {
        setupUI();
        setLocationRelativeTo(null);
    }

    private void setupUI() {
        setTitle("POOLED VS RAW");
        setSize(1300, 900);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        getContentPane().setBackground(Estilos.BG);
        setLayout(new BorderLayout(20, 20));
        ((JPanel)getContentPane()).setBorder(new EmptyBorder(20, 20, 20, 20));

        // --- PANEL DE CONTROL (IZQUIERDA) ---
        JPanel lateral = new JPanel(new BorderLayout(15, 15));
        lateral.setOpaque(false); lateral.setPreferredSize(new Dimension(380, 0));

        JPanel cardCtrl = new JPanel(new GridLayout(0, 1, 8, 8));
        cardCtrl.setBackground(Estilos.CARD);
        cardCtrl.setBorder(new CompoundBorder(new LineBorder(new Color(45,45,60)), new EmptyBorder(15,15,15,15)));

        comboMotor = new JComboBox<>(new String[]{"Motor Pooled (Eficiente)", "Motor Raw (Directo)"});
        comboQueries = new JComboBox<>(new String[]{"SELECT 1", "SELECT NOW()", "SELECT version()"});
        txtQ = new JTextField("20000");

        btnIn = new JButton("INICIAR RAMPA DE CARGA");
        btnIn.setBackground(Estilos.ACCENT);
        btnSt = new JButton("FRENAR SIMULACION");
        btnSt.setBackground(Estilos.RED); btnSt.setForeground(Color.WHITE);
        btnSt.setEnabled(false);

        cardCtrl.add(new JLabel("METODO DE CONEXION:") {{ setForeground(Estilos.SUBTEXT); }});
        cardCtrl.add(comboMotor);
        cardCtrl.add(new JLabel("QUERY:") {{ setForeground(Estilos.SUBTEXT); }});
        cardCtrl.add(comboQueries);
        cardCtrl.add(new JLabel("CANTIDAD TOTAL:") {{ setForeground(Estilos.SUBTEXT); }});
        cardCtrl.add(txtQ);
        cardCtrl.add(btnIn); cardCtrl.add(btnSt);

        logModel = new DefaultListModel<>();
        JScrollPane scroll = new JScrollPane(new JList<>(logModel));
        scroll.setBorder(new TitledBorder(new LineBorder(new Color(45,45,60)), "REGISTRO DE FRAGMENTOS"));
        lateral.add(cardCtrl, BorderLayout.NORTH); lateral.add(scroll, BorderLayout.CENTER);
        // --- DASHBOARD (CENTRO) ---
        JPanel centro = new JPanel(new BorderLayout(20, 20));
        centro.setOpaque(false);

        JPanel panelMetricas = new JPanel(new GridLayout(1, 4, 15, 15));
        panelMetricas.setOpaque(false); panelMetricas.setPreferredSize(new Dimension(0, 110));

        lblExitos = createMetricCard(panelMetricas, "EXITOSAS", Estilos.GREEN);
        lblFallos = createMetricCard(panelMetricas, "FALLIDAS", Estilos.RED);
        lblPorcExito = createMetricCard(panelMetricas, "% EXITO", Color.WHITE);
        lblPorcFallo = createMetricCard(panelMetricas, "% FALLO", Color.WHITE);

        panelGrafica = new PanelGrafico();
        JPanel cardGr = new JPanel(new BorderLayout()); cardGr.setBackground(Estilos.CARD);
        cardGr.setBorder(new LineBorder(new Color(45,45,60)));
        cardGr.add(panelGrafica);

        JPanel inferior = new JPanel(new GridLayout(3, 1, 5, 5));
        inferior.setOpaque(false);
        lblReloj = new JLabel("TIEMPO TOTAL: 0 ms"); lblReloj.setForeground(Color.WHITE);
        lblReintentos = new JLabel("PROMEDIO DE REINTENTOS: 0.00"); lblReintentos.setForeground(Color.YELLOW);
        barra = new JProgressBar(0, 100); barra.setPreferredSize(new Dimension(0, 45));
        barra.setStringPainted(true);

        inferior.add(lblReloj); inferior.add(lblReintentos); inferior.add(barra);
        centro.add(panelMetricas, BorderLayout.NORTH); centro.add(cardGr, BorderLayout.CENTER); centro.add(inferior, BorderLayout.SOUTH);

        add(lateral, BorderLayout.WEST); add(centro, BorderLayout.CENTER);

        btnIn.addActionListener(e -> ejecutarRampa());
        btnSt.addActionListener(e -> running.set(false));
    }

    private JLabel createMetricCard(JPanel p, String t, Color c) {
        JPanel card = new JPanel(new BorderLayout()); card.setBackground(Estilos.CARD); card.setBorder(new LineBorder(new Color(45,45,60)));
        JLabel title = new JLabel(t, SwingConstants.CENTER); title.setForeground(Estilos.SUBTEXT); title.setFont(new Font("Arial", Font.BOLD, 10));
        JLabel val = new JLabel("0", SwingConstants.CENTER); val.setForeground(c); val.setFont(Estilos.BIG_NUM);
        card.add(title, BorderLayout.NORTH); card.add(val, BorderLayout.CENTER);
        p.add(card); return val;
    }

    private void ejecutarRampa() {
        running.set(true); logModel.clear(); panelGrafica.limpiar();
        btnIn.setEnabled(false); btnSt.setEnabled(true);

        int metaTotal = Integer.parseInt(txtQ.getText());
        String sql = comboQueries.getSelectedItem().toString();

        Thread.ofVirtual().start(() -> {
            Ajustes config = CargadorConfig.cargar();
            EstrategiaConexion motor = (comboMotor.getSelectedIndex() == 0) ? new MotorPool(config) : new MotorRaw(config);

            AtomicInteger exitos = new AtomicInteger(0);
            AtomicInteger fallos = new AtomicInteger(0);
            AtomicInteger procesados = new AtomicInteger(0);
            AtomicInteger reintentos = new AtomicInteger(0);
            long inicio = System.currentTimeMillis();

            // Rampa de carga iterativa: aumentamos la cantidad de hilos por fragmento
            int[] rampa = {100, 500, 1000, 5000, metaTotal};

            for (int hilosRafaga : rampa) {
                if (!running.get() || hilosRafaga > metaTotal) break;
                int porLanzar = hilosRafaga - procesados.get();
                if (porLanzar <= 0) continue;

                CountDownLatch pestillo = new CountDownLatch(1);
                SwingUtilities.invokeLater(() -> logModel.insertElementAt("--- FRAGMENTO: " + hilosRafaga + " HILOS ---", 0));

                for (int i = 0; i < porLanzar; i++) {
                    int id = procesados.get() + i + 1;
                    Thread.ofVirtual().start(() -> {
                        try {
                            pestillo.await();
                            boolean ok = false;
                            int r = 0;
                            while (r < config.reintentos() && !ok && running.get()) {
                                try (var conn = motor.obtenerConexion()) {
                                    conn.createStatement().execute(sql);
                                    ok = true; exitos.incrementAndGet();
                                } catch (Exception ex) { r++; reintentos.incrementAndGet(); }
                            }
                            if (!ok) fallos.incrementAndGet();

                            RegistradorLog.escribir(id, ok ? "EXITO" : "FALLO", 0, r);
                            procesados.incrementAndGet();
                            actualizarUI(procesados.get(), metaTotal, exitos.get(), fallos.get(), inicio, reintentos.get());
                        } catch (Exception e) { Thread.currentThread().interrupt(); }
                    });
                }

                long inicioR = System.currentTimeMillis();
                pestillo.countDown(); // DISPARO SIMULTANEO

                while (procesados.get() < hilosRafaga && running.get()) { Thread.onSpinWait(); }
                panelGrafica.agregarDato(System.currentTimeMillis() - inicioR);

                try { Thread.sleep(800); } catch (InterruptedException e) {}
            }

            motor.cerrar();
            double efFinal = (exitos.get() * 100.0) / procesados.get();
            SwingUtilities.invokeLater(() -> {
                btnIn.setEnabled(true); btnSt.setEnabled(false);
                mostrarConclucion(efFinal);
            });
        });
    }

    private void mostrarConclucion(double ef) {
        if (comboMotor.getSelectedIndex() == 0) {
            eficaciaPooledFinal = ef;
            JOptionPane.showMessageDialog(this, "Pooled finalizado. Ahora prueba Raw para comparar.");
        } else {
            String c = "--- ANALISIS DE FRAGMENTACION ---\n\n";
            c += "Eficacia Pooled: " + (eficaciaPooledFinal == -1 ? "N/A" : eficaciaPooledFinal + "%") + "\n";
            c += "Eficacia Raw: " + ef + "%\n\n";
            c += (eficaciaPooledFinal > ef) ? "CONCLUSION: El Motor Pooled es mas efectivo." : "CONCLUSION: Resultados similares.";
            JOptionPane.showMessageDialog(this, c);
        }
    }

    private void actualizarUI(int p, int t, int ok, int fail, long s, int r) {
        if (p == 0) return;
        SwingUtilities.invokeLater(() -> {
            // Correccion: normalizamos porcentajes para mantenerlos entre 0 y 100
            double pOk = Math.min(100.0, (ok * 100.0) / p);
            double pFail = Math.min(100.0, (fail * 100.0) / p);

            lblExitos.setText(String.valueOf(ok));
            lblFallos.setText(String.valueOf(fail));
            lblPorcExito.setText(String.format("%.1f%%", pOk));
            lblPorcFallo.setText(String.format("%.1f%%", pFail));

            int porc = (p * 100) / t;
            barra.setValue(porc);
            barra.setString(porc + "% | " + p + " de " + t);

            lblReintentos.setText(String.format("PROMEDIO DE REINTENTOS: %.2f", (double)r/p));
            lblReloj.setText(String.format("TIEMPO TOTAL: %d ms", System.currentTimeMillis() - s));
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new VentanaGrafica().setVisible(true));
    }
}