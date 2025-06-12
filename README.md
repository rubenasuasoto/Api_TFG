# 🛠️ TFG API - Documentación Técnica

Bienvenido a la documentación oficial de la API del proyecto **TFG**. Este backend está desarrollado en **Kotlin** con **Spring Boot**, usa **MongoDB** como base de datos y cuenta con autenticación y autorización mediante **JWT**.

---

## 📚 Índice

- [Tecnologías utilizadas](#tecnologías-utilizadas)
- [Autenticación](#autenticación)
- [Usuarios](#usuarios)
- [Productos](#productos)
- [Pedidos](#pedidos)
- [Confirmación por Email](#confirmación-por-email)
- [Seguridad y Roles](#seguridad-y-roles)
- [Validación de Direcciones](#validación-de-direcciones)
- [Gestión de Errores](#gestión-de-errores)
- [Modelo de Datos](#modelo-de-datos)

---

## Tecnologías utilizadas

- Kotlin + Spring Boot
- Spring Security (OAuth2 + JWT)
- MongoDB
- WebClient (para integración con APIs externas)
- JavaMailSender (envío de correos HTML)
- GeoAPI.es (validación de direcciones)
- Gradle

---

## Autenticación

### `POST /usuarios/login`

Autentica al usuario y genera un token JWT. Es el punto de entrada a la API para usuarios registrados. No requiere autenticación previa.

---

### `POST /usuarios/register`

Registra un nuevo usuario, validando campos como dirección, formato de email, fortaleza de contraseña y unicidad de nombre/email. Esta operación también está disponible sin autenticación previa.

---

## Usuarios

### `GET /usuarios/self`

Devuelve los datos del usuario autenticado (propio perfil).

---

### `PUT /usuarios/self`

Permite actualizar el email, la dirección o la contraseña del usuario autenticado. Para cambiar la contraseña, es obligatorio proporcionar la contraseña actual.

---

### `DELETE /usuarios/self`

Elimina la cuenta del usuario autenticado.

---

### `GET /usuarios/check-admin`

Verifica si el usuario autenticado tiene el rol de administrador (`ADMIN`).

---

### Acceso Administrativo (requiere `ROLE_ADMIN`)

- `GET /usuarios` – Lista todos los usuarios del sistema.
- `GET /usuarios/{username}` – Muestra los datos completos de un usuario.
- `PUT /usuarios/{username}` – Permite actualizar a otro usuario (email, rol, contraseña, dirección).
- `DELETE /usuarios/{username}` – Elimina a un usuario del sistema.

---

## Productos

### Públicos

- `GET /productos` – Lista todos los productos disponibles.
- `GET /productos/search?query=...` – Busca productos por nombre del artículo.
- `GET /productos/{numeroProducto}` – Devuelve los detalles de un producto específico.

---

### Administrador (requiere `ROLE_ADMIN`)

- `POST /productos` – Crea un nuevo producto.
- `PUT /productos/{numeroProducto}` – Actualiza un producto existente.
- `DELETE /productos/{numeroProducto}` – Elimina un producto por número de producto.

---

## Pedidos

### Usuario autenticado

- `POST /pedidos/self` – Crea un pedido con una lista de productos. Se actualiza el stock y se genera una factura.
- `GET /pedidos/self` – Lista todos los pedidos hechos por el usuario autenticado.
- `DELETE /pedidos/self/{numeroPedido}` – Cancela un pedido si no han pasado más de 3 días desde su creación.

---

### Administrador (`ROLE_ADMIN`)

- `GET /pedidos` – Lista todos los pedidos del sistema.
- `GET /pedidos/{id}` – Devuelve los detalles de un pedido por su ID.
- `POST /pedidos` – Crea un pedido manualmente para un usuario.
- `PUT /pedidos/{numeroPedido}` – Cambia el estado del pedido (`PENDIENTE`, `ENTREGADO`, `CANCELADO`).
- `DELETE /pedidos/{numeroPedido}` – Elimina un pedido del sistema.

---

## Confirmación por Email

Después de crear un pedido, el sistema envía automáticamente un correo HTML al usuario con:

- Número de pedido
- Fecha de creación
- Lista detallada de productos
- Precio total
- Estado del pedido

---

## Seguridad y Roles

- Autenticación basada en tokens JWT (generados en `/usuarios/login`)
- Contraseñas cifradas con BCrypt
- Roles soportados: `USER` (por defecto) y `ADMIN`
- Control de acceso definido en `SecurityConfig.kt` y mediante anotaciones `@PreAuthorize`

---

## Validación de Direcciones

Durante el registro, las direcciones son validadas a través de la API de **GeoAPI.es**, asegurando que tanto la provincia como el municipio existen.

- Consulta de provincias (`/provincias`)
- Consulta de municipios en función del código de provincia (`/municipios?CPRO=...`)

---

## Gestión de Errores

La API utiliza un controlador centralizado de excepciones (`@ControllerAdvice`) que devuelve errores en formato estructurado:

- **400 Bad Request** – Parámetros inválidos, formato incorrecto.
- **401 Unauthorized** – Autenticación fallida.
- **404 Not Found** – Recurso inexistente.
- **409 Conflict** – Conflicto de datos (duplicados).
- **500 Internal Server Error** – Errores no controlados.

Cada error contiene un mensaje y la URI del endpoint afectado.

---

## Modelo de Datos

### Usuario

- `username`: String
- `email`: String
- `password`: String (encriptado)
- `roles`: "USER" o "ADMIN"
- `direccion`: objeto con provincia, municipio, calle, etc.
- `fechacrea`: fecha de creación

---

### Producto

- `numeroProducto`: código único del producto
- `articulo`: nombre del producto
- `descripcion`: descripción opcional
- `precio`: precio en euros
- `stock`: cantidad en inventario
- `imagenUrl`: URL de imagen opcional

---

### Pedido

- `numeroPedido`: UUID del pedido
- `usuario`: username del cliente
- `productos`: lista de códigos de producto
- `detalles`: lista de productos con precio y nombre
- `precioFinal`: total del pedido
- `factura`: objeto `Factura` con número y fecha
- `estado`: `PENDIENTE`, `ENTREGADO`, `CANCELADO`
- `fechaCreacion`: timestamp

---

### Factura

- `numeroFactura`: identificador único
- `fecha`: fecha de emisión

---

### LogSistema

- `usuario`: autor de la acción
- `accion`: descripción de lo realizado
- `referencia`: ID del pedido relacionado
- `fecha`: momento del log

---

## 🧪 Futuras mejoras

- Tests unitarios 
- Generación automática de documentación 
- Exportación a PDF de facturas


---
Debido al tamaño de los videos se mostrara en la denfensa del proyecto 
