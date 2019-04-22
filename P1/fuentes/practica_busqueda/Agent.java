package practica_busqueda;

// Agente de prueba. Plantilla para un agente de este juego.

import core.game.StateObservation;
import ontology.Types;
import tools.ElapsedCpuTimer;
import tools.Vector2d;


import java.util.ArrayList;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.HashSet;

import tools.pathfinder.*;

public class Agent extends BaseAgent{

    PathInformation informacionPlan;
    private SearchInformation searchInfo;
    
    int it = 0;
    
    private PathFinder pf;
    private boolean stop;
    
    private HashMap<ArrayList<Observation>, Integer> mapaCircuitos; // Al iniciar
    
    private ClusterInformation clusterInf;
    
    // Información extra para la planificación
    
    private int sig_cluster = 0; // Indice del cluster actual a coger del circuito
    
    private boolean en_cluster = false; // Vale false si estamos yendo hacia el siguiente clúster y true si ya hemos llegado y estamos cogiendo sus gemas
    
    // Variables para guardar los parámetros del A* cuando se le llama a lo largo de varios turnos
    private int x_search;
    private int y_search;
    private ArrayList<Observation> gems_search;
    
    private LinkedList<Types.ACTIONS> plan_no_morir; // Plan para guardar las acciones a ejecutar para evitar morir -> si no está vacío siempre se ejecutan sus acciones
    private boolean hay_que_replanificar = false; // Vale true cuando se ha ejecutando/está ejecutando el plan_no_morir -> cuando se haya terminado de ejecutar ese plan, se replanifica con el clúster actual (si tiene gemas)
    
    private boolean abandonando_nivel = false; // Vale true cuando se esté ejecutando el plan para salir del nivel, tras conseguir 9 gemas o más
    
    private int it_ultimo_movimiento = 0; // Dice en qué iteración se hizo el último movimiento (desplazamiento de una casilla a otra)
    
    private int last_x, last_y; // Posición x e y del jugador en el turno anterior
    
    private ArrayList<Observation> gemas_no_accesibles; // Array de aquellas gemas de clústers a los que no se ha conseguido llegar
    
    private boolean ignorar_cas_prob_clusters = false; // Vale true cuando no es posible llegar a ningún clúster sin pasar a través de un enemigo
    
    private boolean ignorar_cas_prob_salida = false; // Vale true cuando no se encuentra un camino hacia la salida sin pasar a través de un enemigo
    
    private boolean en_bucle_enemigos = false; // Se pone a true cuando hemos ejecutado 2 veces seguidas la misma acción para evitar enemigos y estando en la misma casilla
    
    private HashSet<Observation> casillas_huir_enemigos = new HashSet<>(); // Casillas donde se encontraba el jugador cuando tuvo que huir de los enemigos
    
    private boolean bucle_2_abandonar_nivel = false;
    private Observation casilla_prob_bucle_2;
    
    public Agent(StateObservation so, ElapsedCpuTimer elapsedTimer){
        super(so, elapsedTimer);
        
        // Uso el pathFinder de GVG-AI para encontrar los caminos entre todas
        // las casillas evitando los muros (el resto de casillas supongo que
        // son atravesables)
        
        ArrayList<core.game.Observation>[] obstaculos = so.getImmovablePositions();
        
        ArrayList<Integer> tiposObs = new ArrayList<Integer>();
        
        for (ArrayList<core.game.Observation> obs : obstaculos ){
            tiposObs.add(obs.get(0).obsID);
        }
        
        tiposObs . add (( int ) 'o' ) ;
        
        pf = new PathFinder ( tiposObs ) ;
        
        pf.VERBOSE = false;
        
        pf.run(so);
        stop = false;
        searchInfo = new SearchInformation();
        
        plan_no_morir = new LinkedList<>();
        
        mapaCircuitos = new HashMap<ArrayList<Observation>, Integer>(); // Creo el mapa que usa getHeuristicGems
        
        clusterInf = new ClusterInformation(); // Creo la información de los clústeres
        
        PlayerObservation jugador = this.getPlayer(so);
        last_x = jugador.getX();
        last_y = jugador.getY();

        gemas_no_accesibles = new ArrayList<>();
        
        // Primera iteración
                
        Observation salida = this.getExit(so);
        ArrayList<Observation> casillas_prohibidas = this.getCasillasProhibidas(so);
                
        // planifico para acercarme al primer clúster     
            clusterInf.createClusters(3, this.getGemsList(so),
                    this.getBouldersList(so), this.getWallsList(so),
                    this.getBatsList(so), this.getScorpionsList(so)); // Creo los clusters
            
            this.saveClustersDistances(clusterInf); // Guardo la matriz de distancias
            this.saveCircuit(clusterInf, jugador.getX(),
            jugador.getY(), salida.getX(),
            salida.getY(), this.getRemainingGems(so)); // Creo el camino a través de los clústeres
            
            
            gems_search = clusterInf.getGemsCircuitCluster(0); // Cojo las gemas del clúster 1 del circuito
            
            int[] casilla_search;
            
            if (1 < clusterInf.circuito.size()) // El circuito tiene al menos dos clusters
                casilla_search = this.getPuntoIntermedioClusters(gems_search,
                    clusterInf.getGemsCircuitCluster(1), so);
            
            else // Si el circuito tiene solo un cluster
                casilla_search = this.getPuntoIntermedioClusterSalida(gems_search,
                    this.getExit(so), so);
            
            x_search = casilla_search[0];
            y_search = casilla_search[1]; // --> Se puede escoger como punto final una gema!!          
            
            if (x_search != -1 && y_search != -1) // Veo si existe una casilla intermedia válida entre este clúster y el siguiente
                informacionPlan = pathExplorer(x_search, y_search,
                                         so, gems_search,
                                         elapsedTimer, 1, casillas_prohibidas);
            else
                informacionPlan = pathExplorer(so, gems_search,
                                         elapsedTimer, 1, casillas_prohibidas);
    }
    
    @Override
    public Types.ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
        PlayerObservation jugador = this.getPlayer(stateObs);
        Observation salida = this.getExit(stateObs);
        Types.ACTIONS accion = Types.ACTIONS.ACTION_NIL; // Acción que se va a ejecutar este turno
        
        // Obtengo este turno las casillas prohibidas (cada turno los enemigos pueden cambiar de posición)
        ArrayList<Observation> casillas_prohibidas;
        ArrayList<Observation> casillas_prohibidas_exit;
        
        if (!ignorar_cas_prob_clusters)
            casillas_prohibidas = this.getCasillasProhibidas(stateObs);
        else
            casillas_prohibidas = new ArrayList<>();
        
        if (!ignorar_cas_prob_salida)
            casillas_prohibidas_exit = this.getCasillasProhibidas(stateObs, salida);
        else
            casillas_prohibidas_exit = new ArrayList<>();
        
        // Compruebo si al aplicar la última acción nos hemos movido de casilla
        
        if (jugador.getX() != last_x || jugador.getY() != last_y)
            it_ultimo_movimiento = it - 1;
        
        last_x = jugador.getX();
        last_y = jugador.getY();

        if (!plan_no_morir.isEmpty()){ // Si tengo acciones del plan para no morir, las ejecuto y termino el act
            it++;
            return plan_no_morir.pollFirst();
        }
        
        // Si tengo 9 gemas, planifico para abandonar el nivel
        if (this.getNumGems(stateObs) >= NUM_GEMS_FOR_EXIT && !abandonando_nivel){
            abandonando_nivel = true;
            
            Observation level_exit = this.getExit(stateObs);
            x_search = level_exit.getX();
            y_search = level_exit.getY();
            
            informacionPlan = pathExplorer(x_search, y_search, stateObs, elapsedTimer, (long) 1, casillas_prohibidas_exit);
            
            if (!informacionPlan.existsPath){
                ignorar_cas_prob_salida = true; // Ya no tengo en cuenta las casillas prohibidas 
                casillas_prohibidas_exit.clear();
            }
        }
        
        if (!hay_que_replanificar && it != 0 && !informacionPlan.searchComplete) { // Si no ha encontrado camino, sigo buscando
            informacionPlan = pathExplorer(x_search, y_search,
                    stateObs, gems_search,
                    elapsedTimer, 5, casillas_prohibidas);
        }

        
        // Si no ha encontrado un camino, tengo que volver a planificar
        if (informacionPlan.searchComplete && !informacionPlan.existsPath){
            hay_que_replanificar = true;
            accion = Types.ACTIONS.ACTION_NIL;
                
            // Clúster -> no puedo coger ese clúster, lo elimino (le queden o no gemas) y vuelvo a crear otro circuito 
            if (this.getNumGems(stateObs) < NUM_GEMS_FOR_EXIT){
                // Vuelvo a crear toda la información de los clústeres desde el principio (no tarda mucho (en el nivel 1 tarda 5 ms como mucho))
                ArrayList<Observation> gemas_actuales = this.getGemsList(stateObs);
                
                gemas_no_accesibles.addAll(gems_search); // Añado las gemas del clúster actual como no accesibles
                
                // Elimino de esas gemas las del cluster al que no se puede llegar
                gemas_actuales.removeAll(gemas_no_accesibles);
                
                if (gemas_actuales.size() < this.getRemainingGems(stateObs)){ // Si no hay suficientes clusters, quito las restricciones
                    ignorar_cas_prob_clusters = true; // Ya no tengo en cuenta las casillas prohibidas 
                    casillas_prohibidas.clear();
                    
                    gemas_no_accesibles.clear();
                    gemas_actuales = this.getGemsList(stateObs);
                }
                
                clusterInf = new ClusterInformation();
                clusterInf.createClusters(3, gemas_actuales,
                this.getBouldersList(stateObs), this.getWallsList(stateObs),
                this.getBatsList(stateObs), this.getScorpionsList(stateObs)); // Creo los clusters
            
                this.saveClustersDistances(clusterInf); // Guardo la matriz de distancias
                this.saveCircuit(clusterInf, jugador.getX(),
                jugador.getY(), salida.getX(),
                salida.getY(), this.getRemainingGems(stateObs)); // Creo el camino a través de los clústeres

                sig_cluster = 0; // Vuelvo a empezar por el principio del circuito
                
                int[] casilla_search;
                gems_search = clusterInf.getGemsCircuitCluster(0);

                if (1 < clusterInf.circuito.size()) // Compruebo si después de este clúster queda otro
                    casilla_search = this.getPuntoIntermedioClusters(gems_search,
                        clusterInf.getGemsCircuitCluster(1), stateObs);

                else{ // Si no, cojo el punto intermedio entre este clúster y la salida (es el último clúster) 
                    casilla_search = this.getPuntoIntermedioClusterSalida(gems_search,
                        this.getExit(stateObs), stateObs);
                }

                x_search = casilla_search[0];
                y_search = casilla_search[1]; // --> Se puede escoger como punto final una gema!!
            }
        }
        
        
        // Tengo que replanificar
        // Veo si quedan gemas en el clúster actual y si es así cojo las que quedan
        // Si no quedan gemas, me voy al punto intermedio entre este clúster y el siguiente
        // Si tengo 9 gemas me voy a la salida
        if (hay_que_replanificar){
            hay_que_replanificar = false;
            
            if (this.getNumGems(stateObs) >= NUM_GEMS_FOR_EXIT){ // Veo si tengo que planificar para abandonar el nivel
                Observation salida_nivel = this.getExit(stateObs);
                
                if (!en_bucle_enemigos)
                    informacionPlan = pathExplorer(salida_nivel.getX(), salida_nivel.getY(), stateObs, elapsedTimer, (long) 1, casillas_prohibidas_exit);
                else{
                    
                    if (!bucle_2_abandonar_nivel){
                        informacionPlan = pathExplorer(salida_nivel.getX(), salida_nivel.getY(), stateObs, elapsedTimer, (long) 1); // Si estoy en bucle, ignoro los enemigos
                        ignorar_cas_prob_salida = true;
                        bucle_2_abandonar_nivel = true;
                        
                        // Añado la casilla prohibida dada por la primera acción de este plan
                        Types.ACTIONS accion_bucle_2 = informacionPlan.plan.get(0);
                        ArrayList<Observation> [][] grid = this.getObservationGrid(stateObs);
                        
                        if (accion_bucle_2 == Types.ACTIONS.ACTION_UP)
                            casilla_prob_bucle_2 = new Observation(jugador.getX(), jugador.getY()-1, grid[jugador.getX()][jugador.getY()-1].get(0).getType());
                        else if (accion_bucle_2 == Types.ACTIONS.ACTION_DOWN)
                            casilla_prob_bucle_2 = new Observation(jugador.getX(), jugador.getY()+1, grid[jugador.getX()][jugador.getY()+1].get(0).getType());
                        else if (accion_bucle_2 == Types.ACTIONS.ACTION_RIGHT)
                            casilla_prob_bucle_2 = new Observation(jugador.getX()+1, jugador.getY(), grid[jugador.getX()+1][jugador.getY()].get(0).getType());
                        else if (accion_bucle_2 == Types.ACTIONS.ACTION_LEFT)
                            casilla_prob_bucle_2 = new Observation(jugador.getX()-1, jugador.getY(), grid[jugador.getX()-1][jugador.getY()].get(0).getType());
                        else
                            casilla_prob_bucle_2 = new Observation(0, 0, ObservationType.WALL); // Por si acaso, aunque este caso no se debería dar
                         
                    }
                    else{ // No es suficiente para salir del bucle el ignorar los enemigos -> sigo repitiendo el mismo camino inválido hacia la salida
                        ArrayList<Observation> array_casilla_prob = new ArrayList<>();
                        array_casilla_prob.add(casilla_prob_bucle_2);
                        informacionPlan = pathExplorer(salida_nivel.getX(), salida_nivel.getY(), stateObs, elapsedTimer, (long) 1, array_casilla_prob);
                        bucle_2_abandonar_nivel = false;
                    }
                }
            }
            
            else{ // Planifico para coger las gemas del clúster o ir al punto intermedio con el siguiente
                // Veo si quedan gemas en el clúster
                
                if (!en_bucle_enemigos){
                    clusterInf.removeCapturedGems(clusterInf.circuito.get(sig_cluster), this.getGemsList(stateObs));
                    
                    int indice_circuito = clusterInf.circuito.get(sig_cluster);
                    Cluster this_cluster;
                      
                    if (indice_circuito < clusterInf.clusters.size())
                        this_cluster = clusterInf.clusters.get(indice_circuito);
                    else
                        this_cluster = clusterInf.clusters.get(0);

                    if (this_cluster.getNumGems() > 0){ // Todavía le quedan gemas
                        gems_search = this_cluster.getGems();

                        informacionPlan = pathExplorer(x_search, y_search,
                                                 stateObs, gems_search,
                                                 elapsedTimer, 5, casillas_prohibidas);

                        if (informacionPlan.searchComplete){ // Veo si ha terminado la planificación en el mismo turno
                            accion = informacionPlan.plan.peekFirst(); // No la borro por si no se ejecuta después
                        }
                        else{             
                            accion = Types.ACTIONS.ACTION_NIL;
                        }
                    }
                    else{ // No le quedan gemas -> uso el A* simple para irme al punto entre este clúster y el siguiente
                        informacionPlan = pathExplorer(x_search, y_search, stateObs, elapsedTimer, (long) 1);
                    }
                }  
                else{ // Estoy en bucle -> vuelvo a crear los clusters pero no puedo ir a ese
                    // Vuelvo a crear toda la información de los clústeres desde el principio (no tarda mucho (en el nivel 1 tarda 5 ms como mucho))
                    ArrayList<Observation> gemas_actuales = this.getGemsList(stateObs);

                    gemas_no_accesibles.addAll(gems_search); // Añado las gemas del clúster actual como no accesibles

                    // Elimino de esas gemas las del cluster al que no se puede llegar
                    gemas_actuales.removeAll(gemas_no_accesibles);

                    if (gemas_actuales.size() < this.getRemainingGems(stateObs)){ // Si no se puede llegar a ningún clúster, vuelvo a intentarlo con el primero
                        ignorar_cas_prob_clusters = true; // Ya no tengo en cuenta las casillas prohibidas 
                        casillas_prohibidas.clear();

                        gemas_no_accesibles.clear();
                        gemas_actuales = this.getGemsList(stateObs);
                    }

                    clusterInf = new ClusterInformation();
                    clusterInf.createClusters(3, gemas_actuales,
                    this.getBouldersList(stateObs), this.getWallsList(stateObs),
                    this.getBatsList(stateObs), this.getScorpionsList(stateObs)); // Creo los clusters

                    this.saveClustersDistances(clusterInf); // Guardo la matriz de distancias
                    this.saveCircuit(clusterInf, jugador.getX(),
                    jugador.getY(), salida.getX(),
                    salida.getY(), this.getRemainingGems(stateObs)); // Creo el camino a través de los clústeres

                    sig_cluster = 0; // Vuelvo a empezar por el principio del circuito 
                    
                    // Replanifico

                    int[] casilla_search;
                    gems_search = clusterInf.getGemsCircuitCluster(0);

                    if (1 < clusterInf.circuito.size()) // Compruebo si después de este clúster queda otro
                        casilla_search = this.getPuntoIntermedioClusters(gems_search,
                            clusterInf.getGemsCircuitCluster(1), stateObs);

                    else{ // Si no, cojo el punto intermedio entre este clúster y la salida (es el último clúster) 
                        casilla_search = this.getPuntoIntermedioClusterSalida(gems_search,
                            this.getExit(stateObs), stateObs);
                    }

                    x_search = casilla_search[0];
                    y_search = casilla_search[1]; // --> Se puede escoger como punto final una gema!!

                    informacionPlan = pathExplorer(x_search, y_search,
                                             stateObs, gems_search,
                                             elapsedTimer, 5, casillas_prohibidas);

                    if (informacionPlan.searchComplete){ // Veo si ha terminado la planificación en el mismo turno
                        accion = informacionPlan.plan.peekFirst(); // No la borro por si no se ejecuta después
                    }
                    else{             
                        accion = Types.ACTIONS.ACTION_NIL;
                    }
                    
                }   
            }
        }
        
        
        if (informacionPlan.searchComplete){ // Ya ha terminado la planificación
            
            if (informacionPlan.plan.isEmpty()) { // Si he acabado de ejecutar el plan, planifico para el clúster siguiente
                
                if (sig_cluster+1 >= clusterInf.circuito.size()){ // Me daría outOfBounds exception porque ya no hay ningún clúster más del circuito
                    hay_que_replanificar = true;
                    
                    if(this.getNumGems(stateObs) < NUM_GEMS_FOR_EXIT){
                        // Vuelvo a crear los clusters
                        clusterInf = new ClusterInformation();
                        clusterInf.createClusters(3, this.getGemsList(stateObs),
                        this.getBouldersList(stateObs), this.getWallsList(stateObs),
                        this.getBatsList(stateObs), this.getScorpionsList(stateObs)); // Creo los clusters

                        this.saveClustersDistances(clusterInf); // Guardo la matriz de distancias
                        this.saveCircuit(clusterInf, jugador.getX(),
                        jugador.getY(), salida.getX(),
                        salida.getY(), this.getRemainingGems(stateObs)); // Creo el camino a través de los clústeres

                        sig_cluster = 0; // Vuelvo a empezar por el principio del circuito
                        
                        int[] casilla_search;
                        gems_search = clusterInf.getGemsCircuitCluster(0);

                        if (1 < clusterInf.circuito.size()) // Compruebo si después de este clúster queda otro
                            casilla_search = this.getPuntoIntermedioClusters(gems_search,
                                clusterInf.getGemsCircuitCluster(1), stateObs);

                        else{ // Si no, cojo el punto intermedio entre este clúster y la salida (es el último clúster) 
                            casilla_search = this.getPuntoIntermedioClusterSalida(gems_search,
                                this.getExit(stateObs), stateObs);
                        }

                        x_search = casilla_search[0];
                        y_search = casilla_search[1]; // --> Se puede escoger como punto final una gema!!
                    }
                    
                    accion = Types.ACTIONS.ACTION_NIL;
                }
                else{    
                    sig_cluster++;
                    gems_search = clusterInf.getGemsCircuitCluster(sig_cluster);
                    int[] casilla_search;

                    if (sig_cluster+1 < clusterInf.circuito.size()) // Compruebo si después de este clúster queda otro
                        casilla_search = this.getPuntoIntermedioClusters(gems_search,
                            clusterInf.getGemsCircuitCluster(sig_cluster+1), stateObs);

                    else{ // Si no, cojo el punto intermedio entre este clúster y la salida (es el último clúster) 
                        casilla_search = this.getPuntoIntermedioClusterSalida(gems_search,
                            this.getExit(stateObs), stateObs);
                    }

                    x_search = casilla_search[0];
                    y_search = casilla_search[1]; // --> Se puede escoger como punto final una gema!!

                    if (x_search != -1 && y_search != -1) // Veo si existe una casilla intermedia válida
                        informacionPlan = pathExplorer(x_search, y_search,
                                             stateObs, gems_search,
                                             elapsedTimer, 5, casillas_prohibidas);
                    else // Si no, le dejo que termine en cualquier gema
                        informacionPlan = pathExplorer(stateObs, gems_search,
                                             elapsedTimer, 5, casillas_prohibidas);

                    if (informacionPlan.searchComplete){ // Veo si ha terminado la planificación en el mismo turno
                        accion = informacionPlan.plan.peekFirst(); // No la borro por si no se ejecuta después
                    }
                    else{             
                        accion = Types.ACTIONS.ACTION_NIL;
                    }
                }
            } else{
                accion = informacionPlan.plan.peekFirst();
            }
        }
        
        // <<Parte reactiva>>
        // Ya he elegido la acción. Ahora veo si se puede ejecutar
     
        int jug_x = jugador.getX();
        int jug_y = jugador.getY();
        Orientation jug_orient = jugador.getOrientation();
        ArrayList<Observation> [][] grid = this.getObservationGrid(stateObs);
        
        // Veo si hay algún enemigo peligroso: veo si el camino del jugador estará conectado con el
        // de algún enemigo tras ejecutar la acción   
            
        // Obtengo los enemigos relativamente cercanos al jugador -> dist_manhattan <= 6
        ArrayList<Observation> enemigos = new ArrayList<>();
        ArrayList<Observation> bats = this.getBatsList(stateObs);
        ArrayList<Observation> scorpions = this.getScorpionsList(stateObs);
        
        for (Observation bat : bats){
            if (this.getHeuristicDistance(jugador, bat) <= 6)
                enemigos.add(bat);
        }
        
        for (Observation scorpion : scorpions){
            if (this.getHeuristicDistance(jugador, scorpion) <= 6)
                enemigos.add(scorpion);
        }
                
        // Veo si hay algún enemigo que sea un peligro
        boolean caminos_conectados;
                    
        boolean enemigos_cercanos = false; 
        
        int dist_umbral = 4; // Distancia manhattan a la que tienen que estar los enemigos para ser considerados un peligro
         
        StateObservation estado_juego = stateObs.copy();
        
        for(int i = 0; i < enemigos.size() && !enemigos_cercanos; i++){
            caminos_conectados = connectionToEnemy(stateObs, accion, enemigos.get(i), jugador);
            
            if (caminos_conectados){ // El camino del enemigo está conectado al del jugador
                
                if (this.getHeuristicDistance(jugador, enemigos.get(i)) <= dist_umbral){
                    enemigos_cercanos = true;
                }
            }
        }
               
        if (enemigos_cercanos){ // La siguiente accion pone al jugador en peligro -> veo si hay alguna acción mejor
            // Casillas posibles a la que ir -> arriba, abajo, derecha o izquierda
            
            // Veo qué casillas son válidas -> si voy a ella no voy a morir por una roca
            
            ArrayList<Observation> casillas_validas = new ArrayList<>();
            boolean casilla_valida;
            Observation obs_pos;
            int x_casilla;
            int y_casilla;
            
            // Arriba
            // Veo si es válida -> no es una roca ni muro y no tiene una roca encima
            casilla_valida = true;
            x_casilla = jug_x;
            y_casilla = jug_y-1;
            obs_pos = new Observation(x_casilla, y_casilla, ObservationType.PLAYER);

            // Compruebo que no sea un muro, roca ni tenga una roca encima
            if (grid[x_casilla][y_casilla].get(0).getType() == ObservationType.WALL
                    || grid[x_casilla][y_casilla].get(0).getType() == ObservationType.BOULDER)
                casilla_valida = false;

            if (casilla_valida && y_casilla-1 >= 0)
                if (grid[x_casilla][y_casilla-1].get(0).getType() == ObservationType.BOULDER)
                    casilla_valida = false;

            if (casilla_valida)
                casillas_validas.add(obs_pos);

            // Abajo
            casilla_valida = true;
            x_casilla = jug_x;
            y_casilla = jug_y+1;
            obs_pos = new Observation(x_casilla, y_casilla, ObservationType.PLAYER);

            // Compruebo que no sea un muro ni roca
            if (grid[x_casilla][y_casilla].get(0).getType() == ObservationType.WALL
                    || grid[x_casilla][y_casilla].get(0).getType() == ObservationType.BOULDER)
                casilla_valida = false;

            if (casilla_valida)
                casillas_validas.add(obs_pos);
            
            // Derecha
            casilla_valida = true;
            x_casilla = jug_x+1;
            y_casilla = jug_y;
            obs_pos = new Observation(x_casilla, y_casilla, ObservationType.PLAYER);

            // Compruebo que no sea un muro, roca ni tenga una roca encima
            if (grid[x_casilla][y_casilla].get(0).getType() == ObservationType.WALL
                    || grid[x_casilla][y_casilla].get(0).getType() == ObservationType.BOULDER)
                casilla_valida = false;

            if (casilla_valida)
                if (grid[x_casilla][y_casilla-1].get(0).getType() == ObservationType.BOULDER)
                    casilla_valida = false;

            if (casilla_valida)
                casillas_validas.add(obs_pos);
            
            // Izquierda
            casilla_valida = true;
            x_casilla = jug_x-1;
            y_casilla = jug_y;
            obs_pos = new Observation(x_casilla, y_casilla, ObservationType.PLAYER);

            // Compruebo que no sea un muro, roca ni tenga una roca encima
            if (grid[x_casilla][y_casilla].get(0).getType() == ObservationType.WALL
                    || grid[x_casilla][y_casilla].get(0).getType() == ObservationType.BOULDER)
                casilla_valida = false;

            if (casilla_valida)
                if (grid[x_casilla][y_casilla-1].get(0).getType() == ObservationType.BOULDER)
                    casilla_valida = false;

            if (casilla_valida)
                casillas_validas.add(obs_pos);

            
            if (!casillas_validas.isEmpty()){
                // De aquellas casillas válidas, veo la distancia al enemigo más cercano
                // al que esa casilla esté conectada. Si no está conectada a ningún enemigo
                // su distancia valdrá 1000
                int num_casillas_validas = casillas_validas.size();
                int[] dist_cas_enem = new int[num_casillas_validas];
                int min_dist;
                int this_dist;
                
                for (int i = 0; i < num_casillas_validas; i++){
                    Observation this_casilla = casillas_validas.get(i);
                    PlayerObservation jugador_connection;
                    PlayerObservation jugador_tras_accion;
                    Types.ACTIONS accion_connection = Types.ACTIONS.ACTION_NIL;
                    
                    // Veo la posición de la casilla_valida
                    if (this_casilla.getX() > jug_x){ // Derecha
                        jugador_connection = new PlayerObservation(jug_x, jug_y, Orientation.E);
                        jugador_tras_accion = new PlayerObservation(jug_x+1, jug_y, Orientation.E);
                        accion_connection = Types.ACTIONS.ACTION_RIGHT;
                    }
                    else if (this_casilla.getX() < jug_x){ // Izquierda
                        jugador_connection = new PlayerObservation(jug_x, jug_y, Orientation.W);
                        jugador_tras_accion = new PlayerObservation(jug_x-1, jug_y, Orientation.W);
                        accion_connection = Types.ACTIONS.ACTION_LEFT;
                    }
                    else{
                        if (this_casilla.getY() < jug_y){ // Arriba
                           jugador_connection = new PlayerObservation(jug_x, jug_y, Orientation.N);
                           jugador_tras_accion = new PlayerObservation(jug_x, jug_y-1, Orientation.N);
                           accion_connection = Types.ACTIONS.ACTION_UP; 
                        }
                        else{ // Abajo
                           jugador_connection = new PlayerObservation(jug_x, jug_y, Orientation.S);
                           jugador_tras_accion = new PlayerObservation(jug_x, jug_y+1, Orientation.S);
                           accion_connection = Types.ACTIONS.ACTION_DOWN; 
                        }
                    }
                    
                    min_dist = 1000;
                    
                    for (Observation enemigo : enemigos){
                        caminos_conectados = connectionToEnemy(stateObs, accion_connection, enemigo, jugador_connection);

                        if (caminos_conectados){ // El camino del enemigo está conectado al del jugador
                            if (jugador_tras_accion != null && !jugador_tras_accion.hasDied()){
                                this_dist = this.getHeuristicDistance(jugador_tras_accion, enemigo);
                            }
                            else
                                this_dist = 0;
                            
                            if (this_dist < min_dist)
                                min_dist = this_dist;
                        }
                    }
                    
                    dist_cas_enem[i] = min_dist;
                }
                
                // Si hay casillas con dist_cas_enem > 4, escojo la más cercana al objetivo
                // Si no, escojo la casilla con mayor dist_cas_enem
                
                boolean casilla_ya_elegida = false;
                Observation casilla_elegida = casillas_validas.get(0); // Casilla por defecto
                
                
                // Calculo la distancia de cada casilla al objetivo (x_search, y_search)
                Observation obs_objetivo = new Observation(x_search, y_search, ObservationType.EXIT);
                int[] dist_cas_obj = new int[num_casillas_validas];
                
                for (int i = 0; i < num_casillas_validas; i++)
                    dist_cas_obj[i] = this.getHeuristicDistance(obs_objetivo, casillas_validas.get(i));
                
                
                // Veo si hay casillas con dist_cas_enem > 4
                int min_dist_obj = 10000;
                int ind_min_dist_obj = -1;

                for (int i = 0; i < num_casillas_validas; i++){
                    if (dist_cas_enem[i] > dist_umbral){
                        if (dist_cas_obj[i] < min_dist_obj){
                            min_dist_obj = dist_cas_obj[i];
                            ind_min_dist_obj = i;
                        }
                    }  
                }
                
                if (ind_min_dist_obj != -1){
                    casilla_ya_elegida = true;
                    casilla_elegida = casillas_validas.get(ind_min_dist_obj);
                }
                
                // Si no hay casillas con dist_cas_enem > 4, escojo la que tenga mayor dist_cas_enem
                
                if (!casilla_ya_elegida){
                    int max_dist_enem = -1;
                    int ind_max_dist_enem = -1;
                    
                    for (int i = 0; i < num_casillas_validas; i++){
                        if (dist_cas_enem[i] > max_dist_enem){
                            max_dist_enem = dist_cas_enem[i];
                            ind_max_dist_enem = i;
                        }
                    }
                    
                    casilla_elegida = casillas_validas.get(ind_max_dist_enem);
                    casilla_ya_elegida = true;
                }
                
                // Ahora transformo la casilla elegida en acciones en plan_no_morir para llegar a ella
                plan_no_morir.clear();
                hay_que_replanificar = true;
                
                // Veo la posición de la casilla_elegida
                if (casilla_elegida.getX() > jug_x){ // Derecha
                    plan_no_morir.add(Types.ACTIONS.ACTION_RIGHT);
                    
                    if (jug_orient != Orientation.E)
                        plan_no_morir.add(Types.ACTIONS.ACTION_RIGHT);
                }
                else if (casilla_elegida.getX() < jug_x){ // Izquierda
                    plan_no_morir.add(Types.ACTIONS.ACTION_LEFT);
                    
                    if (jug_orient != Orientation.W)
                        plan_no_morir.add(Types.ACTIONS.ACTION_LEFT);
                }
                else{
                    if (casilla_elegida.getY() < jug_y){ // Arriba
                        plan_no_morir.add(Types.ACTIONS.ACTION_UP);
                    
                        if (jug_orient != Orientation.N)
                            plan_no_morir.add(Types.ACTIONS.ACTION_UP);
                    }
                    else{ // Abajo
                        plan_no_morir.add(Types.ACTIONS.ACTION_DOWN);
                    
                        if (jug_orient != Orientation.S)
                            plan_no_morir.add(Types.ACTIONS.ACTION_DOWN);  
                    }
                }
                
                // Veo si estoy en un bucle -> el jugador ya se encontró en esta posición
                Observation obs_guardar = new Observation(jug_x, jug_y, ObservationType.EMPTY);
                
                if(casillas_huir_enemigos.contains(obs_guardar)){
                    en_bucle_enemigos = true;
                }
                else{
                    en_bucle_enemigos = false;
                }
                
                casillas_huir_enemigos.add(obs_guardar);

            }
            else{ // Si no hay casillas válidas, no hago nada
                enemigos_cercanos = false;
            }

        }
        
        // Veo si el jugador va a morir en los siguientes 2 turnos. Si enemigos_cercanos = true,
        // cojo las acciones del plan no_morir para ver si moriría. Si vale false, cojo
        // las acciones del plan normal
        
        StateObservation estado_avanzado = stateObs.copy();
        
        LinkedList<Types.ACTIONS> plan_usado;
        
        if(!enemigos_cercanos){
            if (informacionPlan.plan.isEmpty())
                informacionPlan.plan.add(Types.ACTIONS.ACTION_NIL);
            
            plan_usado = informacionPlan.plan;
        }
        else
            plan_usado = plan_no_morir;
        
        
        estado_avanzado.advance(plan_usado.get(0)); // Siguiente estado del juego (en el siguiente turno)  

        PlayerObservation jugador_sig_estado = this.getPlayer(estado_avanzado); // Jugador el siguiente turno
        
        if (plan_usado.size() >= 2)
            estado_avanzado.advance(plan_usado.get(1)); // Estado del juego dentro de dos turnos
        
        PlayerObservation jugador_sig2_estado = this.getPlayer(estado_avanzado); // Jugador dentro de 2 turnos o dentro de 1 si el plan solo tiene una acción
        
        
        // Si el jugador abandona el nivel es como si hubiera muerto!!! (su "x" también vale -1)
        boolean bug_morir = false;
        
        if (jugador.getManhattanDistance(this.getExit(stateObs)) <= 1)
            bug_morir = true;
        
        else if (!jugador_sig_estado.hasDied() && jugador_sig2_estado.hasDied()){
            if (jugador_sig_estado.getManhattanDistance(this.getExit(stateObs)) <= 1)
                bug_morir = true;
        }
        
        boolean condicion_ejec_parte_rocas = !bug_morir && (jugador_sig_estado.hasDied() || jugador_sig2_estado.hasDied());
        
        if (!condicion_ejec_parte_rocas){ // Si no se ejecuta la parte de las rocas, devuelvo ya la acción del plan_no_morir (si no está vacío)
            if (!plan_no_morir.isEmpty()){
                it++;
                return plan_no_morir.pollFirst();
            }
        }
        
        
        if (condicion_ejec_parte_rocas){ // Va a morir y no es por un enemigo -> es por una roca
            
            // Si va a morir ejecuto las acciones necesarias para sobrevivir y después vuelvo a la casilla donde estaba y sigo ejecutando el plan
            
            // Veo si va a morir debido a una roca
            boolean muerte_por_roca = false;
            boolean roca_derecha = false;
            boolean roca_arriba = false;
            boolean roca_izquierda = false;
            
            if (    grid[jug_x+1][jug_y].get(0).getType() == ObservationType.BOULDER ||
                    grid[jug_x+1][jug_y-1].get(0).getType() == ObservationType.BOULDER){ // Hay roca a la derecha
                    
                    roca_derecha = true;
                    muerte_por_roca = true;
                
            }
            if (    grid[jug_x-1][jug_y].get(0).getType() == ObservationType.BOULDER ||
                    grid[jug_x-1][jug_y-1].get(0).getType() == ObservationType.BOULDER){ // Hay roca a la izquierda
                    
                    roca_izquierda = true;
                    muerte_por_roca = true;
                
            }
            if (    grid[jug_x][jug_y].get(0).getType() == ObservationType.BOULDER ||
                    grid[jug_x][jug_y-1].get(0).getType() == ObservationType.BOULDER){ // Hay roca arriba
                    
                    roca_arriba = true;
                    muerte_por_roca = true;
                
            }
            
            if (muerte_por_roca){ // Me va a aplastar una roca pero no hay enemigos cercanos -> me aparto de la roca sin preocuparme de los enemigos
                
                StateObservation estado_prueba;
                plan_no_morir.clear();
                boolean va_a_morir;
                
                // Pruebo primero a quedarme quieto si no me está cayendo una roca encima
                if (!roca_arriba){
                    va_a_morir = false;
                    
                    // Compruebo si al ejecutar ACTION_NIL los dos siguientes turnos el jugador muere o no
                    estado_prueba = stateObs.copy();
                    estado_prueba.advance(Types.ACTIONS.ACTION_NIL);
                    
                    if (this.getPlayer(estado_prueba).hasDied())
                        va_a_morir = true;
                    else{
                        estado_prueba.advance(Types.ACTIONS.ACTION_NIL);
                        
                        if (this.getPlayer(estado_prueba).hasDied())
                            va_a_morir = true;
                    }
                    
                    if (!va_a_morir){ // Si no muere, añado esas acciones al plan
                        plan_no_morir.add(Types.ACTIONS.ACTION_NIL);
                    }
                }
                
                // Si no puedo quedarme quieto, pruebo a moverme a la izquierda
                if (plan_no_morir.isEmpty() && !roca_izquierda){
                    va_a_morir = false;
                    
                    if (jug_orient == Orientation.W){ // No tengo que girar
                        // Ejecuto ACTION_LEFT y ACTION_NIL y veo si muero
                        estado_prueba = stateObs.copy();
                        estado_prueba.advance(Types.ACTIONS.ACTION_LEFT);

                        if (this.getPlayer(estado_prueba).hasDied())
                            va_a_morir = true;
                        else{
                            estado_prueba.advance(Types.ACTIONS.ACTION_NIL);

                            if (this.getPlayer(estado_prueba).hasDied())
                                va_a_morir = true;
                        }
                        
                        if (!va_a_morir){ // Si no muere, añado esas acciones al plan
                            plan_no_morir.add(Types.ACTIONS.ACTION_LEFT);
                            plan_no_morir.add(Types.ACTIONS.ACTION_NIL);
                        }
                    }
                    else{ // Primero giro a la izquierda y después me muevo
                        // Ejecuto ACTION_LEFT y ACTION_LEFT y veo si muero
                        estado_prueba = stateObs.copy();
                        estado_prueba.advance(Types.ACTIONS.ACTION_LEFT);

                        if (this.getPlayer(estado_prueba).hasDied())
                            va_a_morir = true;
                        else{
                            estado_prueba.advance(Types.ACTIONS.ACTION_LEFT);

                            if (this.getPlayer(estado_prueba).hasDied())
                                va_a_morir = true;
                        }
                        
                        if (!va_a_morir){ // Si no muere, añado esas acciones al plan
                            plan_no_morir.add(Types.ACTIONS.ACTION_LEFT);
                            plan_no_morir.add(Types.ACTIONS.ACTION_LEFT);
                        }
                    }   
                }
                
                // Si no ha funcionado, pruebo a moverme a la derecha
                if (plan_no_morir.isEmpty() && !roca_derecha){
                    va_a_morir = false;
                    
                    if (jug_orient == Orientation.E){ // No tengo que girar
                        // Ejecuto ACTION_RIGHT y ACTION_NIL y veo si muero
                        estado_prueba = stateObs.copy();
                        estado_prueba.advance(Types.ACTIONS.ACTION_RIGHT);

                        if (this.getPlayer(estado_prueba).hasDied())
                            va_a_morir = true;
                        else{
                            estado_prueba.advance(Types.ACTIONS.ACTION_NIL);

                            if (this.getPlayer(estado_prueba).hasDied())
                                va_a_morir = true;
                        }
                        
                        if (!va_a_morir){ // Si no muere, añado esas acciones al plan
                            plan_no_morir.add(Types.ACTIONS.ACTION_RIGHT);
                            plan_no_morir.add(Types.ACTIONS.ACTION_NIL);
                        }
                    }
                    else{ // Primero giro a la izquierda y después me muevo
                        // Ejecuto ACTION_RIGHT y ACTION_RIGHT y veo si muero
                        estado_prueba = stateObs.copy();
                        estado_prueba.advance(Types.ACTIONS.ACTION_RIGHT);

                        if (this.getPlayer(estado_prueba).hasDied())
                            va_a_morir = true;
                        else{
                            estado_prueba.advance(Types.ACTIONS.ACTION_RIGHT);

                            if (this.getPlayer(estado_prueba).hasDied())
                                va_a_morir = true;
                        }
                        
                        if (!va_a_morir){ // Si no muere, añado esas acciones al plan
                            plan_no_morir.add(Types.ACTIONS.ACTION_RIGHT);
                            plan_no_morir.add(Types.ACTIONS.ACTION_RIGHT);
                        }
                    }   
                }
                
                // Por último, si no ha funcionado nada, me muevo hacia abajo
                if (plan_no_morir.isEmpty()){
                    estado_prueba = stateObs.copy();
                    estado_prueba.advance(Types.ACTIONS.ACTION_DOWN);
                    estado_prueba.advance(Types.ACTIONS.ACTION_DOWN);
                    plan_no_morir.add(Types.ACTIONS.ACTION_DOWN);
                    
                    if (!this.getPlayer(estado_prueba).hasDied()) // Añado otra acción si no ha muerto con el segundo ACTION_DOWN
                        plan_no_morir.add(Types.ACTIONS.ACTION_DOWN);
                }
            }

            hay_que_replanificar = true; // Cuando termine de ejecutar este plan, tendré que replanificar
            it++;
            
            if (plan_no_morir.isEmpty())
                plan_no_morir.add(Types.ACTIONS.ACTION_NIL);
            
            return plan_no_morir.pollFirst();
        }
        else if (!enemigos_cercanos && !bug_morir && accion != Types.ACTIONS.ACTION_NIL){ // No va a morir en el siguiente turno y la acción no es quedarse quieto
            
            // <Choque con rocas>
            // Veo si la acción tiene el resultado esperado o se va a chocar con una roca que está cayendo
            // En ese caso, se queda quieto y la acción que iba a realizar la ejecuta el siguiente turno (si no vuelve a pasar esto)
            // Excepción -> cuando en la casilla siguiente hay una gema o tierra y encima hay una roca, la orientación y posición del jugador
            // no cambian (pero sí es un movimiento válido) (excava la casilla de abajo de la roca / coge la gema)
            
            // También veo si el plan es inválido: el agente lleva más de 15 turnos aplazando acciones por haber chocado con una roca -> replanifico
            boolean hay_gema_o_tierra = false;
            boolean hay_roca = false;
            boolean plan_invalido = false;
            
            if (jugador_sig_estado.getX() == jugador.getX() && jugador_sig_estado.getY() == jugador.getY() &&
                    jugador_sig_estado.getOrientation() == jugador.getOrientation()){ // En el siguiente turno su posición y orientación no han cambiado
                
                int x_jug = jugador.getX();
                int y_jug = jugador.getY();
                
                // Veo si el plan es inválido (lleva más de 15 turnos quieto)
                
                if (it - it_ultimo_movimiento > 15)
                    plan_invalido = true;
                
                
                if (plan_invalido){ // Si el plan es inválido, este turno me quedo quieto y el siguiente replanifico
                    ignorar_cas_prob_clusters = true;
                    ignorar_cas_prob_salida = true;
                    hay_que_replanificar = true;
                    it++;
                    it_ultimo_movimiento = it; // Reseteo el contador
                    return Types.ACTIONS.ACTION_NIL;
                }

                
                // Veo si se queda "quieto" porque va a excavar debajo de una roca
                
                
                if (jugador.getOrientation() == Orientation.N){
                    for (Observation obs : grid[x_jug][y_jug-1]){
                        if (obs.getType() == ObservationType.GEM || obs.getType() == ObservationType.GROUND)
                            hay_gema_o_tierra = true;
                    }
                        
                    if(y_jug-2 >= 0){    
                        for (Observation obs : grid[x_jug][y_jug-2]){ // Veo si se queda quieto porque pica para tirar la roca
                            if (obs.getType() == ObservationType.BOULDER)
                                hay_roca = true;
                        }
                    }
                }
                else if (jugador.getOrientation() == Orientation.E){
                   for (Observation obs : grid[x_jug+1][y_jug]){
                        if (obs.getType() == ObservationType.GEM || obs.getType() == ObservationType.GROUND)
                            hay_gema_o_tierra = true;
                    }
                        
                    for (Observation obs : grid[x_jug+1][y_jug-1]){
                            if (obs.getType() == ObservationType.BOULDER)
                                hay_roca = true;
                    }
                }
                else if (jugador.getOrientation() == Orientation.W){
                    for (Observation obs : grid[x_jug-1][y_jug]){
                        if (obs.getType() == ObservationType.GEM || obs.getType() == ObservationType.GROUND)
                            hay_gema_o_tierra = true;
                    }
                    
                    for (Observation obs : grid[x_jug-1][y_jug-1]){
                            if (obs.getType() == ObservationType.BOULDER)
                                hay_roca = true;
                    }
                }
                
                
                if (!hay_gema_o_tierra || !hay_roca){ // Si la acción no es para excavar, se ha chocado -> me quedo quieto
                    it++;
                    return Types.ACTIONS.ACTION_NIL; // Me quedo quieto y no ejecuto la acción del plan (la ejecutaré el siguiente turno si no vuelve a pasar)
                }
            }
        }
        
        informacionPlan.plan.removeFirst();
        
        it++;
        return accion;
    }
        
    // Usa el pathFinder para obtener una cota inferior (optimista) de la distancia entre
    // dos casillas (atraviesa las rocas pero no los muros)
    // DA EXCEPCION SI LA POSICION INICIAL O FINAL ESTA SOBRE UN MURO!
    
    private int getHeuristicDistance(int xStart, int yStart, int xGoal, int yGoal){
        if (xStart == xGoal && yStart == yGoal)
            return 0;
        
        // Obtengo la distancia que me da el pathFinder -> número de casillas del array
        ArrayList<tools.pathfinder.Node> camino = 
                pf.getPath(new Vector2d(xStart, yStart), new Vector2d(xGoal, yGoal));
        
        int distance = camino.size();
        
        // Compruebo si existe camino
        if (distance == 0)
            return -1;
        
        // Como sé que si me tengo que mover tanto en X como en Y voy a tener que dar un
        // giro como mínimo, en ese caso añado uno a la distancia

        if (xStart != xGoal && yStart != yGoal)
            distance++;
        
        return distance;
    }

    /**
     * Sobrecarga del metodo getHeuristicDistance
     * Acepta dos observacions y calcula la distancia entre ellos
     *
     * @param obs1 Observacion inicial
     * @param obs2 Observacion final
     *
     * @return Estimacion heuristica de la distnacia entre las dos observaciones
     */
    private int getHeuristicDistance(Observation obs1, Observation obs2) {
        return getHeuristicDistance(obs1.getX(), obs1.getY(), obs2.getX(), obs2.getY());
    }
    
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Metodos de busqueda
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Función para el obtener los vecinos de un nodo
     * @param currentObs Objeto de la clase Observation que representa el
     *                   nodo del que obtener los vecinos
     * @param grid Grid de observación del juego desde el que sacar los vecinos
     * @return Devuelve un ArrayList<Observation> que contiene los vecinos
     *         de un nodo
     */
    private ArrayList<Observation> getNeighbours(Observation currentObs, ArrayList<Observation>[][] grid) {
        ArrayList<Observation> neighbours = new ArrayList<>();
        int x = currentObs.getX(), y = currentObs.getY();

        neighbours.add(grid[x][y - 1].get(0));      // Top neighbour
        neighbours.add(grid[x + 1][y].get(0));      // Right neighbour
        neighbours.add(grid[x][y + 1].get(0));      // Bottom neighbour
        neighbours.add(grid[x - 1][y].get(0));      // Left neighbour

        return neighbours;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // pathExplorer simple

    // Sobrecarga de pathExplorer para cuando la posición de inicio es la del
    // jugador de stateObs
    private PathInformation pathExplorer(int xGoal, int yGoal, StateObservation stateObs,
                                         ElapsedCpuTimer elapsedTimer, long timeThreshold){
        return pathExplorer(this.getPlayer(stateObs), xGoal, yGoal, stateObs, elapsedTimer,
                timeThreshold, null, null, null);
    }


    // Sobrecarga de pathExplorer para cuando la posición de inicio es la del
    // jugador de stateObs
    private PathInformation pathExplorer(int xGoal, int yGoal, StateObservation stateObs,
                                         ElapsedCpuTimer elapsedTimer, long timeThreshold,
                                         ArrayList<Observation> ignoreList){
        return pathExplorer(this.getPlayer(stateObs), xGoal, yGoal, stateObs, elapsedTimer,
                            timeThreshold, ignoreList, null, null);
    }

    // pathExplorer con lista de casillas a ignorar
    private PathInformation pathExplorer(PlayerObservation startingPos, int xGoal, int yGoal, StateObservation stateObs,
                                         ElapsedCpuTimer elapsedTimer, long timeThreshold, ArrayList<Observation> ignoreList,
                                         boolean[][] initialBoulderMap, boolean[][] initialGroundMap) {
        PathInformation plan = new PathInformation();
        PriorityQueue<GridNode> openList = new PriorityQueue<>(
                (GridNode n1, GridNode n2) -> n1.getfCost() - n2.getfCost());
        LinkedList<GridNode> closedList = new LinkedList<>();
        HashSet<GridNode> exploredList = new HashSet<>();

        final ObservationType WALL = ObservationType.WALL;

        ArrayList<Observation>[][] grid = this.getObservationGrid(stateObs);
        boolean foundGoal = false;
        boolean timeout = false;

        GridNode currentNode;
        Observation currentObservation;

        final Types.ACTIONS[] actions = {Types.ACTIONS.ACTION_UP, Types.ACTIONS.ACTION_RIGHT, Types.ACTIONS.ACTION_DOWN, Types.ACTIONS.ACTION_LEFT};
        final Orientation[] orientations = {Orientation.N, Orientation.E, Orientation.S, Orientation.W};
        final Observation goal = grid[xGoal][yGoal].get(0);


        final int XMAX = grid.length,
                YMAX = grid[0].length;


        // Boulder map (contains boulders and walls)
        boolean[][] boulderMap = new boolean[XMAX][YMAX];
        ArrayList<Observation> boulders = this.getBouldersList(stateObs);
        ArrayList<Observation> walls = this.getWallsList(stateObs);
        ArrayList<Observation> obstacles = (ArrayList<Observation>) boulders.clone();
        obstacles.addAll(walls);

        if (initialBoulderMap == null) {
            UtilAlgorithms.initMap(boulderMap, obstacles, XMAX, YMAX);
        } else {
            boulderMap = initialBoulderMap;
        }


        // Create ArrayList containing boulder configurations
        ArrayList<boolean [][]> boulderConfigurations = new ArrayList<>();


        // Ground map
        boolean[][] groundMap = new boolean[XMAX][YMAX];
        ArrayList<Observation> groundList = this.getGroundTilesList(stateObs);

        if (initialGroundMap == null) {
            UtilAlgorithms.initMap(groundMap, groundList, XMAX, YMAX);
        } else {
            groundMap = initialGroundMap;
        }

        // Gems map
        boolean[][] gemsMap = new boolean[XMAX][YMAX];
        ArrayList<Observation> gemsList = this.getGemsList(stateObs);

        UtilAlgorithms.initMap(gemsMap, gemsList, XMAX, YMAX);

        // Simulate initial boulder fall
        UtilAlgorithms.simulateBoulderFall(boulders, boulderMap, groundMap, gemsMap, grid);

        boulderConfigurations.add(boulderMap);

        // Add first node
        openList.add(new GridNode(0, this.getHeuristicDistance(startingPos, goal),
                null, startingPos, startingPos.getOrientation(), 0,
                groundMap, gemsMap, false, 0, null, null));

        while (!foundGoal && !openList.isEmpty()) {

            // Check wether there's a tiemout
            if (elapsedTimer.remainingTimeMillis() <= timeThreshold) {
                timeout = true;
                break;
            }

            // Get first node
            currentNode = openList.poll();

            // Get current observation, boulder map and ground map
            currentObservation = currentNode.getPosition();
            boolean[][] currentBoulders = boulderConfigurations.get(currentNode.getBoulderIndex());
            boolean[][] currentGround = currentNode.getGroundMap();

            if (currentObservation.getX() == xGoal && currentObservation.getY() == yGoal) {
                foundGoal = true;
            } else {
                // Get list of neighbours
                ArrayList<Observation> neighbours = this.getNeighbours(currentObservation, grid);

                // Iterate over each neighbour
                for (int i = 0; i < neighbours.size(); i++) {
                    // Set next grid to explore and get its position
                    Observation nextGrid = neighbours.get(i);
                    int x = nextGrid.getX(), y = nextGrid.getY();

                    // Skip forbidden grid if it's the north grid
                    if (i == 0 && currentNode.getForbiAboveGrid()) {
                        continue;
                    }

                    // Check if the grid is not a boulder in the current boulder map
                    if (!currentBoulders[x][y]) {
                        int numberActions = 1;
                        int bouldIndx = currentNode.getBoulderIndex();
                        Observation nextPosition = nextGrid;
                        boolean[][] nextGround = new boolean[XMAX][YMAX];
                        boolean forbidAboveGrid = false;

                        // Copy the current ground and set the current grid as not ground
                        UtilAlgorithms.copy2DArray(currentGround, nextGround, XMAX, YMAX);

                        nextGround[x][y] = false;

                        // Add actions
                        LinkedList<Types.ACTIONS> actionList = new LinkedList<>();
                        actionList.addFirst(actions[i]);

                        // Check wether an extra action must be done (a turn)
                        if (!currentNode.getOrientation().equals(orientations[i])) {
                            numberActions++;
                            actionList.addFirst(actions[i]);
                        }

                        // Check wether there's a boulder above the current grid and it's nor a gem
                        // nor a wall
                        if (currentBoulders[x][y - 1] && !grid[x][y-1].get(0).getType().equals(WALL)
                                && !grid[x][y].get(0).getType().equals(ObservationType.GEM)) {

                            // Crete new boulder map and copy its old values
                            boolean[][] newBoulders = new boolean[XMAX][YMAX];
                            UtilAlgorithms.copy2DArray(currentBoulders, newBoulders, XMAX, YMAX);

                            int numberBoulders = 0;
                            int boulderPos = y - 1;
                            int emptyPos = y + 1;

                            /* Find out number of boulders above the current grid
                               and the index of the highest grid containing a boulder*/

                            while (newBoulders[x][boulderPos] && !grid[x][boulderPos].get(0).getType().equals(WALL)) {
                                numberBoulders++;
                                boulderPos--;
                            }

                            // Find out the index of the last empty space
                            while (!nextGround[x][emptyPos] && !grid[x][emptyPos].get(0).getType().equals(WALL)) {
                                emptyPos++;
                            }

                            // Modify the boulder map, moving the boulders
                            for (int j = emptyPos - 1; j > boulderPos; j--) {
                                if (j > emptyPos - 1 - numberBoulders) {
                                    newBoulders[x][j] = true;
                                } else {
                                    newBoulders[x][j] = false;
                                }
                            }

                            // Add the new boulder configuration and update boulder map index
                            boulderConfigurations.add(newBoulders);
                            bouldIndx = boulderConfigurations.indexOf(newBoulders);

                            // Set the next position as the same as now
                            nextPosition = currentNode.getPosition();

                            // Forbid the above grid if the current grid is the above grid
                            // and the agent has mined or if the agent has mined another grid
                            // and hasn't changed its position and the previous grid had forbidden
                            // that movement
                            if ((i == 0) || (nextPosition.getX() == currentNode.getPosition().getX() && currentNode.getForbiAboveGrid())) {
                                forbidAboveGrid = true;
                            }
                        }

                        // Check if the agent is trying to go to the above grid without changing its X position
                        // after moving a boulder above him
                        if ((nextPosition.getX() == currentNode.getPosition().getX() && currentNode.getForbiAboveGrid())) {
                            forbidAboveGrid = true;
                        }

                        // Skip nextPosition if its contained in ignoreList
                        if (ignoreList != null && ignoreList.contains(nextPosition)) {
                            continue;
                        }

                        // Create new grid node
                        GridNode node = new GridNode(currentNode.getgCost() + numberActions,
                                                        this.getHeuristicDistance(nextPosition, goal),
                                                        actionList, nextPosition, orientations[i], bouldIndx,
                                                        nextGround, currentNode.getGemsMap(), forbidAboveGrid, 0, null, currentNode);

                        // Add the node to the explored list
                        if (exploredList.add(node)) {
                            openList.add(node);
                        }
                    }
                }
            }

            // Add the current node to the closed list
            closedList.addFirst(currentNode);
        }

        // Return no plan if there's a timeout
        if (timeout) {
            plan.searchComplete = true;
            plan.existsPath = false;
            return plan;
        }

        // Get the last explored grid (goal grid)
        GridNode path = closedList.getFirst();

        // Save the path information
        if (foundGoal) {
            plan.groundMap = path.getGroundMap();
            plan.boulderMap = boulderConfigurations.get(path.getBoulderIndex());
            plan = parsePlan(path);
        } else {
            plan.existsPath = false;
        }

        plan.searchComplete = true;

        return plan;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // pathExplorer con movimiento de rocas

    // Sobrecarga de pathExplorer para cuando la posición de inicio es la del
    // jugador de stateObs
    private PathInformation pathExplorer(int xGoal, int yGoal,
                                         StateObservation stateObs, ArrayList<Observation> goalGems,
                                         ElapsedCpuTimer elapsedTimer, long timeThreshold,
                                         ArrayList<Observation> ignoreList){
        return pathExplorer(this.getPlayer(stateObs), xGoal, yGoal, stateObs,
                                goalGems, elapsedTimer, timeThreshold, ignoreList,
                null, null);
    }

    private PathInformation pathExplorer(PlayerObservation startingPos, int xGoal, int yGoal,
                                         StateObservation stateObs, ArrayList<Observation> goalGems,
                                         ElapsedCpuTimer elapsedTimer, long timeThreshold,
                                         ArrayList<Observation> ignoreList,
                                         boolean[][] initialBoulderMap, boolean[][] initialGroundMap) {
        // Creo el objeto que va a guardar la información para el método getHeuristicGems
        // sobre la distancia de las distintas listas de gemas
        mapaCircuitos.clear();

        // Create new plan
        PathInformation plan = new PathInformation();

        // Set up boolean values for found gem and timeout
        boolean foundGoal = false;
        boolean timeout = false;

        // Get game grid
        ArrayList<Observation>[][] grid = this.getObservationGrid(stateObs);

        // Create variables for current node and observation
        GridNode currentNode;
        Observation currentObservation;

        // Set constants like grid size, walls, actions, orientations and goal grid
        final int XMAX = grid.length, YMAX = grid[0].length;
        final ObservationType WALL = ObservationType.WALL;
        final Types.ACTIONS[] actions = {Types.ACTIONS.ACTION_UP, Types.ACTIONS.ACTION_RIGHT, Types.ACTIONS.ACTION_DOWN, Types.ACTIONS.ACTION_LEFT};
        final Orientation[] orientations = {Orientation.N, Orientation.E, Orientation.S, Orientation.W};
        final Observation goal = grid[xGoal][yGoal].get(0);

        // Create data structures that will store the information
        PriorityQueue<GridNode> openList;
        LinkedList<GridNode> closedList;
        HashSet<GridNode> exploredList;
        ArrayList<boolean [][]> boulderConfigurations;
        int exploredStates;


        // If there was no previous information about a search, create new information
        if (searchInfo.isEmpty()) {
            openList = new PriorityQueue<>( (GridNode n1, GridNode n2) -> n1.getfCost() - n2.getfCost() );
            closedList = new LinkedList<>();
            exploredList = new HashSet<>();
            boulderConfigurations = new ArrayList<>();
            exploredStates = 1;


            // Boulder map (contains boulders and walls)
            boolean[][] boulderMap = new boolean[XMAX][YMAX];
            ArrayList<Observation> boulders = this.getBouldersList(stateObs);
            ArrayList<Observation> walls = this.getWallsList(stateObs);
            ArrayList<Observation> obstacles = (ArrayList<Observation>) boulders.clone();
            obstacles.addAll(walls);

            if (initialBoulderMap == null) {
                UtilAlgorithms.initMap(boulderMap, obstacles, XMAX, YMAX);
            } else {
                boulderMap = initialBoulderMap;
            }

            // Ground map
            boolean[][] groundMap = new boolean[XMAX][YMAX];
            ArrayList<Observation> groundList = this.getGroundTilesList(stateObs);

            if (initialGroundMap == null) {
                UtilAlgorithms.initMap(groundMap, groundList, XMAX, YMAX);
            } else {
                groundMap = initialGroundMap;
            }

            // Gems map
            boolean[][] gemsMap = new boolean[XMAX][YMAX];
            ArrayList<Observation> gemsList = this.getGemsList(stateObs);

            UtilAlgorithms.initMap(gemsMap, gemsList, XMAX, YMAX);

            // Simulate initial boulder fall
            UtilAlgorithms.simulateBoulderFall(boulders, boulderMap, groundMap, gemsMap, grid);

            boulderConfigurations.add(boulderMap);

            // Add first node
            openList.add(new GridNode(0, this.getHeuristicDistance(startingPos, goal),
                    null, startingPos, startingPos.getOrientation(), 0,
                    groundMap, gemsMap, false, goalGems.size(), goalGems, null));
        } else {
            // Load previous search information if there was a timeout
            openList = searchInfo.getOpenList();
            closedList = searchInfo.getClosedList();
            exploredList = searchInfo.getExploredList();
            boulderConfigurations = searchInfo.getBoulderConfigurations();
            exploredStates = searchInfo.getExploredStates();
        }

        while (!foundGoal && !openList.isEmpty()) {

            // Check wether there's a timeout
            if (elapsedTimer.remainingTimeMillis() <= timeThreshold) {
                timeout = true;
                break;
            }

            if (exploredStates >= SearchInformation.getMaxStates()) {
                break;
            }

            // Get first node
            currentNode = openList.poll();

            // Get current observation, boulder map and ground map
            currentObservation = currentNode.getPosition();
            boolean[][] currentBoulders = boulderConfigurations.get(currentNode.getBoulderIndex());
            boolean[][] currentGround = currentNode.getGroundMap();
            boolean[][] currentGems = currentNode.getGemsMap();

            int remainingGems = currentNode.getRemainingGems();
            ArrayList<Observation> currentGemsList = currentNode.getGemsList();

            if (currentObservation.getX() == xGoal && currentObservation.getY() == yGoal && remainingGems == 0) {
                foundGoal = true;
            } else if (currentObservation.getX() == xGoal && currentObservation.getY() == yGoal
                    && remainingGems != 0 && exploredStates != 1) {
                closedList.addFirst(currentNode);
                continue;
            } else {
                // Get list of neighbours
                ArrayList<Observation> neighbours = this.getNeighbours(currentObservation, grid);

                // Iterate over each neighbour
                for (int i = 0; i < neighbours.size(); i++) {
                    // Set next grid to explore and get its position
                    Observation nextGrid = neighbours.get(i);
                    int x = nextGrid.getX(), y = nextGrid.getY();

                    // Skip forbidden grid if it's the north grid
                    if (i == 0 && currentNode.getForbiAboveGrid()) {
                        continue;
                    }

                    // Check if the grid is not a boulder in the current boulder map
                    if (!currentBoulders[x][y]) {
                        int numberActions = 1;
                        int bouldIndx = currentNode.getBoulderIndex();
                        Observation nextPosition = nextGrid;
                        boolean[][] nextGround = new boolean[XMAX][YMAX];
                        boolean forbidAboveGrid = false;
                        int nextRemainingGems = remainingGems;
                        ArrayList<Observation> nextGemsList = (ArrayList<Observation>) currentGemsList.clone();
                        boolean[][] nextGemsMap = new boolean[XMAX][YMAX];

                        // Copy the current ground and set the current grid as not ground
                        UtilAlgorithms.copy2DArray(currentGround, nextGround, XMAX, YMAX);
                        UtilAlgorithms.copy2DArray(currentGems, nextGemsMap, XMAX, YMAX);

                        // Set the grid ground as digged (false)
                        nextGround[x][y] = false;

                        // Add actions
                        LinkedList<Types.ACTIONS> actionList = new LinkedList<>();
                        actionList.addFirst(actions[i]);

                        // Check wether an extra action must be done (a turn)
                        if (!currentNode.getOrientation().equals(orientations[i])) {
                            numberActions++;
                            actionList.addFirst(actions[i]);
                        }

                        if (currentGems[x][y] && nextGemsList.contains(nextGrid)) {
                            nextGemsList.remove(nextGemsList.indexOf(nextGrid));
                            nextGemsMap[x][y] = false;
                            nextRemainingGems--;

                        }

                        // Check wether there's a boulder above the current grid and it's nor a gem
                        // nor a wall
                        if (currentBoulders[x][y - 1] && !grid[x][y-1].get(0).getType().equals(WALL)) {

                            // Crete new boulder map and copy its old values
                            boolean[][] newBoulders = new boolean[XMAX][YMAX];
                            UtilAlgorithms.copy2DArray(currentBoulders, newBoulders, XMAX, YMAX);

                            int numberBoulders = 0;
                            int boulderPos = y - 1;
                            int emptyPos = y + 1;

                            // Find out number of boulders above the current grid
                            // and the index of the highest grid containing a boulder
                            while (newBoulders[x][boulderPos] && !grid[x][boulderPos].get(0).getType().equals(WALL)) {
                                numberBoulders++;
                                boulderPos--;
                            }

                            // Find out the index of the last empty space
                            while (!nextGround[x][emptyPos] && (!grid[x][emptyPos].get(0).getType().equals(WALL) && !nextGemsMap[x][emptyPos] && !currentBoulders[x][emptyPos])) {
                                emptyPos++;
                            }

                            // Modify the boulder map, moving the boulders
                            for (int j = emptyPos - 1; j > boulderPos; j--) {
                                if (j > emptyPos - 1 - numberBoulders) {
                                    newBoulders[x][j] = true;
                                } else {
                                    newBoulders[x][j] = false;
                                }
                            }

                            // Add the new boulder configuration and update boulder map index
                            boulderConfigurations.add(newBoulders);
                            bouldIndx = boulderConfigurations.indexOf(newBoulders);

                            // Set the next position as the same as now
                            nextPosition = currentNode.getPosition();

                            // Forbid the above grid if the current grid is the above grid
                            // and the agent has mined or if the agent has mined another grid
                            // and hasn't changed its position and the previous grid had forbidden
                            // that movement
                            if ((i == 0) || (nextPosition.getX() == currentNode.getPosition().getX() && currentNode.getForbiAboveGrid())) {
                                forbidAboveGrid = true;
                            }

                        }

                        // Compute the heuristic value (h)
                        int heuristic;

                        if (nextRemainingGems > 0) {
                            heuristic = this.getHeuristicGems(nextPosition, goal, nextGemsList);
                        } else {
                            heuristic = this.getHeuristicDistance(nextPosition, goal);
                        }

                        // Check if the agent is trying to go to the above grid without changing its X position
                        // after moving a boulder above him
                        if ((nextPosition.getX() == currentNode.getPosition().getX() && currentNode.getForbiAboveGrid())) {
                            forbidAboveGrid = true;
                        }

                        // Skip nextPosition if its contained in ignoreList
                        if (ignoreList != null && ignoreList.contains(nextPosition)) {
                            continue;
                        }

                        // Create new grid node
                        GridNode node = new GridNode(currentNode.getgCost() + numberActions,
                                                        heuristic, actionList, nextPosition, orientations[i],
                                                        bouldIndx, nextGround, nextGemsMap, forbidAboveGrid,
                                                        nextRemainingGems, nextGemsList, currentNode);

                        // Add the node to the explored list
                        if (exploredList.add(node)) {
                            exploredStates++;
                            openList.add(node);
                        }
                    }
                }
            }

            // Add the current node to the closed list
            closedList.addFirst(currentNode);
        }

        // Check wether there's a timeout
        if (timeout) {
            searchInfo = new SearchInformation(openList, closedList, exploredList, boulderConfigurations, exploredStates);
            plan.plan.add(Types.ACTIONS.ACTION_NIL);
            return plan;
        }


        // Save the path information
        if (foundGoal) {
            // Get the last explored grid (goal grid)
            GridNode path = closedList.getFirst();
            plan.groundMap = path.getGroundMap();
            plan.boulderMap = boulderConfigurations.get(path.getBoulderIndex());
            plan = parsePlan(path);
        } else {
            plan.existsPath = false;
        }

        plan.searchComplete = true;

        searchInfo = new SearchInformation();
        return plan;
    }

    ////////////////////////////////////////////////////////////////////////////////
    // Versiones que paran cuando han cogido todas las gemas

    // Sobrecarga de pathExplorer para cuando la posición de inicio es la del
    // jugador de stateObs
    private PathInformation pathExplorer(StateObservation stateObs, ArrayList<Observation> goalGems,
                                         ElapsedCpuTimer elapsedTimer, long timeThreshold,
                                         ArrayList<Observation> ignoreList){
        return pathExplorer(this.getPlayer(stateObs), stateObs,
                goalGems, elapsedTimer, timeThreshold, ignoreList,
                null, null);
    }

    private PathInformation pathExplorer(PlayerObservation startingPos,
                                         StateObservation stateObs, ArrayList<Observation> goalGems,
                                         ElapsedCpuTimer elapsedTimer, long timeThreshold,
                                         ArrayList<Observation> ignoreList,
                                         boolean[][] initialBoulderMap, boolean[][] initialGroundMap) {
        // Creo el objeto que va a guardar la información para el método getHeuristicGems
        // sobre la distancia de las distintas listas de gemas
        mapaCircuitos.clear();

        // Create new plan
        PathInformation plan = new PathInformation();

        // Set up boolean values for found gem and timeout
        boolean foundGoal = false;
        boolean timeout = false;

        // Get game grid
        ArrayList<Observation>[][] grid = this.getObservationGrid(stateObs);

        // Create variables for current node and observation
        GridNode currentNode;
        Observation currentObservation;

        // Set constants like grid size, walls, actions, orientations and goal grid
        final int XMAX = grid.length, YMAX = grid[0].length;
        final ObservationType WALL = ObservationType.WALL;
        final Types.ACTIONS[] actions = {Types.ACTIONS.ACTION_UP, Types.ACTIONS.ACTION_RIGHT, Types.ACTIONS.ACTION_DOWN, Types.ACTIONS.ACTION_LEFT};
        final Orientation[] orientations = {Orientation.N, Orientation.E, Orientation.S, Orientation.W};

        // Create data structures that will store the information
        PriorityQueue<GridNode> openList;
        LinkedList<GridNode> closedList;
        HashSet<GridNode> exploredList;
        ArrayList<boolean [][]> boulderConfigurations;
        int exploredStates;


        // If there was no previous information about a search, create new information
        if (searchInfo.isEmpty()) {
            openList = new PriorityQueue<>( (GridNode n1, GridNode n2) -> n1.getfCost() - n2.getfCost() );
            closedList = new LinkedList<>();
            exploredList = new HashSet<>();
            boulderConfigurations = new ArrayList<>();
            exploredStates = 1;


            // Boulder map (contains boulders and walls)
            boolean[][] boulderMap = new boolean[XMAX][YMAX];
            ArrayList<Observation> boulders = this.getBouldersList(stateObs);
            ArrayList<Observation> walls = this.getWallsList(stateObs);
            ArrayList<Observation> obstacles = (ArrayList<Observation>) boulders.clone();
            obstacles.addAll(walls);

            if (initialBoulderMap == null) {
                UtilAlgorithms.initMap(boulderMap, obstacles, XMAX, YMAX);
            } else {
                boulderMap = initialBoulderMap;
            }

            // Ground map
            boolean[][] groundMap = new boolean[XMAX][YMAX];
            ArrayList<Observation> groundList = this.getGroundTilesList(stateObs);

            if (initialGroundMap == null) {
                UtilAlgorithms.initMap(groundMap, groundList, XMAX, YMAX);
            } else {
                groundMap = initialGroundMap;
            }

            // Gems map
            boolean[][] gemsMap = new boolean[XMAX][YMAX];
            ArrayList<Observation> gemsList = this.getGemsList(stateObs);

            UtilAlgorithms.initMap(gemsMap, gemsList, XMAX, YMAX);

            // Simulate initial boulder fall
            UtilAlgorithms.simulateBoulderFall(boulders, boulderMap, groundMap, gemsMap, grid);

            boulderConfigurations.add(boulderMap);

            // Add first node
            openList.add(new GridNode(0, this.getHeuristicGems(startingPos, gemsList),
                    null, startingPos, startingPos.getOrientation(), 0,
                    groundMap, gemsMap, false, goalGems.size(), goalGems, null));
        } else {
            // Load previous search information if there was a timeout
            openList = searchInfo.getOpenList();
            closedList = searchInfo.getClosedList();
            exploredList = searchInfo.getExploredList();
            boulderConfigurations = searchInfo.getBoulderConfigurations();
            exploredStates = searchInfo.getExploredStates();
        }

        while (!foundGoal && !openList.isEmpty()) {

            // Check wether there's a timeout
            if (elapsedTimer.remainingTimeMillis() <= timeThreshold) {
                timeout = true;
                break;
            }

            if (exploredStates >= SearchInformation.getMaxStates()) {
                break;
            }

            // Get first node
            currentNode = openList.poll();

            // Get current observation, boulder map and ground map
            currentObservation = currentNode.getPosition();
            boolean[][] currentBoulders = boulderConfigurations.get(currentNode.getBoulderIndex());
            boolean[][] currentGround = currentNode.getGroundMap();
            boolean[][] currentGems = currentNode.getGemsMap();

            int remainingGems = currentNode.getRemainingGems();
            ArrayList<Observation> currentGemsList = currentNode.getGemsList();

            if (remainingGems == 0) {
                foundGoal = true;
            } else {
                // Get list of neighbours
                ArrayList<Observation> neighbours = this.getNeighbours(currentObservation, grid);

                // Iterate over each neighbour
                for (int i = 0; i < neighbours.size(); i++) {
                    // Set next grid to explore and get its position
                    Observation nextGrid = neighbours.get(i);
                    int x = nextGrid.getX(), y = nextGrid.getY();

                    // Skip forbidden grid if it's the north grid
                    if (i == 0 && currentNode.getForbiAboveGrid()) {
                        continue;
                    }

                    // Check if the grid is not a boulder in the current boulder map
                    if (!currentBoulders[x][y]) {
                        int numberActions = 1;
                        int bouldIndx = currentNode.getBoulderIndex();
                        Observation nextPosition = nextGrid;
                        boolean[][] nextGround = new boolean[XMAX][YMAX];
                        boolean forbidAboveGrid = false;
                        int nextRemainingGems = remainingGems;
                        ArrayList<Observation> nextGemsList = (ArrayList<Observation>) currentGemsList.clone();
                        boolean[][] nextGemsMap = new boolean[XMAX][YMAX];

                        // Copy the current ground and set the current grid as not ground
                        UtilAlgorithms.copy2DArray(currentGround, nextGround, XMAX, YMAX);
                        UtilAlgorithms.copy2DArray(currentGems, nextGemsMap, XMAX, YMAX);

                        // Set the grid ground as digged (false)
                        nextGround[x][y] = false;

                        // Add actions
                        LinkedList<Types.ACTIONS> actionList = new LinkedList<>();
                        actionList.addFirst(actions[i]);

                        // Check wether an extra action must be done (a turn)
                        if (!currentNode.getOrientation().equals(orientations[i])) {
                            numberActions++;
                            actionList.addFirst(actions[i]);
                        }

                        if (currentGems[x][y] && nextGemsList.contains(nextGrid)) {
                            nextGemsList.remove(nextGemsList.indexOf(nextGrid));
                            nextGemsMap[x][y] = false;
                            nextRemainingGems--;

                        }

                        // Check wether there's a boulder above the current grid and it's nor a gem
                        // nor a wall
                        if (currentBoulders[x][y - 1] && !grid[x][y-1].get(0).getType().equals(WALL)) {

                            // Crete new boulder map and copy its old values
                            boolean[][] newBoulders = new boolean[XMAX][YMAX];
                            UtilAlgorithms.copy2DArray(currentBoulders, newBoulders, XMAX, YMAX);

                            int numberBoulders = 0;
                            int boulderPos = y - 1;
                            int emptyPos = y + 1;

                            // Find out number of boulders above the current grid
                            // and the index of the highest grid containing a boulder
                            while (newBoulders[x][boulderPos] && !grid[x][boulderPos].get(0).getType().equals(WALL)) {
                                numberBoulders++;
                                boulderPos--;
                            }

                            // Find out the index of the last empty space
                            while (!nextGround[x][emptyPos] && (!grid[x][emptyPos].get(0).getType().equals(WALL) && !nextGemsMap[x][emptyPos] && !currentBoulders[x][emptyPos])) {
                                emptyPos++;
                            }

                            // Modify the boulder map, moving the boulders
                            for (int j = emptyPos - 1; j > boulderPos; j--) {
                                if (j > emptyPos - 1 - numberBoulders) {
                                    newBoulders[x][j] = true;
                                } else {
                                    newBoulders[x][j] = false;
                                }
                            }

                            // Add the new boulder configuration and update boulder map index
                            boulderConfigurations.add(newBoulders);
                            bouldIndx = boulderConfigurations.indexOf(newBoulders);

                            // Set the next position as the same as now
                            nextPosition = currentNode.getPosition();

                            // Forbid the above grid if the current grid is the above grid
                            // and the agent has mined or if the agent has mined another grid
                            // and hasn't changed its position and the previous grid had forbidden
                            // that movement
                            if ((i == 0) || (nextPosition.getX() == currentNode.getPosition().getX() && currentNode.getForbiAboveGrid())) {
                                forbidAboveGrid = true;
                            }

                        }

                        // Compute the heuristic value (h)
                        int heuristic;

                        if (nextRemainingGems > 0) {
                            heuristic = this.getHeuristicGems(nextPosition, nextGemsList);
                        } else {
                            heuristic = 0;
                        }

                        // Check if the agent is trying to go to the above grid without changing its X position
                        // after moving a boulder above him
                        if ((nextPosition.getX() == currentNode.getPosition().getX() && currentNode.getForbiAboveGrid())) {
                            forbidAboveGrid = true;
                        }

                        // Skip nextPosition if its contained in ignoreList
                        if (ignoreList != null && ignoreList.contains(nextPosition)) {
                            continue;
                        }

                        // Create new grid node
                        GridNode node = new GridNode(currentNode.getgCost() + numberActions,
                                heuristic, actionList, nextPosition, orientations[i],
                                bouldIndx, nextGround, nextGemsMap, forbidAboveGrid,
                                nextRemainingGems, nextGemsList, currentNode);

                        // Add the node to the explored list
                        if (exploredList.add(node)) {
                            exploredStates++;
                            openList.add(node);
                        }
                    }
                }
            }

            // Add the current node to the closed list
            closedList.addFirst(currentNode);
        }

        // Check wether there's a timeout
        if (timeout) {
            searchInfo = new SearchInformation(openList, closedList, exploredList, boulderConfigurations, exploredStates);
            plan.plan.add(Types.ACTIONS.ACTION_NIL);
            return plan;
        }


        // Save the path information
        if (foundGoal) {
            // Get the last explored grid (goal grid)
            GridNode path = closedList.getFirst();
            plan.groundMap = path.getGroundMap();
            plan.boulderMap = boulderConfigurations.get(path.getBoulderIndex());
            plan = parsePlan(path);
        } else {
            plan.existsPath = false;
        }

        plan.searchComplete = true;

        searchInfo = new SearchInformation();
        return plan;
    }

    /**
     * Metodo que parsea una sucesion de nodos y devuelve informacion
     * del plan.
     * @param gridPath Objeto de la clase GridNode desde el que se empieza
     *                 a parsear el camino
     * @return Devuelve un nuevo plan, conteniendo la lista de acciones,
     *         las casillas por las que se pasan y la distancia recorrida
     */
    private PathInformation parsePlan(GridNode gridPath) {
        PathInformation plan = new PathInformation();

        while (gridPath.getParent() != null) {
            plan.plan.addAll(0, gridPath.getActionList());
            plan.listaCasillas.add(0, gridPath.getPosition());
            plan.distancia++;

            gridPath = gridPath.getParent();
        }

        plan.listaCasillas.add(0, gridPath.getPosition());
        plan.distancia++;

        plan.existsPath = true;

        return plan;
    }

    private boolean connectionToEnemy(StateObservation stateObs, Types.ACTIONS action, Observation enemy, PlayerObservation player) {

        if (player == null || action == null || player.hasDied()) // Si el jugador ha muerto tras aplicar la acción
            return true;
        
        
        // Get grid after applying the given action
        ArrayList<Observation>[][] grid = this.getObservationGrid(stateObs);

        // Get player positions
        int xPlayer = player.getX(), yPlayer = player.getY();
        
        if (player.getOrientation().equals(Orientation.N) && action.equals(Types.ACTIONS.ACTION_UP)) {
            yPlayer--;
        } else if (player.getOrientation().equals(Orientation.S) && action.equals(Types.ACTIONS.ACTION_DOWN)) {
            yPlayer++;
        } else if (player.getOrientation().equals(Orientation.E) && action.equals(Types.ACTIONS.ACTION_RIGHT)) {
            xPlayer++;
        } else if (player.getOrientation().equals(Orientation.W) && action.equals(Types.ACTIONS.ACTION_LEFT)) {
            xPlayer--;
        }

        // Get next position
        Observation nextPlayerPos = new Observation(xPlayer, yPlayer, ObservationType.EMPTY);

        if(nextPlayerPos.getX() == enemy.getX() && nextPlayerPos.getY() == enemy.getY())
            return true;

        // Create constant expressions
        final ObservationType EMPTY = ObservationType.EMPTY,
                              PLAYER = ObservationType.PLAYER;
        final int XMAX = grid.length, YMAX = grid[0].length;

        // Create new explored map
        boolean[][] exploredMap = new boolean[XMAX][YMAX];

        for (int x = 0; x < XMAX; x++) {
            for (int y = 0; y < YMAX; y++) {
                exploredMap[x][y] = false;
            }
        }

        exploredMap[enemy.getX()][enemy.getY()] = true;

        // Set boolean variable to check if the player has been found
        boolean foundPlayer = false;

        // Create new list of opened and closed observations
        LinkedList<GridNode> openList = new LinkedList<>();
        LinkedList<GridNode> closedList = new LinkedList<>();

        // Add enemy position
        openList.addFirst(new GridNode(enemy, null));

        // Set variables used in search
        GridNode currentNode;
        Observation currentObservation;

        // Search for the player
        while (!foundPlayer && !openList.isEmpty()) {
            // Get current node, observation and position
            currentNode = openList.pollFirst();
            currentObservation = currentNode.getPosition();
            int currentX = currentObservation.getX(), currentY = currentObservation.getY();

            // Finish if the player has been found
            if (currentX == nextPlayerPos.getX() && currentY == nextPlayerPos.getY()) {
                foundPlayer = true;
            } else {
                // Get neighbors
                ArrayList<Observation> nextGrids = getNeighbours(currentObservation, grid);

                // Iterate over each neighbor
                for (Observation obs: nextGrids) {
                    int x = obs.getX(), y = obs.getY();

                    // Add neighbor if it hasn't been explored
                    // Only neighbors which are empty or the player position are added
                    if (!exploredMap[x][y] && (obs.getType().equals(EMPTY) || (x == xPlayer && y == yPlayer))) {
                        openList.addLast(new GridNode(obs, currentNode));
                        exploredMap[x][y] = true;
                    }
                }
            }

            closedList.addFirst(currentNode);
        }
        return foundPlayer;
    }

    private int getHeuristicGems(Observation start, Observation goal, ArrayList<Observation> gems) {
        return getHeuristicGems(start.getX(), start.getY(), goal.getX(), goal.getY(), gems);
    }
    
    // Heurística para el A* que tiene que coger varias gemas
    // Devuelve la heurística asociada a ir desde la casilla xStart, yStart
    // coger todas las gemas de "gems" y acabar en la casilla xGoal, yGoal
    // Cota usada: la distancia es la suma de la distancia de ir de start a
    // la gema más cercana, los "n-1" lados más cortos del grafo formado por las gemas
    // y la distancia de ir desde goal a la gema más cercana
    private int getHeuristicGems(int xStart, int yStart, int xGoal, int yGoal, ArrayList<Observation> gems){
        final float alfa = 2f; // Valor por el que se multiplica el valor de la heurística: pierde admisibilidad y monotonía pero es más eficiente
        
        int total_dist = 0;
        
        // Veo si en mapaCircuitos está guardada la información sobre esta lista de gemas

        int dist_guardada = mapaCircuitos.getOrDefault(gems, -1).intValue();
        
        // La distancia del circuito ya estaba guardada -> solo tengo que calcular
        // la distancia de xStart, yStart a la gema más cercana y sumarle la distancia guardada
        if (dist_guardada != -1){
            int min_dist_orig = 1000;
            int dist_orig;

            for (Observation gem: gems){
                dist_orig = getHeuristicDistance(gem.getX(), gem.getY(), xStart, yStart);

                if (dist_orig < min_dist_orig)
                    min_dist_orig = dist_orig;
            }
            
            total_dist = min_dist_orig + dist_guardada;
        }
        // La distancia no estaba guardada -> la calculo desde 0 y guardo la suma
        // de la distancia de los n-1 lados más cortos y de ir desde goal a la gema
        // más cercana en el hashMap
        else{
            // Primero calculo la distancia optimista (cota inferior) para coger todas las gemas de la lista
            // Calculo una matriz de distancias entre las gemas (usando getHeuristicDistance)
            int num_gems = gems.size();
            int[][] dist_matrix = new int[num_gems][num_gems]; // Matriz triangular inferior

            Observation gem_i;
            int x_gem_i, y_gem_i;
            for (int i = 1; i < num_gems; i++){
                gem_i = gems.get(i);
                x_gem_i = gem_i.getX();
                y_gem_i = gem_i.getY();

                for (int j = 0; j < i; j++){
                    dist_matrix[i][j] = getHeuristicDistance(x_gem_i, y_gem_i, gems.get(j).getX(), gems.get(j).getY());
                }
            }

            // Ahora calculo las "num_gems-1" distancias más pequeñas entre las gemas
            int[] smallest_dist = new int[num_gems-1];

            for (int i = 0; i < num_gems-1; i++)
                smallest_dist[i] = 1000;

            int dist_actual;
            for (int i = 1; i < num_gems; i++){         
                for (int j = 0; j < i; j++){
                    dist_actual = dist_matrix[i][j];

                    // Calculo el máximo del vector smallest_dist
                    int max_dist = -1;
                    int ind_max = -1;
                    for (int k = 0; k < num_gems-1; k++){
                        if (smallest_dist[k] > max_dist){
                            max_dist = smallest_dist[k];
                            ind_max = k;
                        }
                    }

                    // Si dist_actual es menor que ese valor, lo sustituyo
                    if (dist_actual < max_dist)
                        smallest_dist[ind_max] = dist_actual;     
                }
            }
            
            int sum_dist_grafo = 0;
            
            // Le sumo a la distancia total las distancias de smallest_dist
            for (int i = 0; i < num_gems-1; i++){
                total_dist += smallest_dist[i];
                sum_dist_grafo += smallest_dist[i];
            }

            // Calculo las gemas más cercanas al punto de inicio y de fin

            int min_dist_orig = 1000, min_dist_goal = 1000;
            int dist_orig, dist_goal;

            for (Observation gem: gems){
                dist_orig = getHeuristicDistance(gem.getX(), gem.getY(), xStart, yStart);
                dist_goal = getHeuristicDistance(gem.getX(), gem.getY(), xGoal, yGoal);

                if (dist_orig < min_dist_orig)
                    min_dist_orig = dist_orig;

                if (dist_goal < min_dist_goal)
                    min_dist_goal = dist_goal;
            }

            // Sumo esas distancias a la distancia total
            total_dist += min_dist_orig + min_dist_goal;  
            
            // Guardo sum_dist_grafo + min_dist_goal en el mapa, asociado a esta lista de gemas
            mapaCircuitos.put(gems, new Integer(sum_dist_grafo + min_dist_goal));
        }
                                                            
        return (int)(alfa*total_dist);
    }

    // getHeuristicGems sin objetivo con Observation inicial
    private int getHeuristicGems(Observation start, ArrayList<Observation> gems) {
        return getHeuristicGems(start.getX(), start.getY(), gems);
    }

    // getHeuristicGems sin objetivo
    private int getHeuristicGems(int xStart, int yStart, ArrayList<Observation> gems){
        final float alfa = 2f; // Valor por el que se multiplica el valor de la heurística: pierde admisibilidad y monotonía pero es más eficiente

        int total_dist = 0;

        // Veo si en mapaCircuitos está guardada la información sobre esta lista de gemas

        int dist_guardada = mapaCircuitos.getOrDefault(gems, -1).intValue();

        // La distancia del circuito ya estaba guardada -> solo tengo que calcular
        // la distancia de xStart, yStart a la gema más cercana y sumarle la distancia guardada
        if (dist_guardada != -1){
            int min_dist_orig = 1000;
            int dist_orig;

            for (Observation gem: gems){
                dist_orig = getHeuristicDistance(gem.getX(), gem.getY(), xStart, yStart);

                if (dist_orig < min_dist_orig)
                    min_dist_orig = dist_orig;
            }

            total_dist = min_dist_orig + dist_guardada;
        }
        // La distancia no estaba guardada -> la calculo desde 0 y guardo la suma
        // de la distancia de los n-1 lados más cortos y de ir desde goal a la gema
        // más cercana en el hashMap
        else{
            // Primero calculo la distancia optimista (cota inferior) para coger todas las gemas de la lista
            // Calculo una matriz de distancias entre las gemas (usando getHeuristicDistance)
            int num_gems = gems.size();
            int[][] dist_matrix = new int[num_gems][num_gems]; // Matriz triangular inferior

            Observation gem_i;
            int x_gem_i, y_gem_i;
            for (int i = 1; i < num_gems; i++){
                gem_i = gems.get(i);
                x_gem_i = gem_i.getX();
                y_gem_i = gem_i.getY();

                for (int j = 0; j < i; j++){
                    dist_matrix[i][j] = getHeuristicDistance(x_gem_i, y_gem_i, gems.get(j).getX(), gems.get(j).getY());
                }
            }

            // Ahora calculo las "num_gems-1" distancias más pequeñas entre las gemas
            int[] smallest_dist = new int[num_gems-1];

            for (int i = 0; i < num_gems-1; i++)
                smallest_dist[i] = 1000;

            int dist_actual;
            for (int i = 1; i < num_gems; i++){
                for (int j = 0; j < i; j++){
                    dist_actual = dist_matrix[i][j];

                    // Calculo el máximo del vector smallest_dist
                    int max_dist = -1;
                    int ind_max = -1;
                    for (int k = 0; k < num_gems-1; k++){
                        if (smallest_dist[k] > max_dist){
                            max_dist = smallest_dist[k];
                            ind_max = k;
                        }
                    }

                    // Si dist_actual es menor que ese valor, lo sustituyo
                    if (dist_actual < max_dist)
                        smallest_dist[ind_max] = dist_actual;
                }
            }

            int sum_dist_grafo = 0;

            // Le sumo a la distancia total las distancias de smallest_dist
            for (int i = 0; i < num_gems-1; i++){
                total_dist += smallest_dist[i];
                sum_dist_grafo += smallest_dist[i];
            }

            // Calculo las gemas más cercanas al punto de inicio

            int min_dist_orig = 1000;
            int dist_orig;

            for (Observation gem: gems){
                dist_orig = getHeuristicDistance(gem.getX(), gem.getY(), xStart, yStart);

                if (dist_orig < min_dist_orig)
                    min_dist_orig = dist_orig;
            }

            // Sumo esas distancias a la distancia total
            total_dist += min_dist_orig;

            // Guardo sum_dist_grafo + min_dist_goal en el mapa, asociado a esta lista de gemas
            mapaCircuitos.put(gems, new Integer(sum_dist_grafo));
        }

        return (int)(alfa*total_dist);
    }
    
    // Obtiene las distancias aproximadas entre cada pareja de clusters usando una matriz de distancias
    // Se calcula primero para cada pareja de clusters las dos gemas más cercanas
    // entre sí usando la distancia Manhattan y después, con el pathfinder
    // se calcula la distancia entre esos dos clusters
    
    // Guarda la matriz de distancias dentro del objeto clust_inf, de la clase Cluster Information
    // Se ha creado como método de esta clase en vez de ClusterInformation para así poder
    // usar getHeuristicDistance
    private void saveClustersDistances(ClusterInformation clust_inf){   
        int num_clusters = clust_inf.clusters.size();
        int[][] matriz_dist = new int[num_clusters][num_clusters]; // Valor inicial -> 0
        Cluster cluster1, cluster2;
        int num_gems_1, num_gems_2;
        Observation gem_act, gem_act_2;
        int x_act, y_act;
        int min_dist, ind_gem_1 = -1, ind_gem_2 = -1, this_dist;
        Observation gem1, gem2;
        int heuristic_dist;
        PathInformation plan;
        
        // d(a,b) = d(b,a) por lo que solo recorro la diagonal inferior de la matriz
        for (int i = 1; i < num_clusters; i++)
            for (int j = 0; j < i; j++){
                // Calculo la pareja de gemas más cercanas (según el getHeuristicDistance) para esos dos clusters 
                cluster1 = clust_inf.clusters.get(i);
                cluster2 = clust_inf.clusters.get(j);
                
                num_gems_1 = cluster1.getNumGems();
                num_gems_2 = cluster2.getNumGems();
                
                min_dist = 1000;
                
                for (int k = 0; k < num_gems_1; k++){
                    gem_act = cluster1.getGem(k);
                    x_act = gem_act.getX();
                    y_act = gem_act.getY();
                    
                    for (int l = 0; l < num_gems_2; l++){
                        gem_act_2 = cluster2.getGem(l);
                        this_dist = getHeuristicDistance(x_act, y_act, gem_act_2.getX(), gem_act_2.getY());
                        
                        // Valdrá -1 si no existe camino
                        if (this_dist != -1 && this_dist < min_dist){
                            min_dist = this_dist;
                            ind_gem_1 = k;
                            ind_gem_2 = l;
                        }
                    }
                }
                 
                // Calculo usando getHeuristicDistance la distancia entre esas dos gemas
                
                gem1 = cluster1.getGem(ind_gem_1);
                gem2 = cluster2.getGem(ind_gem_2);
                heuristic_dist = getHeuristicDistance(gem1.getX(), gem1.getY(), gem2.getX(), gem2.getY());
                             
                matriz_dist[i][j] = matriz_dist[j][i] = heuristic_dist; // Guardo en la matriz esa distancia como la distancia entre los 2 clústeres
            }
        
        clust_inf.matriz_dist = matriz_dist; // Guardo la matriz de distancias en clust_inf
    }

    // Crea el mejor circuito entre los clústeres de clust_inf y lo guarda dentro de este objeto
    // Start se corresponde con la casilla de inicio del circuito (generalmente la posición del jugador)
    // y Goal con la casilla de destino (generalmente la salida del nivel)
    private void saveCircuit(ClusterInformation clust_inf, int xStart, int yStart, int xGoal, int yGoal, int needed_gems){
        // Creo los 2 vectores de distancias desde Start y Goal a los clústeres de clust_inf
        int num_clusters = clust_inf.getNumClusters();
        
        int[] distClusterStart = new int[num_clusters];
        int[] distClusterGoal = new int[num_clusters];
        
        Cluster cluster_act;
        Observation gem_act;
        int num_gems;
        int min_dist_start;
        int min_dist_goal;
        int x_act, y_act;
        int this_dist_start;
        int this_dist_goal;
        int ind_gem_start = -1;
        int ind_gem_goal = -1;
        Observation gem_goal, gem_start;
        
        for (int i = 0; i < num_clusters; i++){
            // Calculo la pareja de gemas más cercanas (según el getHeuristicDistance) para esos dos clusters 
            cluster_act = clust_inf.clusters.get(i);
                
            num_gems = cluster_act.getNumGems();
                
            min_dist_start = 1000;
            min_dist_goal = 1000;
                
            for (int k = 0; k < num_gems; k++){
                gem_act = cluster_act.getGem(k);
                x_act = gem_act.getX();
                y_act = gem_act.getY();
                    
                this_dist_start = getHeuristicDistance(x_act, y_act, xStart, yStart);
                this_dist_goal = getHeuristicDistance(x_act, y_act, xGoal, yGoal);
                        
                // Valdrá -1 si no existe camino
                if (this_dist_start != -1 && this_dist_start < min_dist_start){
                    min_dist_start = this_dist_start;
                    ind_gem_start = k;
                }

                if (this_dist_goal != -1 && this_dist_goal < min_dist_goal){
                    min_dist_goal = this_dist_goal;
                    ind_gem_goal = k;
                }
            }
                 
            // Calculo usando getHeuristicDistance la distancia entre esas gemas con Start y Goal    
            gem_goal = cluster_act.getGem(ind_gem_goal);
            gem_start = cluster_act.getGem(ind_gem_start);
            
            distClusterStart[i] = getHeuristicDistance(gem_start.getX(), gem_start.getY(), xStart, yStart);
            distClusterGoal[i] = getHeuristicDistance(gem_goal.getX(), gem_goal.getY(), xGoal, yGoal);
        }

        // Llamo al método createCircuit de clusterInformation
        clust_inf.createCircuit(xStart, yStart, xGoal, yGoal, distClusterStart, distClusterGoal, needed_gems);
    }

    // Dados dos clústeres, devuelve un punto intermedio entre los dos que sea lo más "seguro" posible
    // Es decir, que si existe camino se pueda llegar a él
    // Algoritmo: busca un punto cercano a la casilla intermedia entre las dos gemas más cercanas de los
    // dos clústeres. Esta casilla no puede tener ninguna gema a la derecha, izquierda, arriba ni abajo, ni tampoco ningún enemigo en esas casillas
    // Si ese punto no existe, se devuelve (-1, -1) y al A* no se le pasará ninguna casilla final (podrá terminar en la gema que sea)
    // Aparte ese punto no puede ser un muro (aunque sí puede tener muros alrededor)
    
    // Da error si se llama y los dos clusters son iguales!!
    
    private int[] getPuntoIntermedioClusters(ArrayList<Observation> gems_1, ArrayList<Observation>  gems_2, StateObservation stateObs){
        int[] casilla = new int[2];
        casilla[0] = -1;
        casilla[1] = -1;
        
        // Calculo la pareja de gemas más cercanas
        int num_gems_1 = gems_1.size();
        int num_gems_2 = gems_2.size();
        int min_dist = 10000;
        int ind_1 = -1;
        int ind_2 = -1;
        int dist;
        Observation this_gem;
        
        for (int i = 0; i < num_gems_1; i++){
            this_gem = gems_1.get(i);
            
            for (int j = 0; j < num_gems_2; j++){
                dist = this.getHeuristicDistance(this_gem, gems_2.get(j));
                
                if (dist < min_dist){
                    min_dist = dist;
                    ind_1 = i;
                    ind_2 = j;
                }   
            }
        }
        
        int x_1 = gems_1.get(ind_1).getX();
        int y_1 = gems_1.get(ind_1).getY();
        int x_2 = gems_2.get(ind_2).getX();
        int y_2 = gems_2.get(ind_2).getY();
        
        int x_centro = (x_1 + x_2) / 2; // Empiezo en el punto intermedio entre las dos gemas más cercanas
        int y_centro = (y_1 + y_2) / 2;
        
        ArrayList<Observation>[][] grid = this.getObservationGrid(stateObs);
        int ancho_grid = grid.length;
        int alto_grid = grid[0].length;
        
        boolean[][] matriz_rocas = new boolean[ancho_grid][alto_grid]; // Guardo las rocas en una matriz de booleanos
        boolean[][] matriz_muros = new boolean[ancho_grid][alto_grid]; // Guardo los muros en una matriz de booleanos
        boolean[][] matriz_enemigos = new boolean[ancho_grid][alto_grid]; // Guardo los enemigos en una matriz de booleanos
        
        for (int x = 0; x < ancho_grid; x++)
            for (int y = 0; y < alto_grid; y++){
                matriz_rocas[x][y] = false;
                matriz_muros[x][y] = false;
                matriz_enemigos[x][y] = false;
                
                for (Observation obs : grid[x][y]){
                    if (obs.getType() == ObservationType.BOULDER)
                        matriz_rocas[x][y] = true;
                    else if (obs.getType() == ObservationType.WALL)
                        matriz_muros[x][y] = true;
                    else if (obs.getType() == ObservationType.BAT)
                        matriz_enemigos[x][y] = true;
                    else if (obs.getType() == ObservationType.SCORPION)
                        matriz_enemigos[x][y] = true;
                }
            }
        
        
        // Exploro esa casilla y las que están en un radio de +3,-3 en x e y
         boolean casilla_encontrada = false;
        
        if (matriz_rocas[x_centro][y_centro] == false && matriz_muros[x_centro][y_centro] == false && matriz_enemigos[x_centro][y_centro] == false
                        && matriz_rocas[x_centro+1][y_centro] == false && matriz_enemigos[x_centro+1][y_centro] == false
                        && matriz_rocas[x_centro-1][y_centro] == false && matriz_enemigos[x_centro-1][y_centro] == false
                        && matriz_rocas[x_centro][y_centro+1] == false && matriz_enemigos[x_centro][y_centro+1] == false
                        && matriz_rocas[x_centro][y_centro-1] == false && matriz_enemigos[x_centro][y_centro-1] == false){
            casilla[0] = x_centro;
            casilla[1] = y_centro;
            casilla_encontrada = true;
            }
        
        int this_x, this_y;
        
        for (int x_add = 1; x_add <= 3 && !casilla_encontrada; x_add++)
            for (int y_add = 1; y_add <= 3 && !casilla_encontrada; y_add++){
                
                // Exploro las 8 casillas dadas por +-x_add y +-y_add
                for (int sig_x = -1; sig_x <= 1 && !casilla_encontrada; sig_x++) // Signo de x
                    for (int sig_y = -1; sig_y <=1 && !casilla_encontrada; sig_y++){ // Signo de y
                        
                        if (sig_x != 0 || sig_y != 0){ // No puede ser la casilla central
                            this_x = x_centro + x_add*sig_x;
                            this_y = y_centro + y_add*sig_y;
                            
                            
                            if (matriz_rocas[this_x][this_y] == false && matriz_muros[this_x][this_y] == false && matriz_enemigos[this_x][this_y] == false
                                && matriz_rocas[this_x+1][this_y] == false && matriz_enemigos[this_x+1][this_y] == false
                                && matriz_rocas[this_x-1][this_y] == false && matriz_enemigos[this_x-1][this_y] == false
                                && matriz_rocas[this_x][this_y+1] == false && matriz_enemigos[this_x][this_y+1] == false
                                && matriz_rocas[this_x][this_y-1] == false && matriz_enemigos[this_x][this_y-1] == false){
                                
                                casilla[0] = this_x;
                                casilla[1] = this_y;
                                casilla_encontrada = true;
                            }
                        }
                    } 
            }
        
        return casilla;
    }

    // Se le llama para el último clúster
    // Es necesario usarlo para saber que cuando cojamos las gemas de ese clúster no nos quedaremos encerrados y podremos ir a la salida
    
    private int[] getPuntoIntermedioClusterSalida(ArrayList<Observation> gems, Observation exit, StateObservation stateObs){
        int[] casilla = new int[2];
        casilla[0] = -1;
        casilla[1] = -1;
        
        // Calculo la gema más cercana a la salida
        int num_gems = gems.size();
        int min_dist = 10000;
        int ind = -1;
        int dist;
        Observation this_gem;
        
        for (int i = 0; i < num_gems; i++){
            this_gem = gems.get(i);
            
            dist = this.getHeuristicDistance(this_gem, exit);
  
            if (dist < min_dist){
                min_dist = dist;
                ind = i;
            }
        }
        
        int x_1 = gems.get(ind).getX();
        int y_1 = gems.get(ind).getY();
        int x_2 = exit.getX(); // x e y de la salida
        int y_2 = exit.getY();
        
        int x_centro = (x_1 + x_2) / 2; // Empiezo en el punto intermedio
        int y_centro = (y_1 + y_2) / 2;
        
        ArrayList<Observation>[][] grid = this.getObservationGrid(stateObs);
        int ancho_grid = grid.length;
        int alto_grid = grid[0].length;
        
        boolean[][] matriz_rocas = new boolean[ancho_grid][alto_grid]; // Guardo las rocas en una matriz de booleanos
        boolean[][] matriz_muros = new boolean[ancho_grid][alto_grid]; // Guardo los muros en una matriz de booleanos
        boolean[][] matriz_enemigos = new boolean[ancho_grid][alto_grid]; // Guardo los enemigos en una matriz de booleanos
        
        for (int x = 0; x < ancho_grid; x++)
            for (int y = 0; y < alto_grid; y++){
                matriz_rocas[x][y] = false;
                matriz_muros[x][y] = false;
                matriz_enemigos[x][y] = false;
                
                for (Observation obs : grid[x][y]){
                    if (obs.getType() == ObservationType.BOULDER)
                        matriz_rocas[x][y] = true;
                    else if (obs.getType() == ObservationType.WALL)
                        matriz_muros[x][y] = true;
                    else if (obs.getType() == ObservationType.BAT)
                        matriz_enemigos[x][y] = true;
                    else if (obs.getType() == ObservationType.SCORPION)
                        matriz_enemigos[x][y] = true;
                }
            }
        
        
        // Exploro esa casilla y las que están en un radio de +3,-3 en x e y
         boolean casilla_encontrada = false;
        
        if (matriz_rocas[x_centro][y_centro] == false && matriz_muros[x_centro][y_centro] == false && matriz_enemigos[x_centro][y_centro] == false
                        && matriz_rocas[x_centro+1][y_centro] == false && matriz_enemigos[x_centro+1][y_centro] == false
                        && matriz_rocas[x_centro-1][y_centro] == false && matriz_enemigos[x_centro-1][y_centro] == false
                        && matriz_rocas[x_centro][y_centro+1] == false && matriz_enemigos[x_centro][y_centro+1] == false
                        && matriz_rocas[x_centro][y_centro-1] == false && matriz_enemigos[x_centro][y_centro-1] == false){
            casilla[0] = x_centro;
            casilla[1] = y_centro;
            casilla_encontrada = true;
            }
        
        int this_x, this_y;
        
        for (int x_add = 1; x_add <= 3 && !casilla_encontrada; x_add++)
            for (int y_add = 1; y_add <= 3 && !casilla_encontrada; y_add++){
                
                // Exploro las 8 casillas dadas por +-x_add y +-y_add
                for (int sig_x = -1; sig_x <= 1 && !casilla_encontrada; sig_x++) // Signo de x
                    for (int sig_y = -1; sig_y <=1 && !casilla_encontrada; sig_y++){ // Signo de y
                        
                        if (sig_x != 0 || sig_y != 0){ // No puede ser la casilla central
                            this_x = x_centro + x_add*sig_x;
                            this_y = y_centro + y_add*sig_y;
                            
                            
                            if (matriz_rocas[this_x][this_y] == false && matriz_muros[this_x][this_y] == false && matriz_enemigos[this_x][this_y] == false
                                && matriz_rocas[this_x+1][this_y] == false && matriz_enemigos[this_x+1][this_y] == false
                                && matriz_rocas[this_x-1][this_y] == false && matriz_enemigos[this_x-1][this_y] == false
                                && matriz_rocas[this_x][this_y+1] == false && matriz_enemigos[this_x][this_y+1] == false
                                && matriz_rocas[this_x][this_y-1] == false && matriz_enemigos[this_x][this_y-1] == false){
                                
                                casilla[0] = this_x;
                                casilla[1] = this_y;
                                casilla_encontrada = true;
                            }
                        }
                    } 
            }
        
        return casilla;
    }
    
    // Devuelve una lista de casillas (observaciones) que se corresponden con las casillas
    // donde hay enemigos y aquellas casillas a una dist. manhattan de 1 (arriba, abajo, derecha e izquierda)
    // Si estas casillas adyacentes están libres, también se añaden sus adyacentes como casillas prohibidas
    // Este arraylist de observaciones se le pasará al A* después para que evite estas casillas
  
    private ArrayList<Observation> getCasillasProhibidas(StateObservation stateObs){
        ArrayList<Observation> bats = this.getBatsList(stateObs);
        ArrayList<Observation> scorpions = this.getScorpionsList(stateObs);
        ArrayList<Observation> enemigos = new ArrayList<>(); // Array con todos los tipos de enemigos
        ArrayList<Observation> casillas_prohibidas = new ArrayList<>();
        Observation obs_cas;
        ObservationType tipo_obs_cas;
        
        enemigos.addAll(bats);
        enemigos.addAll(scorpions);
        
        ArrayList<Observation>[][] grid = this.getObservationGrid(stateObs);
        int ancho = grid.length;
        int alto = grid[0].length;
        boolean[][] matrix_cas_prob = new boolean[ancho][alto]; // Por defecto a false
        int x_act, y_act;
        
        for (Observation enemigo : enemigos){
            x_act = enemigo.getX();
            y_act = enemigo.getY();
            
            matrix_cas_prob[x_act][y_act] = true; // Casilla del enemigo
            
            matrix_cas_prob[x_act+1][y_act] = true; // Casillas adyacentes
            matrix_cas_prob[x_act-1][y_act] = true;
            matrix_cas_prob[x_act][y_act+1] = true;
            matrix_cas_prob[x_act][y_act-1] = true;
            
            // Si esas casillas adyacentes están vacías (el enemigo puede moverse por ellas)
            // entonces también pongo a true sus adyacentes
            
            obs_cas = grid[x_act+1][y_act].get(0); // Derecha
            tipo_obs_cas = obs_cas.getType();
            
            if (tipo_obs_cas == ObservationType.EMPTY || tipo_obs_cas == ObservationType.BAT
                 || tipo_obs_cas == ObservationType.PLAYER || tipo_obs_cas == ObservationType.SCORPION){
                
                matrix_cas_prob[x_act+2][y_act] = true;
                matrix_cas_prob[x_act+1][y_act+1] = true;
                matrix_cas_prob[x_act+1][y_act-1] = true;
            }
                    
            
            obs_cas = grid[x_act-1][y_act].get(0); // Izquierda
            tipo_obs_cas = obs_cas.getType();
            
            if (tipo_obs_cas == ObservationType.EMPTY || tipo_obs_cas == ObservationType.BAT
                 || tipo_obs_cas == ObservationType.PLAYER || tipo_obs_cas == ObservationType.SCORPION){
                
                matrix_cas_prob[x_act-2][y_act] = true;
                matrix_cas_prob[x_act-1][y_act+1] = true;
                matrix_cas_prob[x_act-1][y_act-1] = true;
            }
            
            
            obs_cas = grid[x_act][y_act-1].get(0); // Arriba
            tipo_obs_cas = obs_cas.getType();
            
            if (tipo_obs_cas == ObservationType.EMPTY || tipo_obs_cas == ObservationType.BAT
                 || tipo_obs_cas == ObservationType.PLAYER || tipo_obs_cas == ObservationType.SCORPION){
                
                matrix_cas_prob[x_act][y_act-2] = true;
                matrix_cas_prob[x_act+1][y_act-1] = true;
                matrix_cas_prob[x_act-1][y_act-1] = true;
            }
            
            
            obs_cas = grid[x_act][y_act+1].get(0); // Abajo
            tipo_obs_cas = obs_cas.getType();
            
            if (tipo_obs_cas == ObservationType.EMPTY || tipo_obs_cas == ObservationType.BAT
                 || tipo_obs_cas == ObservationType.PLAYER || tipo_obs_cas == ObservationType.SCORPION){
                
                matrix_cas_prob[x_act][y_act+2] = true;
                matrix_cas_prob[x_act+1][y_act+1] = true;
                matrix_cas_prob[x_act-1][y_act+1] = true;
            }
        }

        // Recorro la matriz y por cada casilla que valga true añado una observacion con esa posición
        for (int this_x = 0; this_x < ancho; this_x++)
            for (int this_y = 0; this_y < alto; this_y++){
                if (matrix_cas_prob[this_x][this_y] == true)
                    casillas_prohibidas.add(new Observation(this_x, this_y, grid[this_x][this_y].get(0).getType()));
            }

        return casillas_prohibidas;
    }

    // Sobrecarga de getCasillasProhibidas
    // Si se le pasa la salida, la casilla donde está y las 8 de alrededor se quitan de prohibidas
    // si estaban
    
    private ArrayList<Observation> getCasillasProhibidas(StateObservation stateObs, Observation exit){
        ArrayList<Observation> bats = this.getBatsList(stateObs);
        ArrayList<Observation> scorpions = this.getScorpionsList(stateObs);
        ArrayList<Observation> enemigos = new ArrayList<>(); // Array con todos los tipos de enemigos
        ArrayList<Observation> casillas_prohibidas = new ArrayList<>();
        Observation obs_cas;
        ObservationType tipo_obs_cas;
        
        enemigos.addAll(bats);
        enemigos.addAll(scorpions);
        
        ArrayList<Observation>[][] grid = this.getObservationGrid(stateObs);
        int ancho = grid.length;
        int alto = grid[0].length;
        boolean[][] matrix_cas_prob = new boolean[ancho][alto]; // Por defecto a false
        int x_act, y_act;
        
        for (Observation enemigo : enemigos){
            x_act = enemigo.getX();
            y_act = enemigo.getY();
            
            matrix_cas_prob[x_act][y_act] = true; // Casilla del enemigo
            
            matrix_cas_prob[x_act+1][y_act] = true; // Casillas adyacentes
            matrix_cas_prob[x_act-1][y_act] = true;
            matrix_cas_prob[x_act][y_act+1] = true;
            matrix_cas_prob[x_act][y_act-1] = true;
            
            // Si esas casillas adyacentes están vacías (el enemigo puede moverse por ellas)
            // entonces también pongo a true sus adyacentes
            
            obs_cas = grid[x_act+1][y_act].get(0); // Derecha
            tipo_obs_cas = obs_cas.getType();
            
            if (tipo_obs_cas == ObservationType.EMPTY || tipo_obs_cas == ObservationType.BAT
                 || tipo_obs_cas == ObservationType.PLAYER || tipo_obs_cas == ObservationType.SCORPION){
                
                matrix_cas_prob[x_act+2][y_act] = true;
                matrix_cas_prob[x_act+1][y_act+1] = true;
                matrix_cas_prob[x_act+1][y_act-1] = true;
            }
                    
            
            obs_cas = grid[x_act-1][y_act].get(0); // Izquierda
            tipo_obs_cas = obs_cas.getType();
            
            if (tipo_obs_cas == ObservationType.EMPTY || tipo_obs_cas == ObservationType.BAT
                 || tipo_obs_cas == ObservationType.PLAYER || tipo_obs_cas == ObservationType.SCORPION){
                
                matrix_cas_prob[x_act-2][y_act] = true;
                matrix_cas_prob[x_act-1][y_act+1] = true;
                matrix_cas_prob[x_act-1][y_act-1] = true;
            }
            
            
            obs_cas = grid[x_act][y_act-1].get(0); // Arriba
            tipo_obs_cas = obs_cas.getType();
            
            if (tipo_obs_cas == ObservationType.EMPTY || tipo_obs_cas == ObservationType.BAT
                 || tipo_obs_cas == ObservationType.PLAYER || tipo_obs_cas == ObservationType.SCORPION){
                
                matrix_cas_prob[x_act][y_act-2] = true;
                matrix_cas_prob[x_act+1][y_act-1] = true;
                matrix_cas_prob[x_act-1][y_act-1] = true;
            }
            
            
            obs_cas = grid[x_act][y_act+1].get(0); // Abajo
            tipo_obs_cas = obs_cas.getType();
            
            if (tipo_obs_cas == ObservationType.EMPTY || tipo_obs_cas == ObservationType.BAT
                 || tipo_obs_cas == ObservationType.PLAYER || tipo_obs_cas == ObservationType.SCORPION){
                
                matrix_cas_prob[x_act][y_act+2] = true;
                matrix_cas_prob[x_act+1][y_act+1] = true;
                matrix_cas_prob[x_act-1][y_act+1] = true;
            }
        }
        
        // Pongo a false las casillas alrededor de la salida
        int x_exit = exit.getX();
        int y_exit = exit.getY();
        
        for (int i = -1; i <= 1; i++)
            for (int j = -1; j <= 1; j++)
                matrix_cas_prob[x_exit+i][y_exit+j] = false;
        
        // Recorro la matriz y por cada casilla que valga true añado una observacion con esa posición
        for (int this_x = 0; this_x < ancho; this_x++)
            for (int this_y = 0; this_y < alto; this_y++){
                if (matrix_cas_prob[this_x][this_y] == true)
                    casillas_prohibidas.add(new Observation(this_x, this_y, grid[this_x][this_y].get(0).getType()));
            }
        
        return casillas_prohibidas;
    }
     
}
