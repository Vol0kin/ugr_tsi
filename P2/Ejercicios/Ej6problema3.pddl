(define (problem belkan-problem)
	(:domain Ejercicio6)
	(:objects player1 player2 - Player 
			  z1 z2 z3 z4 z5 z6 z7 z8 z9 z10 z11 z12 z13 - zone 
			  oscar1 oscar2 oscar3 - Oscar
			  apple1 apple2 apple3 - Manzana
			  teacher1 - Profesor
			  prince1 - Principe
			  princess1 - Princesa
			  leo1 - Leonardo
			  witch1 - Bruja
			  shoe1 - Zapatilla
			  bikini1 - Bikini
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
		(connected z13 E z1)
		(connected z1 W z13)
		(connected z13 S z2)
		(connected z2 N z13)
		(terrain z1 Piedra)
		(terrain z2 Agua)
		(terrain z3 Arena)
		(terrain z4 Piedra)
		(terrain z5 Bosque)
		(terrain z5 Piedra)
		(terrain z7 Arena)
		(terrain z8 Piedra)
		(terrain z9 Arena)
		(terrain z10 Arena)
		(terrain z11 Bosque)
		(terrain z12 Arena)
		(terrain z13 Precipicio)
		(oriented player1 S)
		(oriented player2 S)
		(at player1 z8)
		(at player2 z10)
		(at leo1 z1)
		(at prince1 z6)
		(at princess1 z7)
		(at witch1 z9)
		(at teacher1 z12)
		(at apple1 z10)
		(at apple2 z8)
		(at apple3 z9)
		(at oscar2 z2)
		(at oscar1 z3)
		(at oscar3 z11)
		(at shoe1 z9)
		(is_zapatilla shoe1)
		(at bikini1 z10)
		(is_bikini bikini1)
		(emptyhand player1)
		(emptybag player1)
		(emptyhand player2)
		(emptybag player2)
		(= (traveled player1) 0)
		(= (traveled player2) 0)
		(= (received teacher1) 0)
		(= (received prince1) 0)
		(= (received princess1) 0)
		(= (received leo1) 0)
		(= (received witch1) 0)
		(= (distance z1 z3) 2)
		(= (distance z3 z1) 2)
		(= (distance z3 z2) 4)
		(= (distance z2 z3) 4)
		(= (distance z2 z5) 3)
		(= (distance z5 z2) 3)
		(= (distance z5 z4) 4)
		(= (distance z4 z5) 4)
		(= (distance z4 z7) 5)
		(= (distance z7 z4) 5)
		(= (distance z5 z6) 5)
		(= (distance z6 z5) 5)
		(= (distance z5 z8) 6)
		(= (distance z8 z5) 6)
		(= (distance z8 z10) 7)
		(= (distance z10 z8) 7)
		(= (distance z10 z9) 6)
		(= (distance z9 z10) 6)
		(= (distance z10 z11) 5)
		(= (distance z11 z10) 5)
		(= (distance z11 z12) 3)
		(= (distance z12 z11) 3)
		(= (distance z2 z13) 1)
		(= (distance z13 z2) 1)
		(= (distance z1 z13) 1)
		(= (distance z13 z1) 1)
		(= (total_score) 0)
		(= (player-score player1) 0)
		(= (player-score player2) 0)
		(= (score leo1 oscar1) 10)
		(= (score leo1 oscar2) 10)
		(= (score leo1 oscar3) 10)
		(= (score leo1 apple1) 3)
		(= (score leo1 apple2) 3)
		(= (score leo1 apple3) 3)
		(= (score princess1 oscar1) 5)
		(= (score princess1 oscar2) 5)
		(= (score princess1 oscar3) 5)
		(= (score princess1 apple1) 1)
		(= (score princess1 apple2) 1)
		(= (score princess1 apple3) 1)
		(= (score witch1 oscar1) 4)
		(= (score witch1 oscar2) 4)
		(= (score witch1 oscar3) 4)
		(= (score witch1 apple1) 10)
		(= (score witch1 apple2) 10)
		(= (score witch1 apple3) 10)
		(= (score teacher1 oscar1) 3)
		(= (score teacher1 oscar2) 3)
		(= (score teacher1 oscar3) 3)
		(= (score teacher1 apple1) 5)
		(= (score teacher1 apple2) 5)
		(= (score teacher1 apple3) 5)
		(= (score prince1 oscar1) 1)
		(= (score prince1 oscar2) 1)
		(= (score prince1 oscar2) 1)
		(= (score prince1 apple1) 4)
		(= (score prince1 apple2) 4)
		(= (score prince1 apple3) 4)
		(= (pocket-capacity teacher1) 1)
		(= (pocket-capacity leo1) 3)
		(= (pocket-capacity witch1) 2)
		(= (pocket-capacity princess1) 1)
		(= (pocket-capacity prince1) 1)
		(has-pocket teacher1)
		(has-pocket leo1)
		(has-pocket witch1)
		(has-pocket princess1)
		(has-pocket prince1)
	)
	(:goal (AND
		(>= (total_score) 30)
		(>= (player-score player1) 10)
		(>= (player-score player2) 10)
		)
	)
)
