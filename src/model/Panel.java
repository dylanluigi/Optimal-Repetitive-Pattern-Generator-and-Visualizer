package model;

import java.util.concurrent.atomic.AtomicInteger;

public class Panel {
    private int[][] board;
    private int size;
    private int misR=-1;
    private int misC=-1;
    private int tileId;

    /**
     * Crea un nou panell amb una peça absent especificada.
     *
     * @param s La mida del panell.
     * @param missingRow La fila de la peça absent.
     * @param missingCol La columna de la peça absent.
     */
    public Panel(int s,int missingRow, int missingCol) {
        size=s;
        board=new int[size][size];
        misR=missingRow;
        misC=missingCol;
        board[misR][misC] = -1;
        tileId = 1;
    }

    /**
     * Crea un nou panell sense peça absent.
     *
     * @param s La mida del panell.
     */
    public Panel(int s) {
        size=s;
        board=new int[size][size];
        tileId = 1;

    }

    /**
     * Retorna l'identificador actual del tile.
     *
     * @return L'identificador del tile.
     */
    public Integer getId(){
        return tileId;
    }

    /**
     * Decrementa l'identificador del tile.
     */
    public void decrementId(){
       tileId--;
    }


    /**
     * Incrementa l'identificador del tile.
     */
    public void incrementId(){
        tileId++;
    }

    /**
     * Retorna la matriu que representa el tauler.
     *
     * @return La matriu del tauler.
     */
    public int[][] getBoard() {
        return board;
    }

    /**
     * Retorna el valor d'una cel·la del tauler.
     *
     * @param x La fila.
     * @param y La columna.
     * @return El valor de la cel·la.
     */
    public int getBoardValue(int x,int y) {
        return board[x][y];
    }

    /**
     * Retorna la fila de la peça absent.
     *
     * @return La fila de la peça absent.
     */
    public int getMissRow() {
        if(misR==-1)return 0;
        return misR;
    }

    /**
     * Assigna l'identificador actual a la cel·la especificada.
     *
     * @param x La fila.
     * @param y La columna.
     */
    public void setId(int x,int y){
        board[x][y] = (tileId);
    }

    /**
     * Estableix la fila de la peça absent.
     *
     * @param missRow La nova fila de la peça absent.
     */
    public void setMissRow(int missRow) {
        misR=missRow;
    }

    /**
     * Retorna la columna de la peça absent.
     *
     * @return La columna de la peça absent.
     */
    public int getMissCol() {
        if(misC==-1)return 0;
        return misC;
    }
    /**
     * Estableix la columna de la peça absent.
     *
     * @param missCol La nova columna de la peça absent.
     */
    public void setMissCol(int missCol) {
        misC=missCol;
    }

    /**
     * Retorna la mida del panell.
     *
     * @return La mida del panell.
     */
    public int getSize() {
        return size;
    }

    /**
     * Estableix el valor d'una cel·la del tauler.
     *
     * @param x La fila.
     * @param y La columna.
     * @param value El valor a assignar.
     */
    public void setBoradValue(int x,int y,int value) {
        board[x][y]=value;
    }

}
