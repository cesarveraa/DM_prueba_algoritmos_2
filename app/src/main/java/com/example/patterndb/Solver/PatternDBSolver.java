package com.example.patterndb.Solver;

import android.content.Context;
import android.util.JsonReader;
import android.util.Log;

import com.example.patterndb.Models.Puzzle;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Esta clase implementa el algoritmo IDA* con PatternDB para puzzles 4x4.
 */
public class PatternDBSolver {


    public static ArrayList<Set<Integer>> groups = new ArrayList<>();
    public static ArrayList<HashMap<String, Integer>> patternDbDict = new ArrayList<>();
    public static final int INF = 100000;

    public static void init(Context context, int boardSize) {
        try {
            InputStream is = context.getAssets().open("patternDb_" + boardSize + ".json");
            JsonReader reader = new JsonReader(new InputStreamReader(is, "UTF-8"));

            // Se espera un objeto raíz con dos arrays: "groups" y "patternDbDict"
            reader.beginObject();
            while (reader.hasNext()) {
                String name = reader.nextName();
                if (name.equals("groups")) {
                    groups.clear();
                    reader.beginArray();
                    while (reader.hasNext()) {
                        // Cada grupo es un array de enteros
                        HashSet<Integer> group = new HashSet<>();
                        reader.beginArray();
                        while (reader.hasNext()) {
                            group.add(reader.nextInt());
                        }
                        reader.endArray();
                        groups.add(group);
                    }
                    reader.endArray();
                } else if (name.equals("patternDbDict")) {
                    patternDbDict.clear();
                    reader.beginArray();
                    while (reader.hasNext()) {
                        // Cada diccionario es un objeto JSON
                        HashMap<String, Integer> map = new HashMap<>();
                        reader.beginObject();
                        while (reader.hasNext()) {
                            String key = reader.nextName();
                            int value = reader.nextInt();
                            map.put(key, value);
                        }
                        reader.endObject();
                        patternDbDict.add(map);
                    }
                    reader.endArray();
                } else {
                    reader.skipValue();
                }
            }
            reader.endObject();
            reader.close();
        } catch (Exception e) {
            Log.e("PatternDBSolver", "Error al cargar PatternDB: " + e.getMessage());
        }
    }


    /**
     * Ejecuta el algoritmo IDA*.
     * @param puzzle Estado inicial del puzzle.
     * @return Lista de direcciones (cada una como int[2]) para llegar a la solución.
     */
    public static List<int[]> idaStar(Puzzle puzzle) {
        if (puzzle.checkWin()) return new ArrayList<>();
        if (patternDbDict.isEmpty()) {
            // En este ejemplo se asume que init() se llamó al iniciar la app
            Log.e("PatternDBSolver", "PatternDB no inicializado.");
            return null;
        }

        long startTime = System.nanoTime();
        int bound = hScore(puzzle);
        List<Puzzle> path = new ArrayList<>();
        path.add(puzzle);
        List<int[]> dirs = new ArrayList<>();

        while (true) {
            int t = search(path, 0, bound, dirs);
            if (t == -1) { // -1 indica que se encontró solución
                long deltaTime = System.nanoTime() - startTime;
                Log.d("PatternDBSolver", "Solución encontrada en " + (deltaTime / 1e9)
                        + " segundos con " + dirs.size() + " movimientos");
                return dirs;
            } else if (t == INF) {
                return null;
            }
            bound = t;
        }
    }

    private static int search(List<Puzzle> path, int g, int bound, List<int[]> dirs) {
        Puzzle cur = path.get(path.size() - 1);
        int f = g + hScore(cur);
        if (f > bound) return f;
        if (cur.checkWin()) return -1;
        int min = INF;

        for (int[] dir : Puzzle.DIRECTIONS) {
            // Evitar movimientos inversos
            if (!dirs.isEmpty()) {
                int[] lastDir = dirs.get(dirs.size()-1);
                if (dir[0] == -lastDir[0] && dir[1] == -lastDir[1])
                    continue;
            }
            Puzzle simPuzzle = cur.clone();
            boolean validMove = simPuzzle.move(dir);
            if (!validMove || path.contains(simPuzzle)) continue;
            path.add(simPuzzle);
            dirs.add(dir);
            int t = search(path, g + 1, bound, dirs);
            if (t == -1) return -1;
            if (t < min) min = t;
            path.remove(path.size()-1);
            dirs.remove(dirs.size()-1);
        }
        return min;
    }

    /**
     * Calcula la heurística usando el PatternDB.
     */
    private static int hScore(Puzzle puzzle) {
        int h = 0;
        for (int i = 0; i < groups.size(); i++) {
            Set<Integer> group = groups.get(i);
            String hashString = puzzle.hash(group);
            HashMap<String, Integer> dict = patternDbDict.get(i);
            if (dict.containsKey(hashString)) {
                h += dict.get(hashString);
            } else {
                Log.d("PatternDBSolver", "No se encontró patrón en DB, usando Manhattan");
                // Cálculo de Manhattan como respaldo
                for (int r = 0; r < puzzle.boardSize; r++) {
                    for (int c = 0; c < puzzle.boardSize; c++) {
                        int tile = puzzle.board[r][c];
                        if (tile != 0 && group.contains(tile)) {
                            int destRow = (tile - 1) / puzzle.boardSize;
                            int destCol = (tile - 1) % puzzle.boardSize;
                            h += Math.abs(destRow - r) + Math.abs(destCol - c);
                        }
                    }
                }
            }
        }
        return h;
    }
}
