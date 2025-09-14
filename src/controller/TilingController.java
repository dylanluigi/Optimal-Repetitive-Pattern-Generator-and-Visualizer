package controller;

import model.*;
import view.GUITiling;

import java.awt.geom.Point2D;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TilingController implements TilingNotificar {
    private final GUITiling gui;
    private ScheduledExecutorService animationExecutor;
    private ExecutorService computeExecutor;
    private volatile boolean isRunning = false;
    private boolean animationEnabled = true;
    private TilingAlgorithm currentAlgorithm;
    private int currentBoardSize;

    /**
     * Crea una nova instància de TilingController i inicialitza la interfície gràfica.
     */
    public TilingController() {
        gui = new GUITiling(this);
    }

    /**
     * Inicia el procés de tiling utilitzant l'algoritme especificat.
     *
     * @param boardSize La mida del tauler.
     * @param maxDepth La profunditat màxima del càlcul.
     * @param algorithmName El nom de l'algoritme a utilitzar.
     */
    public synchronized void startTiling(int boardSize, int maxDepth, String algorithmName) {
        startTiling(boardSize, maxDepth, algorithmName, 0, 0);
    }

    /**
     * Inicia el procés de tiling per a l'algoritme Tromino, indicant la posició de la peça absent.
     *
     * @param boardSize La mida del tauler.
     * @param maxDepth La profunditat màxima del càlcul.
     * @param algorithmName El nom de l'algoritme.
     * @param missingRow La fila de la peça absent.
     * @param missingCol La columna de la peça absent.
     */
    public synchronized void startTiling(int boardSize, int maxDepth, String algorithmName, int missingRow, int missingCol) {
        if (isRunning) return;
        isRunning = true;
        currentBoardSize = boardSize;

        if (animationEnabled) {
            animationExecutor = Executors.newSingleThreadScheduledExecutor();
            computeExecutor = null;
        } else {
            computeExecutor = new ForkJoinPool();
            animationExecutor = null;
        }
        switch (algorithmName) {
            case "Tromino":
                currentAlgorithm = new TrominoTiling(boardSize, missingRow, missingCol);
                break;
            case "Domino":
                currentAlgorithm = new DominoTiling(boardSize);
                break;
            case "Hilbert":
                currentAlgorithm = new HilbertCurve(boardSize);
                break;
            case "Sierpinski":
                currentAlgorithm = new SierpinskiTriangle(boardSize);
                break;
            case "Koch":
                currentAlgorithm = new KochCurve(boardSize);
                break;
            case "Square":
                currentAlgorithm = new SquareModel(boardSize, maxDepth);
                break;
            default:
                throw new IllegalArgumentException("Unknown algorithm: " + algorithmName);
        }
        gui.resetBoard(boardSize);

        if (animationEnabled) {
            animationExecutor.execute(() -> currentAlgorithm.calculateTiling(this, maxDepth));
        } else {
            computeExecutor.execute(() -> currentAlgorithm.calculateTiling(this, maxDepth));
        }
    }

    /**
     * Retorna si l'animació està activada.
     *
     * @return Cert si l'animació està activada, fals en cas contrari.
     */

    public boolean isAnimationEnabled() {
        return animationEnabled;
    }

    /**
     * Estableix si l'animació està activada.
     *
     * @param enabled Cert per activar l'animació, fals per desactivar-la.
     */

    public void setAnimationEnabled(boolean enabled) {
        this.animationEnabled = enabled;
    }

    /**
     * Retorna l'executor de tasques per a l'animació.
     *
     * @return L'executor de tasques per a l'animació.
     */
    public ScheduledExecutorService getAnimationExecutor() {
        return animationExecutor;
    }

    /**
     * Retorna l'executor per a càlculs en paral·lel.
     *
     * @return L'executor per a càlculs.
     */
    public ExecutorService getComputeExecutor() {
        return computeExecutor;
    }

    /**
     * Notifica que el procés de tiling ha començat.
     *
     * @param boardSize La mida del tauler.
     * @param maxDepth La profunditat màxima.
     */
    @Override
    public void onTilingStarted(int boardSize, int maxDepth) {
        // Optionally update GUI for start (not used in this design)
    }

    /**
     * Notifica un pas en el procés de tiling.
     *
     * @param step El número del pas actual.
     * @param boardState L'estat actual del tauler.
     */
    @Override
    public void onTilingStep(int step, int[][] boardState) {
        if (!isRunning) return;
        if (boardState != null) {

            gui.updateBoard(boardState, step);
        } else {
            List<Point2D.Double> points = null;
            boolean invertY = false;
            if (currentAlgorithm instanceof HilbertCurve) {
                points = ((HilbertCurve) currentAlgorithm).getPoints();
                invertY = true;
            } else if (currentAlgorithm instanceof KochCurve) {
                points = ((KochCurve) currentAlgorithm).getPoints();
            } else if (currentAlgorithm instanceof SierpinskiTriangle) {
                points = ((SierpinskiTriangle) currentAlgorithm).getPoints();
            } else if (currentAlgorithm instanceof SquareModel) {
                points = ((SquareModel) currentAlgorithm).getPoints();
            }
            if (points != null) {
                gui.showCurve(points, invertY);
            }
        }
    }

    /**
     * Notifica que el procés de tiling s'ha completat.
     */
    @Override
    public void onTilingCompleted() {
        isRunning = false;

        if (currentAlgorithm instanceof HilbertCurve) {
            List<Point2D.Double> curve = ((HilbertCurve) currentAlgorithm).getPoints();
            gui.showCurve(curve, true);
        } else if (currentAlgorithm instanceof KochCurve) {
            List<Point2D.Double> curve = ((KochCurve) currentAlgorithm).getPoints();
            gui.showCurve(curve, false);
        } else if (currentAlgorithm instanceof SierpinskiTriangle) {
            List<Point2D.Double> curve = ((SierpinskiTriangle) currentAlgorithm).getPoints();
            gui.showCurve(curve, false);
        } else if (currentAlgorithm instanceof SquareModel) {
            List<Point2D.Double> curve = ((SquareModel) currentAlgorithm).getPoints();
            gui.showCurve(curve, false);
        }

        if (animationExecutor != null) {
            animationExecutor.shutdown();
            animationExecutor = null;
        }
        if (computeExecutor != null) {
            computeExecutor.shutdown();
            computeExecutor = null;
        }
    }

    /**
     * Notifica que s'ha produït un error durant el procés de tiling.
     *
     * @param errorMessage El missatge d'error.
     */
    @Override
    public void onTilingError(String errorMessage) {
        isRunning = false;
        gui.showError(errorMessage);

        if (animationExecutor != null) {
            animationExecutor.shutdownNow();
            animationExecutor = null;
        }
        if (computeExecutor != null) {
            computeExecutor.shutdownNow();
            computeExecutor = null;
        }
    }

    /**
     * Atura el procés de tiling en curs.
     */
    public void stopTiling() {

        isRunning = false;
        if (animationExecutor != null) {
            animationExecutor.shutdownNow();
        }
        if (computeExecutor != null) {
            computeExecutor.shutdownNow();
        }
    }

    /**
     * Atura el procés de tiling i neteja la interfície gràfica.
     */
    public void stopAndClean() {

        stopTiling();
        if (currentBoardSize > 0) {
            gui.resetBoard(currentBoardSize);
        }
        currentAlgorithm = null;
    }

    /**
     * Estima el temps d'execució basant-se en la mida del tauler, la profunditat i l'algoritme.
     *
     * @param boardSize La mida del tauler.
     * @param maxDepth La profunditat màxima.
     * @param algorithm L'algoritme utilitzat.
     * @return Una cadena amb la estimació del temps.
     */
    public String estimateTime(int boardSize, int maxDepth, String algorithm) {
        double steps = 0;
        double delayPerStep = 0;
        switch (algorithm) {
            case "Tromino":
                steps = (double)(boardSize * boardSize - 1) / 3.0;
                delayPerStep = 0.1;
                break;
            case "Domino":
                steps = (double)(boardSize * boardSize) / 2.0;
                delayPerStep = 0.05;
                break;
            case "Hilbert":
                steps = Math.pow(4, maxDepth);
                delayPerStep = 0.02;
                break;
            case "Koch":
                steps = Math.pow(4, maxDepth);
                delayPerStep = 0.05;
                break;
            case "Sierpinski":
                steps = Math.pow(3, maxDepth);
                delayPerStep = 0.1;
                break;
            case "Square":
                steps = Math.pow(8, maxDepth);
                delayPerStep = 0.05;
                break;
            default:
                steps = 0;
        }
        double estimatedSeconds;
        if (animationEnabled) {
            estimatedSeconds = steps * delayPerStep;
        } else {
            estimatedSeconds = Math.max(0.1, steps * 0.001);
        }
        if (estimatedSeconds < 1) {
            return String.format("%.2f seconds", estimatedSeconds);
        } else {
            return String.format("%.1f seconds", estimatedSeconds);
        }
    }

    /**
     * Retorna si el procés de tiling està en execució.
     *
     * @return Cert si està en execució, fals en cas contrari.
     */
    public boolean isRunning() {
        return  isRunning;
    }

    /**
     * Punt d'entrada de l'aplicació.
     */
    public static void main(String[] args) {
        new TilingController();
    }
}
