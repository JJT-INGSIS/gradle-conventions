# Gradle conventions

Convención compartida para los servicios Kotlin/Spring Boot de Snippet Searcher.

`jjt.spring-service` configura Kotlin 2.4.10, Spring Boot 4.1.1, JDK 21, ktlint, detekt, JUnit y reportes JaCoCo HTML/XML. `check` ejecuta las verificaciones y genera el reporte de cobertura. No hay un porcentaje mínimo de cobertura configurado todavía.

Detekt usa `2.0.0-alpha.6` porque esa versión declara compatibilidad con Kotlin 2.4.10. Hay que verificar el conjunto completo con Gradle 9.3.0 antes de publicar la primera versión.

## Verificación y publicación

Con JDK 21:

```powershell
.\gradlew.bat check
.\gradlew.bat publishToMavenLocal
```

Los servicios pueden probar la versión publicada localmente con `-PuseLocalConventions=true`. Para publicar `0.1.0` en GitHub Packages, configurar `GITHUB_ACTOR` y `GITHUB_TOKEN` con permisos de publicación y ejecutar:

```powershell
.\gradlew.bat publish
```

El ID del plugin y su versión se fijan en cada servicio. Al cambiar la convención, publicar una versión nueva y actualizar explícitamente los consumidores.
