# Gradle conventions

Convención compartida para los servicios Kotlin/Spring Boot de Snippet Searcher.

`jjt.spring-service` configura Kotlin 2.4.10, Spring Boot 4.1.1, JDK 21, ktlint, detekt, JUnit y reportes JaCoCo HTML/XML. `check` ejecuta las verificaciones y genera el reporte de cobertura. No hay un porcentaje mínimo de cobertura configurado todavía.

Detekt usa `2.0.0-alpha.6` porque esa versión declara compatibilidad con Kotlin 2.4.10.

## Formato y lint

Las reglas viven en este repositorio y los servicios las reciben al subir de versión. Ningún servicio tiene su propia copia.

- **ktlint** `1.8.0`, estilo `ktlint_official`, 120 columnas. Se configura en `jjt.spring-service.gradle.kts`.
- **detekt** parte de la config por defecto (`buildUponDefaultConfig`) y aplica los cambios de `src/main/resources/jjt/detekt.yml`. Cada regla del archivo explica por qué se activa o se ajusta.

`check` ejecuta `detektMain` y `detektTest`, que analizan con type resolution. La tarea `detekt` no la tiene, y las reglas que dependen de los tipos (por ejemplo `NullableToStringCall`) se saltearían sin avisar.

Para cambiar una regla: editar `detekt.yml`, publicar una versión nueva y actualizarla en los servicios. Un error de tipeo en el archivo hace fallar el build, porque detekt valida la configuración.

## Git hooks

La convención agrega la tarea `installGitHooks`. Se corre una vez por clon, desde la raíz del servicio:

```bash
./gradlew installGitHooks
```

| Hook | Qué hace |
|---|---|
| `pre-commit` | Formatea con `ktlintFormat` los archivos Kotlin en stage, los vuelve a agregar al commit y corre `detektMain` y `detektTest`. Mientras tanto aparta lo que no se está commiteando, así el formato y el análisis ven exactamente el commit. |
| `pre-push` | Corre `check` completo, con los tests. |

- Si un archivo está solo en parte en stage, el `pre-commit` no toca nada y pide agregar o apartar el resto: formatearlo podría impedir devolver la parte que queda afuera.
- Al publicar una versión nueva de los scripts, hay que volver a correr `installGitHooks`.
- El CI no usa los hooks: corre `build` y verifica sin modificar código.

Los scripts están en `src/main/resources/jjt/git-hooks/`.

## Agente de New Relic (SNI-81)

`jjt.spring-service` aplica `jjt.newrelic-agent`. Antes de `bootJar`, la tarea `prepareNewRelicAgent` deja en `build/newrelic`:

| Archivo | Contenido |
|---|---|
| `newrelic.jar` | Agente Java oficial `9.5.0` de Maven Central. El build falla si su SHA-256 no es `411587286dbb83b06fcdd042ad1c9c1818655831366717aef3611962ea2c1b73` |
| `newrelic.yml` | Configuración común de los servicios (`src/main/resources/jjt/newrelic/newrelic.yml`) |
| `start-service.sh` | Script de arranque con el nombre del servicio (`rootProject.name`) |

Cada Dockerfile copia la carpeta y arranca con el script:

```dockerfile
COPY --from=build /app/build/newrelic /app/newrelic
ENTRYPOINT ["/app/newrelic/start-service.sh", "/app/app.jar"]
```

| Variable | Uso |
|---|---|
| `NEW_RELIC_ENABLED` | `false` por defecto: arranca con `java -jar`, igual que antes. Cualquier valor que no sea `true` o `false` es un error |
| `NEW_RELIC_ENVIRONMENT` | `local`, `dev` o `prod`; obligatoria si está activado |
| `NEW_RELIC_LICENSE_FILE` | Archivo con la licencia; por defecto el secreto de Swarm `/run/secrets/newrelic_license`. Nunca va en la imagen, en variables del stack ni en Git |
| `NEW_RELIC_SERVICE_VERSION` | Etiqueta `version` (letras, números, `.`, `_`, `-`); por defecto `unknown` |

Activado, el script valida todo antes de arrancar (sale con 64 sin mostrar la licencia), define el nombre `<servicio> (<ambiente>)` y las etiquetas `environment`, `service` y `version`, y agrega `-javaagent`. La licencia pasa a la JVM como variable de entorno del proceso, nunca como argumento.

| Configuración | Por qué |
|---|---|
| Solo encabezados W3C; `remote_parent_sampled: always_on` | Los servicios llamados siguen la decisión de muestreo de Snippets |
| Sin headers ni parámetros de request y respuesta; SQL ofuscado; sin `explain`; mensajes de excepción despojados | No enviar datos de usuario |
| `expected_status_codes: 400-499` | Código inválido, denegaciones y conflictos no cuentan como errores técnicos |
| Reenvío de logs con el MDC `request.id`, `operation`, `outcome`, `duration.ms` y, desde 0.3.1, `correlation.id`, `event.id`, `job.id` y `attempt` | Logs del mismo pedido y del mismo evento o trabajo de fondo (SNI-83), sin mandar otros campos del MDC |
| Log del agente a `STDOUT` | El usuario 10001 no puede escribir en `/app/newrelic` |

Para cambiar la versión del agente: actualizar versión y SHA-256 en `jjt.newrelic-agent.gradle.kts`, publicar una versión nueva y subirla en los servicios.

## Verificación y publicación

Con JDK 21:

```bash
./gradlew check
./gradlew publishToMavenLocal
```

Para publicar en GitHub Packages, configurar `GITHUB_ACTOR` y `GITHUB_TOKEN` con permisos de publicación y ejecutar:

```bash
./gradlew publish
```

El ID del plugin y su versión se fijan en cada servicio. Al cambiar la convención, publicar una versión nueva y actualizar explícitamente los consumidores.