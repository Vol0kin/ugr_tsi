package practica_busqueda;

import ontology.Types;

import java.util.LinkedList;
import java.util.ArrayList;

/**
 * Clase que guarda la informacion relacionada a un plan
 * @author vladislav
 */
class PathInformation {
    LinkedList<Types.ACTIONS> plan;
    ArrayList<Observation> listaCasillas;
    int distancia;
    double probabilidadEnemigos;
    boolean existsPath;
    boolean searchComplete;
    boolean[][] boulderMap;
    boolean[][] groundMap;
    
    PathInformation() {
        plan = new LinkedList<>();
        listaCasillas = new ArrayList<>();
        distancia = 0;
        probabilidadEnemigos = 0.0;
        existsPath = false;
        searchComplete = false;
        boulderMap = null;
        groundMap = null;
    }
}
