// patterndb_solver.cpp

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
#include <pthread.h>
#include "include/nlohmann/json.hpp"  // Usando nlohmann::json
#include <android/asset_manager.h>
#include <android/asset_manager_jni.h>

using json = nlohmann::json;
using namespace std;

const int INF = 100000;
AAssetManager* g_assetManager = nullptr;

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
                if (i == boardSize - 1 && j == boardSize - 1)
                    continue;
                if (board[i][j] != i * boardSize + j + 1)
                    return false;
            }
        }
        return true;
    }

    // Mueve la celda vacía en la dirección (dx, dy)
    bool move(int dx, int dy) {
        int newRow = blankRow + dx;
        int newCol = blankCol + dy;
        if (newRow < 0 || newRow >= boardSize || newCol < 0 || newCol >= boardSize)
            return false;
        board[blankRow][blankCol] = board[newRow][newCol];
        board[newRow][newCol] = 0;
        blankRow = newRow;
        blankCol = newCol;
        return true;
    }

    // Simula un movimiento y retorna (bool, Puzzle)
    pair<bool, Puzzle> simulateMove(const pair<int,int>& dir) const {
        Puzzle sim(*this);
        bool valid = sim.move(dir.first, dir.second);
        return make_pair(valid, sim);
    }

    // Genera un hash del estado considerando sólo las piezas en "group"
    string hash(const unordered_set<int>& group) const {
        ostringstream oss;
        for (int i = 0; i < boardSize; i++){
            for (int j = 0; j < boardSize; j++){
                int tile = board[i][j];
                if (group.find(tile) != group.end()){
                    oss << i << j;  // Suponiendo boardSize < 10
                }
            }
        }
        return oss.str();
    }

    // Representa el estado en forma de cadena
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

// Función para cargar la PatternDB desde un archivo JSON en assets
bool loadPatternDB(const string& filename, int boardSize) {
    if(g_assetManager == nullptr) {
        return false;
    }
    AAsset* asset = AAssetManager_open(g_assetManager, filename.c_str(), AASSET_MODE_STREAMING);
    if (asset == nullptr) {
        return false;
    }
    size_t size = AAsset_getLength(asset);
    string jsonStr;
    jsonStr.resize(size);
    int bytesRead = AAsset_read(asset, &jsonStr[0], size);
    AAsset_close(asset);
    if(bytesRead <= 0) {
        return false;
    }
    try {
        json j = json::parse(jsonStr);
        g_groups.clear();
        g_patternDbDict.clear();
        for (auto& grp : j["groups"]) {
            unordered_set<int> group;
            for (auto& num : grp) {
                group.insert(num.get<int>());
            }
            g_groups.push_back(group);
        }
        for (auto& obj : j["patternDbDict"]) {
            unordered_map<string, int> dict;
            for (auto it = obj.begin(); it != obj.end(); ++it) {
                dict[it.key()] = it.value().get<int>();
            }
            g_patternDbDict.push_back(dict);
        }
    } catch (...) {
        return false;
    }
    return true;
}

// ------------------------------------------------------
// Funciones heurísticas
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
    for (size_t i = 0; i < g_groups.size(); i++){
        const auto &group = g_groups[i];
        string hashStr = puzzle.hash(group);
        const auto &dict = g_patternDbDict[i];
        auto it = dict.find(hashStr);
        if (it != dict.end()){
            h += it->second;
        } else {
            h += manhattan(puzzle, group);
        }
    }
    return h;
}

// ------------------------------------------------------
// Estructura para simular un nodo en la búsqueda iterativa
// ------------------------------------------------------
struct Node {
    Puzzle state;
    int g; // costo acumulado (profundidad)
    int dirIndex; // índice del siguiente movimiento a probar
    vector<pair<int,int>> moves; // camino de movimientos

    // Constructor para inicializar todos los campos
    Node(const Puzzle &state, int g, int dirIndex, const vector<pair<int,int>> &moves)
            : state(state), g(g), dirIndex(dirIndex), moves(moves) {}
};

// ------------------------------------------------------
// Función iterativa IDA* usando un stack en el heap
// ------------------------------------------------------
vector<pair<int,int>> iterativeIDAStar(const Puzzle &initial) {
    int bound = hScore(initial);
    while (true) {
        vector<Node> stack;
        Node root(initial, 0, 0, {});
        root.state = initial;
        root.g = 0;
        root.dirIndex = 0;
        root.moves = {};
        stack.push_back(root);

        int newBound = INF;
        bool found = false;
        vector<pair<int,int>> solutionMoves;

        while (!stack.empty() && !found) {
            Node &top = stack.back();
            int f = top.g + hScore(top.state);
            if (f > bound) {
                newBound = min(newBound, f);
                stack.pop_back();
                continue;
            }
            if (top.state.checkWin()) {
                found = true;
                solutionMoves = top.moves;
                break;
            }
            if (top.dirIndex >= (int)Puzzle::DIRECTIONS.size()) {
                stack.pop_back();
                continue;
            }
            pair<int,int> dir = Puzzle::DIRECTIONS[top.dirIndex];
            top.dirIndex++;  // Incrementa para probar el siguiente movimiento en futuras iteraciones

            // Evitar revertir el último movimiento
            if (!top.moves.empty()) {
                pair<int,int> last = top.moves.back();
                if (dir.first == -last.first && dir.second == -last.second)
                    continue;
            }
            auto sim = top.state.simulateMove(dir);
            if (!sim.first)
                continue;
            Node child(initial, 0, 0, {});
            child.state = sim.second;
            child.g = top.g + 1;
            child.dirIndex = 0;
            child.moves = top.moves;
            child.moves.push_back(dir);
            stack.push_back(child);
        }
        if (found) {
            return solutionMoves;
        }
        if (newBound == INF)
            return vector<pair<int,int>>(); // No se encontró solución
        bound = newBound;
    }
}

// ------------------------------------------------------
// Reconstruye el camino de estados (cada matriz) a partir de los movimientos
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
// Función JNI auxiliar: Ejecuta la solución en un hilo con mayor stack
// ------------------------------------------------------
struct SolveData {
    string input;
    string result;
};

void* solveThread(void* arg) {
    SolveData* data = (SolveData*) arg;
    // Parsear la cadena, crear Puzzle, cargar PatternDB y resolver
    // Parsear la entrada en una matriz 4x4
    vector<vector<int>> matrix;
    istringstream iss(data->input);
    string rowStr;
    while(getline(iss, rowStr, ';')) {
        istringstream rowStream(rowStr);
        vector<int> row;
        int val;
        while(rowStream >> val) {
            row.push_back(val);
        }
        matrix.push_back(row);
    }
    if(matrix.size() != 4 || matrix[0].size() != 4) {
        data->result = "Error: La matriz debe ser 4x4.";
        return nullptr;
    }
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
    if (!loadPatternDB("patternDb_4.json", 4)) {
        data->result = "Error al cargar PatternDB.";
        return nullptr;
    }
    auto moves = iterativeIDAStar(puzzle);
    vector<string> pathStates = reconstructPath(puzzle, moves);
    ostringstream oss;
    for (size_t i = 0; i < pathStates.size(); i++) {
        oss << "Paso " << i << ":\n" << pathStates[i] << "\n";
    }
    data->result = oss.str();
    return nullptr;
}

// ------------------------------------------------------
// Función JNI para resolver el puzzle (se ejecuta en un hilo con mayor stack)
// Recibe un jstring con la matriz y retorna un jstring con el camino de solución.
// ------------------------------------------------------
extern "C"
JNIEXPORT jstring JNICALL
Java_com_example_patterndb_NativeSolver_NativeSolver_solvePuzzle(JNIEnv* env, jobject thiz, jstring puzzleStr) {
    const char* cPuzzleStr = env->GetStringUTFChars(puzzleStr, nullptr);
    string input(cPuzzleStr);
    env->ReleaseStringUTFChars(puzzleStr, cPuzzleStr);

    SolveData data;
    data.input = input;

    pthread_t thread;
    pthread_attr_t attr;
    pthread_attr_init(&attr);
    size_t stackSize = 16 * 1024 * 1024; // 16 MB de stack
    pthread_attr_setstacksize(&attr, stackSize);

    int ret = pthread_create(&thread, &attr, solveThread, &data);
    pthread_attr_destroy(&attr);
    if(ret != 0) {
        return env->NewStringUTF("Error al crear el hilo.");
    }
    pthread_join(thread, nullptr);
    return env->NewStringUTF(data.result.c_str());
}

// ------------------------------------------------------
// Función JNI para configurar el AssetManager (únicamente)
// ------------------------------------------------------
extern "C"
JNIEXPORT void JNICALL
Java_com_example_patterndb_NativeSolver_NativeSolver_setAssetManager(JNIEnv* env, jobject thiz, jobject assetManagerObj) {
g_assetManager = AAssetManager_fromJava(env, assetManagerObj);
}
