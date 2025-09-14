package model;

import controller.TilingController;
import controller.TilingNotificar;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.ExecutorService;

public class SierpinskiTriangle implements TilingAlgorithm {
    private int boardSize;
    private List<Point2D.Double> curvePoints;

    /**
     * Crea una nova instància de SierpinskiTriangle per a generar el triangle de Sierpinski.
     *
     * @param boardSize La mida del tauler.
     */
    public SierpinskiTriangle(int boardSize) {
        this.boardSize = boardSize;
        this.curvePoints = new ArrayList<>();
    }

    /**
     * Calcula el triangle de Sierpinski i notifica els passos del càlcul.
     *
     * @param notifier L'objecte que rep les notificacions.
     * @param maxDepth La profunditat màxima del fractal.
     */
    @Override
    public void calculateTiling(TilingNotificar notifier, int maxDepth) {
        notifier.onTilingStarted(boardSize, maxDepth);

        double margin = boardSize * 0.05;
        double effectiveSize = boardSize - 2 * margin;
        Point2D.Double a = new Point2D.Double(margin, boardSize - margin);
        Point2D.Double b = new Point2D.Double(boardSize - margin, boardSize - margin);
        double height = effectiveSize * Math.sqrt(3) / 2;
        Point2D.Double c = new Point2D.Double(boardSize / 2.0, boardSize - margin - height);
        boolean animate = (notifier instanceof TilingController) && ((TilingController) notifier).isAnimationEnabled();
        if (animate) {

            ForkJoinPool tempPool = new ForkJoinPool();
            SierpinskiTask rootTask = new SierpinskiTask(a, b, c, maxDepth);
            List<Point2D.Double> fullPoints = tempPool.invoke(rootTask);
            tempPool.shutdown();
            ScheduledExecutorService scheduler = ((TilingController) notifier).getAnimationExecutor();
            curvePoints.clear();
            if (!fullPoints.isEmpty()) {
                curvePoints.add(fullPoints.get(0));
            }
            long delayMs = 50;
            for (int i = 1; i < fullPoints.size(); i++) {
                final int index = i;
                scheduler.schedule(() -> {
                    if (!(notifier instanceof TilingController) || ((TilingController) notifier).isRunning()) {
                        curvePoints.add(fullPoints.get(index));
                        notifier.onTilingStep(curvePoints.size(), null);
                    }
                }, delayMs * i, TimeUnit.MILLISECONDS);
            }
            scheduler.schedule(() -> notifier.onTilingCompleted(), delayMs * (fullPoints.size() + 1), TimeUnit.MILLISECONDS);
        } else {
            ForkJoinPool pool;
            if (notifier instanceof TilingController) {
                ExecutorService exec = ((TilingController) notifier).getComputeExecutor();
                if (exec instanceof ForkJoinPool) {
                    pool = (ForkJoinPool) exec;
                } else {
                    pool = new ForkJoinPool();
                }
            } else {
                pool = new ForkJoinPool();
            }
            SierpinskiTask rootTask = new SierpinskiTask(a, b, c, maxDepth);
            curvePoints = pool.invoke(rootTask);
            notifier.onTilingStep(curvePoints.size(), null);
            notifier.onTilingCompleted();
        }
    }

    /**
     * Retorna la llista de punts que defineixen el triangle de Sierpinski.
     *
     * @return La llista de punts.
     */
    public List<Point2D.Double> getPoints() {
        return curvePoints;
    }

    /**
     * Calcula el punt mig entre dos punts.
     *
     * @param p El primer punt.
     * @param q El segon punt.
     * @return El punt mig.
     */
    private Point2D.Double midpoint(Point2D.Double p, Point2D.Double q) {
        return new Point2D.Double((p.x + q.x) / 2.0, (p.y + q.y) / 2.0);
    }

    private class SierpinskiTask extends RecursiveTask<List<Point2D.Double>> {
        private Point2D.Double a, b, c;
        private int depth;
        SierpinskiTask(Point2D.Double a, Point2D.Double b, Point2D.Double c, int depth) {
            this.a = a;
            this.b = b;
            this.c = c;
            this.depth = depth;
        }

        /**
         * Calcula recursivament els punts del triangle de Sierpinski per al segment actual.
         *
         * @return La llista de punts generada per aquest segment.
         */
        @Override
        protected List<Point2D.Double> compute() {
            if (Thread.currentThread().isInterrupted()) {
                return new ArrayList<>();
            }
            if (depth == 0) {
                List<Point2D.Double> triangle = new ArrayList<>();
                // Add edges of the triangle
                triangle.add(a);
                triangle.add(b);
                triangle.add(b);
                triangle.add(c);
                triangle.add(c);
                triangle.add(a);
                triangle.add(null);
                return triangle;
            }
            Point2D.Double ab = midpoint(a, b);
            Point2D.Double bc = midpoint(b, c);
            Point2D.Double ca = midpoint(c, a);
            SierpinskiTask task1 = new SierpinskiTask(a, ab, ca, depth - 1);
            SierpinskiTask task2 = new SierpinskiTask(b, bc, ab, depth - 1);
            SierpinskiTask task3 = new SierpinskiTask(c, ca, bc, depth - 1);
            task1.fork();
            List<Point2D.Double> list2 = task2.compute();
            List<Point2D.Double> list3 = task3.compute();
            List<Point2D.Double> list1 = task1.join();
            List<Point2D.Double> result = new ArrayList<>();
            result.addAll(list1);
            result.addAll(list2);
            result.addAll(list3);
            return result;
        }
    }
}
