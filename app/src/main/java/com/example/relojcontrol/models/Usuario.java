package com.example.relojcontrol.models;

public class Usuario {
    private int idUsuario;
    private String rut;
    private String nombre;
    private String apellido;
    private String correo;
    private String contrasena;
    private String estadoUsuario;
    private int idRol;

    public Usuario() {}

    public Usuario(int idUsuario, String rut, String nombre, String apellido, String correo,
                   String contrasena, String estadoUsuario, int idRol) {
        this.idUsuario = idUsuario;
        this.rut = rut;
        this.nombre = nombre;
        this.apellido = apellido;
        this.correo = correo;
        this.contrasena = contrasena;
        this.estadoUsuario = estadoUsuario;
        this.idRol = idRol;
    }

    // Getters y Setters
    public int getIdUsuario() { return idUsuario; }
    public void setIdUsuario(int idUsuario) { this.idUsuario = idUsuario; }

    public String getRut() { return rut; }
    public void setRut(String rut) { this.rut = rut; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }

    public String getNombreCompleto() { return nombre + " " + apellido; }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }

    public String getContrasena() { return contrasena; }
    public void setContrasena(String contrasena) { this.contrasena = contrasena; }

    public String getEstadoUsuario() { return estadoUsuario; }
    public void setEstadoUsuario(String estadoUsuario) { this.estadoUsuario = estadoUsuario; }

    public int getIdRol() { return idRol; }
    public void setIdRol(int idRol) { this.idRol = idRol; }
}
