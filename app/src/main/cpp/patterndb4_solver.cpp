#include <jni.h>
#include <string>
#include <vector>
#include <sstream>
#include <fstream>
#include <cstdlib>
#include <cmath>
#include <limits>
#include <chrono>
#include <unordered_set>
#include <unordered_map>
#include "json.hpp"  // Usando nlohmann::json

using json = nlohmann::json;
using namespace std;

const int INF = 100000;

// ------------------------------------------------------
// Clase Puzzle
// ------------------------------------------------------
class Puzzle {
public:
    int boardSize;
    vector<vector<int>> board;
    int blankRow, blankCol;

    // Direcciones: abajo, arriba, derecha, izquierda
    static const vector<pair<int,int>> DIRECTIONS;

    Puzzle(int boardSize) : boardSize(boardSize) {
        board.resize(boardSize, vector<int>(boardSize));
        for (int i = 0; i < boardSize; i++){
            for (int j = 0; j < boardSize; j++){
                board[i][j] = i * boardSize + j + 1;
            }
        }
        blankRow = boardSize - 1;
        blankCol = boardSize - 1;
        board[blankRow][blankCol] = 0;
    }

    // Constructor copia
    Puzzle(const Puzzle &other) {
        boardSize = other.boardSize;
        board = other.board;
        blankRow = other.blankRow;
        blankCol = other.blankCol;
    }

    Puzzle& operator=(const Puzzle &other) {
        if (this != &other) {
            boardSize = other.boardSize;
            board = other.board;
            blankRow = other.blankRow;
            blankCol = other.blankCol;
        }
        return *this;
    }

    // Comprueba si el puzzle está resuelto
    bool checkWin() const {
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

    // Mueve la celda vacía en la dirección (dx, dy)
    // Retorna true si se pudo mover, false en caso contrario.
    bool move(int dx, int dy) {
        int newRow = blankRow + dx;
        int newCol = blankCol + dy;
        if (newRow < 0 || newRow >= boardSize || newCol < 0 || newCol >= boardSize)
            return false;
        // Intercambiar la celda vacía con la adyacente
        board[blankRow][blankCol] = board[newRow][newCol];
        board[newRow][newCol] = 0;
        blankRow = newRow;
        blankCol = newCol;
        return true;
    }

    // Simula un movimiento: retorna un par (bool, Puzzle)
    // donde el bool indica si el movimiento es válido, y Puzzle es el estado resultante.
    pair<bool, Puzzle> simulateMove(const pair<int,int>& dir) const {
        Puzzle sim(*this);
        bool valid = sim.move(dir.first, dir.second);
        return make_pair(valid, sim);
    }

    // Retorna una cadena hash del estado considerando solo las piezas en "group".
    // Se concatena la fila y columna de cada pieza en el grupo.
    string hash(const unordered_set<int>& group) const {
        ostringstream oss;
        for (int i = 0; i < boardSize; i++){
            for (int j = 0; j < boardSize; j++){
                int tile = board[i][j];
                if (group.find(tile) != group.end()){
                    oss << i << j;  // Asume boardSize < 10 para simplicidad
                }
            }
        }
        return oss.str();
    }

    // Representa el estado del puzzle en una cadena (cada fila separada por salto de línea)
    string toString() const {
        ostringstream oss;
        for (int i = 0; i < boardSize; i++){
            for (int j = 0; j < boardSize; j++){
                oss << board[i][j] << "\t";
            }
            oss << "\n";
        }
        return oss.str();
    }
};

const vector<pair<int,int>> Puzzle::DIRECTIONS = { {1,0}, {-1,0}, {0,1}, {0,-1} };

// ------------------------------------------------------
// Global PatternDB variables
// ------------------------------------------------------
vector<unordered_set<int>> g_groups;
vector<unordered_map<string, int>> g_patternDbDict;

// Función para cargar la PatternDB desde un archivo JSON
// Se espera un JSON con dos campos: "groups" (array de arrays de int)
// y "patternDbDict" (array de objetos: cada clave es un string y valor un int)
bool loadPatternDB(const string& filename, int boardSize) {
    ifstream inFile(filename);
    if (!inFile.is_open()) {
        return false;
    }
    json j;
    inFile >> j;
    inFile.close();

    g_groups.clear();
    g_patternDbDict.clear();

    // Cargar grupos: convertir cada array a un unordered_set<int>
    for (auto& grp : j["groups"]) {
        unordered_set<int> group;
        for (auto& num : grp) {
            group.insert(num.get<int>());
        }
        g_groups.push_back(group);
    }
    // Cargar patternDbDict: cada objeto se convierte en unordered_map<string,int>
    for (auto& obj : j["patternDbDict"]) {
        unordered_map<string, int> dict;
        for (auto it = obj.begin(); it != obj.end(); ++it) {
            dict[it.key()] = it.value().get<int>();
        }
        g_patternDbDict.push_back(dict);
    }
    return true;
}

// ------------------------------------------------------
// Heurística: usa PatternDB si se encuentra la huella, sino Manhattan
// ------------------------------------------------------
int manhattan(const Puzzle &puzzle, const unordered_set<int>& group) {
    int h = 0;
    for (int i = 0; i < puzzle.boardSize; i++){
        for (int j = 0; j < puzzle.boardSize; j++){
            int tile = puzzle.board[i][j];
            if (tile != 0 && group.find(tile) != group.end()){
                int destRow = (tile - 1) / puzzle.boardSize;
                int destCol = (tile - 1) % puzzle.boardSize;
                h += abs(destRow - i) + abs(destCol - j);
            }
        }
    }
    return h;
}

int hScore(const Puzzle &puzzle) {
    int h = 0;
    // Para cada grupo en la PatternDB
    for (size_t i = 0; i < g_groups.size(); i++){
        const auto &group = g_groups[i];
        string hashStr = puzzle.hash(group);
        const auto &dict = g_patternDbDict[i];
        auto it = dict.find(hashStr);
        if (it != dict.end()){
            h += it->second;
        } else {
            // Si no se encuentra en la DB, usar Manhattan
            h += manhattan(puzzle, group);
        }
    }
    return h;
}

// ------------------------------------------------------
// Función recursiva search (IDA*) – similar a la versión Python
// ------------------------------------------------------
int search(vector<Puzzle>& path, int g, int bound, vector<pair<int,int>>& dirs) {
    Puzzle &cur = path.back();
    int f = g + hScore(cur);
    if (f > bound)
        return f;
    if (cur.checkWin())
        return -1; // Señal de solución encontrada
    int min_val = INF;
    for (const auto &dir : Puzzle::DIRECTIONS) {
        // Evitar retroceder el último movimiento
        if (!dirs.empty()){
            auto last = dirs.back();
            if (dir.first == -last.first && dir.second == -last.second)
                continue;
        }
        // Simular movimiento
        auto sim = cur.simulateMove(dir);
        if (!sim.first)
            continue;
        if (false) { 
            // Aquí podrías verificar si el estado ya está en el camino para evitar ciclos.
            // Para simplicidad, se omite (pero en una implementación robusta se haría)
        }
        path.push_back(sim.second);
        dirs.push_back(dir);
        int t = search(path, g + 1, bound, dirs);
        if (t == -1)
            return -1;
        if (t < min_val)
            min_val = t;
        path.pop_back();
        dirs.pop_back();
    }
    return min_val;
}

vector<pair<int,int>> idaStar(Puzzle puzzle) {
    if (puzzle.checkWin())
        return vector<pair<int,int>>(); // Ya resuelto
    int bound = hScore(puzzle);
    vector<Puzzle> path;
    path.push_back(puzzle);
    vector<pair<int,int>> dirs;
    while (true) {
        int t = search(path, 0, bound, dirs);
        if (t == -1)
            return dirs; // Secuencia de movimientos óptima encontrada
        if (t == INF)
            return vector<pair<int,int>>(); // No se encontró solución
        bound = t;
    }
}

// ------------------------------------------------------
// Función para reconstruir el camino de estados (cada matriz)
// ------------------------------------------------------
vector<string> reconstructPath(const Puzzle &initial, const vector<pair<int,int>> &moves) {
    vector<string> states;
    Puzzle temp = initial;
    states.push_back(temp.toString());
    for (auto move : moves) {
        temp.move(move.first, move.second);
        states.push_back(temp.toString());
    }
    return states;
}

// ------------------------------------------------------
// Función JNI que recibe la matriz (en string) y retorna el camino (en string)
// Formato de entrada: "1 2 3 4;5 6 7 8;9 10 11 12;13 14 15 0"
// Cada fila separada por ';' y números separados por espacios.
// ------------------------------------------------------
extern "C"
JNIEXPORT jstring JNICALL
Java_com_example_patterndb_NativeSolver_solvePuzzle(JNIEnv* env, jobject /* this */, jstring puzzleStr) {
    // Convertir jstring a std::string
    const char* puzzleCStr = env->GetStringUTFChars(puzzleStr, nullptr);
    string input(puzzleCStr);
    env->ReleaseStringUTFChars(puzzleStr, puzzleCStr);

    // Parsear la cadena para obtener una matriz 4x4
    vector<vector<int>> matrix;
    istringstream iss(input);
    string rowStr;
    while(getline(iss, rowStr, ';')) {
        istringstream rowStream(rowStr);
        vector<int> row;
        int val;
        while (rowStream >> val) {
            row.push_back(val);
        }
        matrix.push_back(row);
    }
    if(matrix.size() != 4 || matrix[0].size() != 4) {
        string error = "Error: La matriz debe ser 4x4.";
        return env->NewStringUTF(error.c_str());
    }

    // Crear el objeto Puzzle a partir de la matriz
    Puzzle puzzle(4);
    for (int i = 0; i < 4; i++){
        for (int j = 0; j < 4; j++){
            puzzle.board[i][j] = matrix[i][j];
            if(matrix[i][j] == 0){
                puzzle.blankRow = i;
                puzzle.blankCol = j;
            }
        }
    }

    // Cargar la PatternDB desde el archivo JSON (ajusta la ruta según tu despliegue)
    // Por ejemplo, suponiendo que el archivo se encuentra en el directorio de trabajo:
    if (!loadPatternDB("patternDb_4.json", 4)) {
        string err = "Error al cargar PatternDB.";
        return env->NewStringUTF(err.c_str());
    }

    // Ejecutar el algoritmo IDA*
    auto moves = idaStar(puzzle);

    // Reconstruir el camino de estados
    vector<string> pathStates = reconstructPath(puzzle, moves);

    // Construir una cadena con el camino: cada paso numerado
    ostringstream result;
    for (size_t i = 0; i < pathStates.size(); i++) {
        result << "Paso " << i << ":\n" << pathStates[i] << "\n";
    }
    string resultStr = result.str();
    return env->NewStringUTF(resultStr.c_str());
}
