# Gradle conventions

Convención compartida para los servicios Kotlin/Spring Boot de Snippet Searcher.

[`jjt.spring-service`](src/main/kotlin/jjt.spring-service.gradle.kts) configura Kotlin, Spring Boot, JDK 21, ktlint, Detekt, JUnit y cobertura con JaCoCo. Las versiones de los plugins se fijan en [`build.gradle.kts`](build.gradle.kts).

## Formato y lint

- **ktlint** usa el estilo `ktlint_official` y un límite de 120 columnas.
- **Detekt** aplica las reglas compartidas de [`detekt.yml`](src/main/resources/jjt/detekt.yml) sobre su configuración predeterminada y valida el archivo al ejecutar el análisis.

`check` corre ktlint, `detektMain`, `detektTest` y los tests con JUnit Platform. Las dos tareas de Detekt analizan con resolución de tipos, necesaria para reglas como `NullableToStringCall`. JaCoCo genera reportes HTML y XML sin imponer un mínimo de cobertura.

## Git hooks

Para instalar los hooks compartidos, ejecutá desde la raíz del clon del servicio:

```bash
./gradlew installGitHooks
```

| Hook | Qué hace |
|---|---|
| `pre-commit` | Formatea con `ktlintFormat` los archivos Kotlin en stage, los vuelve a agregar al commit y corre `detektMain` y `detektTest`. Mientras tanto aparta lo que no se está commiteando, así el formato y el análisis ven exactamente el commit. |
| `pre-push` | Corre `check` completo, con los tests. |

- Si un archivo está solo en parte en stage, el `pre-commit` no toca nada y pide agregar o apartar el resto: formatearlo podría impedir devolver la parte que queda afuera.
- `installGitHooks` respeta la ruta de hooks configurada en Git. Si encuentra un hook distinto, lo conserva y muestra un aviso. Para reemplazar un archivo regular por el hook compartido, ejecutá `./gradlew installGitHooks -PforceGitHooks`.
- El CI no usa los hooks: corre `build` y verifica sin modificar código.

Los scripts están en [`git-hooks/`](src/main/resources/jjt/git-hooks/).

## Agente de New Relic

`jjt.spring-service` aplica [`jjt.newrelic-agent`](src/main/kotlin/jjt.newrelic-agent.gradle.kts). Antes de `bootJar`, la tarea `prepareNewRelicAgent` prepara estos archivos en `build/newrelic`:

| Archivo | Contenido |
|---|---|
| `newrelic.jar` | Agente Java oficial de Maven Central, validado contra el SHA-256 fijado en la convención. |
| `newrelic.yml` | [Configuración compartida del agente](src/main/resources/jjt/newrelic/newrelic.yml). |
| `start-service.sh` | [Script de arranque](src/main/resources/jjt/newrelic/start-service.sh) con el nombre del proyecto Gradle. |

Gradle vuelve a ejecutar la tarea si cambia el JAR, el SHA-256 esperado, la configuración o el script generado.

Cada Dockerfile copia la carpeta y arranca con el script:

```dockerfile
COPY --from=build /app/build/newrelic /app/newrelic
ENTRYPOINT ["/app/newrelic/start-service.sh", "/app/app.jar"]
```

| Variable | Uso |
|---|---|
| `NEW_RELIC_ENABLED` | Acepta `true` o `false`. El valor predeterminado es `false` y arranca con `java -jar`. |
| `NEW_RELIC_ENVIRONMENT` | `local`, `dev` o `prod`. Obligatoria cuando el agente está activado. |
| `NEW_RELIC_LICENSE_FILE` | Ruta al archivo con la licencia. Por defecto usa el secreto de Swarm `/run/secrets/newrelic_license`. |
| `NEW_RELIC_SERVICE_VERSION` | Etiqueta `version` (letras, números, `.`, `_` y `-`). El valor predeterminado es `unknown`. |

Al activar el agente, el script valida su configuración, define el nombre `<servicio> (<ambiente>)` y las etiquetas `environment`, `service` y `version`, y arranca la JVM con `-javaagent`. La licencia se lee del archivo montado y se pasa a la JVM mediante el entorno del proceso.

| Señal | Configuración |
|---|---|
| Trazas | Propagación W3C. Cuando recibe una traza de otro servicio, el agente conserva la decisión de muestreo que viene en el encabezado. |
| Atributos y SQL | Excluye encabezados de solicitudes y respuestas, parámetros de solicitudes y de mensajes. Ofusca SQL y desactiva `EXPLAIN` y la captura de SQL lento. |
| Errores | Omite los mensajes de excepción y clasifica los HTTP `400–499` como errores esperados. |
| Logs de aplicación | Reenvía el texto del mensaje y únicamente los campos MDC `request.id`, `operation`, `outcome`, `duration.ms`, `correlation.id`, `event.id`, `job.id` y `attempt`. El texto del log debe quedar libre de datos sensibles. |
| Logs del agente | Salida por `STDOUT`. |
| Browser | La autoinstrumentación del navegador está desactivada en el agente Java. |

## Verificación y publicación

Para verificar la convención con JDK 21:

```bash
./gradlew check
```

Para probar el plugin desde un consumidor local, publicalo primero en Maven Local con `./gradlew publishToMavenLocal`.

Para publicar en GitHub Packages, configurar `GITHUB_ACTOR` y `GITHUB_TOKEN` con permisos de publicación y ejecutar:

```bash
./gradlew publish
```

Cada servicio fija el ID y la versión del plugin. Los cambios de la convención llegan a los servicios cuando estos adoptan una versión publicada.
