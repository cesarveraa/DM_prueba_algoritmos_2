package com.example.patterndb.human_strategic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PuzzleSolver {

    // Clase para representar una posición (fila, columna)
    public static class Position {
        public int row, col;

        public Position(int row, int col) {
            this.row = row;
            this.col = col;
        }
    }

    // Clase que representa el puzzle (estado actual)
    public static class Puzzle {
        public String[][] matrix;
        public int rows, cols;
        public int blankRow, blankCol; // posición del espacio en blanco

        // Límites de la porción “no resuelta”
        public int topRowProgress, leftColProgress, botRowProgress, rightColProgress;
        // Variables para indicar la fila/columna que se está resolviendo
        public int rowInProgress, colInProgress;
        public int rowProgressCol, colProgressRow;
        // Flags para indicar el sentido de resolución
        public boolean solvingRowTopDown = true;
        public boolean solvingColLeftRight = true;
        public boolean solvingRow = true;

        // Lista de movimientos realizados (cada movimiento es "LEFT", "RIGHT", "UP" o "DOWN")
        public List<String> solutionMoves;

        // Se espera que el espacio en blanco se represente con cadena vacía ""
        public Puzzle(String[][] matrix) {
            this.rows = matrix.length;
            this.cols = matrix[0].length;
            // Se hace copia de la matriz
            this.matrix = new String[rows][cols];
            for (int i = 0; i < rows; i++) {
                System.arraycopy(matrix[i], 0, this.matrix[i], 0, cols);
            }
            findBlank();
        }

        // Busca la posición del espacio en blanco
        private void findBlank() {
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {
                    if (matrix[i][j].equals("")) {
                        blankRow = i;
                        blankCol = j;
                        return;
                    }
                }
            }
        }

        // Compara este puzzle con otro
        public boolean isEqualToPuzzle(Puzzle other) {
            if (this.rows != other.rows || this.cols != other.cols) return false;
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {
                    if (!this.matrix[i][j].equals(other.matrix[i][j])) {
                        return false;
                    }
                }
            }
            return true;
        }

        // Devuelve un mapeo de cada valor (string) a su posición (fila, columna)
        public static Map<String, Position> getMatrixMapping(String[][] matrix) {
            Map<String, Position> mapping = new HashMap<>();
            for (int i = 0; i < matrix.length; i++) {
                for (int j = 0; j < matrix[i].length; j++) {
                    mapping.put(matrix[i][j], new Position(i, j));
                }
            }
            return mapping;
        }

        // Comprueba si la fila “row” del puzzle actual es igual a la del puzzle meta
        public static boolean isRowEqual(Puzzle goal, Puzzle current, int row) {
            for (int j = 0; j < current.cols; j++) {
                if (!current.matrix[row][j].equals(goal.matrix[row][j])) {
                    return false;
                }
            }
            return true;
        }

        // Comprueba si la columna “col” del puzzle actual es igual a la del puzzle meta
        public static boolean isColEqual(Puzzle goal, Puzzle current, int col) {
            for (int i = 0; i < current.rows; i++) {
                if (!current.matrix[i][col].equals(goal.matrix[i][col])) {
                    return false;
                }
            }
            return true;
        }

        // Métodos para mover el espacio en blanco. Cada “slide” intercambia el espacio en blanco
        // con la ficha adyacente y actualiza la posición.
        public void slideRight() {
            if (canSlideRight()) {
                matrix[blankRow][blankCol] = matrix[blankRow][blankCol + 1];
                matrix[blankRow][blankCol + 1] = "";
                blankCol++;
            }
        }

        public void slideLeft() {
            if (canSlideLeft()) {
                matrix[blankRow][blankCol] = matrix[blankRow][blankCol - 1];
                matrix[blankRow][blankCol - 1] = "";
                blankCol--;
            }
        }

        public void slideUp() {
            if (canSlideUp()) {
                matrix[blankRow][blankCol] = matrix[blankRow - 1][blankCol];
                matrix[blankRow - 1][blankCol] = "";
                blankRow--;
            }
        }

        public void slideDown() {
            if (canSlideDown()) {
                matrix[blankRow][blankCol] = matrix[blankRow + 1][blankCol];
                matrix[blankRow + 1][blankCol] = "";
                blankRow++;
            }
        }

        public boolean canSlideRight() {
            return blankCol < cols - 1;
        }

        public boolean canSlideLeft() {
            return blankCol > 0;
        }

        public boolean canSlideUp() {
            return blankRow > 0;
        }

        public boolean canSlideDown() {
            return blankRow < rows - 1;
        }
    }

    // Clase para encapsular la solución del puzzle
    public static class PuzzleSolution {
        public Puzzle solutionPuzzle;
        public long runtimeMs;
        public List<String> solutionMoves;
        public int maxPuzzlesInMemory;

        public PuzzleSolution(Puzzle solutionPuzzle, long runtimeMs, List<String> solutionMoves, int maxPuzzlesInMemory) {
            this.solutionPuzzle = solutionPuzzle;
            this.runtimeMs = runtimeMs;
            this.solutionMoves = solutionMoves;
            this.maxPuzzlesInMemory = maxPuzzlesInMemory;
        }
    }

    // MÉTODO PRINCIPAL: adapta el algoritmo "strategi algorithm"
    // Se reciben dos matrices de strings: la configuración inicial y la meta.
    public static PuzzleSolution solvePuzzleStrategically(String[][] puzzleMatrix, String[][] goalPuzzleMatrix) {
        long startTime = System.currentTimeMillis();
        Puzzle puzzle = new Puzzle(puzzleMatrix);
        Puzzle goalPuzzle = new Puzzle(goalPuzzleMatrix);

        // Si el puzzle ya está solucionado, se retorna la solución sin movimientos.
        if (goalPuzzle.isEqualToPuzzle(puzzle)) {
            return new PuzzleSolution(puzzle, 0, new ArrayList<>(), 1);
        }

        List<String> solutionMoves = new ArrayList<>();
        puzzle.solutionMoves = solutionMoves;
        String[][] goalMatrix = goalPuzzle.matrix;
        Map<String, Position> goalMapping = Puzzle.getMatrixMapping(goalMatrix);

        // Se inicializan los límites del área no resuelta
        puzzle.topRowProgress = 0;
        puzzle.leftColProgress = 0;
        puzzle.botRowProgress = puzzle.rows - 1;
        puzzle.rightColProgress = puzzle.cols - 1;

        // Variables de progreso para resolver filas y columnas
        puzzle.rowInProgress = 0;
        puzzle.colInProgress = 0;
        puzzle.rowProgressCol = 0;
        puzzle.colProgressRow = 0;
        puzzle.solvingRowTopDown = true;
        puzzle.solvingColLeftRight = true;

        // Bucle principal: se continúa hasta que el puzzle sea igual al estado meta.
        while (!goalPuzzle.isEqualToPuzzle(puzzle)) {

            // Mientras haya más de 2 filas sin resolver y se tengan al menos tantas filas como columnas sin resolver:
            while (moreThanTwoUnsolvedRows(puzzle) && moreUnsolvedRowsThanCols(puzzle)) {
                puzzle.solvingRow = true;
                if (rowFinishedAndNotInGoalRow(goalPuzzle, puzzle)) {
                    if (puzzle.solvingRowTopDown) {
                        puzzle.topRowProgress++;
                        puzzle.rowInProgress = puzzle.topRowProgress;
                    } else {
                        puzzle.botRowProgress--;
                        puzzle.rowInProgress = puzzle.botRowProgress;
                    }
                    puzzle.rowProgressCol = 0;
                } else {
                    if (puzzle.solvingRowTopDown) {
                        if (puzzle.rowInProgress == goalPuzzle.blankRow) {
                            puzzle.solvingRowTopDown = false;
                            puzzle.rowInProgress = puzzle.botRowProgress;
                        } else {
                            puzzle.rowInProgress = puzzle.topRowProgress;
                        }
                    } else {
                        puzzle.rowInProgress = puzzle.botRowProgress;
                    }

                    int rowIteration = 0;
                    String targetValue = goalMatrix[puzzle.rowInProgress][puzzle.rowProgressCol];
                    while (!Puzzle.isRowEqual(goalPuzzle, puzzle, puzzle.rowInProgress)) {
                        if (rowIteration > 1) {
                            // Se ha llegado a un ciclo infinito; se puede optar por lanzar una excepción o retornar null
                            return null;
                        }

                        // Mientras no se esté en las dos últimas fichas de la fila
                        if (!targetValue.equals(goalMatrix[puzzle.rowInProgress][puzzle.rightColProgress - 1])) {
                            moveTile(puzzle, targetValue, goalMapping.get(targetValue).row, goalMapping.get(targetValue).col);
                            puzzle.rowProgressCol++;
                            targetValue = goalMatrix[puzzle.rowInProgress][puzzle.rowProgressCol];
                        } else {
                            // Caso especial: últimas dos fichas de la fila
                            String lastValue = goalMatrix[puzzle.rowInProgress][puzzle.rightColProgress];
                            if (puzzle.solvingRowTopDown) {
                                moveTile(puzzle, lastValue, goalMapping.get(lastValue).row + 2, goalMapping.get(lastValue).col);
                                moveTile(puzzle, targetValue, goalMapping.get(lastValue).row, goalMapping.get(lastValue).col);
                                moveTile(puzzle, lastValue, goalMapping.get(lastValue).row + 1, goalMapping.get(lastValue).col);

                                moveBlankToCol(puzzle, goalMapping.get(lastValue).col - 1);
                                moveBlankToRow(puzzle, goalMapping.get(lastValue).row);
                                puzzle.slideRight();
                                puzzle.slideDown();
                                solutionMoves.add("RIGHT");
                                solutionMoves.add("DOWN");

                                rowIteration++;
                                puzzle.rowProgressCol = 0;
                                targetValue = goalMatrix[puzzle.rowInProgress][puzzle.rowProgressCol];
                            } else {
                                moveTile(puzzle, lastValue, goalMapping.get(lastValue).row - 2, goalMapping.get(lastValue).col);
                                moveTile(puzzle, targetValue, goalMapping.get(lastValue).row, goalMapping.get(lastValue).col);
                                moveTile(puzzle, lastValue, goalMapping.get(lastValue).row - 1, goalMapping.get(lastValue).col);
                                moveBlankToCol(puzzle, goalMapping.get(lastValue).col - 1);
                                moveBlankToRow(puzzle, goalMapping.get(lastValue).row);
                                puzzle.slideRight();
                                puzzle.slideUp();
                                solutionMoves.add("RIGHT");
                                solutionMoves.add("UP");

                                rowIteration++;
                                puzzle.rowProgressCol = 0;
                                targetValue = goalMatrix[puzzle.rowInProgress][puzzle.rowProgressCol];
                            }
                        }
                    }
                }
            }

            // Resolver columnas cuando haya más columnas sin resolver que filas (y detenerse en un 2x2)
            while (moreThanTwoUnsolvedCols(puzzle) && moreUnsolvedColsThanRows(puzzle)) {
                puzzle.solvingRow = false;
                if (colFinishedAndNotInGoalCol(goalPuzzle, puzzle)) {
                    puzzle.colProgressRow = 0;
                    if (puzzle.solvingColLeftRight) {
                        puzzle.leftColProgress++;
                        puzzle.colInProgress = puzzle.leftColProgress;
                    } else {
                        puzzle.rightColProgress--;
                        puzzle.colInProgress = puzzle.rightColProgress;
                    }
                } else {
                    if (puzzle.solvingColLeftRight) {
                        if (puzzle.colInProgress == goalPuzzle.blankCol) {
                            puzzle.solvingColLeftRight = false;
                            puzzle.colInProgress = puzzle.rightColProgress;
                        } else {
                            puzzle.colInProgress = puzzle.leftColProgress;
                        }
                    } else {
                        puzzle.colInProgress = puzzle.rightColProgress;
                    }

                    int colIteration = 0;
                    String targetValue = goalMatrix[puzzle.topRowProgress][puzzle.colInProgress];
                    while (!Puzzle.isColEqual(goalPuzzle, puzzle, puzzle.colInProgress)) {
                        if (colIteration > 1) {
                            return null;
                        }

                        if (!targetValue.equals(goalMatrix[puzzle.botRowProgress - 1][puzzle.colInProgress])) {
                            moveTile(puzzle, targetValue, goalMapping.get(targetValue).row, goalMapping.get(targetValue).col);
                            puzzle.colProgressRow++;
                            targetValue = goalMatrix[puzzle.colProgressRow][puzzle.colInProgress];
                        } else {
                            String lastValue = goalMatrix[puzzle.botRowProgress][puzzle.colInProgress];
                            if (puzzle.solvingColLeftRight) {
                                moveTile(puzzle, lastValue, goalMapping.get(lastValue).row, goalMapping.get(lastValue).col + 2);
                                moveTile(puzzle, targetValue, goalMapping.get(lastValue).row, goalMapping.get(lastValue).col);
                                moveTile(puzzle, lastValue, goalMapping.get(lastValue).row, goalMapping.get(lastValue).col + 1);

                                moveBlankToRow(puzzle, goalMapping.get(lastValue).row - 1);
                                moveBlankToCol(puzzle, goalMapping.get(lastValue).col);
                                puzzle.slideDown();
                                puzzle.slideRight();
                                solutionMoves.add("DOWN");
                                solutionMoves.add("RIGHT");

                                colIteration++;
                                puzzle.colProgressRow = 0;
                                targetValue = goalMatrix[puzzle.colProgressRow][puzzle.colInProgress];
                            } else {
                                moveTile(puzzle, lastValue, goalMapping.get(lastValue).row, goalMapping.get(lastValue).col - 2);
                                moveTile(puzzle, targetValue, goalMapping.get(lastValue).row, goalMapping.get(lastValue).col);
                                moveTile(puzzle, lastValue, goalMapping.get(lastValue).row, goalMapping.get(lastValue).col - 1);
                                moveBlankToRow(puzzle, goalMapping.get(lastValue).row - 1);
                                moveBlankToCol(puzzle, goalMapping.get(lastValue).col);
                                puzzle.slideDown();
                                puzzle.slideLeft();
                                solutionMoves.add("DOWN");
                                solutionMoves.add("LEFT");

                                colIteration++;
                                puzzle.colProgressRow = 0;
                                targetValue = goalMatrix[puzzle.colProgressRow][puzzle.colInProgress];
                            }
                        }
                    }
                }
            }

            // Caso 2x2: se rota el espacio en blanco hasta llegar al estado meta,
            // alternando entre movimientos verticales y horizontales.
            if (unsolvedPuzzleIsTwoByTwo(puzzle)) {
                int iterations = 0;
                boolean slideVertically = true;
                while (!goalPuzzle.isEqualToPuzzle(puzzle)) {
                    if (slideVertically) {
                        if (puzzle.canSlideDown() && puzzle.blankRow - 1 <= goalPuzzle.blankRow - 1) {
                            puzzle.slideDown();
                            solutionMoves.add("DOWN");
                        } else {
                            puzzle.slideUp();
                            solutionMoves.add("UP");
                        }
                        slideVertically = false;
                    } else {
                        if (puzzle.canSlideRight() && puzzle.blankCol + 1 <= goalPuzzle.blankCol + 1) {
                            puzzle.slideRight();
                            solutionMoves.add("RIGHT");
                        } else {
                            puzzle.slideLeft();
                            solutionMoves.add("LEFT");
                        }
                        slideVertically = true;
                    }
                    iterations++;
                    if (iterations > 20) {
                        return null;
                    }
                }
            }
        }

        long endTime = System.currentTimeMillis();
        return new PuzzleSolution(puzzle, endTime - startTime, solutionMoves, 1);
    }

    // MÉTODOS AUXILIARES DEL ALGORITMO

    // Mueve la ficha con el valor dado hasta su posición meta
    private static void moveTile(Puzzle puzzle, String value, int goalRow, int goalCol) {
        Map<String, Position> matrixMapping = Puzzle.getMatrixMapping(puzzle.matrix);
        Position pos = matrixMapping.get(value);
        if (pos == null) return;
        int valueRow = pos.row;
        int valueCol = pos.col;
        if (valueRow == goalRow && valueCol == goalCol) {
            return;
        }
        // Se usan variables locales para “simular” la ficha en movimiento.
        int tileRow = valueRow;
        int tileCol = valueCol;
        if (puzzle.solvingRow) {
            while (tileCol > goalCol) {
                moveTileLeft(puzzle, value, tileRow, tileCol);
                tileCol--;
            }
            while (tileCol < goalCol) {
                moveTileRight(puzzle, value, tileRow, tileCol);
                tileCol++;
            }
            while (tileRow > goalRow) {
                moveTileUp(puzzle, value, tileRow, tileCol);
                tileRow--;
            }
            while (tileRow < goalRow) {
                moveTileDown(puzzle, value, tileRow, tileCol);
                tileRow++;
            }
        } else {
            while (tileRow > goalRow) {
                moveTileUp(puzzle, value, tileRow, tileCol);
                tileRow--;
            }
            while (tileRow < goalRow) {
                moveTileDown(puzzle, value, tileRow, tileCol);
                tileRow++;
            }
            while (tileCol > goalCol) {
                moveTileLeft(puzzle, value, tileRow, tileCol);
                tileCol--;
            }
            while (tileCol < goalCol) {
                moveTileRight(puzzle, value, tileRow, tileCol);
                tileCol++;
            }
        }
    }

    // Mueve la ficha hacia la izquierda (se pasan la posición actual y el valor)
    private static void moveTileLeft(Puzzle puzzle, String value, int tileRow, int tileCol) {
        if (puzzle.blankCol > tileCol && tileRow == puzzle.blankRow) {
            moveBlankUpOrDown(puzzle);
        }
        if (!puzzle.solvingRow && puzzle.solvingColLeftRight) {
            if (tileCol == puzzle.colInProgress + 1) {
                if (tileRow != puzzle.botRowProgress) {
                    if (puzzle.blankCol >= tileCol && puzzle.blankRow < tileRow) {
                        moveBlankToCol(puzzle, tileCol + 1);
                        moveBlankToRow(puzzle, tileRow + 1);
                    }
                } else {
                    moveBlankToRow(puzzle, tileRow - 1);
                    moveBlankToCol(puzzle, tileCol);
                }
            }
        }
        moveBlankToCol(puzzle, tileCol - 1);
        moveBlankToRow(puzzle, tileRow);
        puzzle.slideRight();
        puzzle.solutionMoves.add("RIGHT");
    }

    private static void moveTileRight(Puzzle puzzle, String value, int tileRow, int tileCol) {
        if (puzzle.blankCol < tileCol && tileRow == puzzle.blankRow) {
            moveBlankUpOrDown(puzzle);
        }
        if (puzzle.solvingRow) {
            if (puzzle.solvingRowTopDown) {
                if (puzzle.blankRow == puzzle.rowInProgress &&
                        !(puzzle.blankRow + 1 == tileRow && puzzle.blankCol == tileCol)) {
                    if (puzzle.canSlideDown()) {
                        puzzle.slideDown();
                        puzzle.solutionMoves.add("DOWN");
                    }
                }
            } else {
                if (puzzle.blankRow == puzzle.rowInProgress &&
                        !(puzzle.blankRow - 1 == tileRow && puzzle.blankCol == tileCol)) {
                    if (puzzle.canSlideUp()) {
                        puzzle.slideUp();
                        puzzle.solutionMoves.add("UP");
                    }
                }
            }
        } else {
            if (!puzzle.solvingColLeftRight) {
                if (tileCol == puzzle.colInProgress - 1) {
                    if (tileRow != puzzle.botRowProgress) {
                        if (puzzle.blankCol <= tileCol && puzzle.blankRow < tileRow) {
                            moveBlankToCol(puzzle, tileCol - 1);
                            moveBlankToRow(puzzle, tileRow + 1);
                        }
                    } else {
                        moveBlankToRow(puzzle, tileRow - 1);
                        moveBlankToCol(puzzle, tileCol);
                    }
                }
            }
        }
        moveBlankToCol(puzzle, tileCol + 1);
        moveBlankToRow(puzzle, tileRow);
        puzzle.slideLeft();
        puzzle.solutionMoves.add("LEFT");
    }

    private static void moveTileUp(Puzzle puzzle, String value, int tileRow, int tileCol) {
        if (puzzle.solvingRow && puzzle.solvingRowTopDown) {
            if (tileRow == puzzle.rowInProgress + 1) {
                if (tileCol != puzzle.rightColProgress) {
                    if (puzzle.blankCol <= tileCol && puzzle.blankRow >= tileRow) {
                        moveBlankToRow(puzzle, tileRow + 1);
                        moveBlankToCol(puzzle, tileCol + 1);
                    }
                } else {
                    moveBlankToCol(puzzle, tileCol - 1);
                    moveBlankToRow(puzzle, tileRow);
                }
            }
        }
        if (puzzle.blankRow > tileRow && puzzle.blankCol == tileCol) {
            moveBlankLeftOrRight(puzzle);
        }
        moveBlankToRow(puzzle, tileRow - 1);
        moveBlankToCol(puzzle, tileCol);
        puzzle.slideDown();
        puzzle.solutionMoves.add("DOWN");
    }

    private static void moveTileDown(Puzzle puzzle, String value, int tileRow, int tileCol) {
        if (!puzzle.solvingRow) {
            if (puzzle.solvingColLeftRight) {
                if (puzzle.blankCol == puzzle.colInProgress &&
                        !(puzzle.blankCol + 1 == tileCol && puzzle.blankRow == tileRow)) {
                    if (puzzle.canSlideRight()) {
                        puzzle.slideRight();
                        puzzle.solutionMoves.add("RIGHT");
                    }
                }
            } else {
                if (puzzle.blankCol == puzzle.colInProgress &&
                        !(puzzle.blankCol - 1 == tileCol && puzzle.blankRow == tileRow)) {
                    if (puzzle.canSlideLeft()) {
                        puzzle.slideLeft();
                        puzzle.solutionMoves.add("LEFT");
                    }
                }
            }
        }
        if (puzzle.solvingRow && !puzzle.solvingRowTopDown) {
            if (tileRow == puzzle.rowInProgress - 1) {
                if (tileCol != puzzle.rightColProgress) {
                    if (puzzle.blankCol <= tileCol && puzzle.blankRow <= tileRow) {
                        moveBlankToRow(puzzle, tileRow - 1);
                        moveBlankToCol(puzzle, tileCol + 1);
                    }
                } else {
                    moveBlankToCol(puzzle, tileCol - 1);
                    moveBlankToRow(puzzle, tileRow);
                }
            }
        }
        if (puzzle.blankRow < tileRow && puzzle.blankCol == tileCol) {
            moveBlankLeftOrRight(puzzle);
        }
        moveBlankToRow(puzzle, tileRow + 1);
        moveBlankToCol(puzzle, tileCol);
        puzzle.slideUp();
        puzzle.solutionMoves.add("UP");
    }

    // Mueve el espacio en blanco a la izquierda o a la derecha (según corresponda)
    private static void moveBlankLeftOrRight(Puzzle puzzle) {
        if (puzzle.blankCol == puzzle.rightColProgress) {
            puzzle.slideLeft();
            puzzle.solutionMoves.add("LEFT");
        } else if (puzzle.blankCol == puzzle.leftColProgress) {
            puzzle.slideRight();
            puzzle.solutionMoves.add("RIGHT");
        } else {
            if (puzzle.solvingColLeftRight) {
                puzzle.slideRight();
                puzzle.solutionMoves.add("RIGHT");
            } else {
                puzzle.slideLeft();
                puzzle.solutionMoves.add("LEFT");
            }
        }
    }

    // Mueve el espacio en blanco hacia arriba o abajo
    private static void moveBlankUpOrDown(Puzzle puzzle) {
        if (puzzle.blankRow == puzzle.topRowProgress) {
            puzzle.slideDown();
            puzzle.solutionMoves.add("DOWN");
        } else if (puzzle.blankRow == puzzle.botRowProgress) {
            puzzle.slideUp();
            puzzle.solutionMoves.add("UP");
        } else {
            if (puzzle.solvingRowTopDown) {
                puzzle.slideDown();
                puzzle.solutionMoves.add("DOWN");
            } else {
                puzzle.slideUp();
                puzzle.solutionMoves.add("UP");
            }
        }
    }

    // Mueve el espacio en blanco hasta la columna “targetCol”
    private static void moveBlankToCol(Puzzle puzzle, int targetCol) {
        while (puzzle.blankCol != targetCol) {
            if (puzzle.blankCol < targetCol) {
                if (puzzle.canSlideRight()) {
                    puzzle.slideRight();
                    puzzle.solutionMoves.add("RIGHT");
                } else {
                    break;
                }
            } else {
                if (puzzle.canSlideLeft()) {
                    puzzle.slideLeft();
                    puzzle.solutionMoves.add("LEFT");
                } else {
                    break;
                }
            }
        }
    }

    // Mueve el espacio en blanco hasta la fila “targetRow”
    private static void moveBlankToRow(Puzzle puzzle, int targetRow) {
        while (puzzle.blankRow != targetRow) {
            if (puzzle.blankRow < targetRow) {
                if (puzzle.canSlideDown()) {
                    puzzle.slideDown();
                    puzzle.solutionMoves.add("DOWN");
                } else {
                    break;
                }
            } else {
                if (puzzle.canSlideUp()) {
                    puzzle.slideUp();
                    puzzle.solutionMoves.add("UP");
                } else {
                    break;
                }
            }
        }
    }

    // MÉTODOS AUXILIARES para contar filas/columnas sin resolver

    private static boolean moreThanTwoUnsolvedRows(Puzzle puzzle) {
        return (puzzle.botRowProgress + 1 - puzzle.topRowProgress) > 2;
    }

    private static boolean moreThanTwoUnsolvedCols(Puzzle puzzle) {
        return (puzzle.rightColProgress + 1 - puzzle.leftColProgress) > 2;
    }

    private static boolean moreUnsolvedRowsThanCols(Puzzle puzzle) {
        return (puzzle.botRowProgress - puzzle.topRowProgress + 1) >= (puzzle.rightColProgress + 1 - puzzle.leftColProgress);
    }

    private static boolean moreUnsolvedColsThanRows(Puzzle puzzle) {
        return (puzzle.rightColProgress + 1 - puzzle.leftColProgress) > (puzzle.botRowProgress + 1 - puzzle.topRowProgress);
    }

    private static boolean colFinishedAndNotInGoalCol(Puzzle goalPuzzle, Puzzle puzzle) {
        return Puzzle.isColEqual(goalPuzzle, puzzle, puzzle.colInProgress) && puzzle.colInProgress != goalPuzzle.blankCol;
    }

    private static boolean rowFinishedAndNotInGoalRow(Puzzle goalPuzzle, Puzzle puzzle) {
        return Puzzle.isRowEqual(goalPuzzle, puzzle, puzzle.rowInProgress) && puzzle.rowInProgress != goalPuzzle.blankRow;
    }

    private static boolean unsolvedPuzzleIsTwoByTwo(Puzzle puzzle) {
        return (puzzle.botRowProgress + 1 - puzzle.topRowProgress == 2) &&
                (puzzle.rightColProgress + 1 - puzzle.leftColProgress == 2);
    }
}
