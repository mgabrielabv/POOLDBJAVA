package com.MariaBermudez.GUI;
import javax.swing.*;
import java.awt.*;

public class PanelGrafico extends JPanel {
    // Contadores para cada motor
    private int poolExitos = 0;
    private int poolFallos = 0;
    private int rawExitos = 0;
    private int rawFallos = 0;

    public PanelGrafico() { setOpaque(false); }

    @SuppressWarnings("unused")
    // Actualiza los contadores de ambos motores y repinta
    public synchronized void actualizarContadores(int pEx, int pFa, int rEx, int rFa) {
        this.poolExitos = pEx;
        this.poolFallos = pFa;
        this.rawExitos = rEx;
        this.rawFallos = rFa;
        repaint();
    }

    // Resetea los contadores
    public synchronized void limpiar() { poolExitos = poolFallos = rawExitos = rawFallos = 0; repaint(); }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        int size = Math.min(w / 2 - 40, h - 80);
        int x1 = 20;
        int y = (h - size) / 2;
        int x2 = w / 2 + 20;


        int poolTotal = poolExitos + poolFallos;
        int rawTotal = rawExitos + rawFallos;
        float poolPct = (poolTotal == 0) ? 0f : (poolExitos * 100f) / poolTotal;
        float rawPct = (rawTotal == 0) ? 0f : (rawExitos * 100f) / rawTotal;

        // Dibujar fondo y títulos
        g2.setColor(Estilos.SUBTEXT);
        g2.setFont(Estilos.LBL);
        g2.drawString("POOL", x1 + size/2 - 20, y - 10);
        g2.drawString("RAW", x2 + size/2 - 15, y - 10);

        // Dibujar donut para Pool
        drawDonut(g2, x1, y, size, poolExitos, poolFallos, Estilos.GREEN, Estilos.RED, poolPct);

        // Dibujar donut para Raw
        drawDonut(g2, x2, y, size, rawExitos, rawFallos, Estilos.ACCENT, Estilos.RED, rawPct);

        // Leyendas y contadores
        g2.setFont(Estilos.MONO);
        g2.setColor(Estilos.TEXT);
        String poolText = String.format("E: %d  F: %d  (%.1f%%)", poolExitos, poolFallos, poolPct);
        String rawText = String.format("E: %d  F: %d  (%.1f%%)", rawExitos, rawFallos, rawPct);
        g2.drawString(poolText, x1 + 10, y + size + 25);
        g2.drawString(rawText, x2 + 10, y + size + 25);
    }

    //Grafico con porcion de exito y fallo
    private void drawDonut(Graphics2D g2, int x, int y, int size, int exitos, int fallos, Color cEx, Color cFa, float pct) {
        int start = 90;
        int total = exitos + fallos;
        int angleEx = (total == 0) ? 0 : Math.round((exitos * 360f) / total);

        // Capa fondo (gris suave)
        g2.setColor(new Color(40, 40, 48));
        g2.fillOval(x, y, size, size);

        // Arco de exitos
        g2.setColor(cEx);
        g2.fillArc(x, y, size, size, start, -angleEx);

        // Arco de fallos
        g2.setColor(cFa);
        g2.fillArc(x, y, size, size, start - angleEx, -(360 - angleEx));

        // Circulo interior para efecto donut
        int inner = (int)(size * 0.55);
        int ix = x + (size - inner) / 2;
        int iy = y + (size - inner) / 2;
        g2.setColor(Estilos.BG);
        g2.fillOval(ix, iy, inner, inner);

        // Texto central con porcentaje
        g2.setColor(Estilos.TEXT);
        g2.setFont(Estilos.BIG_NUM);
        String txt = (total == 0) ? ".." : String.format("%.0f%%", pct);
        FontMetrics fm = g2.getFontMetrics();
        int tx = ix + (inner - fm.stringWidth(txt)) / 2;
        int ty = iy + (inner + fm.getAscent()) / 2 - 4;
        g2.drawString(txt, tx, ty);
    }
}