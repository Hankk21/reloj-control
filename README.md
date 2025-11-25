# 🕒 RelojControl - Sistema de Gestión de Asistencia
RelojControl es una aplicación nativa de Android diseñada para modernizar el control de asistencia laboral. Permite a las empresas gestionar marcas de entrada/salida, tramitar justificaciones y licencias médicas, y generar reportes gerenciales, todo sincronizado en tiempo real mediante la nube.

---
## 🖼️ Capturas
| Menu de la aplicacion | Menu de Empleado | 
| :---: | :---: |
| ![Menu](https://github.com/user-attachments/assets/ada781f6-83b1-48f5-94fc-1389043b066f) | ![Menu Empleado](https://github.com/user-attachments/assets/3a129541-0f6d-460d-8a76-e16fd0eaa91b)

| Solicitudes | Licencias |
| :---: | :---: |
| ![Solicitudes](https://github.com/user-attachments/assets/b4f82506-acab-496e-9718-a34c9ee6e699) | ![Licencias](https://github.com/user-attachments/assets/2952b4e1-25f2-4846-b4c6-d7f386cdf0f5)

| Menu de Administrador | Reporte de asistencia |
| :---: | :---: |
| ![Menu Admin](https://github.com/user-attachments/assets/a6bbf0ed-7ae5-4047-b685-bfea46b5864a) | ![Reporte de asistencia](https://github.com/user-attachments/assets/70297041-7890-4f0d-9e81-de25a9276f26)

| Usuarios | Justificaciones |
| :---: | :---: |
| ![Usuarios](https://github.com/user-attachments/assets/5fac7246-83a4-4c12-8bae-3508a8fd36d2) | ![Justificaciones](https://github.com/user-attachments/assets/1f716b29-7900-47c8-8be8-3856629816a9)

| Gestion Usuarios | Añadir Usuario |
| :---: | :---: |
| ![Gestion Usuarios](https://github.com/user-attachments/assets/aee12b95-13c8-41d5-baf2-3d5ce286556b) | ![Añadir Usuario](https://github.com/user-attachments/assets/54c6ec45-8a73-4ecd-af11-8caf987da8cf)

---

## 📂 Estructura del Proyecto
El código fuente está organizado siguiendo el patrón MVC, separando claramente la lógica de negocio, la interfaz de usuario y la conexión a datos.
```
☕ Java (com.example.relojcontrol)
com.example.relojcontrol
├── 📂 activities                # Controladores de Pantalla (Logic)
│   ├── 📂 admin                 # Gestión de solicitudes (Justificaciones/Licencias)
│   ├── 📂 empleado              # Pantalla principal del empleado
│   ├── AnadirUsuarioActivity    # Formulario de creación de empleados
│   ├── LoginActivity            # Autenticación
│   ├── MainAdminActivity        # Dashboard principal del administrador
│   ├── ReportesActivity         # Generación y exportación de datos
│   └── ... (Otras actividades generales y de gestión de usuarios)
│
├── 📂 adapters                  # Adaptadores para RecyclerViews
│   ├── AsistenciaAdapter        # Listado de historial
│   ├── UsuarioAdapter           # Lista de empleados (Admin)
│   ├── ReportesAdapter          # Vista previa de reportes
│   └── ... (Adaptadores para justificaciones y licencias)
│
├── 📂 models                    # Modelos de Datos (POJOs)
│   ├── Asistencia               # Estructura de marca de tiempo
│   ├── Usuario                  # Datos del empleado y roles
│   ├── Justificacion / Licencia # Solicitudes
│   └── Reporte
│
└── 📂 network                   # Capa de Red y Datos
    ├── FirebaseClient           # Instancia de cliente
    └── FirebaseRepository       # Repositorio para consultas a la BDD
    
🎨 Recursos (res)    
res
├── 📂 layout                    # Interfaz de Usuario (XML)
│   ├── activity_*.xml           # Diseños de pantallas completas
│   └── item_*.xml               # Diseños de filas para listas (RecyclerView)
│
└── 📂 menu                      # Menús de navegación (Toolbar)
    ├── admin_menu.xml           # Opciones para Administrador
    └── empleado_menu.xml        # Opciones para Empleado
```
---

## 📱 Características Principales
La aplicación cuenta con un sistema de roles robusto (Administrador y Empleado) que adapta la interfaz y funcionalidades dinámicamente.

---

👤 Módulo de Empleado
Marcaje Rápido: Registro de Entrada y Salida con validación de estado para evitar inconsistencias.

Historial Personal: Visualización inmediata de marcas del día e histórico.

Gestión de Solicitudes: Envío de Justificaciones y Licencias Médicas con capacidad de adjuntar evidencias (PDF/Imágenes).

Feedback en Tiempo Real: Estado de solicitudes visual (Pendiente, Aprobado, Rechazado).

Seguridad: Cierre de sesión forzoso automático si la cuenta es desactivada por la empresa.

---

## 🛡️ Módulo de Administrador
- Dashboard Gerencial: Estadísticas en tiempo real de asistencia, ausencias y atrasos.
- Gestión de Usuarios (CRUD): Alta, baja, modificación y asignación de roles.
- Control de Acceso: Activación o bloqueo inmediato de usuarios.
- Centro de Aprobaciones: Bandeja de entrada para aprobar o rechazar justificaciones y licencias.
- Reportes Avanzados: Filtrado y exportación a CSV (Excel) utilizando la API MediaStore (compatible con Android 10+).

---

## 🛠️ Tecnologías y Arquitectura
El proyecto sigue una arquitectura limpia modularizada por funcionalidad:
Componente,Tecnología / Librería
- Lenguaje, Java (JDK 8+)
- Arquitectura, MVC (Model-View-Controller)
- Interfaz (UI), "Material Design 3, CardViews, Custom RecyclerViews"
- Base de Datos, Firebase Realtime Database (Sincronización en vivo)
- Autenticación, Firebase Authentication (Sesiones seguras)
- Concurrencia, Callbacks asíncronos para operaciones de red
  
---

## 💡 Puntos Clave de Implementación
- Mapeo de IDs: Sistema personalizado que vincula los UIDs alfanuméricos de Firebase con IDs numéricos cortos y legibles para la gestión interna de la empresa.
- Optimización de Modelos: Uso de anotaciones @Exclude en los modelos para manejar lógica interna sin ensuciar la base de datos al serializar objetos.
- Gestión de Memoria: Implementación de addListenerForSingleValueEvent para lecturas únicas y limpieza rigurosa de listeners en el ciclo de vida onDestroy para prevenir memory leaks.
- Separación de Menús: Lógica de inflado de menús diferenciada en el res/menu para cargar opciones contextualmente según el rol del usuario logueado.
  
---

## ⚙️ Instalación
1. Clona el repositorio: https://github.com/Hankk21/reloj-control.git
2. Ábrelo en Android Studio.  
3. Compila el proyecto y ejecútalo en un emulador o dispositivo Android físico.
   
---

## 👨‍💻 Autor
Desarrollado por 
[Kenjiro Aguilera](https://github.com/mrskenchan).
[Cristobal Gómez](https://github.com/cristobalGomez189).
[Matías Ulloa](https://github.com/Hankk21).

---

## 🧾 Licencia
Distribuido bajo la licencia MIT. Consulta el archivo `LICENSE` para más información.  

---
