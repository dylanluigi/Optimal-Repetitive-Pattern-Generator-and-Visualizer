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

public class KochCurve implements TilingAlgorithm {
    private int boardSize;
    private List<Point2D.Double> points;

    /**
     * Crea una nova instància de KochCurve amb la mida especificada.
     *
     * @param boardSize La mida del tauler.
     */
    public KochCurve(int boardSize) {
        this.boardSize = boardSize;
        this.points = new ArrayList<>();
    }

    /**
     * Calcula la corba de Koch i notifica els passos del càlcul.
     *
     * @param notifier L'objecte que rep les notificacions.
     * @param maxDepth La profunditat màxima de la corba.
     */
    @Override
    public void calculateTiling(TilingNotificar notifier, int maxDepth) {
        notifier.onTilingStarted(boardSize, maxDepth);
        boolean animate = (notifier instanceof TilingController) && ((TilingController) notifier).isAnimationEnabled();
        Point2D.Double start = new Point2D.Double(0, boardSize / 2.0);
        Point2D.Double end = new Point2D.Double(boardSize - 1, boardSize / 2.0);
        if (animate) {

            List<Point2D.Double> fullPoints = generateKochPoints(start, end, maxDepth);
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
            KochTask mainTask = new KochTask(start, end, maxDepth);
            points = pool.invoke(mainTask);
            notifier.onTilingStep(points.size(), null);
            notifier.onTilingCompleted();
        }
    }

    /**
     * Retorna la llista de punts generats per la corba de Koch.
     *
     * @return La llista de punts.
     */
    public List<Point2D.Double> getPoints() {
        return points;
    }


    /**
     * Genera recursivament la llista de punts per a la corba de Koch.
     *
     * @param a El punt d'inici.
     * @param b El punt final.
     * @param depth La profunditat actual de recursió.
     * @return La llista de punts generada.
     */
    private List<Point2D.Double> generateKochPoints(Point2D.Double a, Point2D.Double b, int depth) {
        List<Point2D.Double> result = new ArrayList<>();
        if (depth == 0) {
            result.add(a);
            result.add(b);
            return result;
        }
        double deltaX = b.x - a.x;
        double deltaY = b.y - a.y;
        Point2D.Double p1 = new Point2D.Double(a.x + deltaX / 3, a.y + deltaY / 3);
        Point2D.Double p3 = new Point2D.Double(a.x + 2 * deltaX / 3, a.y + 2 * deltaY / 3);
        double angle = Math.atan2(deltaY, deltaX) - Math.PI / 3;
        double dist = Math.hypot(deltaX, deltaY) / 3;
        Point2D.Double p2 = new Point2D.Double(p1.x + dist * Math.cos(angle), p1.y + dist * Math.sin(angle));
        result.addAll(generateKochPoints(a, p1, depth - 1));
        if (!result.isEmpty()) result.remove(result.size() - 1);
        result.addAll(generateKochPoints(p1, p2, depth - 1));
        if (!result.isEmpty()) result.remove(result.size() - 1);
        result.addAll(generateKochPoints(p2, p3, depth - 1));
        if (!result.isEmpty()) result.remove(result.size() - 1);
        result.addAll(generateKochPoints(p3, b, depth - 1));
        return result;
    }


    private class KochTask extends RecursiveTask<List<Point2D.Double>> {
        private Point2D.Double a, b;
        private int depth;

        KochTask(Point2D.Double a, Point2D.Double b, int depth) {
            this.a = a;
            this.b = b;
            this.depth = depth;
        }

        /**
         * Calcula recursivament els punts de la corba de Koch en paral·lel.
         *
         * @return La llista de punts per al segment actual.
         */
        @Override
        protected List<Point2D.Double> compute() {
            if (Thread.currentThread().isInterrupted()) {
                return new ArrayList<>();
            }
            if (depth == 0) {
                List<Point2D.Double> segment = new ArrayList<>();
                segment.add(a);
                segment.add(b);
                return segment;
            }
            double deltaX = b.x - a.x;
            double deltaY = b.y - a.y;
            Point2D.Double p1 = new Point2D.Double(a.x + deltaX / 3, a.y + deltaY / 3);
            Point2D.Double p3 = new Point2D.Double(a.x + 2 * deltaX / 3, a.y + 2 * deltaY / 3);
            double angle = Math.atan2(deltaY, deltaX) - Math.PI / 3;
            double dist = Math.hypot(deltaX, deltaY) / 3;
            Point2D.Double p2 = new Point2D.Double(p1.x + dist * Math.cos(angle), p1.y + dist * Math.sin(angle));
            KochTask task1 = new KochTask(a, p1, depth - 1);
            KochTask task2 = new KochTask(p1, p2, depth - 1);
            KochTask task3 = new KochTask(p2, p3, depth - 1);
            KochTask task4 = new KochTask(p3, b, depth - 1);
            task1.fork();
            List<Point2D.Double> list2 = task2.compute();
            task3.fork();
            List<Point2D.Double> list4 = task4.compute();
            List<Point2D.Double> list3 = task3.join();
            List<Point2D.Double> list1 = task1.join();
            List<Point2D.Double> result = new ArrayList<>();
            result.addAll(list1);
            if (!result.isEmpty()) result.remove(result.size() - 1);
            result.addAll(list2);
            if (!result.isEmpty()) result.remove(result.size() - 1);
            result.addAll(list3);
            if (!result.isEmpty()) result.remove(result.size() - 1);
            result.addAll(list4);
            return result;
        }
    }
}
