# Auditoría de Seguridad - Chat Service

Basado en la teoría de Seguridad (CIA Triad, Tácticas y Patrones Arquitectónicos):

## Cumple

### Tácticas: Resistir Ataques
*   **Encrypt Data:** TLS/mTLS configurado en Spring Boot puerto **8443**. El `application.yml` define `server.ssl.enabled: true`, protocolo TLS con `enabled-protocols: TLSv1.2,TLSv1.3`, y cifrados AEAD negociados por el JRE. RabbitMQ se conecta vía **AMQPS 5671** con `ssl.enabled: true`, algoritmo `TLSv1.3`, y `verify-hostname: true`.
*   **Authenticate Actor:** 
    *   **mTLS mutua:** `client-auth: need` en el conector HTTPS. Solo clientes que presenten un certificado firmado por la CA interna (`truststore.jks`) pueden establecer conexión TLS.
    *   **Aplicación (JWT/Firebase):** `pom.xml` incluye `spring-boot-starter-security`, `spring-boot-starter-oauth2-resource-server` y `firebase-admin`. La clase `SecurityConfig.java` (`@EnableWebSecurity`) valida tokens JWT vía `JWT_ISSUER_URI`.
*   **Limit Access:** 
    *   CORS restringido a orígenes explícitos (`ALLOWED_ORIGINS`).
    *   En docker-compose solo se expone el puerto **8443** internamente (ningún puerto mapeado al host directamente); el tráfico entra únicamente a través del gateway NGINX.
*   **Change Default Settings:** No hay passwords hardcodeadas en el runtime de Spring Boot; todas se inyectan por variables de entorno (`SSL_KEYSTORE_PASSWORD`, `SSL_TRUSTSTORE_PASSWORD`, `SPRING_RABBITMQ_PASSWORD`).

### Tácticas: Detectar Ataques / Recuperar
*   **Verify Message Integrity:** La integridad de los tokens JWT entrantes se valida mediante criptografía asimétrica/firmas (OAuth2 Resource Server).
*   **Maintain Audit Trail:** Logs estructurados de Spring Boot, endpoint de health expuesto (`/actuator/health`, `/health`), y healthcheck en Docker Compose con mTLS (curl con certificado de cliente).

## No Cumple / Gaps conocidos
*   **Dockerfile desincronizado:** El `Dockerfile` expone `8081` y el healthcheck interno del contenedor no refleja el puerto TLS 8443. Esto no afecta la seguridad en producción (docker-compose sobreescribe con 8443 y healthcheck HTTPS), pero debe alinearse para evitar confusiones operativas.
*   **Usuario root en contenedor:** El `Dockerfile` no incluye instrucción `USER`; el proceso Java corre como root dentro del contenedor. Recomendación: añadir un usuario `non-root`.
*   **GraphiQL expuesto:** `spring.graphql.graphiql.enabled: true` permite acceso al IDE interactivo en producción. En un entorno productivo debe deshabilitarse o restringirse por IP/autenticación adicional.

## Decisiones del Laboratorio 5
*   **Aplicación del Secure Channel Pattern en este servicio:** Se implementó TLS 1.2/1.3 en el puerto 8443 con autenticación mutua (`client-auth: need`) mediante keystore PKCS12 y truststore JKS. La comunicación con RabbitMQ se migró a AMQPS 5671 con verificación de hostname y truststore JKS, cerrando el canal de eventos anteriormente en texto plano.
