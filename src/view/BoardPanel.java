package view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@SuppressWarnings("serial")
public class BoardPanel extends JPanel {



    private int[][] board;



    private List<Point2D.Double> curvePoints;
    private boolean invertCurveY;


    private boolean colorEnabled = true;


    private final Map<Integer, Color> tileColors = new HashMap<>();


    private boolean gridEnabled = false;
    private int highlightRow = -1;
    private int highlightCol = -1;
    private boolean selectMode = false;
    private MissingTileListener missingTileListener;


    public interface MissingTileListener {
        void onMissingTileSelected(int row, int col);
    }

    /**
     * Crea un nou panell per visualitzar el tauler.
     */
    public BoardPanel() {
        super();
        setBackground(Color.WHITE);


        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (selectMode && board != null) {
                    int row = highlightRow;
                    int col = highlightCol;
                    if (row >= 0 && col >= 0 && row < board.length && col < board[0].length) {
                        if (missingTileListener != null) {
                            missingTileListener.onMissingTileSelected(row, col);
                        }
                        selectMode = false;
                        repaint();
                    }
                }
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                if (selectMode && board != null) {
                    int rows = board.length;
                    int cols = board[0].length;
                    int cellW = getWidth() / cols;
                    int cellH = getHeight() / rows;

                    int newRow = e.getY() / cellH;
                    int newCol = e.getX() / cellW;

                    if (newRow >= 0 && newRow < rows && newCol >= 0 && newCol < cols &&
                            (newRow != highlightRow || newCol != highlightCol)) {
                        highlightRow = newRow;
                        highlightCol = newCol;
                        repaint();
                    }
                }
            }
        });
    }

    /**
     * Estableix o reemplaça l'array de tauler 2D per a l'emplenat de tessel·les basat en graella.
     * @param board L'array de tauler 2D amb identificadors de cel·les.
     */
    public void setBoard(int[][] board) {
        this.board = board;

        this.curvePoints = null;
        repaint();
    }

    /**
     * Activa o desactiva el mode de selecció de cel·la en el tauler.
     *
     * @param enabled Cert per activar la selecció.
     * @param listener L'objecte que notificarà la selecció de la peça.
     */
    public void setSelectMode(boolean enabled, MissingTileListener listener) {
        this.selectMode = enabled;
        this.missingTileListener = listener;
        this.gridEnabled = enabled;
        this.highlightRow = -1;
        this.highlightCol = -1;
        setCursor(enabled ? new Cursor(Cursor.HAND_CURSOR) : new Cursor(Cursor.DEFAULT_CURSOR));
        repaint();
    }

    /**
     * Pinta el contingut del panell, incloent el tauler, la corba o la selecció.
     *
     * @param g L'objecte Graphics per dibuixar.
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (curvePoints != null && !curvePoints.isEmpty()) {
            drawCurve((Graphics2D) g);
            return;
        }
        if (board != null) {
            drawTiledBoard(g);


            if (gridEnabled) {
                drawGrid(g);
            }


            if (selectMode && highlightRow >= 0 && highlightCol >= 0 &&
                    highlightRow < board.length && highlightCol < board[0].length) {
                drawHighlight(g);
            }
        }
    }

    /**
     * Dibuixa les línies de la graella per ajudar en la selecció.
     *
     * @param g L'objecte Graphics per dibuixar.
     */
    private void drawGrid(Graphics g) {
        int rows = board.length;
        int cols = board[0].length;
        int cellW = getWidth() / cols;
        int cellH = getHeight() / rows;

        g.setColor(new Color(200, 200, 200, 150));


        for (int j = 1; j < cols; j++) {
            int x = j * cellW;
            g.drawLine(x, 0, x, getHeight());
        }


        for (int i = 1; i < rows; i++) {
            int y = i * cellH;
            g.drawLine(0, y, getWidth(), y);
        }
    }

    /**
     * Dibuixa una cel·la destacada per indicar la selecció actual.
     *
     * @param g L'objecte Graphics per dibuixar.
     */
    private void drawHighlight(Graphics g) {
        int rows = board.length;
        int cols = board[0].length;
        int cellW = getWidth() / cols;
        int cellH = getHeight() / rows;

        int x = highlightCol * cellW;
        int y = highlightRow * cellH;


        g.setColor(new Color(100, 100, 255, 100));
        g.fillRect(x, y, cellW, cellH);


        g.setColor(new Color(0, 0, 200));
        g.drawRect(x, y, cellW, cellH);
    }

    /**
     * Estableix la llista de punts que defineixen una corba contínua i desactiva el dibuix de la graella.
     *
     * @param points La llista de punts de la corba.
     * @param invertY Indica si s'ha d'invertir l'eix Y.
     */
    public void setCurvePoints(List<Point2D.Double> points, boolean invertY) {
        this.curvePoints = points;
        this.invertCurveY = invertY;

        this.board = null;
        this.selectMode = false;
        this.gridEnabled = false;
        repaint();
    }

    /**
     * Activa o desactiva el mode de colors per als tiles.
     *
     * @param enabled Cert per activar el mode de colors.
     */
    public void setColorMode(boolean enabled) {
        this.colorEnabled = enabled;
        repaint();
    }

    /**
     * Neteja el mapa de colors per assignar nous colors als identificadors dels tiles.
     */
    public void resetColorMapping() {
        tileColors.clear();
    }

    /**
     * Genera un color aleatori brillant per omplir els tiles.
     *
     * @return Un color aleatori.
     */
    private Color generateRandomColor() {
        float hue = (float) Math.random();
        float saturation = 0.6f + (float) Math.random() * 0.4f;
        float brightness = 0.8f + (float) Math.random() * 0.2f;
        return Color.getHSBColor(hue, saturation, brightness);
    }

    /**
     * Dibuixa la corba fractal com una polilínia, gestionant interrupcions en la seqüència.
     *
     * @param g2 L'objecte Graphics2D per dibuixar.
     */
    private void drawCurve(Graphics2D g2) {
        if (curvePoints == null || curvePoints.isEmpty()) {
            return;
        }

        g2.setColor(Color.BLACK);
        g2.setStroke(new BasicStroke(2.0f));
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);


        double minX = Double.POSITIVE_INFINITY, maxX = Double.NEGATIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;

        for (Point2D.Double p : curvePoints) {
            if (p == null) continue;
            if (p.x < minX) minX = p.x;
            if (p.x > maxX) maxX = p.x;
            if (p.y < minY) minY = p.y;
            if (p.y > maxY) maxY = p.y;
        }


        double padding = Math.max((maxX - minX), (maxY - minY)) * 0.05;
        minX -= padding;
        maxX += padding;
        minY -= padding;
        maxY += padding;


        double width = getWidth();
        double height = getHeight();
        double scaleX = (maxX > minX) ? (width - 20) / (maxX - minX) : 1;
        double scaleY = (maxY > minY) ? (height - 20) / (maxY - minY) : 1;


        double scale = Math.min(scaleX, scaleY);


        double offsetX = (width - (maxX - minX) * scale) / 2;
        double offsetY = (height - (maxY - minY) * scale) / 2;

        Point2D.Double prev = null;
        for (Point2D.Double curr : curvePoints) {
            if (curr == null) {
                prev = null;
                continue;
            }

            if (prev != null) {
                int x1 = (int) (offsetX + (prev.x - minX) * scale);
                int y1 = (int) (offsetY + (prev.y - minY) * scale);
                int x2 = (int) (offsetX + (curr.x - minX) * scale);
                int y2 = (int) (offsetY + (curr.y - minY) * scale);

                if (invertCurveY) {
                    y1 = (int) (height - y1);
                    y2 = (int) (height - y2);
                }

                g2.drawLine(x1, y1, x2, y2);
            }
            prev = curr;
        }
    }


    /**
     * Dibuixa el tauler amb els tiles i les línies de contorn.
     *
     * @param g L'objecte Graphics per dibuixar.
     */
    private void drawTiledBoard(Graphics g) {
        int rows = board.length;
        int cols = board[0].length;
        int cellW = getWidth() / cols;
        int cellH = getHeight() / rows;


        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                int val = board[i][j];

                if (val > 0) {
                    Color fillColor = Color.WHITE;
                    if (colorEnabled) {
                        fillColor = tileColors.computeIfAbsent(val, k -> generateRandomColor());
                    }
                    g.setColor(fillColor);
                    g.fillRect(j * cellW, i * cellH, cellW, cellH);
                } else if (val == -1) {

                    g.setColor(Color.DARK_GRAY);
                    g.fillRect(j * cellW, i * cellH, cellW, cellH);
                }
            }
        }


        g.setColor(Color.BLACK);


        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols - 1; j++) {
                int leftVal = board[i][j];
                int rightVal = board[i][j + 1];
                boolean leftFilled = leftVal > 0;
                boolean rightFilled = rightVal > 0;

                if ((leftFilled != rightFilled) || (leftFilled && rightFilled && leftVal != rightVal)) {
                    int x = (j + 1) * cellW;
                    int y1 = i * cellH;
                    int y2 = y1 + cellH;
                    g.drawLine(x, y1, x, y2);
                }
            }
        }


        for (int i = 0; i < rows - 1; i++) {
            for (int j = 0; j < cols; j++) {
                int topVal = board[i][j];
                int botVal = board[i + 1][j];
                boolean topFilled = topVal > 0;
                boolean botFilled = botVal > 0;
                if ((topFilled != botFilled) || (topFilled && botFilled && topVal != botVal)) {
                    int y = (i + 1) * cellH;
                    int x1 = j * cellW;
                    int x2 = x1 + cellW;
                    g.drawLine(x1, y, x2, y);
                }
            }
        }



        for (int j = 0; j < cols; j++) {
            if (board[0][j] > 0) {
                int x1 = j * cellW;
                int x2 = x1 + cellW;
                g.drawLine(x1, 0, x2, 0);
            }
        }

        for (int j = 0; j < cols; j++) {
            if (board[rows - 1][j] > 0) {
                int x1 = j * cellW;
                int x2 = x1 + cellW;
                int y = rows * cellH;
                g.drawLine(x1, y, x2, y);
            }
        }

        for (int i = 0; i < rows; i++) {
            if (board[i][0] > 0) {
                int y1 = i * cellH;
                int y2 = y1 + cellH;
                g.drawLine(0, y1, 0, y2);
            }
        }

        for (int i = 0; i < rows; i++) {
            if (board[i][cols - 1] > 0) {
                int x = cols * cellW;
                int y1 = i * cellH;
                int y2 = y1 + cellH;
                g.drawLine(x, y1, x, y2);
            }
        }
    }
}