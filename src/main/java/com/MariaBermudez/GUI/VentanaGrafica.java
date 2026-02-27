package com.MariaBermudez.GUI;

import com.MariaBermudez.configuracion.CargadorConfig;
import com.MariaBermudez.modelos.Ajustes;
import com.MariaBermudez.motores.*;
import com.MariaBermudez.utilidades.RegistradorLog;
import java.awt.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.*;
import javax.swing.border.*;

public class VentanaGrafica extends JFrame {
    private JProgressBar barra;
    private JLabel lblExitos, lblFallos, lblPorcExito, lblPorcFallo, lblReloj, lblReintentos;
    private JLabel lblReintPool, lblReintRaw;
    private JLabel lblPoolStats, lblRawStats;
    private DefaultListModel<String> logModel;
    private JTextField txtQ;
    private JButton btnIn, btnSt;

    private PanelGrafico panelGrafica;

    // Metricas para el analisis y comparacion entre motores

     private final AtomicBoolean running = new AtomicBoolean(false);

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

        // PANEL DE CONTROL (IZQUIERDA)
        JPanel lateral = new JPanel(new BorderLayout(15, 15));
        lateral.setOpaque(false); lateral.setPreferredSize(new Dimension(380, 0));

        JPanel cardCtrl = new JPanel(new GridLayout(0, 1, 8, 8));
        cardCtrl.setBackground(Estilos.CARD);
        cardCtrl.setBorder(new CompoundBorder(new LineBorder(new Color(45,45,60)), new EmptyBorder(15,15,15,15)));

        txtQ = new JTextField("20000");

        btnIn = new JButton("INICIAR");
        btnIn.setBackground(Estilos.ACCENT);
        btnSt = new JButton("FRENAR");
        btnSt.setBackground(Estilos.RED); btnSt.setForeground(Color.WHITE);
        btnSt.setEnabled(false);

        cardCtrl.add(new JLabel("CANTIDAD TOTAL:") {{ setForeground(Estilos.SUBTEXT); }});
        cardCtrl.add(txtQ);
        cardCtrl.add(btnIn); cardCtrl.add(btnSt);

        logModel = new DefaultListModel<>();
        JScrollPane scroll = new JScrollPane(new JList<>(logModel));
        scroll.setBorder(new TitledBorder(new LineBorder(new Color(45,45,60)), "REGISTRO"));
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

        JPanel inferior = new JPanel(new GridLayout(6, 1, 5, 5));
        inferior.setOpaque(false);
        lblReloj = new JLabel("TIEMPO TOTAL: 0 ms"); lblReloj.setForeground(Color.WHITE);
        lblReintentos = new JLabel("PROMEDIO DE REINTENTOS TOTALES: 0.00"); lblReintentos.setForeground(Color.YELLOW);
        lblReintPool = new JLabel("PROMEDIO DE REINTENTOS POOL (avg): 0.00"); lblReintPool.setForeground(Color.CYAN);
        lblReintRaw = new JLabel("PROMEDIO DE REINTENTOS RAW (avg): 0.00"); lblReintRaw.setForeground(Color.MAGENTA);
        lblPoolStats = new JLabel("POOL: Exitosas:0 Fallidas:0 (0.0%)"); lblPoolStats.setForeground(Color.GREEN);
        lblRawStats = new JLabel("RAW: Exitosas:0 Fallidas:0 (0.0%)"); lblRawStats.setForeground(Estilos.ACCENT);
        barra = new JProgressBar(0, 100); barra.setPreferredSize(new Dimension(0, 45));
        barra.setStringPainted(true);

        inferior.add(lblReloj); inferior.add(lblReintentos); inferior.add(lblReintPool); inferior.add(lblReintRaw); inferior.add(lblPoolStats); inferior.add(lblRawStats);
        JPanel barraHolder = new JPanel(new BorderLayout()); barraHolder.setOpaque(false); barraHolder.add(barra, BorderLayout.CENTER);
        centro.add(barraHolder, BorderLayout.SOUTH);
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
        Thread.ofVirtual().start(() -> {
            Ajustes config = CargadorConfig.cargar();
            // Usar la query del config.json
            final String uiQuery = config.query();
            EstrategiaConexion motorPool = MotorPool.getInstance(config);
            EstrategiaConexion motorRaw = new MotorRaw(config);

            int[] rampa = {100, 500, 1000, 5000, metaTotal};
            long inicioGlobal = System.currentTimeMillis();

            class Resultado {
                final String nombre;
                final AtomicInteger exitos = new AtomicInteger(0);
                final AtomicInteger fallos = new AtomicInteger(0);
                final AtomicInteger procesados = new AtomicInteger(0);
                final AtomicInteger reintentos = new AtomicInteger(0);
                long tiempoTotal = 0;
                Resultado(String n) { this.nombre = n; }
            }

            Resultado resPool = new Resultado("POOL");
            Resultado resRaw = new Resultado("RAW");

            CountDownLatch inicioCombinado = new CountDownLatch(1);
            CountDownLatch finCombinado = new CountDownLatch(2);

            // Trabajador que ejecuta la misma rampa para un motor y completa su Resultado
            java.util.function.BiConsumer<EstrategiaConexion, Resultado> worker = (motor, res) -> {
                try {
                    for (int hilosRafaga : rampa) {
                        if (!running.get() || hilosRafaga > metaTotal) break;
                        int porLanzar = hilosRafaga - res.procesados.get();
                        if (porLanzar <= 0) continue;

                        CountDownLatch pestillo = new CountDownLatch(1);
                        SwingUtilities.invokeLater(() -> logModel.insertElementAt("--- " + res.nombre + " FRAGMENTO: " + hilosRafaga + " HILOS ---", 0));

                        for (int i = 0; i < porLanzar; i++) {
                            int id = res.procesados.get() + i + 1;
                            Thread.ofVirtual().start(() -> {
                                try {
                                    pestillo.await();
                                    boolean ok = false;
                                    int r = 0;
                                    while (r < config.reintentos() && !ok && running.get()) {
                                        try (var conn = motor.obtenerConexion()) {
                                            try (var stmt = conn.createStatement()) {
                                                long t0q = System.currentTimeMillis();
                                                if (uiQuery.toUpperCase().startsWith("SELECT")) {
                                                    int rowCount = 0;
                                                    try (var rs = stmt.executeQuery(uiQuery)) {
                                                        while (rs.next()) { rowCount++; }
                                                    }
                                                    long latq = System.currentTimeMillis() - t0q;
                                                    ok = true; res.exitos.incrementAndGet();
                                                    RegistradorLog.escribir(id, "EXITO_ROWS:" + rowCount, latq, r, res.nombre, uiQuery);
                                                } else {
                                                    int updated = stmt.executeUpdate(uiQuery);
                                                    long latq = System.currentTimeMillis() - t0q;
                                                    ok = true; res.exitos.incrementAndGet();
                                                    RegistradorLog.escribir(id, "EXITO_UPDATED:" + updated, latq, r, res.nombre, uiQuery);
                                                }
                                            }
                                         } catch (Exception ex) { r++; res.reintentos.incrementAndGet(); }
                                    }
                                    if (!ok) {
                                        res.fallos.incrementAndGet();
                                        RegistradorLog.escribir(id, "FALLO", 0, r, res.nombre, uiQuery);
                                    }
                                     res.procesados.incrementAndGet();

                                    // Actualizar la UI combinada usando los resultados de ambos motores
                                    int procPool = resPool.procesados.get();
                                    int procRaw = resRaw.procesados.get();
                                    int okTotal = resPool.exitos.get() + resRaw.exitos.get();
                                    int failTotal = resPool.fallos.get() + resRaw.fallos.get();
                                    int procTotal = procPool + procRaw;
                                    int totalMeta = metaTotal * 2; // ambos motores usan la misma meta
                                    int reintTotal = resPool.reintentos.get() + resRaw.reintentos.get();

                                    actualizarUI(procTotal, totalMeta, okTotal, failTotal, inicioGlobal, reintTotal);

                                    // Actualizar las gráficas redondas (donut) en el EDT
                                    SwingUtilities.invokeLater(() -> {
                                        panelGrafica.actualizarContadores(resPool.exitos.get(), resPool.fallos.get(), resRaw.exitos.get(), resRaw.fallos.get());
                                        // Promedio de reintentos por motor
                                        double avgPool = (resPool.procesados.get() == 0) ? 0.0 : ((double) resPool.reintentos.get() / resPool.procesados.get());
                                        double avgRaw = (resRaw.procesados.get() == 0) ? 0.0 : ((double) resRaw.reintentos.get() / resRaw.procesados.get());
                                        lblReintPool.setText(String.format("REINTENTOS POOL (avg): %.2f", avgPool));
                                        lblReintRaw.setText(String.format("REINTENTOS RAW (avg): %.2f", avgRaw));
                                        // Estadísticas por motor
                                        lblPoolStats.setText(String.format("POOL: E:%d F:%d (%.1f%%)", resPool.exitos.get(), resPool.fallos.get(),
                                                (resPool.procesados.get() == 0) ? 0.0 : (resPool.exitos.get() * 100.0 / resPool.procesados.get())));
                                        lblRawStats.setText(String.format("RAW: E:%d F:%d (%.1f%%)", resRaw.exitos.get(), resRaw.fallos.get(),
                                                (resRaw.procesados.get() == 0) ? 0.0 : (resRaw.exitos.get() * 100.0 / resRaw.procesados.get())));
                                    });

                                } catch (Exception e) { Thread.currentThread().interrupt(); }
                            });
                        }

                        long inicioR = System.currentTimeMillis();
                        pestillo.countDown(); // DISPARO SIMULTANEO

                        while (res.procesados.get() < hilosRafaga && running.get()) { Thread.onSpinWait(); }
                        long dur = System.currentTimeMillis() - inicioR;
                        res.tiempoTotal += dur;

                        try { Thread.sleep(800); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                    }
                } finally {
                    try { motor.cerrar(); } catch (Exception e) { RegistradorLog.escribir(-1, "ERROR_CERRAR: " + e.getMessage(), 0, 0, motor == null ? "UNKNOWN" : motor.getClass().getSimpleName(), ""); }
                     finCombinado.countDown();
                 }
             };

            // Iniciar ambos trabajadores
            Thread.ofVirtual().start(() -> { try { inicioCombinado.await(); worker.accept(motorPool, resPool); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } });
            Thread.ofVirtual().start(() -> { try { inicioCombinado.await(); worker.accept(motorRaw, resRaw); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } });

            // Lanzar ambos al mismo tiempo
            inicioCombinado.countDown();

            try { finCombinado.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

            // Ambos terminaron; calcular eficacias
            double efPool = (resPool.procesados.get() == 0) ? 0.0 : (resPool.exitos.get() * 100.0) / resPool.procesados.get();
            double efRaw = (resRaw.procesados.get() == 0) ? 0.0 : (resRaw.exitos.get() * 100.0) / resRaw.procesados.get();

            SwingUtilities.invokeLater(() -> {
                btnIn.setEnabled(true); btnSt.setEnabled(false);
                mostrarComparacion(efPool, efRaw);
            });
        });
    }


    // Metodo nuevo para mostrar la comparacion directa después de la ejecución paralela
    private void mostrarComparacion(double efPool, double efRaw) {
        String c = "--- COMPARACION FINAL ---\n\n";
        c += String.format("Eficacia Motor Pooled: %.2f%%\n", efPool);
        c += String.format("Eficacia Motor Raw:    %.2f%%\n\n", efRaw);
        if (efPool > efRaw) c += "CONCLUSION: El Motor Pooled es mas eficiente.";
        else if (efRaw > efPool) c += "CONCLUSION: El Motor Raw es mas eficiente.";
        else c += "CONCLUSION: Empate tecnico, eficacias iguales.";

        JOptionPane.showMessageDialog(this, c);
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
