package com.example.patterndb.View;

import android.content.res.AssetManager;
import android.os.Bundle;
import android.widget.Chronometer;
import android.os.SystemClock;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.patterndb.Api.ApiClient;
import com.example.patterndb.NativeSolver.NativeSolver;
import com.example.patterndb.R;
import com.example.patterndb.human_strategic.PuzzleSolver;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    public String error;  // Agregar campo para mensajes de error

    private GridLayout gridPuzzle;
    private Button btnAddRow, btnAddColumn, btnSolve;
    private Chronometer chronometer;
    private TextView tvSteps;

    // Dimensiones iniciales de la grilla
    private int numRows = 3;
    private int numCols = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        gridPuzzle = findViewById(R.id.gridPuzzle);
        btnAddRow = findViewById(R.id.btnAddRow);
        btnAddColumn = findViewById(R.id.btnAddColumn);
        btnSolve = findViewById(R.id.btnSolve);
        chronometer = findViewById(R.id.chronometer);
        tvSteps = findViewById(R.id.tvSteps);

        // Inicializa la grilla con dimensiones iniciales
        initializeGrid(null);

        btnAddRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                List<List<String>> currentValues = getGridValues();
                numRows++;
                initializeGrid(currentValues);
            }
        });

        btnAddColumn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                List<List<String>> currentValues = getGridValues();
                numCols++;
                initializeGrid(currentValues);
            }
        });

        btnSolve.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Obtiene la matriz ingresada en la UI
                String[][] puzzleMatrix = getPuzzleMatrix();

                // Validación: ninguna celda debe estar vacía
                if (isMatrixEmpty(puzzleMatrix)) {
                    Toast.makeText(MainActivity.this, "Completa el puzzle", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Verifica que el puzzle sea 4x4, ya que la función nativa lo espera
                /*
                if (numRows != 4 || numCols != 4) {
                    Toast.makeText(MainActivity.this, "El algoritmo nativo solo soporta puzzles 4x4.", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Formatea la matriz al string esperado: cada fila separada por ';' y números por espacios
                StringBuilder inputBuilder = new StringBuilder();
                for (int i = 0; i < 4; i++) {
                    for (int j = 0; j < 4; j++) {
                        inputBuilder.append(puzzleMatrix[i][j]);
                        if (j < 3) {
                            inputBuilder.append(" ");
                        }
                    }
                    if (i < 3) {
                        inputBuilder.append(";");
                    }
                }
                String inputMatrix = inputBuilder.toString();

                // Inicia el cronómetro


                // Llama al método nativo implementado en C++/NDK
                NativeSolver solver = new NativeSolver();
                AssetManager assetManager = getAssets();
                solver.setAssetManager(assetManager);
                chronometer.setBase(SystemClock.elapsedRealtime());
                chronometer.start();
                String solutionPath = solver.solvePuzzle(inputMatrix);

                // Detiene el cronómetro
                chronometer.stop();
                long elapsedMillis = SystemClock.elapsedRealtime() - chronometer.getBase();
                double elapsedSeconds = elapsedMillis / 1000.0;

                String result = solutionPath + "\nTiempo: " + elapsedSeconds + " segundos";
                tvSteps.setText(result);
                )
                 */
                solveWithServer(puzzleMatrix);
                // Convierte los "0" en "" para que el algoritmo lo entienda como espacio en blanco
                String[][] convertedMatrix = convertZerosToEmpty(puzzleMatrix);

                solveWithLocalAlgorithm(convertedMatrix);


            }
        });
    }

    /**
     * Inicializa o reconstruye la grilla de EditTexts.
     *
     * @param existingValues Valores previos para conservar (puede ser null).
     */
    private void initializeGrid(List<List<String>> existingValues) {
        gridPuzzle.removeAllViews();
        gridPuzzle.setColumnCount(numCols);
        gridPuzzle.setRowCount(numRows);

        for (int i = 0; i < numRows; i++) {
            for (int j = 0; j < numCols; j++) {
                EditText et = new EditText(this);
                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                params.width = 150;
                params.height = 150;
                params.setMargins(8, 8, 8, 8);
                params.rowSpec = GridLayout.spec(i);
                params.columnSpec = GridLayout.spec(j);
                et.setLayoutParams(params);
                et.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                et.setInputType(InputType.TYPE_CLASS_NUMBER);
                String text = "";
                if (existingValues != null && i < existingValues.size() && j < existingValues.get(i).size()) {
                    text = existingValues.get(i).get(j);
                }
                et.setText(text);
                gridPuzzle.addView(et);
            }
        }
    }
    private String[][] convertZerosToEmpty(String[][] matrix) {
        String[][] converted = new String[matrix.length][matrix[0].length];
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[i].length; j++) {
                if (matrix[i][j].equals("0")) {
                    converted[i][j] = "";
                } else {
                    converted[i][j] = matrix[i][j];
                }
            }
        }
        return converted;
    }
    // Dentro de la clase MainActivity, agrega este método nuevo:
    private void solveWithServer(String[][] puzzleMatrix) {
        // Verificar que sea 4x4
        if (numRows != 4 || numCols != 4) {
            Toast.makeText(this, "El servidor solo soporta puzzles 4x4", Toast.LENGTH_SHORT).show();
            return;
        }

        // Conversión a matriz de enteros (existente)
        int[][] matrix = new int[4][4];
        try {
            for (int i = 0; i < 4; i++) {
                for (int j = 0; j < 4; j++) {
                    int num = Integer.parseInt(puzzleMatrix[i][j]);
                    if (num < 0 || num > 15) throw new NumberFormatException();
                    matrix[i][j] = num;
                }
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Valores deben ser 0-15", Toast.LENGTH_SHORT).show();
            return;
        }

        chronometer.setBase(SystemClock.elapsedRealtime());
        chronometer.start();

        ApiClient client = ApiClient.getClient();
        Call<ApiClient.SolutionResponse> call = client.solvePuzzle(new ApiClient.PuzzleRequest(matrix));

        call.enqueue(new Callback<ApiClient.SolutionResponse>() {
            @Override
            public void onResponse(Call<ApiClient.SolutionResponse> call, Response<ApiClient.SolutionResponse> response) {
                chronometer.stop();
                long elapsedMillis = SystemClock.elapsedRealtime() - chronometer.getBase();
                double elapsedSeconds = elapsedMillis / 1000.0;

                if (response.isSuccessful() && response.body() != null) {
                    List<int[][]> solution = response.body().solution;
                    String error = response.body().error;

                    if (error != null && !error.isEmpty()) {
                        tvSteps.setText("Error: " + error);
                        return;
                    }

                    if (solution == null || solution.isEmpty()) {
                        tvSteps.setText("El puzzle ya está resuelto!");
                        return;
                    }

                    // Construir cadena con todos los pasos
                    StringBuilder solutionText = new StringBuilder();
                    solutionText.append("Pasos: ").append(solution.size())
                            .append("\nTiempo: ").append(elapsedSeconds).append(" segundos\n\n");

                    // Dentro de solveWithServer
                    for (int step = 0; step < solution.size(); step++) {
                        solutionText.append("Paso ").append(step + 1).append(":\n");
                        int[][] board = solution.get(step);

                        for (int i = 0; i < 4; i++) {
                            for (int j = 0; j < 4; j++) {
                                solutionText.append(String.format("%2d ", board[i][j]));
                            }
                            solutionText.append("\n");
                        }
                        solutionText.append("\n");
                    }

                    tvSteps.setText(solutionText.toString());

                } else {
                    tvSteps.setText("Error del servidor: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<ApiClient.SolutionResponse> call, Throwable t) {
                chronometer.stop();
                tvSteps.setText("Error de conexión: " + t.getMessage());
            }
        });
    }
    private void solveWithLocalAlgorithm(String[][] puzzleMatrix) {
        // Se elimina la validación de 4x4 para soportar cualquier tamaño
        int rows = puzzleMatrix.length;
        int cols = puzzleMatrix[0].length;

        // Construir la matriz meta de forma dinámica: números de 1 a (rows*cols - 1) y el último espacio es blanco ("")
        String[][] goalMatrix = new String[rows][cols];
        int num = 1;
        for (int i = 0; i < rows; i++){
            for (int j = 0; j < cols; j++){
                if (i == rows - 1 && j == cols - 1) {
                    goalMatrix[i][j] = "";
                } else {
                    goalMatrix[i][j] = String.valueOf(num++);
                }
            }
        }

        // Inicia el cronómetro
        chronometer.setBase(SystemClock.elapsedRealtime());
        chronometer.start();

        // Llama al algoritmo local
        PuzzleSolver.PuzzleSolution solution = PuzzleSolver.solvePuzzleStrategically(puzzleMatrix, goalMatrix);

        // Detiene el cronómetro
        chronometer.stop();
        long elapsedMillis = SystemClock.elapsedRealtime() - chronometer.getBase();
        double elapsedSeconds = elapsedMillis / 1000.0;

        if (solution == null) {
            tvSteps.setText("Error en la solución del puzzle.");
            return;
        }

        // Simula los movimientos para obtener los estados intermedios
        List<String[][]> states = getIntermediateStates(puzzleMatrix, solution.solutionMoves);

        // Construye la salida: muestra el número de pasos, tiempo y cada estado (en formato de matriz)
        StringBuilder solutionText = new StringBuilder();
        solutionText.append("Movimientos: ").append(solution.solutionMoves.size())
                .append("\nTiempo: ").append(elapsedSeconds).append(" segundos\n\n");
        for (int step = 0; step < states.size(); step++) {
            solutionText.append("Paso ").append(step).append(":\n");
            solutionText.append(formatMatrix(states.get(step)));
            solutionText.append("\n");
        }
        tvSteps.setText(solutionText.toString());
    }

    /**
     * Crea una copia de la matriz.
     */
    private String[][] copyMatrix(String[][] matrix) {
        int rows = matrix.length;
        int cols = matrix[0].length;
        String[][] copy = new String[rows][cols];
        for (int i = 0; i < rows; i++){
            System.arraycopy(matrix[i], 0, copy[i], 0, cols);
        }
        return copy;
    }

    /**
     * Formatea una matriz en un String para mostrarla.
     */
    private String formatMatrix(String[][] matrix) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < matrix.length; i++){
            for (int j = 0; j < matrix[i].length; j++){
                // Si la celda es el espacio en blanco se muestra un espacio en blanco
                sb.append(String.format("%4s", matrix[i][j].isEmpty() ? " " : matrix[i][j]));
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * Simula la secuencia de movimientos generada por el algoritmo.
     * A partir del estado inicial, se aplica cada movimiento y se guarda el estado resultante.
     */
    private List<String[][]> getIntermediateStates(String[][] initialMatrix, List<String> moves) {
        List<String[][]> states = new ArrayList<>();
        // Se crea un nuevo Puzzle (la clase Puzzle del solver copia la matriz internamente)
        PuzzleSolver.Puzzle currentPuzzle = new PuzzleSolver.Puzzle(initialMatrix);
        // Guarda el estado inicial
        states.add(copyMatrix(currentPuzzle.matrix));
        for (String move : moves) {
            switch (move) {
                case "RIGHT":
                    currentPuzzle.slideRight();
                    break;
                case "LEFT":
                    currentPuzzle.slideLeft();
                    break;
                case "UP":
                    currentPuzzle.slideUp();
                    break;
                case "DOWN":
                    currentPuzzle.slideDown();
                    break;
            }
            states.add(copyMatrix(currentPuzzle.matrix));
        }
        return states;
    }

    /**
     * Recorre la grilla y obtiene una matriz de valores (lista de listas).
     */
    private List<List<String>> getGridValues() {
        List<List<String>> matrix = new ArrayList<>();
        for (int i = 0; i < numRows; i++) {
            List<String> row = new ArrayList<>();
            for (int j = 0; j < numCols; j++) {
                int index = i * numCols + j;
                EditText et = (EditText) gridPuzzle.getChildAt(index);
                row.add(et.getText().toString());
            }
            matrix.add(row);
        }
        return matrix;
    }

    /**
     * Recorre la grilla y obtiene una matriz bidimensional de Strings.
     */
    private String[][] getPuzzleMatrix() {
        String[][] matrix = new String[numRows][numCols];
        for (int i = 0; i < numRows; i++){
            for (int j = 0; j < numCols; j++){
                int index = i * numCols + j;
                EditText et = (EditText) gridPuzzle.getChildAt(index);
                matrix[i][j] = et.getText().toString().trim();
            }
        }
        return matrix;
    }

    /**
     * Verifica si alguna celda de la matriz está vacía.
     */
    private boolean isMatrixEmpty(String[][] matrix) {
        for (int i = 0; i < matrix.length; i++){
            for (int j = 0; j < matrix[i].length; j++){
                if (matrix[i][j].isEmpty()){
                    return true;
                }
            }
        }
        return false;
    }
}
