package com.example.relojcontrol.models;

import com.google.firebase.database.PropertyName;
import com.google.firebase.database.Exclude;
import java.io.Serializable;

public class Usuario implements Serializable {
    @PropertyName("id_usuario")
    private int idUsuario;
    private String rut;
    private String nombre;
    private String apellido;
    private String correo;
    private String contrasena;

    @PropertyName("estado_usuario")
    private String estadoUsuario;
    @PropertyName("id_rol")
    private int idRol;

    public Usuario() {}


    // Getters y Setters
    @PropertyName("id_usuario")
    public int getIdUsuario() { return idUsuario; }
    @PropertyName("id_usuario")
    public void setIdUsuario(int idUsuario) { this.idUsuario = idUsuario; }

    public String getRut() { return rut; }
    public void setRut(String rut) { this.rut = rut; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }

    public String getContrasena() { return contrasena; }
    public void setContrasena(String contrasena) { this.contrasena = contrasena; }
    @PropertyName("estado_usuario")
    public String getEstadoUsuario() { return estadoUsuario; }
    @PropertyName("estado_usuario")
    public void setEstadoUsuario(String estadoUsuario) { this.estadoUsuario = estadoUsuario; }
    @PropertyName("id_rol")
    public int getIdRol() { return idRol; }
    @PropertyName("id_rol")
    public void setIdRol(int idRol) { this.idRol = idRol;}

    // Métodos helper
    @Exclude
    public boolean isActivo() {
        return "activo".equalsIgnoreCase(estadoUsuario);
    }
    @Exclude
    public String getRolTexto() {
        return idRol == 1 ? "Administrador" : "Empleado";
    }
    @Exclude
    public String getNombreCompleto() {
        return nombre + " " + apellido;
    }


}
