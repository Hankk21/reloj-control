package com.example.relojcontrol.models;

public class Justificacion {
    private int idJustificacion;
    private int idUsuario;
    private String fecha;
    private String motivo;
    private String evidencia;
    private int idEstado;

    // Campos para display (si vienen del JOIN)
    private String nombreUsuario;
    private String rutUsuario;

    public Justificacion() {}

    // Getters y Setters (mantener tus originales)
    public int getIdJustificacion() { return idJustificacion; }
    public void setIdJustificacion(int idJustificacion) { this.idJustificacion = idJustificacion; }

    public int getIdUsuario() { return idUsuario; }
    public void setIdUsuario(int idUsuario) { this.idUsuario = idUsuario; }

    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }

    public String getEvidencia() { return evidencia; }
    public void setEvidencia(String evidencia) { this.evidencia = evidencia; }

    public int getIdEstado() { return idEstado; }
    public void setIdEstado(int idEstado) { this.idEstado = idEstado; }

    public String getNombreUsuario() { return nombreUsuario; }
    public void setNombreUsuario(String nombreUsuario) { this.nombreUsuario = nombreUsuario; }

    public String getRutUsuario() { return rutUsuario; }
    public void setRutUsuario(String rutUsuario) { this.rutUsuario = rutUsuario; }

    // MÉTODOS UTILITARIOS (NUEVOS)
    public boolean tieneEvidencia() {
        return evidencia != null && !evidencia.isEmpty();
    }

    public String getEstadoTexto() {
        switch (idEstado) {
            case 1: return "Pendiente";
            case 2: return "Aprobado";
            case 3: return "Rechazado";
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