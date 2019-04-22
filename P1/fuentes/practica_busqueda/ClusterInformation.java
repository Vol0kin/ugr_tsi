package practica_busqueda;

// Clase que guarda toda la información de los clústeres.

import java.util.ArrayList;

// Guarda:
// > Los clústeres realizados, con las gemas de cada uno
// > La matriz de distancias entre clústeres
// > La "dificultad" de cada clúster, según la distancia entre gemas y las piedras
// > El camino actual entre los clústeres (cuáles se cogen y en qué orden) 

public class ClusterInformation {
    public ArrayList<Cluster> clusters;
    public int[][] matriz_dist;
    public ArrayList<Integer> circuito; // circuito.get(i) es el índice de la ciudad nº "i" a visitar
    public ArrayList<Integer> nodos_circuito; // Sucesión de enteros que representan los nodos(gemas) que unen a los clústeres
  
    public int getNumClusters(){
        return clusters.size();
    }
    
    // Obtiene las gemas del clúster número i del circuito
    
    public ArrayList<Observation> getGemsCircuitCluster(int i){
        return clusters.get(circuito.get(i)).getGems();
    }
    
    // <Clústerización> -> Tarda alrededor de 0.08 ms
    // Uso el algoritmo DBSCAN para clasificar las gemas en clústeres (grupos)
    // Aquellas gemas clasificadas como ruido las añado a un clúster en el que solo hay una gema
    // epsilon: radio (en distancia Manhattan) en el que se buscan gemas para el mismo clúster
    // gems -> lista de gemas del mapa
    // Este método también llama al método de calcular la dicultad para cada clúster
    public void createClusters(int epsilon, ArrayList<Observation> gems, ArrayList<Observation> rocas, ArrayList<Observation> muros,
        ArrayList<Observation> bats, ArrayList<Observation> scorpions){
        
        int gems_size = gems.size();
        
        int[] ind_cluster = new int[gems_size]; // Indice del clúster al que pertenece la gema -> -1 si no pertenece a ningún clúster todavía
        
        for (int i = 0; i < ind_cluster.length; i++)
            ind_cluster[i] = -1;
       
        int num_cluster = -1;
        Observation this_gem;
        
        for (int i = 0; i < gems_size; i++){
            this_gem = gems.get(i);
            
            if (ind_cluster[i] == -1){ // No pertenece a ningún clúster -> creo un nuevo clúster y la añado
                // Antes de crear un nuevo clúster veo si está lo suficientemente cerca de uno existente. Si es así, la añado a ese clúster
                boolean ya_asignada = false;
                
                for (int j = 0; j < gems_size; j++){
                    if(ind_cluster[j] != -1)
                        if (this_gem.getManhattanDistance(gems.get(j)) <= epsilon){
                            ind_cluster[i] = ind_cluster[j];
                            ya_asignada = true;
                        }
                }
                
                if (!ya_asignada){
                    num_cluster++;
                    ind_cluster[i] = num_cluster; // La gema pertenece a ese clúster
                }
            }
            // Añado al clúster de la gema las gemas del vecindario
            
            for (int j = 0; j < gems_size; j++){
                
                if (ind_cluster[j] == -1) // Veo si esa gema no era de ningún clúster todavía
                    if (this_gem.getManhattanDistance(gems.get(j)) <= epsilon) // Esa gema es del vecindario -> la añado al clúster
                        ind_cluster[j] = ind_cluster[i];
            }
        }
        
        clusters = new ArrayList();
        
        for (int i = 0; i <= num_cluster; i++)
            clusters.add(new Cluster());
        
        // Añado cada gema a su clúster correspondiente
        for (int i = 0; i < gems_size; i++)
            clusters.get(ind_cluster[i]).addGem(gems.get(i));
    
        // Calculo la dificultad de cada cluster (ese método ya calcula el pathlength y num rocas y muros)
        for (int i = 0; i <= num_cluster; i++)
            clusters.get(i).calcularDificultad(rocas, muros, bats, scorpions);
    }
    
    // Este método se tiene que llamar después de createClusters.
    // Crea un circuito como si fuera un viajante de comercio
    // El primer nodo es la posición inicial del jugador (que se le pasa al método)
    // y el último nodo es la salida (que también se le pasa como parámetro)
    // Elige un camino a través de los clústeres de forma que el número de gemas
    // total sea NEEDED_GEMS o más y que sea el tour más corto posible
    // Para medir la distancia entre clústeres se usa la matriz de distancias
    // (que usa el método getHeuristicDistance)
    // > Parámetros distClusterStart y distClusterGoal: v[i] es la distancia del
    // clúster "i" con el punto de inicio o de fin, respectivamente
    // Algoritmo: usa un algoritmo B&B

    public void createCircuit(int xStart, int yStart, int xGoal, int yGoal, int[] distClusterStart, int[] distClusterGoal, int needed_gems){
        int num_clusters = clusters.size();
        
        if (num_clusters == 1){ // Si solo hay un clúster, no tiene sentido usar el algoritmo
            circuito = new ArrayList<>();
            circuito.add(0);
        }
        else{   
            int[] sol_act = new int[num_clusters];
            int pos_act = 0; // Indice de sol_act -> sol_act[pos_act]
            int num_gems_sol = 0; // Número de gemas total de los clústeres en sol_act
            int dist_act = 0; // Distancia total de sol_act
            int dist_mejor_sol = 100000; // Distancia total de la mejor solución encontrada hasta la fecha
            int[] mejor_sol = new int[num_clusters];
            int[] num_gems_cluster = new int[num_clusters]; // Guarda el número de gemas que tiene cada clúster
            int[] dificultad_cluster = new int[num_clusters]; // Guarda el valor heurístico de dificultad de cada clúster
            boolean cluster_repetido;
            int dist_sumada;

            for (int i = 0; i < num_clusters; i++){
                sol_act[i] = -1;
                mejor_sol[i] = -1;
                num_gems_cluster[i] = clusters.get(i).getNumGems();
                dificultad_cluster[i] = clusters.get(i).getDificultad();
            }

            boolean busqueda_terminada = false;

            while (!busqueda_terminada){
                if (sol_act[pos_act] != -1){ // Si ya había escogido un clúster para esta posición, resto lo que había sumado antes
                    num_gems_sol -= num_gems_cluster[sol_act[pos_act]];
                    
                    if (pos_act == 0){ // Si es el primer clúster del camino, le sumo la distancia del origen a ese clúster y la dificultad del clúster escogido
                        dist_act -= distClusterStart[sol_act[pos_act]] + dificultad_cluster[sol_act[pos_act]];
                    }
                    else{
                        dist_act -= matriz_dist[sol_act[pos_act-1]][sol_act[pos_act]] + dificultad_cluster[sol_act[pos_act]]; // En otro caso, le sumo la distancia entre este clúster elegido y el anteriormente elegido y la dificultad del clúster escogido
                    }
                }
                sol_act[pos_act]++; // Escojo el siguiente clúster

                // Escojo el primer clúster que no hubiera sido escogido ya
                do{
                    cluster_repetido = false;

                    for (int i = 0; i < pos_act && !cluster_repetido; i++){
                        if (sol_act[i] == sol_act[pos_act])
                            cluster_repetido = true;
                    }

                    if (cluster_repetido)
                        sol_act[pos_act]++; // Ya estaba escogido ese clúster: cojo el siguiente
                } while(cluster_repetido);

                // La solución cumple las restricciones (clústeres no repetidos)

                // Veo si ya he agotado todos los posibles clústeres para esta posición -> hago backtracking
                // o si pos_act = 0 termina el algoritmo y devuelvo la mejor solución
                if (sol_act[pos_act] >= num_clusters){   
                    if (pos_act == 0){ // Ya se ha agotado todo el espacio de búsqueda -> termino el algoritmo
                        busqueda_terminada = true;
                    }
                    else{ // Solo se han agotado las posibilidades para esta posición -> hago backtracking
                        sol_act[pos_act] = -1;
                        pos_act--;
                    }
                }
                else{
                    // Añado la contribución del clúster escogido a la solución:
                    // le sumo la distancia y el número de gemas
                    num_gems_sol += num_gems_cluster[sol_act[pos_act]]; // Sumo las gemas

                    // Sumo la distancia
                    if (pos_act == 0){ // Si es el primer clúster del camino, le sumo la distancia del origen a ese clúster y la dificultad del clúster escogido
                        dist_sumada = distClusterStart[sol_act[pos_act]] + dificultad_cluster[sol_act[pos_act]];
                    }
                    else{
                        dist_sumada = matriz_dist[sol_act[pos_act-1]][sol_act[pos_act]] + dificultad_cluster[sol_act[pos_act]]; // En otro caso, le sumo la distancia entre este clúster elegido y el anteriormente elegido y la dificultad del clúster escogido
                    }
                    dist_act += dist_sumada;

                    // Compruebo si la solución puede ser mejor que la encontrada hasta la fecha

                    if (dist_act >= dist_mejor_sol){ // Si se cumple, ya no tiene sentido seguir -> se desecha la solución (se hace backtracking)

                        if (pos_act != 0){
                            dist_act -= dist_sumada;
                            num_gems_sol -= num_gems_cluster[sol_act[pos_act]];
                            sol_act[pos_act] = -1;
                            pos_act--;
                        }
                    }
                    else{
                        
                        // Compruebo si he conseguido un número suficiente de gemas para abandonar el nivel
                        if (num_gems_sol >= needed_gems){ // Es solución final -> veo si al sumarle la distancia hasta la casilla final sigue siendo la mejor solución
                            
                            if (dist_act + distClusterGoal[sol_act[pos_act]] < dist_mejor_sol){
                                int j;
                                for (j = 0; j <= pos_act; j++) // Copio la solución
                                    mejor_sol[j] = sol_act[j];
                                
                                for (j = pos_act+1; j < num_clusters; j++) // Pongo el resto de elementos a -1
                                    mejor_sol[j]=-1;
                                
                                dist_mejor_sol = dist_act + distClusterGoal[sol_act[pos_act]]; // Le añado la distancia del último clúster a la salida
                            }
                            // Hago backtracking (no tiene sentido añadir más clústeres a esta solución)
                            if (pos_act != 0){
                                dist_act -= dist_sumada;
                                num_gems_sol -= num_gems_cluster[sol_act[pos_act]];
                                sol_act[pos_act] = -1;
                                pos_act--;
                            }
                            // Si pos_act es 0, no hay que hacer backtracking, sino solo pasar al siguiente clúster en la misma posición
                            
                        }
                        else{
                            
                            // No es solución final -> sigo añadiendo clústeres MAAAL
                            if (pos_act + 1 < sol_act.length)
                                pos_act++;

                        }
                        
                    }

                }
            }
  
            // Guardo la mejor solución encontrada por el algoritmo en el atributo circuito      
            circuito = new ArrayList<>();
            
            for (int i = 0; i < num_clusters; i++)
                if (mejor_sol[i] != -1){
                    circuito.add(new Integer(mejor_sol[i]));
                }
        }
    }

    // Método que mira si se ha cogido alguna gema que antes pertenecía al clúster
    // Si ese es el caso (hay una gema que no está en remaining_gems) la borra
    // del clúster.
    // NO vuelve a calcular su dificultad, distancia, etc.
    // Ind_clúster es el índice de clusters, no de los clusters del circuito!
    
    public void removeCapturedGems(int ind_cluster, ArrayList<Observation> remaining_gems){
        if (ind_cluster < clusters.size())
            clusters.get(ind_cluster).removeCapturedGems(remaining_gems);
    }
}
