package com.example.relojcontrol.models;

public class Asistencia {
    private int idAsistencia;
    private int idUsuario;
    private String fecha;
    private String hora;
    private int idTipoAccion;
    private int idEstado;

    public Asistencia() {}

    public Asistencia(int idAsistencia, int idUsuario, String fecha, String hora, int idTipoAccion, int idEstado) {
        this.idAsistencia = idAsistencia;
        this.idUsuario = idUsuario;
        this.fecha = fecha;
        this.hora = hora;
        this.idTipoAccion = idTipoAccion;
        this.idEstado = idEstado;
    }

    // Getters y Setters
    public int getIdAsistencia() { return idAsistencia; }
    public void setIdAsistencia(int idAsistencia) { this.idAsistencia = idAsistencia; }

    public int getIdUsuario() { return idUsuario; }
    public void setIdUsuario(int idUsuario) { this.idUsuario = idUsuario; }

    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }

    public String getHora() { return hora; }
    public void setHora(String hora) { this.hora = hora; }

    public int getIdTipoAccion() { return idTipoAccion; }
    public void setIdTipoAccion(int idTipoAccion) { this.idTipoAccion = idTipoAccion; }

    public int getIdEstado() { return idEstado; }
    public void setIdEstado(int idEstado) { this.idEstado = idEstado; }
}
