(define (domain BELKAN)
  (:requirements :strips :typing)
  (:types oscar apple algorithm gold rose - objects
  		  princess prince witch teacher leonardo - npc
  		  player
  		  zone
  )
  (:constants N S E W - orientation)
  (:predicates (oriented ?p - player ?o - orientation)
  			   (at ?p - player ?z - zone)
  			   (connected ?z1 - zone ?o - orientation ?z2 - zone))
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
)

