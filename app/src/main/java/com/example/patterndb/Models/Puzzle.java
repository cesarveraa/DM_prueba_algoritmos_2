package com.example.patterndb.Models;

import java.util.Arrays;
import java.util.Set;

public class Puzzle implements Cloneable {
    public int boardSize;
    public int[][] board;
    public int blankRow, blankCol;

    // Definimos las direcciones: {fila, columna}
    public static final int[][] DIRECTIONS = { {1,0}, {-1,0}, {0,1}, {0,-1} };

    public Puzzle(int boardSize) {
        this.boardSize = boardSize;
        board = new int[boardSize][boardSize];
        // Inicializa el puzzle en orden (solucionado)
        for (int i = 0; i < boardSize; i++){
            for (int j = 0; j < boardSize; j++){
                board[i][j] = i * boardSize + j + 1;
            }
        }
        // El 0 representa la celda vacía (última posición)
        blankRow = boardSize - 1;
        blankCol = boardSize - 1;
        board[blankRow][blankCol] = 0;
    }

    /**
     * Intenta mover la celda según la dirección dada.
     * @param dir Vector de dirección {dx, dy}
     * @return true si se pudo mover; false en caso contrario.
     */
    public boolean move(int[] dir) {
        int newRow = blankRow + dir[0];
        int newCol = blankCol + dir[1];
        if (newRow < 0 || newRow >= boardSize || newCol < 0 || newCol >= boardSize)
            return false;
        board[blankRow][blankCol] = board[newRow][newCol];
        board[newRow][newCol] = 0;
        blankRow = newRow;
        blankCol = newCol;
        return true;
    }

    /**
     * Comprueba si el puzzle está resuelto.
     */
    public boolean checkWin() {
        for (int i = 0; i < boardSize; i++){
            for (int j = 0; j < boardSize; j++){
                // La última celda es la vacía
                if (i == boardSize - 1 && j == boardSize - 1)
                    continue;
                if (board[i][j] != i * boardSize + j + 1)
                    return false;
            }
        }
        return true;
    }

    /**
     * Genera un hash para el estado del puzzle usando los números del grupo.
     * Se concatena la posición de cada número en el grupo.
     */
    public String hash(Set<Integer> group) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < boardSize; i++){
            for (int j = 0; j < boardSize; j++){
                int tile = board[i][j];
                if (group.contains(tile))
                    sb.append(i).append(j);
                else
                    sb.append("x"); // O se omite, según tu lógica
            }
        }
        // En este ejemplo se remueven las "x" para emular el comportamiento del código Python
        return sb.toString().replace("x", "");
    }

    @Override
    public Puzzle clone() {
        Puzzle copy = new Puzzle(this.boardSize);
        copy.blankRow = this.blankRow;
        copy.blankCol = this.blankCol;
        for (int i = 0; i < boardSize; i++){
            copy.board[i] = Arrays.copyOf(this.board[i], boardSize);
        }
        return copy;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Puzzle))
            return false;
        Puzzle other = (Puzzle) o;
        if (this.boardSize != other.boardSize)
            return false;
        for (int i = 0; i < boardSize; i++){
            if (!Arrays.equals(this.board[i], other.board[i]))
                return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return Arrays.deepHashCode(board);
    }
}
