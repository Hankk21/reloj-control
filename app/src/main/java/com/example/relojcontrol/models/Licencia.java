package com.example.relojcontrol.models;

public class Licencia {
    private int idLicencia;
    private int idUsuario;
    private String fechaInicio;
    private String fechaFin;
    private String documento;
    private int idEstado;

    public Licencia() {}

    public Licencia(int idLicencia, int idUsuario, String fechaInicio, String fechaFin,
                    String documento, int idEstado) {
        this.idLicencia = idLicencia;
        this.idUsuario = idUsuario;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.documento = documento;
        this.idEstado = idEstado;
    }

    // Getters y Setters
    public int getIdLicencia() { return idLicencia; }
    public void setIdLicencia(int idLicencia) { this.idLicencia = idLicencia; }

    public int getIdUsuario() { return idUsuario; }
    public void setIdUsuario(int idUsuario) { this.idUsuario = idUsuario; }

    public String getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(String fechaInicio) { this.fechaInicio = fechaInicio; }

    public String getFechaFin() { return fechaFin; }
    public void setFechaFin(String fechaFin) { this.fechaFin = fechaFin; }

    public String getDocumento() { return documento; }
    public void setDocumento(String documento) { this.documento = documento; }

    public int getIdEstado() { return idEstado; }
    public void setIdEstado(int idEstado) { this.idEstado = idEstado; }
}
