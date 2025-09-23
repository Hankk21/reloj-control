package com.example.relojcontrol.network;

public class ApiEndpoints {
    private static final String BASE_URL = "http://10.0.2.2/asistencia-api/api/";
    /*usuarios*/
    public static final String LOGIN = BASE_URL + "auth/login.php";
    public static final String USUARIOS_LIST = BASE_URL + "usuarios/list.php";
    public static final String USUARIOS_CREATE = BASE_URL + "usuarios/create.php";
    public static final String USUARIOS_UPDATE = BASE_URL + "usuarios/update.php";
    public static final String USUARIOS_DELETE = BASE_URL + "usuarios/delete.php";
    /*justificaciones*/
    public static final String JUSTIFICACIONES_CREATE = BASE_URL + "justificaciones/create.php";
    public static final String JUSTIFICACIONES_UPLOAD = BASE_URL + "justificaciones/subir.php";
    /*licencias*/
    public static final String LICENCIAS_UPLOAD = BASE_URL + "licencias/subir.php";
}
