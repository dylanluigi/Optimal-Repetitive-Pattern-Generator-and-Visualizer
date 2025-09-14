package model;

import controller.TilingController;
import controller.TilingNotificar;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SquareModel implements TilingAlgorithm {
    private int boardSize;
    private int depth;
    private List<Point2D.Double> points;
    private double size;

    /**
     * Crea una nova instància de SquareModel per a generar un fractal de quadrats.
     *
     * @param boardSize La mida del tauler.
     * @param depth La profunditat del fractal.
     */
    public SquareModel(int boardSize, int depth) {
        this.boardSize = boardSize;
        this.depth = depth;
        this.size = boardSize;
        this.points = new ArrayList<>();
    }

    /**
     * Calcula el fractal de quadrats (Square Carpet) i notifica els passos del càlcul.
     *
     * @param notifier L'objecte que rep les notificacions.
     * @param maxDepth La profunditat màxima per al càlcul.
     */
    @Override
    public void calculateTiling(TilingNotificar notifier, int maxDepth) {
        notifier.onTilingStarted(boardSize, depth);
        boolean animate = (notifier instanceof TilingController) && ((TilingController) notifier).isAnimationEnabled();
        if (animate) {

            List<Point2D.Double> fullPoints = new ArrayList<>();
            generateCarpetPoints(0, 0, size, depth, fullPoints);
            ScheduledExecutorService scheduler = (notifier instanceof TilingController)
                    ? ((TilingController) notifier).getAnimationExecutor()
                    : java.util.concurrent.Executors.newSingleThreadScheduledExecutor();
            points.clear();
            if (!fullPoints.isEmpty()) {
                points.add(fullPoints.get(0));
            }
            long delayMs = 50;
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
            generateCarpet(0, 0, size, depth);
            notifier.onTilingStep(points.size(), null);
            notifier.onTilingCompleted();
        }
    }

    /**
     * Genera recursivament els punts per al tapís de quadrats.
     *
     * @param x La coordenada x d'inici.
     * @param y La coordenada y d'inici.
     * @param size La mida del quadrat.
     * @param level El nivell de recursió restant.
     */
    private void generateCarpet(double x, double y, double size, int level) {
        if (level <= 0) {
            addSquare(x, y, size);
            return;
        }
        double third = size / 3;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (i == 1 && j == 1) continue;  // skip center
                generateCarpet(x + i * third, y + j * third, third, level - 1);
            }
        }
    }

    /**
     * Genera recursivament els punts que defineixen els contorns del tapís de quadrats.
     *
     * @param x La coordenada x d'inici.
     * @param y La coordenada y d'inici.
     * @param size La mida del quadrat.
     * @param level El nivell de recursió restant.
     * @param outList La llista on s'afegiran els punts generats.
     */
    private void generateCarpetPoints(double x, double y, double size, int level, List<Point2D.Double> outList) {
        if (level <= 0) {
            // Add outline of one square
            outList.add(new Point2D.Double(x, y));
            outList.add(new Point2D.Double(x + size, y));
            outList.add(new Point2D.Double(x + size, y));
            outList.add(new Point2D.Double(x + size, y + size));
            outList.add(new Point2D.Double(x + size, y + size));
            outList.add(new Point2D.Double(x, y + size));
            outList.add(new Point2D.Double(x, y + size));
            outList.add(new Point2D.Double(x, y));
            outList.add(null);
            return;
        }
        double third = size / 3;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (i == 1 && j == 1) continue;
                generateCarpetPoints(x + i * third, y + j * third, third, level - 1, outList);
            }
        }
    }

    /**
     * Afegeix els punts que defineixen el contorn d'un quadrat a la llista de punts.
     *
     * @param x La coordenada x del quadrat.
     * @param y La coordenada y del quadrat.
     * @param size La mida del quadrat.
     */
    private void addSquare(double x, double y, double size) {
        points.add(new Point2D.Double(x, y));
        points.add(new Point2D.Double(x + size, y));
        points.add(new Point2D.Double(x + size, y));
        points.add(new Point2D.Double(x + size, y + size));
        points.add(new Point2D.Double(x + size, y + size));
        points.add(new Point2D.Double(x, y + size));
        points.add(new Point2D.Double(x, y + size));
        points.add(new Point2D.Double(x, y));
        points.add(null);
    }

    /**
     * Retorna la llista de punts generats pel fractal de quadrats.
     *
     * @return La llista de punts.
     */
    public List<Point2D.Double> getPoints() {
        return points;
    }
}
