package model;

import controller.TilingController;
import controller.TilingNotificar;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class HilbertCurve implements TilingAlgorithm {
    private int boardSize;
    private List<Point2D.Double> points;

    private double x, y;
    private double angle;
    private double step;

    /**
     * Crea una nova instància de HilbertCurve amb la mida especificada.
     *
     * @param boardSize La mida del tauler.
     */
    public HilbertCurve(int boardSize) {
        this.boardSize = boardSize;
        this.points = new ArrayList<>();
    }

    /**
     * Calcula la corba de Hilbert i notifica els passos del càlcul.
     *
     * @param notifier L'objecte que rep les notificacions.
     * @param maxDepth La profunditat màxima de la corba.
     */
    @Override
    public void calculateTiling(TilingNotificar notifier, int maxDepth) {
        notifier.onTilingStarted(boardSize, maxDepth);
        boolean animate = (notifier instanceof TilingController) && ((TilingController) notifier).isAnimationEnabled();

        step = (double) (boardSize - 1) / (Math.pow(2, maxDepth) - 1);

        x = 0;
        y = 0;
        angle = 0;
        points.clear();
        points.add(new Point2D.Double(x, y));
        if (animate) {

            List<Point2D.Double> fullPoints = new ArrayList<>();
            generateHilbert(maxDepth, 90, fullPoints);
            ScheduledExecutorService scheduler = (notifier instanceof TilingController)
                    ? ((TilingController) notifier).getAnimationExecutor()
                    : java.util.concurrent.Executors.newSingleThreadScheduledExecutor();
            points.clear();
            if (!fullPoints.isEmpty()) {
                points.add(fullPoints.get(0));
            }
            long delayMs = 20;
            for (int i = 1; i < fullPoints.size(); i++) {
                final int index = i;
                scheduler.schedule(() -> {
                    if (!(notifier instanceof TilingController) || ((TilingController) notifier).isRunning()) {
                        points.add(fullPoints.get(index));
                        notifier.onTilingStep(points.size(), null);
                    }
                }, delayMs * i, TimeUnit.MILLISECONDS);
            }
            scheduler.schedule(() -> notifier.onTilingCompleted(), delayMs * (fullPoints.size() + 1), TimeUnit.MILLISECONDS);
        } else {

            points.clear();
            generateHilbert(maxDepth, 90, points);
            notifier.onTilingStep(points.size(), null);
            notifier.onTilingCompleted();
        }
    }

    /**
     * Genera recursivament la corba de Hilbert.
     *
     * @param level El nivell actual de recursió.
     * @param theta L'angle de rotació.
     * @param outPoints La llista on s'afegiran els punts generats.
     */
    private void generateHilbert(int level, double theta, List<Point2D.Double> outPoints) {
        if (level == 0) return;
        angle += theta;
        generateHilbert(level - 1, -theta, outPoints);
        forward(outPoints);
        angle -= theta;
        generateHilbert(level - 1, theta, outPoints);
        forward(outPoints);
        generateHilbert(level - 1, theta, outPoints);
        angle -= theta;
        forward(outPoints);
        generateHilbert(level - 1, -theta, outPoints);
        angle += theta;
    }

    /**
     * Avança en la direcció actual i afegeix el nou punt a la llista.
     *
     * @param outPoints La llista de punts de la corba.
     */
    private void forward(List<Point2D.Double> outPoints) {
        double rad = Math.toRadians(angle);
        x += step * Math.cos(rad);
        y += step * Math.sin(rad);
        outPoints.add(new Point2D.Double(x, y));
    }

    /**
     * Retorna la llista de punts generats per la corba de Hilbert.
     *
     * @return La llista de punts.
     */
    public List<Point2D.Double> getPoints() {
        return points;
    }

    /**
     * Retorna una còpia actual de la llista de punts de la corba.
     *
     * @return La llista actual de punts.
     */
    public List<Point2D.Double> getCurrentPoints() {
        return new ArrayList<>(points);
    }
}
