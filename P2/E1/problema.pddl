(define (problem belkan-problem)
	(:domain BELKAN)
	(:objects player1 - Player 
			  z1 z2 z3 z4 z5 z6 z7 z8 z9 z10 z11 z12 - zone 
			  oscar1 - Oscar
			  apple1 - Manzana
			  algorithm1 - Algoritmo
			  gold1 - Oro
			  rose1 - Rosa
			  teacher1 - Profesor
			  prince1 - Principe
			  princess1 - Princesa
			  leo1 - Leonardo
			  witch1 - Bruja
	)
	(:init
		(connected z1 S z3)
		(connected z3 N z1)
		(connected z3 W z2)
		(connected z2 E z3)
		(connected z2 S z5)
		(connected z5 N z2)
		(connected z5 S z8)
		(connected z8 N z5)
		(connected z8 S z10)
		(connected z10 N z8)
		(connected z5 E z6)
		(connected z6 W z5)
		(connected z5 W z4)
		(connected z4 E z5)
		(connected z4 S z7)
		(connected z7 N z4)
		(connected z10 W z9)
		(connected z9 E z10)
		(connected z10 E z11)
		(connected z11 W z10)
		(connected z11 E z12)
		(connected z12 W z11)
		(oriented player1 S)
		(at player1 z8)
		(at leo1 z1)
		(at prince1 z6)
		(at princess1 z7)
		(at witch1 z9)
		(at teacher1 z12)
		(at apple1 z2)
		(at algorithm1 z3)
		(at gold1 z4)
		(at rose1 z10)
		(at oscar1 z11)
		(emptyhand player1)
		(= (received teacher1) 0)
		(= (received prince1) 0)
		(= (received princess1) 0)
		(= (received leo1) 0)
		(= (received witch1) 0)
	)
	(:goal (AND 
		(oriented player1 N)
		(at player1 z3)
		(>= (received teacher1) 1)
		(>= (received prince1) 1)
		(>= (received princess1) 1)
		(>= (received witch1) 1)
		(>= (received leo1) 1) )
	)
)