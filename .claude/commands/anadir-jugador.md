El usuario quiere añadir un jugador al listado de jugadores del club (`jugadores.md`), o cambiarle el nombre o el FIDE ID a uno existente.

**Nunca edites `jugadores.md` a mano.** Es un fichero generado. La fuente de verdad es `chessratingscli/src/main/resources/application.properties`.

Sigue estos pasos:

## 1. Identificar parámetros

Del mensaje del usuario extrae:
- **Nombre completo** del jugador.
- **FIDE ID**, si lo tiene. Si el usuario dice que no tiene FIDE ID todavía, el valor se deja vacío.

Si el usuario no menciona el FIDE ID ni dice que no lo tiene, pregúntaselo antes de continuar.

## 2. Comprobar que no existe ya

Lee `chessratingscli/src/main/resources/application.properties` y busca el nombre (ignorando tildes, mayúsculas y orden de palabras) y el FIDE ID. Si ya existe, actualiza la entrada en vez de duplicarla.

## 3. Añadir la entrada en `application.properties`

Cada jugador son dos propiedades con la misma clave:

```
players.<clave>.fullname=<Nombre completo>
players.<clave>.fideid=<FIDE ID o vacío>
```

- **clave**: minúsculas, sin tildes, sin espacios. Normalmente el nombre de pila (`vicente`, `efren`). Si ya hay otro jugador con ese nombre, añade el apellido (`javierdiaz`, `juancarlosgordillo`, `ignaciomartinez`).
- **fullname**: escríbelo sin tildes ni caracteres especiales. El fichero está en una codificación antigua y las tildes se corrompen. Vale tanto `Nombre Apellidos` como `Apellidos, Nombre`.
- **fideid**: solo el número. Si no tiene, deja `players.<clave>.fideid=` vacío.

Añade las dos líneas al final del fichero.

## 4. Regenerar `jugadores.md`

Ejecuta el CLI desde su directorio:

```
cd chessratingscli && ./gradlew run -q
```

El CLI consulta los ratings en FIDE y reescribe `jugadores.md` en la raíz del repositorio. Los jugadores con rating aparecen ordenados por rating estándar; los que no tienen FIDE ID aparecen al final sin enlace.

## 5. Comprobar el resultado

Ejecuta `git diff jugadores.md` y confirma que el nuevo jugador aparece en la tabla.

Avisa al usuario de que, al regenerar, el diff puede incluir también cambios de rating y de orden de otros jugadores, porque el CLI descarga los datos actuales de FIDE. Es el mismo efecto que tiene el workflow semanal `.github/workflows/update-jugadores.yml`.

## 6. No commitear

Deja los cambios sin commitear salvo que el usuario lo pida.
