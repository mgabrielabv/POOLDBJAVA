package com.MariaBermudez.GUI;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class PanelGrafico extends JPanel {
    private final List<Long> tiempos = new ArrayList<>();

    public PanelGrafico() { setOpaque(false); }

    public void agregarDato(long tiempo) {
        if (tiempos.size() > 40) tiempos.remove(0);
        tiempos.add(tiempo);
        repaint();
    }

    public void limpiar() { tiempos.clear(); repaint(); }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int p = 40;
        int w = getWidth() - (p * 2);
        int h = getHeight() - (p * 2);

        g2.setColor(Estilos.SUBTEXT);
        g2.drawLine(p, p, p, getHeight() - p);
        g2.drawLine(p, getHeight() - p, getWidth() - p, getHeight() - p);

        if (tiempos.isEmpty()) return;
        long max = tiempos.stream().max(Long::compare).orElse(1L);
        int barW = Math.max(w / 40, 5);

        for (int i = 0; i < tiempos.size(); i++) {
            int barH = (int) ((tiempos.get(i) * h) / max);
            int x = p + 5 + (i * (barW + 2));
            int y = getHeight() - p - barH;
            g2.setColor(new Color(0, 191, 255, 100));
            g2.fillRect(x, y, barW, barH);
            g2.setColor(Estilos.ACCENT);
            g2.drawRect(x, y, barW, barH);
        }
    }
}