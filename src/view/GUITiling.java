package view;

import controller.TilingController;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Point2D;

@SuppressWarnings("serial")
public class GUITiling extends JFrame {
    private final TilingController controller;
    private final BoardPanel boardPanel;

    private final JTextField depthField;
    private final JSlider slider;
    private final JComboBox<String> algorithmBox;
    private final JButton startButton;
    private final JButton stopButton;
    private final JButton stopCleanButton;

    private final JCheckBox animationCheck;
    private final JCheckBox colorCheck;

    private final JLabel missingRowLabel;
    private final JLabel missingColLabel;
    private final JTextField missingRowField;
    private final JTextField missingColField;

    private final JToggleButton selectTileButton;
    private int selectedMissingRow = 0;
    private int selectedMissingCol = 0;

    private final JLabel timeEstimateLabel;

    /**
     * Crea una nova instància de GUITiling.
     *
     * @param controller El controlador de tiling.
     */
    public GUITiling(TilingController controller) {
        this.controller = controller;
        setTitle("Tiling Visualization");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());


        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));


        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        slider = new JSlider(JSlider.HORIZONTAL, 1, 10, 3);
        slider.setMajorTickSpacing(1);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        row1.add(slider);
        row1.add(new JLabel("Max Depth:"));
        depthField = new JTextField("4", 5);
        row1.add(depthField);
        row1.add(new JLabel("Algorithm:"));
        algorithmBox = new JComboBox<>(new String[]{
                "Tromino", "Domino", "Hilbert", "Sierpinski", "Koch", "Square"
        });
        row1.add(algorithmBox);


        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        missingRowLabel = new JLabel("Missing Row:");
        missingColLabel = new JLabel("Missing Col:");
        missingRowField = new JTextField("0", 3);
        missingColField = new JTextField("0", 3);
        row2.add(missingRowLabel);
        row2.add(missingRowField);
        row2.add(missingColLabel);
        row2.add(missingColField);
        selectTileButton = new JToggleButton("Select Tile");
        row2.add(selectTileButton);


        JPanel row3 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        animationCheck = new JCheckBox("Animate", true);
        colorCheck = new JCheckBox("Color", true);
        row3.add(animationCheck);
        row3.add(colorCheck);
        timeEstimateLabel = new JLabel("Estimated time: N/A");
        row3.add(timeEstimateLabel);
        startButton = new JButton("Start");
        stopButton = new JButton("Stop");
        stopCleanButton = new JButton("Stop and Clean");
        row3.add(startButton);
        row3.add(stopButton);
        row3.add(stopCleanButton);


        topPanel.add(row1);
        topPanel.add(row2);
        topPanel.add(row3);


        add(topPanel, BorderLayout.NORTH);


        boardPanel = new BoardPanel();
        add(boardPanel, BorderLayout.CENTER);


        startButton.addActionListener(e -> onStartTiling());
        stopButton.addActionListener(e -> controller.stopTiling());
        stopCleanButton.addActionListener(e -> controller.stopAndClean());
        selectTileButton.addActionListener(e -> toggleTileSelectionMode());
        animationCheck.addActionListener(e -> {

        });
        colorCheck.addActionListener(e -> boardPanel.setColorMode(colorCheck.isSelected()));


        slider.addChangeListener(e -> {
            if (selectTileButton.isSelected()) {
                int size = (int) Math.pow(2, slider.getValue());
                boardPanel.setBoard(new int[size][size]);
            }
        });

        pack();
        setSize(800, 600);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    /**
     * Activa o desactiva el mode de selecció de peça.
     * Quan s'activa, reinicialitza el tauler amb la mida actual.
     */
    private void toggleTileSelectionMode() {

        int size = (int) Math.pow(2, slider.getValue());
        if (selectTileButton.isSelected()) {

            boardPanel.resetColorMapping();
            boardPanel.setCurvePoints(null, false);
            boardPanel.setBoard(new int[size][size]);


            boardPanel.setSelectMode(true, (row, col) -> {
                selectedMissingRow = row;
                selectedMissingCol = col;
                missingRowField.setText(String.valueOf(row));
                missingColField.setText(String.valueOf(col));
            });
        } else {
            boardPanel.setSelectMode(false, null);
        }
    }

    /**
     * Reinicialitza el tauler amb la mida especificada.
     *
     * @param boardSize La mida del tauler.
     */
    public void resetBoard(int boardSize) {
        boardPanel.resetColorMapping();
        boardPanel.setCurvePoints(null, false);
        boardPanel.setBoard(new int[boardSize][boardSize]);

        selectTileButton.setSelected(false);
        boardPanel.setSelectMode(false, (row, col) -> {
            selectedMissingRow = row;
            selectedMissingCol = col;
            missingRowField.setText(String.valueOf(row));
            missingColField.setText(String.valueOf(col));
        });
    }

    /**
     * Actualitza el tauler mostrat amb l'estat actual.
     *
     * @param board L'estat del tauler.
     * @param step El pas actual de l'execució.
     */
    public void updateBoard(final int[][] board, final int step) {
        SwingUtilities.invokeLater(() -> {
            boardPanel.setBoard(board);
            boardPanel.repaint();
        });
    }

    /**
     * Mostra la corba generada a la interfície gràfica.
     *
     * @param curvePoints La llista de punts que defineixen la corba.
     * @param invertY Indica si s'ha d'invertir l'eix Y.
     */
    public void showCurve(final java.util.List<Point2D.Double> curvePoints, final boolean invertY) {
        SwingUtilities.invokeLater(() -> {
            boardPanel.setCurvePoints(curvePoints, invertY);
        });
    }

    /**
     * Mostra un missatge d'error a l'usuari.
     *
     * @param message El missatge d'error.
     */
    public void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Inicia el procés de tiling amb els paràmetres especificats.
     */
    private void onStartTiling() {
        try {
            int size = (int) Math.pow(2, slider.getValue());
            int depth = Integer.parseInt(depthField.getText().trim());
            String algo = (String) algorithmBox.getSelectedItem();

            controller.setAnimationEnabled(animationCheck.isSelected());
            boardPanel.setColorMode(colorCheck.isSelected());

            String estimate = controller.estimateTime(size, depth, algo);
            timeEstimateLabel.setText("Estimated time: " + estimate);

            if ("Tromino".equals(algo)) {
                int missR = Integer.parseInt(missingRowField.getText().trim());
                int missC = Integer.parseInt(missingColField.getText().trim());
                if (missR < 0 || missR >= size || missC < 0 || missC >= size) {
                    showError("Missing tile coordinates are out of range!");
                    return;
                }
                controller.startTiling(size, depth, algo, missR, missC);
            } else {
                controller.startTiling(size, depth, algo);
            }
        } catch (NumberFormatException e) {
            showError("Invalid input for size or depth.");
        }
    }
}
