package com.example.relojcontrol.models;

import java.io.Serializable;

public class Usuario implements Serializable {
    private int idUsuario;
    private String rut;
    private String nombre;
    private String apellido;
    private String correo;
    private String contrasena;
    private String estadoUsuario;
    private int idRol;

    public Usuario() {}


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
    public void setIdRol(int idRol) { this.idRol = idRol;}

    // Métodos helper
    public boolean isActivo() {
        return "activo".equalsIgnoreCase(estadoUsuario);
    }

    public String getRolTexto() {
        return idRol == 1 ? "Administrador" : "Empleado";
    }


}
