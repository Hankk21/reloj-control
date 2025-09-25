package com.example.relojcontrol.network;

public class ApiEndpoints {
    private static final String BASE_URL = "http://10.0.2.2/asistencia-api";

    /* Auth */
    public static final String LOGIN = BASE_URL + "auth/login.php";

    /* TEST */
    public static final String SIMPLE_TEST = BASE_URL + "auth/simple_test.php";

    /* Dashboard */
    public static final String DASHBOARD_ADMIN = BASE_URL + "dashboard/admin.php";

    /* Usuarios */
    public static final String USUARIOS_LIST = BASE_URL + "usuarios/list.php";
    public static final String USUARIOS_CREATE = BASE_URL + "usuarios/create.php";
    public static final String USUARIOS_UPDATE = BASE_URL + "usuarios/update.php";
    public static final String USUARIOS_DELETE = BASE_URL + "usuarios/delete.php";

    /* Asistencia */
    public static final String ASISTENCIA_REGISTRAR = BASE_URL + "asistencia/registrar.php";
    public static final String ASISTENCIA_HISTORIAL = BASE_URL + "asistencia/historial.php";
    public static final String ASISTENCIA_HOY = BASE_URL + "asistencia/hoy.php";

    /* Justificaciones */
    public static final String JUSTIFICACIONES_CREATE = BASE_URL + "justificaciones/crear.php";
    public static final String JUSTIFICACIONES_UPLOAD = BASE_URL + "justificaciones/subir.php";
    public static final String JUSTIFICACIONES_LIST = BASE_URL + "justificaciones/list.php";

    /* Licencias */
    public static final String LICENCIAS_CREATE = BASE_URL + "licencias/crear.php";
    public static final String LICENCIAS_UPLOAD = BASE_URL + "licencias/subir.php";
    public static final String LICENCIAS_LIST = BASE_URL + "licencias/list.php";

    /* Reportes */
    public static final String REPORTES_GENERAR = BASE_URL + "reportes/generar.php";
}
