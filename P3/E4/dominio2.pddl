(define (domain zeno-travel)


(:requirements
  :typing
  :fluents
  :derived-predicates
  :negative-preconditions
  :universal-preconditions
  :disjuntive-preconditions
  :conditional-effects
  :htn-expansion

  ; Requisitos adicionales para el manejo del tiempo
  :durative-actions
  :metatags
 )

(:types aircraft person city - object)
(:constants slow fast - object)
(:predicates (at ?x - (either person aircraft) ?c - city)
             (in ?p - person ?a - aircraft)
             (different ?x ?y) (igual ?x ?y)
             (hay-fuel-fly ?a ?c1 ?c2)
             (hay-fuel-zoom ?a ?c1 ?c2)
             (can-fly ?a ?c1 ?c2)
             (can-zoom ?a ?c1 ?c2)
             (can-fly-time ?a ?c1 ?c2)
             (can-zoom-time ?a ?c1 ?c2)
             (destino ?x - person ?y - city)
             )
(:functions (fuel ?a - aircraft)
            (distance ?c1 - city ?c2 - city)
            (slow-speed ?a - aircraft)
            (fast-speed ?a - aircraft)
            (slow-burn ?a - aircraft)
            (fast-burn ?a - aircraft)
            (capacity ?a - aircraft)
            (refuel-rate ?a - aircraft)
            (total-fuel-used ?a - aircraft)
            (boarding-time)
            (debarking-time)
            (fuel-limit ?a - aircraft)
            (max-passengers ?a - aircraft)
            (passengers ?a - aircraft)
            (time-limit ?a - aircraft)
            )

;; el consecuente "vac�o" se representa como "()" y significa "siempre verdad"
(:derived
  (igual ?x ?x) ())

(:derived 
  (different ?x ?y) (not (igual ?x ?y)))


(:derived 
  (hay-fuel-fly ?a - aircraft ?c1 - city ?c2 - city)
  (> (fuel ?a) (* (distance ?c1 ?c2) (slow-burn ?a)))
)

(:derived 
  (hay-fuel-zoom ?a - aircraft ?c1 - city ?c2 - city)
  (> (fuel ?a) (* (distance ?c1 ?c2) (fast-burn ?a)))
)

(:derived
  (can-fly ?a - aircraft ?c1 - city ?c2 - city)
  (< (+ (total-fuel-used ?a) (* (distance ?c1 ?c2) (slow-burn ?a))) (fuel-limit ?a))
)

(:derived
  (can-zoom ?a - aircraft ?c1 - city ?c2 - city)
  (< (+ (total-fuel-used ?a) (* (distance ?c1 ?c2) (fast-burn ?a))) (fuel-limit ?a))
)

(:derived
  (can-fly-time ?a - aircraft ?c1 - city ?c2 - city)
  (< (/ (distance ?c1 ?c2) (slow-speed ?a)) (time-limit ?a))
)

(:derived
  (can-zoom-time ?a - aircraft ?c1 - city ?c2 - city)
  (< (/ (distance ?c1 ?c2) (fast-speed ?a)) (time-limit ?a))
)

; Tarea para transportar una persona
(:task transport-person
	:parameters (?p - person ?c - city)
	
  (:method Case1 ; si la persona est� en la ciudad no se hace nada
	 :precondition (at ?p ?c)
	 :tasks ()
  )
	 
   
   (:method Case2 ;si no est� en la ciudad destino, pero avion y persona est�n en la misma ciudad
	  :precondition (and (at ?p - person ?c1 - city)
			                 (at ?a - aircraft ?c1 - city))
				     
	  :tasks ( 
      (board-all-passengers ?a ?c1)
		  (mover-avion ?a ?c1 ?c)
		  (debark-all-passengers?a ?c)
    )
   )
  
  (:method WaitCatchFlight
    :precondition(and (at ?p - person ?c1 - city)
                      (at ?a - aircraft ?c2 - city))
    :tasks(
        (mover-avion ?a ?c2 ?c1)
        (board-all-passengers ?a ?c1)
        (mover-avion ?a ?c1 ?c)
        (debark-all-passengers ?a ?c)
    )
  )
)

(:task mover-avion
 :parameters (?a - aircraft ?c1 - city ?c2 -city)
 
  (:method ZoomAircraft
    :precondition(and
      (hay-fuel-zoom ?a ?c1 ?c2)
      (can-zoom ?a ?c1 ?c2)
      (can-zoom-time ?a ?c1 ?c2)
    )
    :tasks(
      (zoom ?a ?c1 ?c2)
    )
  )
  (:method RefuelZoomAircraft
    :precondition(and
      (not (hay-fuel-zoom ?a ?c1 ?c2))
      (can-zoom ?a ?c1 ?c2)
      (can-zoom-time ?a ?c1 ?c2)
    )
    :tasks(
      (refuel ?a ?c1)
      (zoom ?a ?c1 ?c2)
    )
  )
  (:method FlyAircraft
    :precondition(and
      (hay-fuel-fly ?a ?c1 ?c2)
      (can-fly ?a ?c1 ?c2)
      (can-fly-time ?a ?c1 ?c2)
    )
    :tasks(
      (fly ?a ?c1 ?c2)
    )
  )
  (:method RefuelFlyAircraft
    :precondition(and
      (not (hay-fuel-fly ?a ?c1 ?c2))
      (can-fly ?a ?c1 ?c2)
      (can-fly-time ?a ?c1 ?c2)
    )
    :tasks(
      (refuel ?a ?c1)
      (fly ?a ?c1 ?c2)
    )
  )
)

(:task board-all-passengers
  :parameters (?a - aircraft ?c - city)

  ; Embarcar de forma recursiva aquellos pasajeros en el aeropuerto
  (:method RecursiveBoard
    :precondition(and
      (at ?p - person ?c)
      (not (destino ?p ?c))
      (< (passengers ?a) (max-passengers ?a))
    )
    :tasks(
      (board ?p ?a ?c)
      (board-all-passengers ?a ?c)
    )
  )

  ; Metodo base del embarque recursivo
  (:method BaseBoard
    :precondition()
    :tasks()
  )
)

(:task debark-all-passengers
  :parameters (?a - aircraft ?c - city)

  (:method RecursiveDebark
    :precondition(and
      (in ?p - person ?a)
      (destino ?p ?c)
    )
    :tasks(
      (debark ?p ?a ?c)
      (debark-all-passengers ?a ?c)
    )
  )   

  ; Metodo base del desembarque recursivo
  (:method BaseDebark
    :precondition()
    :tasks()
  )
)
 
(:import "Primitivas-ZenoTravel.pddl") 


)
