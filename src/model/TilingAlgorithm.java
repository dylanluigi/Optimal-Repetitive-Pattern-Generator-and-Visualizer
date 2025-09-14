package model;

import controller.TilingNotificar;

public interface TilingAlgorithm {
    /**
     * Calcula el tiling i notifica els canvis.
     *
     * @param notifier L'objecte que rep les notificacions.
     * @param maxDepth La profunditat màxima per al càlcul.
     */
    void calculateTiling(TilingNotificar notifier, int maxDepth);
}
