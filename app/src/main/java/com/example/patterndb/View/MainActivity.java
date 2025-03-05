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

import com.example.patterndb.NativeSolver.NativeSolver;
import com.example.patterndb.R;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

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
