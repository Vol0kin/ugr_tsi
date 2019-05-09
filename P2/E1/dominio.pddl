(define (domain BELKAN)
  (:requirements :strips :typing :fluents)
  (:types items npc player - locatable
  		  oscar apple algorithm gold rose - items
  		  princess prince witch teacher leo - npc
  		  player
  		  zone
  )
  (:constants N S E W - orientation)
  (:predicates (oriented ?p - player ?o - orientation)
  			   (at ?l - locatable ?z - zone)
  			   (connected ?z1 - zone ?o - orientation ?z2 - zone)
  			   (emptyhand ?p - player)
  			   (taken ?obj - items ?p - player)
  			   (given ?obj - items)
  )
  (:functions
  	(received ?n - npc)
  )
  (:action turn-left
  	:parameters (?player - player ?)
  	:effect (and
	  	(when (oriented ?player N) (and (oriented ?player W) (not (oriented ?player N))))
	  	(when (oriented ?player W) (and (oriented ?player S) (not (oriented ?player W))))
	  	(when (oriented ?player S) (and (oriented ?player E) (not (oriented ?player S))))
	  	(when (oriented ?player E) (and (oriented ?player N) (not (oriented ?player E))))
  	)
  )
  (:action turn-right
  	:parameters (?player - player)
  	:effect (and
	  	(when (oriented ?player N) (and (oriented ?player E) (not (oriented ?player N))))
	  	(when (oriented ?player E) (and (oriented ?player S) (not (oriented ?player E))))
	  	(when (oriented ?player S) (and (oriented ?player W) (not (oriented ?player S))))
	  	(when (oriented ?player W) (and (oriented ?player N) (not (oriented ?player W))))
  	)
  )
  (:action move
  	:parameters (?p - player ?z1 ?z2 - zone ?o - orientation)
  	:precondition (and (at ?p ?z1) (connected ?z1 ?o ?z2) (oriented ?p ?o))
  	:effect (and
  		(not (at ?p ?z1))
  		(at ?p ?z2)
  	)
  )
  (:action pick-up-item
  	:parameters (?p - player ?o - items ?z - zone)
  	:precondition (and (emptyhand ?p) (at ?p ?z) (at ?o ?z) (not (given ?o)))
  	:effect (and
  		(not (emptyhand ?p))
  		(not (at ?o ?z))
  		(taken ?o ?p)
  	)
  )
  (:action drop-item
  	:parameters (?p - player ?o - items ?z - zone)
  	:precondition (and (taken ?o ?p) (at ?p ?z))
  	:effect (and 
  		(not (taken ?o ?p))
  		(emptyhand ?p)
  		(at ?o ?z)
  	)
  )
  (:action give-item
  	:parameters (?p - player ?n - npc ?o - items ?z - zone)
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

