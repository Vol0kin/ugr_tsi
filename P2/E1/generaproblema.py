import sys
import re

# Declarar conjuntos que contendran a los personajes
leonardos = set()
princesses = set()
princes = set()
witches = set()
teachers = set()
players = set()

# Declarar conjuntos que contendran los items
apples = set()
algorithms = set()
gold = set()
oscars = set()
roses = set()

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

            # Eliminar parentesis de los objetos
            formated_objects = [re.sub(r'[\[\]]', '', objects) for objects in line_objects]

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
                        if npc_object.endswith('Bruja'):
                            witch = npc_object.split('-')[0]
                            witches.add(witch)
                            positions.add('(at ' + zone + ' ' + witch + ')\n')
                        elif npc_object.endswith('Princesa'):
                            princess = npc_object.split('-')[0]
                            princesses.add(princess)
                            positions.add('(at ' + zone + ' ' + princess + ')\n')
                        elif npc_object.endswith('Principe'):
                            prince = npc_object.split('-')[0]
                            princes.add(princess)
                            positions.add('(at ' + zone + ' ' + prince + ')\n')
                        elif npc_object.endswith('Profesor'):
                            teacher = npc_object.split('-')[0]
                            teachers.add(teacher)
                            positions.add('(at ' + zone + ' ' + teacher + ')\n')
                        elif npc_object.endswith('Leonardo'):
                            leo = npc_object.split('-')[0]
                            leonardos.add(leo)
                            positions.add('(at ' + zone + ' ' + leo + ')\n')
                        elif npc_object.endswith('Player'):
                            player = npc_object.split('-')[0]
                            players.add(player)
                            positions.add('(at ' + zone + ' ' + player + ')\n')
                        elif npc_object.endswith('Oscar'):
                            oscar = npc_object.split('-')[0]
                            oscars.add(oscar)
                            positions.add('(at ' + zone + ' ' + oscar + ')\n')
                        elif npc_object.endswith('Manzana'):
                            apple = npc_object.split('-')[0]
                            apples.add(apple)
                            positions.add('(at ' + zone + ' ' + apple + ')\n')
                        elif npc_object.endswith('Algoritmo'):
                            algorithm = npc_object.split('-')[0]
                            algorithms.add(algorithm)
                            positions.add('(at ' + zone + ' ' + algorithm + ')\n')
                        elif npc_object.endswith('Oro'):
                            obj_gold = npc_object.split('-')[0]
                            gold.add(obj_gold)
                            positions.add('(at ' + zone + ' ' + obj_gold + ')\n')
                        elif npc_object.endswith('Rosa'):
                            rose = npc_object.split('-')[0]
                            roses.add(rose)
                            positions.add('(at ' + zone + ' ' + rose + ')\n')

        # Siguiente linea
        line = in_file.readline()

with open(sys.argv[2], 'w') as out_file:
    tab_size = tab_increment
    out_file.write(problem)
    out_file.write('\t'.expandtabs(tab_size) + domain)

    out_file.write(')')


print(domain)
print(problem)
print(out_zones)
print(connections)
print(sorted(positions))
