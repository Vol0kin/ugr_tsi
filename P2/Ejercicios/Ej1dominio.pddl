(define (domain Ejercicio1)
  (:requirements :strips :typing :fluents)
  (:types items npc Player - locatable
  		  Oscar Manzana Algoritmo Oro Rosa - items
  		  Princesa Principe Bruja Profesor Leonardo - npc
  		  Player
  		  zone
  )
  (:constants N S E W - orientation)
  (:predicates (oriented ?p - Player ?o - orientation)
  			   (at ?l - locatable ?z - zone)
  			   (connected ?z1 - zone ?o - orientation ?z2 - zone)
  			   (emptyhand ?p - Player)
  			   (taken ?obj - items ?p - Player)
  			   (given ?obj - items)
  )
  (:functions
  	(received ?n - npc)
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
  	:parameters (?p - Player ?z1 ?z2 - zone ?o - orientation)
  	:precondition (and (at ?p ?z1) (connected ?z1 ?o ?z2) (oriented ?p ?o))
  	:effect (and
  		(not (at ?p ?z1))
  		(at ?p ?z2)
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
  	:precondition (and (taken ?o ?p) (at ?p ?z))
  	:effect (and 
  		(not (taken ?o ?p))
  		(emptyhand ?p)
  		(at ?o ?z)
  	)
  )
  (:action give-item
  	:parameters (?p - Player ?n - npc ?o - items ?z - zone)
  	:precondition (and (taken ?o ?p) (at ?p ?z) (at ?n ?z) )
  	:effect (and
  		(not (taken ?o ?p))
  		(emptyhand ?p)
  		(at ?o ?z)
  		(given ?o)
  		(increase (received ?n) 1)
  	)
  )
)

