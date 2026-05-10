El usuario quiere generar una entrada del blog en español para `gambitodeguada.com` con el rendimiento de los jugadores del Gambito de Guada en un torneo de info64.org.

La entrada **siempre se redacta en español**, aunque el usuario haga la petición en otro idioma.

## 1. Identificar la URL de info64

Del mensaje del usuario extrae la URL de info64.org del torneo, por ejemplo:

```
https://info64.org/iii-liga-club-gambito-de-guada-b/standings
```

Si la URL no apunta a `/standings`, añade ese sufijo. No vuelvas a preguntar la URL: viene como argumento del comando.

## 2. Pedir los datos que faltan

Usa `AskUserQuestion` (en una sola llamada, con tres preguntas) para recopilar:

1. **Nombre del torneo** — ej. *III Liga Club Gambito de Guada B*.
2. **Fechas** — texto libre, tal como el usuario quiere que aparezca en el post, ej. *del 1 de marzo al 27 de abril de 2026* o *el domingo 13 de abril de 2026*.
3. **Lugar** — ej. *el Centro San José de Guadalajara*.

No inventes valores. Si el usuario salta alguno, vuelve a preguntar antes de continuar.

## 3. Descargar la clasificación de info64

Usa `WebFetch` sobre la URL de standings para obtener la tabla de clasificación. Extrae:

- `all_entries`: lista ordenada de todos los jugadores con su URL individual de info64. Cada entrada tiene la forma `[Nombre Apellidos](https://info64.org/{torneo-slug}/{jugador-slug})`. Conserva el orden de la clasificación.
- `num_players`: total de participantes (longitud de `all_entries`).
- Para cada jugador, su **puntuación total** (columna `Pts` o `Puntos`) y el **número de partidas jugadas** (columna `Partidas`, `Games`, o derivable de la suma de `+`/`=`/`-` o `W`/`D`/`L`).

Para los jugadores del Gambito (no hace falta para el resto), abre además la página individual (`WebFetch` sobre `url_info64`) y extrae:

- `played_games`: rondas en las que el jugador disputó una partida real (gana, pierde, tabla con oponente).
- `byes`: rondas marcadas como bye, descanso o sin oponente con puntuación. Para cada bye, anota el valor (típicamente `0,5`, a veces `1` o `0`).
- `bye_score`: suma de los puntos de bye.
- `total_rounds`: número total de rondas del torneo.

Si la celda de puntuación está vacía, registra `score = None`. Si no se puede determinar el número de partidas o byes, registra `None`.

## 4. Filtrar jugadores del Gambito

Lee `jugadores.md` para obtener la lista oficial de socios del club. Cruza esa lista con `all_entries` haciendo una comparación flexible (ignora tildes, mayúsculas y orden de palabras: en `jugadores.md` los nombres están como `Apellido1 Apellido2, Nombre` y en info64 como `Nombre Apellido1 Apellido2`).

El resultado, `gambito_entries`, conserva el orden de la clasificación general.

## 5. Calcular posición, puntos, partidas y byes de cada jugador del Gambito

Para cada entrada de `gambito_entries`:

- **final_position**: índice 1-based del jugador en `all_entries`.
- **score**: puntos totales extraídos en el paso 3 (incluyen los byes si los hay).
- **games**: número de partidas jugadas (sin contar byes).
- **bye_score**: suma de puntos de bye (`0` si no tuvo byes).
- **total_rounds**: rondas del torneo (`games` + número de byes).

## 6. Construir la lista de rendimiento

Genera una viñeta por jugador, en orden de clasificación, con ordinales en español.

**Sin byes** (`bye_score == 0`):

```
- [Nombre Apellidos](url_info64) — Xº con Y/N pts
```

Donde `Y` son los puntos y `N` el número de partidas jugadas. Punto como separador decimal (ej. `3.5/6 pts`).

**Con byes** (`bye_score > 0`):

```
- [Nombre Apellidos](url_info64) — Xº con Y,Y/T con B,B Bye
```

Donde `Y,Y` es la puntuación total **con coma decimal**, `T` es `total_rounds`, y `B,B` es el `bye_score` con coma decimal. Ejemplo: `2,5/7 con 0,5 Bye` significa 2,5 puntos en 7 rondas, con 0,5 procedente de un bye.

Reglas de formato:

| Posición | Texto |
|---|---|
| 1 | `1º` |
| 2 | `2º` |
| 3 | `3º` |
| n | `nº` |

- Si `games = None` y `score` está disponible, escribe solo `Y pts` sin denominador.
- Si `score = None`, omite el sufijo `con Y/N pts` para esa línea.
- Si hay varios byes, suma sus puntos en `bye_score` y muéstralo como un único número (ej. `1,0 Bye` para dos byes de 0,5).
- Si no hay ningún jugador del Gambito en el torneo, en lugar de la lista escribe la frase: *Ningún jugador del club participó en este torneo.*

## 7. Componer el post (en español)

Usa esta plantilla con front matter de Jekyll:

```
---
layout: post
title:  {Título del post}
date:   {YYYY-MM-DD HH:MM:SS +0200}
categories: torneos
---

{Fechas} se celebró en {Lugar} el torneo {Nombre del torneo}, con la participación de {num_players} jugadores.

## Rendimiento de los jugadores del Gambito de Guada

{Lista del paso 6}

[Clasificación general]({URL info64})
```

Sustituciones:

- `{Título del post}`: usa el nombre del torneo.
- `{YYYY-MM-DD HH:MM:SS +0200}`: usa la fecha de hoy al inicio del día: `00:00:00 +0200`. Jekyll oculta los posts cuya fecha es futura respecto al momento del build, así que poner la hora a 00:00 evita que el post no aparezca el mismo día.
- `{Fechas}`, `{Lugar}`, `{Nombre del torneo}`: lo recogido en el paso 2, **sin reformatear las fechas**.
- `{num_players}`: del paso 3.
- `{URL info64}`: la URL original del paso 1.

## 8. Guardar el post

Deriva `{torneo-slug}` del path de la URL de info64 (ej. `iii-liga-club-gambito-de-guada-b`).

Guárdalo en:

```
_posts/{YYYY-MM-DD}-gambito-{torneo-slug}.md
```

Donde `{YYYY-MM-DD}` es la fecha de hoy.

## 9. Mostrar el resultado

Indica al usuario la ruta del fichero creado y muestra el contenido generado.

## Reglas

- El cuerpo del post **siempre va en español**, aunque la petición venga en inglés.
- Conserva el orden de la clasificación en la lista de jugadores del Gambito.
- No vuelvas a preguntar la URL de info64: es el argumento del comando.
- No reformatees las fechas que indique el usuario.
- Si no hubo jugadores del Gambito, el post se genera igualmente con la introducción y la frase de ausencia.
