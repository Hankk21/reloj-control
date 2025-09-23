package com.example.relojcontrol.models;

public class Justificacion {
    private int idJustificacion;
    private int idUsuario;
    private String fecha;
    private String motivo;
    private String evidencia;
    private int idEstado;

    public Justificacion() {}

    public Justificacion(int idJustificacion, int idUsuario, String fecha, String motivo,
                         String evidencia, int idEstado) {
        this.idJustificacion = idJustificacion;
        this.idUsuario = idUsuario;
        this.fecha = fecha;
        this.motivo = motivo;
        this.evidencia = evidencia;
        this.idEstado = idEstado;
    }

    // Getters y Setters
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
}
