package com.example.relojcontrol.models;

import com.google.firebase.database.PropertyName;
import java.io.Serializable;

public class Justificacion implements Serializable {
    private String id; // ID de Firebase (clave del nodo)

    @PropertyName("id_justificacion")
    private int idJustificacion;

    @PropertyName("id_usuario")
    private int idUsuario;

    @PropertyName("fecha_justificar")
    private String fechaJustificar; // Fecha que se va a justificar

    @PropertyName("fecha_creacion")
    private String fechaCreacion; // Fecha cuando se creó la solicitud

    private String motivo;
    private String descripcion; // Agregado para más detalle

    @PropertyName("url_documento")
    private String urlDocumento; // Cambiado de "evidencia" a "urlDocumento"

    @PropertyName("id_estado")
    private int idEstado;

    // Campos para display (si vienen del JOIN o consultas adicionales)
    private String nombreUsuario;
    private String rutUsuario;

    public Justificacion() {}

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    @PropertyName("id_justificacion")
    public int getIdJustificacion() { return idJustificacion; }

    @PropertyName("id_justificacion")
    public void setIdJustificacion(int idJustificacion) { this.idJustificacion = idJustificacion; }

    @PropertyName("id_usuario")
    public int getIdUsuario() { return idUsuario; }

    @PropertyName("id_usuario")
    public void setIdUsuario(int idUsuario) { this.idUsuario = idUsuario; }

    @PropertyName("fecha_justificar")
    public String getFechaJustificar() { return fechaJustificar; }

    @PropertyName("fecha_justificar")
    public void setFechaJustificar(String fechaJustificar) { this.fechaJustificar = fechaJustificar; }

    @PropertyName("fecha_creacion")
    public String getFechaCreacion() { return fechaCreacion; }

    @PropertyName("fecha_creacion")
    public void setFechaCreacion(String fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

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
