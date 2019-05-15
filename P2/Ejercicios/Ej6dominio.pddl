(define (domain Ejercicio6)
  (:requirements :strips :typing :fluents)
  (:types items npc Player - locatable
  		  Oscar Manzana Algoritmo Oro Rosa Zapatilla Bikini - items
  		  Princesa Principe Bruja Profesor Leonardo - npc
  		  Player
  		  zone
  )
  (:constants N S E W - orientation
              Bosque Agua Precipicio Arena Piedra - surface
  )
  (:predicates (oriented ?p - Player ?o - orientation)
  			   (at ?l - locatable ?z - zone)
  			   (connected ?z1 - zone ?o - orientation ?z2 - zone)
  			   (emptyhand ?p - Player)
  			   (taken ?obj - items ?p - Player)
  			   (given ?obj - items)
           (terrain ?z - zone ?s - surface)
           (emptybag ?p - Player)
           (inbag ?o - Object ?p - Player)
           (is_zapatilla ?o - items)
           (is_bikini ?o - items)
  )
  (:functions
  	(received ?n - npc)
    (distance ?z1 ?z2 - zone)
    (traveled ?p - Player)
    (player-score ?p)
    (total_score)
    (score ?n - npc ?o - items)
    (pocket-capacity ?n - npc)
  )
  (:action turn-left
  	:parameters (?p - Player ?)
  	:effect (and
	  	(when (oriented ?p N) (and (oriented ?p W) (not (oriented ?p N))))
	  	(when (oriented ?p W) (and (oriented ?p S) (not (oriented ?p W))))
	  	(when (oriented ?p S) (and (oriented ?p E) (not (oriented ?p S))))
	  	(when (oriented ?p E) (and (oriented ?p N) (not (oriented ?p E))))
  	)
  )
  (:action turn-right
  	:parameters (?p - Player)
  	:effect (and
	  	(when (oriented ?p N) (and (oriented ?p E) (not (oriented ?p N))))
	  	(when (oriented ?p E) (and (oriented ?p S) (not (oriented ?p E))))
	  	(when (oriented ?p S) (and (oriented ?p W) (not (oriented ?p S))))
	  	(when (oriented ?p W) (and (oriented ?p N) (not (oriented ?p W))))
  	)
  )
  (:action move
  	:parameters (?p - Player ?z1 ?z2 - zone ?o - orientation ?it - items)
  	:precondition (and (at ?p ?z1) (connected ?z1 ?o ?z2) (oriented ?p ?o) (not (terrain ?z2 Precipicio)))
  	:effect (and
      (when (or 
              (and (not (terrain ?z2 Bosque)) (not (terrain ?z2 Agua)))
              (and (terrain ?z2 Bosque) (is_zapatilla ?it) (or (taken ?it ?p) (inbag ?it ?p)))
              (and (terrain ?z2 Agua) (is_bikini ?it) (or (taken ?it ?p) (inbag ?it ?p)))
            ) 
  		  (and
          (not (at ?p ?z1))
  		    (at ?p ?z2)
          (increase (traveled ?p) (distance ?z1 ?z2))
        )
      )
    )
  )
  (:action pick-up-item
  	:parameters (?p - Player ?o - items ?z - zone)
  	:precondition (and (emptyhand ?p) (at ?p ?z) (at ?o ?z) (not (given ?o)))
  	:effect (and
  		(not (emptyhand ?p))
  		(not (at ?o ?z))
  		(taken ?o ?p)
  	)
  )
  (:action drop-item
  	:parameters (?p - Player ?o - items ?z - zone)
  	:precondition (and (taken ?o ?p) (at ?p ?z) (not (emptybag ?p)))
  	:effect (and 
  		(not (taken ?o ?p))
  		(emptyhand ?p)
  		(at ?o ?z)
  	)
  )
  (:action give-item
  	:parameters (?p - Player ?n - npc ?o - items ?z - zone)
  	:precondition (and (taken ?o ?p) (at ?p ?z) (at ?n ?z) (not (is_zapatilla ?o)) (> (pocket-capacity ?n) (received ?n)))
  	:effect (and
  		(not (taken ?o ?p))
  		(emptyhand ?p)
  		(at ?o ?z)
  		(given ?o)
  		(increase (received ?n) 1)
      (increase (total_score) (score ?n ?o))
      (increase (player-score ?p) (score ?n ?o))
  	)
  )
  (:action put-in-bag
    :parameters (?p - Player ?o - items)
    :precondition (and (taken ?o ?p) (emptybag ?p))
    :effect (and
      (not (emptybag ?p))
      (not (taken ?o ?p))
      (emptyhand ?p)
      (inbag ?o ?p)
    )
  )
  (:action get-from-bag
    :parameters (?p - Player ?o - items)
    :precondition (and (inbag ?o ?p) (emptyhand ?p))
    :effect (and
      (not (inbag ?o ?p))
      (not (emptyhand ?p))
      (taken ?o ?p)
      (emptybag ?p)
    )
  )
)

