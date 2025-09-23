package com.example.relojcontrol.models;

public class Reporte {
    private int idReporte;
    private int tipoReporte;
    private String fecha;
    private int idUsuario;

    public Reporte() {}

    public Reporte(int idReporte, int tipoReporte, String fecha, int idUsuario) {
        this.idReporte = idReporte;
        this.tipoReporte = tipoReporte;
        this.fecha = fecha;
        this.idUsuario = idUsuario;
    }

    // Getters y Setters
    public int getIdReporte() { return idReporte; }
    public void setIdReporte(int idReporte) { this.idReporte = idReporte; }

    public int getTipoReporte() { return tipoReporte; }
    public void setTipoReporte(int tipoReporte) { this.tipoReporte = tipoReporte; }

    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }

    public int getIdUsuario() { return idUsuario; }
    public void setIdUsuario(int idUsuario) { this.idUsuario = idUsuario; }
}
