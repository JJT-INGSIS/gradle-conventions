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