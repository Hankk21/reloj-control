package com.example.relojcontrol.models;

import com.google.firebase.database.PropertyName;
import java.io.Serializable;

public class Licencia implements Serializable {
    private String id; // ID de Firebase (clave del nodo)

    @PropertyName("id_licencia")
    private int idLicencia;

    @PropertyName("id_usuario")
    private int idUsuario;

    @PropertyName("fecha_inicio")
    private String fechaInicio;

    @PropertyName("fecha_fin")
    private String fechaFin;

    @PropertyName("fecha_creacion")
    private String fechaCreacion; // Agregado

    private String motivo; // Agregado para más contexto

    @PropertyName("url_documento")
    private String urlDocumento; // Cambiado de "documento"

    @PropertyName("id_estado")
    private int idEstado;

    // Campos para display
    private String nombreUsuario;
    private String rutUsuario;

    public Licencia() {}

    public Licencia(int idLicencia, int idUsuario, String fechaInicio, String fechaFin,
                    String urlDocumento, int idEstado) {
        this.idLicencia = idLicencia;
        this.idUsuario = idUsuario;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.urlDocumento = urlDocumento;
        this.idEstado = idEstado;
    }

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    @PropertyName("id_licencia")
    public int getIdLicencia() { return idLicencia; }

    @PropertyName("id_licencia")
    public void setIdLicencia(int idLicencia) { this.idLicencia = idLicencia; }

    @PropertyName("id_usuario")
    public int getIdUsuario() { return idUsuario; }

    @PropertyName("id_usuario")
    public void setIdUsuario(int idUsuario) { this.idUsuario = idUsuario; }

    @PropertyName("fecha_inicio")
    public String getFechaInicio() { return fechaInicio; }

    @PropertyName("fecha_inicio")
    public void setFechaInicio(String fechaInicio) { this.fechaInicio = fechaInicio; }

    @PropertyName("fecha_fin")
    public String getFechaFin() { return fechaFin; }

    @PropertyName("fecha_fin")
    public void setFechaFin(String fechaFin) { this.fechaFin = fechaFin; }

    @PropertyName("fecha_creacion")
    public String getFechaCreacion() { return fechaCreacion; }

    @PropertyName("fecha_creacion")
    public void setFechaCreacion(String fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }

    @PropertyName("url_documento")
    public String getUrlDocumento() { return urlDocumento; }

    @PropertyName("url_documento")
    public void setUrlDocumento(String urlDocumento) { this.urlDocumento = urlDocumento; }

    @PropertyName("id_estado")
    public int getIdEstado() { return idEstado; }

    @PropertyName("id_estado")
    public void setIdEstado(int idEstado) { this.idEstado = idEstado; }

    public String getNombreUsuario() { return nombreUsuario; }
    public void setNombreUsuario(String nombreUsuario) { this.nombreUsuario = nombreUsuario; }

    public String getRutUsuario() { return rutUsuario; }
    public void setRutUsuario(String rutUsuario) { this.rutUsuario = rutUsuario; }

    // MÉTODOS UTILITARIOS
    public boolean tieneDocumento() {
        return urlDocumento != null && !urlDocumento.isEmpty();
    }

    public String getEstadoTexto() {
        switch (idEstado) {
            case 1: return "Registrado";
            case 2: return "Pendiente";
            case 3: return "Aprobado";
            case 4: return "Rechazado";
            default: return "Desconocido";
        }
    }

    public String getInfoUsuario() {
        if (nombreUsuario != null && rutUsuario != null) {
            return nombreUsuario + " (" + rutUsuario + ")";
        }
        return "Usuario #" + idUsuario;
    }
}
