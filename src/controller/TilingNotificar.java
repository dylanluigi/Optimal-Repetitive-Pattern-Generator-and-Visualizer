package controller;

public interface TilingNotificar {
    /**
     * Notifica que el procés de tiling ha començat.
     *
     * @param boardSize La mida del tauler.
     * @param maxDepth La profunditat màxima.
     */
    void onTilingStarted(int boardSize, int maxDepth);

    /**
     * Notifica un pas del procés de tiling.
     *
     * @param step El número del pas actual.
     * @param boardState L'estat actual del tauler.
     */
    void onTilingStep(int step, int[][] boardState);

    /**
     * Notifica que el procés de tiling s'ha completat.
     */
    void onTilingCompleted();

    /**
     * Notifica que s'ha produït un error durant el procés de tiling.
     *
     * @param errorMessage El missatge d'error.
     */
    void onTilingError(String errorMessage);
}
