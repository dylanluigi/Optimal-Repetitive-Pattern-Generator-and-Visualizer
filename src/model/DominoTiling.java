package model;

import controller.TilingController;
import controller.TilingNotificar;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class DominoTiling implements TilingAlgorithm {
    private int boardSize;
    private int[][] board;
    private AtomicInteger dominoId;

    /**
     * Crea una nova instància de DominoTiling amb la mida especificada.
     *
     * @param boardSize La mida del tauler.
     */
    public DominoTiling(int boardSize) {
        this.boardSize = boardSize;
        this.board = new int[boardSize][boardSize];
        this.dominoId = new AtomicInteger(1);
    }

    /**
     * Calcula el tiling amb peces de dòmino i notifica els canvis.
     *
     * @param notifier L'objecte que rep les notificacions.
     * @param maxDepth La profunditat màxima (no s'utilitza per a DominoTiling).
     */
    @Override
    public void calculateTiling(TilingNotificar notifier, int maxDepth) {
        notifier.onTilingStarted(boardSize, maxDepth);
        boolean animate = (notifier instanceof TilingController) && ((TilingController) notifier).isAnimationEnabled();
        if (animate) {
            if (notifier instanceof TilingController) {
                ScheduledExecutorService scheduler = ((TilingController) notifier).getAnimationExecutor();
                AtomicInteger activeTasks = new AtomicInteger(0);
                activeTasks.incrementAndGet();
                scheduler.execute(() -> scheduleTiling(notifier, activeTasks));
            } else {
                tileSequential();
                notifier.onTilingStep(dominoId.get(), board);
                notifier.onTilingCompleted();
            }
        } else {
            tileSequential();
            notifier.onTilingStep(dominoId.get(), board);
            notifier.onTilingCompleted();
        }
    }

    /**
     * Programa la colocació de peces de dòmino amb un retard per a l'animació.
     *
     * @param notifier L'objecte que rep les notificacions.
     * @param activeTasks Un comptador d'operacions actives.
     */
    private void scheduleTiling(TilingNotificar notifier, AtomicInteger activeTasks) {
        if (!(notifier instanceof TilingController) || !((TilingController) notifier).isRunning()) {
            return;
        }
        activeTasks.incrementAndGet();
        int r = -1, c = -1;
        outer:
        for (int i = 0; i < boardSize; i++) {
            for (int j = 0; j < boardSize; j++) {
                if (board[i][j] == 0) {
                    r = i;
                    c = j;
                    break outer;
                }
            }
        }
        if (r == -1) {
            if (activeTasks.decrementAndGet() == 0) {
                notifier.onTilingCompleted();
            }
            return;
        }
        board[r][c] = -1;
        ScheduledExecutorService scheduler = ((TilingController) notifier).getAnimationExecutor();
        if (c + 1 < boardSize && board[r][c + 1] == 0) {
            int id = dominoId.getAndIncrement();
            board[r][c] = id;
            board[r][c + 1] = id;
            notifier.onTilingStep(id, board);
            scheduler.schedule(() -> scheduleTiling(notifier, activeTasks), 50, TimeUnit.MILLISECONDS);
            if (activeTasks.decrementAndGet() == 0) {
                notifier.onTilingCompleted();
            }
            return;
        }
        if (r + 1 < boardSize && board[r + 1][c] == 0) {
            int id = dominoId.getAndIncrement();
            board[r][c] = id;
            board[r + 1][c] = id;
            notifier.onTilingStep(id, board);
            scheduler.schedule(() -> scheduleTiling(notifier, activeTasks), 50, TimeUnit.MILLISECONDS);
            if (activeTasks.decrementAndGet() == 0) {
                notifier.onTilingCompleted();
            }
            return;
        }
        board[r][c] = dominoId.getAndIncrement();
        notifier.onTilingStep(dominoId.get(), board);
        if (activeTasks.decrementAndGet() == 0) {
            notifier.onTilingCompleted();
        }
    }

    /**
     * Calcula el tiling de manera seqüencial.
     *
     * @return Cert si el tiling s'ha completat, fals en cas contrari.
     */
    private boolean tileSequential() {
        for (int i = 0; i < boardSize; i++) {
            for (int j = 0; j < boardSize; j++) {
                if (board[i][j] == 0) {
                    if (j + 1 < boardSize && board[i][j + 1] == 0) {
                        int id = dominoId.getAndIncrement();
                        board[i][j] = id;
                        board[i][j + 1] = id;
                        if (tileSequential()) return true;
                        board[i][j] = 0;
                        board[i][j + 1] = 0;
                        dominoId.decrementAndGet();
                    }
                    if (i + 1 < boardSize && board[i + 1][j] == 0) {
                        int id = dominoId.getAndIncrement();
                        board[i][j] = id;
                        board[i + 1][j] = id;
                        if (tileSequential()) return true;
                        board[i][j] = 0;
                        board[i + 1][j] = 0;
                        dominoId.decrementAndGet();
                    }
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Retorna la matriu del tauler amb el tiling calculat.
     *
     * @return La matriu del tauler.
     */
    public int[][] getBoard() {
        return board;
    }
}
