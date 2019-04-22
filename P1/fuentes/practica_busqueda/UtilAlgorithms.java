package practica_busqueda;

import java.util.ArrayList;

// Interfaz que contiene algunos metodos utiles para las busquedas

public interface UtilAlgorithms {

    // Metodo para inicializar un mapa de booleanos a partir de un ArrayList
    static void initMap(boolean[][] map, ArrayList<Observation> information, int xSize, int ySize) {

        for (int x = 0; x < xSize; x++) {
            for (int y = 0; y < ySize; y++) {
                map[x][y] = false;
            }
        }

        for (Observation info: information) {
            map[info.getX()][info.getY()] = true;
        }
    }

    // Metodo para copiar una matriz 2D en otra
    static void copy2DArray(boolean[][] source, boolean[][] destination, int xSize, int ySize) {

        for (int x = 0; x < xSize; x++) {
            for (int y = 0; y < ySize; y++) {
                destination[x][y] = source[x][y];
            }
        }
    }

    // Metodo para simular la caida de rocas al principio del turno
    static void simulateBoulderFall(ArrayList<Observation> boulders, boolean[][] boulderMap, boolean[][] groundMap,
                                    boolean[][] gemsMap, ArrayList<Observation>[][] grid) {
        ArrayList<Observation> fallingBoulders = findFallingBoulders(boulders, grid);


        for (Observation boulder: fallingBoulders) {
            int x = boulder.getX(), y = boulder.getY();

            int numberBoulders = 0;
            int boulderPos = y;
            int emptyPos = y + 1;

            // Find out number of boulders above the current grid
            // and the index of the highest grid containing a boulder
            while (boulderMap[x][boulderPos] && !grid[x][boulderPos].get(0).getType().equals(ObservationType.WALL)) {
                numberBoulders++;
                boulderPos--;
            }

            // Find out the index of the last empty space
            while (!groundMap[x][emptyPos] &&
                    (!grid[x][emptyPos].get(0).getType().equals(ObservationType.WALL) && !gemsMap[x][emptyPos] && !boulderMap[x][emptyPos])) {
                emptyPos++;
            }

            // Modify the boulder map, moving the boulders
            for (int j = emptyPos - 1; j > boulderPos; j--) {
                if (j > emptyPos - 1 - numberBoulders) {
                    boulderMap[x][j] = true;
                } else {
                    boulderMap[x][j] = false;
                }
            }
        }
    }

    // Metodo para encontrar las rocas que pueden caer
    static ArrayList<Observation> findFallingBoulders(ArrayList<Observation> boulders, ArrayList<Observation>[][] grid) {
        ArrayList<Observation> fallingBoudlers = new ArrayList<>();

        for (Observation boulder: boulders) {

            if (grid[boulder.getX()][boulder.getY() + 1].get(0).getType().equals(ObservationType.EMPTY)) {
                fallingBoudlers.add(boulder);
            }
        }


        return fallingBoudlers;
    }
}
