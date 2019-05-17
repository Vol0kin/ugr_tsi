import sys
import re

# Declarar diccionario que contendra los objetos y los personajes
npc_obj_dict = {}
npc_obj_dict['Leonardo'] = set()
npc_obj_dict['Bruja'] = set()
npc_obj_dict['Principe'] = set()
npc_obj_dict['Princesa'] = set()
npc_obj_dict['Profesor'] = set()
npc_obj_dict['Player'] = set()
npc_obj_dict['Oscar'] = set()
npc_obj_dict['Manzana'] = set()
npc_obj_dict['Algoritmo'] = set()
npc_obj_dict['Oro'] = set()
npc_obj_dict['Rosa'] = set()

# Claves de los jugadores
player_keys = ['Player']
npc_keys = ['Leonardo', 'Bruja', 'Principe', 'Princesa', 'Profesor']

# Declarar conjuntos que contendran las zonas
zones = []
connections = []
positions = set()

# Crear strings que se escribiran
out_zones = ''
domain = ''
problem = ''
out_apples = ''
out_algorithms = ''
out_gold = ''
out_oscars = ''
out_roses = ''
out_leonardos = ''
out_princesses = ''
out_princes = ''
out_witches = ''
out_teachers = ''
out_players = ''

tab_increment = 4

# Variables booleanas
use_costs = False

with open(sys.argv[1]) as in_file:

    # Obtener la primera linea
    line = in_file.readline()

    # Seguir leyendo mientras haya lineas
    while line:
        if line.startswith('Problema'):
            # Extraer el problema
            problem_name = line.split(':')[1].replace('\n', '')
            problem = '(define (problem {})\n'.format(problem_name)
        elif line.startswith('Dominio'):
            # Extraer el dominio
            domain_name = line.split(':')[1].replace('\n', '')
            domain = '(:domain {})\n'.format(domain_name)
        elif line.startswith('numero'):
            # Extraer el numero de zonas
            num_zones = int(line.split(':')[1])

            # Generar 1 ... num_zones + 1 zonas
            for i in range(1, num_zones + 1):
                zones.append('z{}'.format(i))

            for zone in zones:
                out_zones += zone + ' '

            out_zones += '- zone\n'
        elif not line == '\n':
            order, zone_list = line.split(' -> ')

            # Obtener las zonas y los objetos de cada zona
            line_zones = re.findall(r'z[1-9]+', zone_list)
            line_objects = re.findall(r'\[.*?\]', zone_list)

            # Obtener las distancias de cada zona
            costs = re.findall(r'=[0-9]*=', zone_list)

            # Eliminar parentesis de los objetos
            formated_objects = [re.sub(r'[\[\]]', '', objects) for objects in line_objects]
            formated_costs = [re.sub(r'=', '', cost) for cost in costs]

            # Obtener numero de zonas
            num_zones = len(line_zones)

            if order == 'V':
                for i in range(num_zones - 1):
                    connections.append('(connected {} S {})\n'.format(line_zones[i], line_zones[i + 1]))
                    connections.append('(connected {} N {})\n'.format(line_zones[i + 1], line_zones[i]))
            else:
                for i in range(num_zones - 1):
                    connections.append('(connected {} E {})\n'.format(line_zones[i], line_zones[i + 1]))
                    connections.append('(connected {} W {})\n'.format(line_zones[i + 1], line_zones[i]))
            
            # Procesar las zonas
            for zone, objects in zip(line_zones, formated_objects):
                # Si hay algun objeto en la zona, procesar cada uno de ellos
                if not objects == '':
                    objects_list = objects.split(' ')

                    for npc_object in objects_list:
                        var, var_class = npc_object.split('-')
                        npc_obj_dict[var_class].add(var)
                        positions.add('(at {} {})\n'.format(var, zone))
            
            # Insertar costes (si existen)
            if len(costs) > 0:
                use_cost = True

                for i in range(len(costs)):
                    connections.append('(= (distance {} {}) {})\n'.format(line_zones[i], line_zones[i + 1], formated_costs[i]))
                    connections.append('(= (distance {} {}) {})\n'.format(line_zones[i + 1], line_zones[i], formated_costs[i]))


        # Siguiente linea
        line = in_file.readline()

with open(sys.argv[2], 'w') as out_file:
    tab_size = tab_increment

    # Escribir nombre de problema
    out_file.write(problem)

    # Escribir nombre del dominio
    out_file.write('\t'.expandtabs(tab_size) + domain)

    # Escribir variables e incrementar dist. tabulado
    out_file.write('\t'.expandtabs(tab_size) + '(:objects\n')
    tab_size += tab_increment
    
    out_file.write('\t'.expandtabs(tab_size) + out_zones)

    for class_var, var in npc_obj_dict.items():
        if not len(var) == 0:
            out_file.write('\t'.expandtabs(tab_size) + ' '.join(var) + ' - ' + class_var + '\n')

    # Restaurar desplazamiento del tabulado y cerrar variables
    tab_size -= tab_increment
    out_file.write('\t'.expandtabs(tab_size) + ')\n')

    # Escribir hechos iniciales e incrementar dist. tabulado
    out_file.write('\t'.expandtabs(tab_size) + '(:init\n')
    tab_size += tab_increment
    
    # Escribir zonas
    for connection in connections:
        out_file.write('\t'.expandtabs(tab_size) + connection)
    
    # Escribir objetos y personajes
    for npc_obj in positions:
        out_file.write('\t'.expandtabs(tab_size) + npc_obj)

    # Escribir mano vacia para cada jugador y su orientacion
    for key in player_keys:
        players = npc_obj_dict[key]

        if len(players) > 0:
            for player in players:
                out_file.write('\t'.expandtabs(tab_size) + '(emptyhand {})\n'.format(player))
                out_file.write('\t'.expandtabs(tab_size) + '(oriented {} S)\n'.format(player))

    # Escribir objetos recibidos de cada personaje
    for key in npc_keys:
        npcs = npc_obj_dict[key]

        if len(npcs) > 0:
            for npc in npcs:
                out_file.write('\t'.expandtabs(tab_size) + '(= (received {}) 0)\n'.format(npc))

    # Restaurar desplazamiento del tabulado y cerrar hechos iniciales
    tab_size -= tab_increment
    out_file.write('\t'.expandtabs(tab_size) + ')\n')

    out_file.write(')')


print(domain)
print(problem)
print(out_zones)
print(connections)
print(sorted(positions))
print(npc_obj_dict)
