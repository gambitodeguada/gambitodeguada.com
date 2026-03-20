El usuario quiere actualizar el resultado de una partida en un fichero de torneo.

Sigue estos pasos:

## 1. Identificar parámetros

Del mensaje del usuario extrae:
- **Fichero de torneo**: la ruta al fichero `.md` mencionado (ej. `@torneos/iii-liga-gambito-de-guada-grupo-b.md`)
- **Jugadores**: los dos jugadores mencionados (pueden estar escritos de forma aproximada)
- **Resultado**: el resultado de la partida. Interpreta expresiones como:
  - "gana X", "X gana", "X wins" → el jugador X gana: "1 - 0" si X juega de blancas, "0 - 1" si juega de negras
  - "tablas", "empate", "draw" → "½-½"
  - "0-1", "1-0", "½-½", "0.5-0.5" → usarlo directamente, normalizando el formato

## 2. Obtener nombres exactos de los jugadores

Lee el fichero `jugadores.md` para obtener los nombres exactos de los dos jugadores mencionados por el usuario. Haz una búsqueda flexible ignorando tildes, mayúsculas y orden de palabras.

## 3. Leer el fichero de torneo

Lee el fichero de torneo indicado.

## 4. Localizar la partida

Busca en la sección "Emparejamientos por Ronda" la fila que contenga a ambos jugadores. El jugador en la columna **Blancas** juega con piezas blancas y el de **Negras** con piezas negras. Esto determina el sentido del resultado.

## 5. Actualizar el resultado

Reemplaza el resultado actual (normalmente `...`) por el nuevo resultado. Usa el formato consistente con el resto del fichero (preferentemente `1 - 0`, `0 - 1`, `½-½`).

## 6. Recalcular la clasificación

Recorre **todos** los emparejamientos del fichero para calcular para cada jugador:
- **Partidas**: número de partidas con resultado definitivo (sin contar `...`)
- **Puntuación**: suma de puntos (victoria=1, tablas=0.5, derrota=0)
- **Media**: `Puntuación - (Partidas / 2)`

## 7. Actualizar y ordenar la tabla de clasificación

Actualiza la tabla con los valores recalculados. Ordena las filas por:
1. **Puntuación** descendente
2. En caso de empate, usa los siguientes criterios de desempate: 
- 1. **Media** descendente
- 2. Encuentro directo
- 3. KS, Koya system, se suman los puntos que haces contra los jugadores que han hecho el 50% de los puntos o más.
  

Reasigna los números de posición (#) según el nuevo orden.

## 8. Aplicar los cambios

Edita el fichero con los cambios necesarios: la fila del resultado en los emparejamientos y todas las filas de la tabla de clasificación.
