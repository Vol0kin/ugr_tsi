package practica_busqueda;

import java.util.ArrayList;
import java.util.HashSet;

// Clúster de gemas
// Guarda toda la información necesaria sobre el clúster

public class Cluster {
    private ArrayList<Observation> gems; // Gemas del clúster
    private int pathLength; // Longitud aproximada del camino que recorre todas las gemas del clúster
    private int numRocas; // Número de rocas que hay en el cuadrado (dado por la gema más arriba a la izquierda y abajo a la derecha)
    private int numMuros; // Número de muros que hay en el cuadrado
    private int numEnemigos; // Número de enemigos (de cualquier tipo) en el cuadrado
    private int dificultad; // Valor heurístico que representa la dificultad de coger todas las gemas (cuanto mayor sea, peor)
     
    public Cluster(){
        this.pathLength = 0;
        this.gems = new ArrayList();
    }
       
    public Observation getGem(int i){
        return gems.get(i);
    }
    
    public int getNumGems(){
        return gems.size();
    }
    
    public ArrayList<Observation> getGems(){
        return gems;
    }
    
    public int getPathLenght(){
        return pathLength;
    }
    
    public int getNumRocas(){
        return numRocas;
    }
    
    public int getNumMuros(){
        return numMuros;
    }
    
    public int getDificultad(){
        return dificultad;
    }
    
    public void addGem(Observation newGem){
        gems.add(newGem);
    }
    
    // Calcula la longitud del camino más corto que desde una gema pasa por todas las demás
    // Algoritmo (camino greedy): empiezo en la primera gema y calculo la dist Manhattan
    // a la gema más cercana y la sumo a la distancia total. Repito el proceso con la siguiente
    // gema sin poder volver a elegir una gema ya elegida y así hasta todas las gemas menos la última
    
    private void calculatePathLength(){
        if (gems.size() == 1)
            pathLength = 0;
        else{  
            pathLength = 0;
            
            int numGems = gems.size();
            int minDist;
            int indMinDist;
            int dist;
            Observation thisGem;
            HashSet<Integer> gemas_elegidas = new HashSet<>();

            gemas_elegidas.add(new Integer(0)); // Empiezo por la gema número 0
            thisGem = gems.get(0);
            
            for (int num_nodos = 1; num_nodos < numGems; num_nodos+=1){
                minDist = 1000;
                indMinDist = -1;
                
                for (int i = 0; i < numGems; i++){ // Veo la gema más cercana a thisGem que no haya sido elegida ya
                    if (!gemas_elegidas.contains(new Integer(i))){
                        dist = thisGem.getManhattanDistance(gems.get(i));

                        if (dist != 0 && dist < minDist){
                            minDist = dist;
                            indMinDist = i;
                        }
                    }
                }
                pathLength += minDist;
                
                thisGem = gems.get(indMinDist); // Vuelvo a repetir el proceso con la gema elegida ahora
                gemas_elegidas.add(new Integer(indMinDist));
            }
        }
    }
    
    // Calculo el número de rocas y muros en el cuadrado del clúster
    // El cuadrado del clúster viene dado por la gema superior izquierda y la inferior derecha
    // Se cuentan todas las gemas y muros dentro de ese cuadrado, inclusive las filas y columnas
    // de esas dos gemas mencionadas
    private void calculateNumRocasMurosyEnemigos(ArrayList<Observation> rocas, ArrayList<Observation> muros,
            ArrayList<Observation> bats, ArrayList<Observation> scorpions){
        if (gems.size() == 1){
            numRocas = numMuros = numEnemigos = 0;
        }
        else{
            numRocas = numMuros = numEnemigos = 0;
            
            // Calculo el cuadrado -> calculo x_min, x_max, y_min, y_max
            int x_min = 1000, x_max = -1, y_min = 1000, y_max = -1;
            int this_x, this_y;
            int numGems = gems.size();
            
            for (int i = 0; i < numGems; i++){ // Recorro las gemas
                this_x = gems.get(i).getX();
                this_y = gems.get(i).getY();
                
                if (this_x < x_min){
                    x_min = this_x;
                }
                if (this_x > x_max){ // No se puede poner else if!!
                    x_max = this_x;
                }
                
                if (this_y < y_min){
                    y_min = this_y;
                }
                if (this_y > y_max){
                    y_max = this_y;
                }
            }
            
            // Veo el número de muros, rocas y enemigos que hay en ese cuadrado
            int x_obs, y_obs;
            
            for (Observation roca : rocas){ // Rocas
                x_obs = roca.getX();
                y_obs = roca.getY();
                
                if (x_obs >= x_min && x_obs <= x_max && y_obs >= y_min && y_obs <= y_max) // Veo si está dentro del cuadrado
                    numRocas += 1;
            }
            
            for (Observation muro : muros){ // Muros
                x_obs = muro.getX();
                y_obs = muro.getY();
                
                if (x_obs >= x_min && x_obs <= x_max && y_obs >= y_min && y_obs <= y_max)
                    numMuros += 1;
            }
            
            for (Observation bat : bats){ // Murciélagos
                x_obs = bat.getX();
                y_obs = bat.getY();
                
                if (x_obs >= x_min && x_obs <= x_max && y_obs >= y_min && y_obs <= y_max)
                    numEnemigos += 1;
            }
            
            for (Observation scorpion : scorpions){ // Escorpiones
                x_obs = scorpion.getX();
                y_obs = scorpion.getY();
                
                if (x_obs >= x_min && x_obs <= x_max && y_obs >= y_min && y_obs <= y_max)
                    numEnemigos += 1;
            }
        }
    }
    
    // Calcula un valor heurístico que representa la dificultad de obtener las gemas del clúster
    // Cuanto más alto sea peor (más difícil/más se tardará en coger todas las gemas)
    // Tiene en cuenta el pathLength, numRocas y numMuros
    // Heurística: valor = pathLength + numMuros*0.5 + numRocas + numEnemigos*10, redondeado hacia arriba
    
    // Se encarga de llamar a los métodos calculatePathLength y calculateNumRocasyMuros
    public void calcularDificultad(ArrayList<Observation> rocas, ArrayList<Observation> muros,
            ArrayList<Observation> bats, ArrayList<Observation> scorpions){
        this.calculatePathLength();
        this.calculateNumRocasMurosyEnemigos(rocas, muros, bats, scorpions);
        
        float valor;
        
        valor = (float) (pathLength + numMuros*0.5 + numRocas + numEnemigos*10);
        
        dificultad = (int)Math.ceil(valor);
    }

    // Elimina las gemas del clúster que no están en remaining_gems
    public void removeCapturedGems(ArrayList<Observation> remaining_gems){
        boolean gema_encontrada;
        Observation gem_cluster;
        int num_gems_cluster = gems.size();
        ArrayList<Observation> gemas_a_eliminar = new ArrayList<>();
        
        for (int i = 0; i < num_gems_cluster; i++){
            gem_cluster = gems.get(i);
            gema_encontrada = false;
            
            for (Observation gem_mapa : remaining_gems){
                if (gem_cluster.getX() == gem_mapa.getX() && gem_cluster.getY() == gem_mapa.getY())
                    gema_encontrada = true;
            }
            
            if (!gema_encontrada) // Si no se ha encontrado la gema, ya no existe -> la elimino
                gemas_a_eliminar.add(gem_cluster);
        }
        
        for (Observation this_gem : gemas_a_eliminar) // Elimino las gemas que ya no existen
            gems.remove(this_gem);
    }
}
