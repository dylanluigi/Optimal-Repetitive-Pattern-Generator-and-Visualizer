package model;

import controller.TilingController;
import controller.TilingNotificar;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class TrominoTiling implements TilingAlgorithm {
    private int boardSize;
    private int[][] board;
    private AtomicInteger tileId;

    public TrominoTiling(int boardSize, int missingRow, int missingCol) {
        this.boardSize = boardSize;
        board = new int[boardSize][boardSize];
        board[missingRow][missingCol] = -1;
        tileId = new AtomicInteger(1);
    }

    @Override
    public void calculateTiling(TilingNotificar notifier, int maxDepth) {
        notifier.onTilingStarted(boardSize, maxDepth);
        boolean animate = (notifier instanceof TilingController) && ((TilingController) notifier).isAnimationEnabled();
        if (animate) {
            if (notifier instanceof TilingController) {
                ScheduledExecutorService scheduler = ((TilingController) notifier).getAnimationExecutor();
                AtomicInteger activeTasks = new AtomicInteger(0);
                // Increment counter for the initial call and schedule it
                activeTasks.incrementAndGet();
                scheduler.execute(() -> scheduleTiling(0, 0, boardSize, findMissingRow(), findMissingCol(), notifier, activeTasks));
            } else {
                new TrominoTask(0, 0, boardSize, findMissingRow(), findMissingCol(), notifier, false).compute();
                notifier.onTilingStep(tileId.get(), board);
                notifier.onTilingCompleted();
            }
        } else {
            ForkJoinPool pool;
            if (notifier instanceof TilingController) {
                java.util.concurrent.ExecutorService exec = ((TilingController) notifier).getComputeExecutor();
                if (exec instanceof ForkJoinPool) {
                    pool = (ForkJoinPool) exec;
                } else {
                    pool = new ForkJoinPool();
                }
            } else {
                pool = new ForkJoinPool();
            }
            pool.invoke(new TrominoTask(0, 0, boardSize, findMissingRow(), findMissingCol(), notifier, false));
            notifier.onTilingCompleted();
        }
    }

    private int findMissingRow() {
        for (int i = 0; i < boardSize; i++)
            for (int j = 0; j < boardSize; j++)
                if (board[i][j] == -1) return i;
        return 0;
    }

    private int findMissingCol() {
        for (int i = 0; i < boardSize; i++)
            for (int j = 0; j < boardSize; j++)
                if (board[i][j] == -1) return j;
        return 0;
    }

    private void scheduleTiling(int r, int c, int size, int missingR, int missingC,
                                TilingNotificar notifier, AtomicInteger activeTasks) {
        if (!(notifier instanceof TilingController) || !((TilingController) notifier).isRunning()) {
            return;
        }
        if (size == 1) {
            if (activeTasks.decrementAndGet() == 0) {
                notifier.onTilingCompleted();
            }
            return;
        }
        int half = size / 2;
        int centerR = r + half - 1;
        int centerC = c + half - 1;
        boolean missingTL = (missingR < r + half && missingC < c + half);
        boolean missingTR = (missingR < r + half && missingC >= c + half);
        boolean missingBL = (missingR >= r + half && missingC < c + half);
        boolean missingBR = (missingR >= r + half && missingC >= c + half);
        int t = tileId.getAndIncrement();
        if (!missingTL) board[centerR][centerC] = t;
        if (!missingTR) board[centerR][centerC + 1] = t;
        if (!missingBL) board[centerR + 1][centerC] = t;
        if (!missingBR) board[centerR + 1][centerC + 1] = t;
        notifier.onTilingStep(t, board);

        ScheduledExecutorService scheduler = ((TilingController) notifier).getAnimationExecutor();
        long delay = 100;

        activeTasks.incrementAndGet();
        scheduler.schedule(() -> scheduleTiling(r, c, half, missingTL ? missingR : centerR, missingTL ? missingC : centerC, notifier, activeTasks),
                delay, TimeUnit.MILLISECONDS);
        activeTasks.incrementAndGet();
        scheduler.schedule(() -> scheduleTiling(r, c + half, half, missingTR ? missingR : centerR, missingTR ? missingC : centerC + 1, notifier, activeTasks),
                delay, TimeUnit.MILLISECONDS);
        activeTasks.incrementAndGet();
        scheduler.schedule(() -> scheduleTiling(r + half, c, half, missingBL ? missingR : centerR + 1, missingBL ? missingC : centerC, notifier, activeTasks),
                delay, TimeUnit.MILLISECONDS);
        activeTasks.incrementAndGet();
        scheduler.schedule(() -> scheduleTiling(r + half, c + half, half, missingBR ? missingR : centerR + 1, missingBR ? missingC : centerC + 1, notifier, activeTasks),
                delay, TimeUnit.MILLISECONDS);

        if (activeTasks.decrementAndGet() == 0) {
            notifier.onTilingCompleted();
        }
    }

    private class TrominoTask extends RecursiveAction {
        private int r, c, size, missingR, missingC;
        private TilingNotificar notifier;
        private boolean animate;

        public TrominoTask(int r, int c, int size, int missingR, int missingC,
                           TilingNotificar notifier, boolean animate) {
            this.r = r;
            this.c = c;
            this.size = size;
            this.missingR = missingR;
            this.missingC = missingC;
            this.notifier = notifier;
            this.animate = animate;
        }

        @Override
        protected void compute() {
            if (size == 1) return;
            int half = size / 2;
            int centerR = r + half - 1;
            int centerC = c + half - 1;
            boolean missingTL = (missingR < r + half && missingC < c + half);
            boolean missingTR = (missingR < r + half && missingC >= c + half);
            boolean missingBL = (missingR >= r + half && missingC < c + half);
            boolean missingBR = (missingR >= r + half && missingC >= c + half);
            int t = tileId.getAndIncrement();
            if (!missingTL) board[centerR][centerC] = t;
            if (!missingTR) board[centerR][centerC + 1] = t;
            if (!missingBL) board[centerR + 1][centerC] = t;
            if (!missingBR) board[centerR + 1][centerC + 1] = t;
            notifier.onTilingStep(t, board);
            if (animate) {
                new TrominoTask(r, c, half, missingTL ? missingR : centerR, missingTL ? missingC : centerC, notifier, true).compute();
                new TrominoTask(r, c + half, half, missingTR ? missingR : centerR, missingTR ? missingC : centerC + 1, notifier, true).compute();
                new TrominoTask(r + half, c, half, missingBL ? missingR : centerR + 1, missingBL ? missingC : centerC, notifier, true).compute();
                new TrominoTask(r + half, c + half, half, missingBR ? missingR : centerR + 1, missingBR ? missingC : centerC + 1, notifier, true).compute();
            } else {
                invokeAll(
                        new TrominoTask(r, c, half, missingTL ? missingR : centerR, missingTL ? missingC : centerC, notifier, false),
                        new TrominoTask(r, c + half, half, missingTR ? missingR : centerR, missingTR ? missingC : centerC + 1, notifier, false),
                        new TrominoTask(r + half, c, half, missingBL ? missingR : centerR + 1, missingBL ? missingC : centerC, notifier, false),
                        new TrominoTask(r + half, c + half, half, missingBR ? missingR : centerR + 1, missingBR ? missingC : centerC + 1, notifier, false)
                );
            }
        }
    }
}
