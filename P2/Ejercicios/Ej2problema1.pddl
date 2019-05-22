(define (problem Problema1)
    (:domain Ejercicio2)
    (:objects
        z1 z2 z3 z4 z5 z6 z7 z8 z9 z10 z11 z12 z13 z14 z15 z16 z17 z18 z19 z20 z21 z22 z23 z24 z25 - zone
        leonardo1 - Leonardo
        bruja1 - Bruja
        principe1 - Principe
        princesa1 - Princesa
        profesor1 - Profesor
        player1 - Player
        oscar1 - Oscar
        manzana1 - Manzana
        algoritmo1 - Algoritmo
        oro1 - Oro
        rosa1 - Rosa
    )
    (:init
        (connected z1 E z2)
        (connected z2 W z1)
        (connected z2 E z3)
        (connected z3 W z2)
        (connected z3 E z4)
        (connected z4 W z3)
        (connected z4 E z5)
        (connected z5 W z4)
        (= (distance z1 z2) 1)
        (= (distance z2 z1) 1)
        (= (distance z2 z3) 3)
        (= (distance z3 z2) 3)
        (= (distance z3 z4) 2)
        (= (distance z4 z3) 2)
        (= (distance z4 z5) 1)
        (= (distance z5 z4) 1)
        (connected z6 E z7)
        (connected z7 W z6)
        (connected z7 E z8)
        (connected z8 W z7)
        (connected z8 E z9)
        (connected z9 W z8)
        (connected z9 E z1)
        (connected z1 W z9)
        (= (distance z6 z7) 1)
        (= (distance z7 z6) 1)
        (= (distance z7 z8) 3)
        (= (distance z8 z7) 3)
        (= (distance z8 z9) 5)
        (= (distance z9 z8) 5)
        (= (distance z9 z1) 3)
        (= (distance z1 z9) 3)
        (connected z11 E z12)
        (connected z12 W z11)
        (connected z12 E z13)
        (connected z13 W z12)
        (connected z13 E z14)
        (connected z14 W z13)
        (connected z14 E z15)
        (connected z15 W z14)
        (= (distance z11 z12) 2)
        (= (distance z12 z11) 2)
        (= (distance z12 z13) 1)
        (= (distance z13 z12) 1)
        (= (distance z13 z14) 5)
        (= (distance z14 z13) 5)
        (= (distance z14 z15) 4)
        (= (distance z15 z14) 4)
        (connected z16 E z17)
        (connected z17 W z16)
        (connected z17 E z18)
        (connected z18 W z17)
        (connected z18 E z19)
        (connected z19 W z18)
        (connected z19 E z2)
        (connected z2 W z19)
        (= (distance z16 z17) 4)
        (= (distance z17 z16) 4)
        (= (distance z17 z18) 3)
        (= (distance z18 z17) 3)
        (= (distance z18 z19) 1)
        (= (distance z19 z18) 1)
        (= (distance z19 z2) 2)
        (= (distance z2 z19) 2)
        (connected z21 E z22)
        (connected z22 W z21)
        (connected z22 E z23)
        (connected z23 W z22)
        (connected z23 E z24)
        (connected z24 W z23)
        (connected z24 E z25)
        (connected z25 W z24)
        (= (distance z21 z22) 5)
        (= (distance z22 z21) 5)
        (= (distance z22 z23) 4)
        (= (distance z23 z22) 4)
        (= (distance z23 z24) 2)
        (= (distance z24 z23) 2)
        (= (distance z24 z25) 3)
        (= (distance z25 z24) 3)
        (connected z1 S z6)
        (connected z6 N z1)
        (connected z6 S z11)
        (connected z11 N z6)
        (connected z11 S z16)
        (connected z16 N z11)
        (connected z16 S z21)
        (connected z21 N z16)
        (= (distance z1 z6) 2)
        (= (distance z6 z1) 2)
        (= (distance z6 z11) 3)
        (= (distance z11 z6) 3)
        (= (distance z11 z16) 1)
        (= (distance z16 z11) 1)
        (= (distance z16 z21) 4)
        (= (distance z21 z16) 4)
        (connected z2 S z7)
        (connected z7 N z2)
        (connected z7 S z12)
        (connected z12 N z7)
        (connected z12 S z17)
        (connected z17 N z12)
        (connected z17 S z22)
        (connected z22 N z17)
        (= (distance z2 z7) 5)
        (= (distance z7 z2) 5)
        (= (distance z7 z12) 3)
        (= (distance z12 z7) 3)
        (= (distance z12 z17) 2)
        (= (distance z17 z12) 2)
        (= (distance z17 z22) 2)
        (= (distance z22 z17) 2)
        (connected z3 S z8)
        (connected z8 N z3)
        (connected z8 S z13)
        (connected z13 N z8)
        (connected z13 S z18)
        (connected z18 N z13)
        (connected z18 S z23)
        (connected z23 N z18)
        (= (distance z3 z8) 4)
        (= (distance z8 z3) 4)
        (= (distance z8 z13) 2)
        (= (distance z13 z8) 2)
        (= (distance z13 z18) 2)
        (= (distance z18 z13) 2)
        (connected z4 S z9)
        (connected z9 N z4)
        (connected z9 S z14)
        (connected z14 N z9)
        (connected z14 S z19)
        (connected z19 N z14)
        (connected z19 S z24)
        (connected z24 N z19)
        (= (distance z4 z9) 1)
        (= (distance z9 z4) 1)
        (= (distance z9 z14) 5)
        (= (distance z14 z9) 5)
        (= (distance z14 z19) 2)
        (= (distance z19 z14) 2)
        (= (distance z19 z24) 1)
        (= (distance z24 z19) 1)
        (connected z5 S z1)
        (connected z1 N z5)
        (connected z1 S z15)
        (connected z15 N z1)
        (connected z15 S z2)
        (connected z2 N z15)
        (connected z2 S z25)
        (connected z25 N z2)
        (= (distance z5 z1) 2)
        (= (distance z1 z5) 2)
        (= (distance z1 z15) 3)
        (= (distance z15 z1) 3)
        (= (distance z15 z2) 1)
        (= (distance z2 z15) 1)
        (= (distance z2 z25) 2)
        (= (distance z25 z2) 2)
        (at algoritmo1 z18)
        (at bruja1 z6)
        (at leonardo1 z19)
        (at manzana1 z25)
        (at oro1 z4)
        (at oscar1 z1)
        (at player1 z13)
        (at princesa1 z1)
        (at principe1 z23)
        (at profesor1 z16)
        (at rosa1 z22)
        (emptyhand player1)
        (oriented player1 S)
        (= (traveled player1) 0)
        (= (received leonardo1) 0)
        (= (received princesa1) 0)
        (= (received bruja1) 0)
        (= (received profesor1) 0)
        (= (received principe1) 0)
    )
    (:goal
    		(AND
    				(= (received leonardo1) 1)
       			(= (received princesa1) 1)
        		(= (received bruja1) 1)
        		(= (received profesor1) 1)
        		(= (received principe1) 1)
        )
    )
    (:metric minimize (traveled player1))
)
