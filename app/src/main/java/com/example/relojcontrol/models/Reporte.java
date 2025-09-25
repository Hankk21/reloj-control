package com.example.relojcontrol.models;

public class Reporte {
    private int idReporte;
    private int tipoReporte;
    private String fecha;
    private int idUsuario;

    public Reporte() {}

    // Getters y Setters (tus originales)
    public int getIdReporte() { return idReporte; }
    public void setIdReporte(int idReporte) { this.idReporte = idReporte; }

    public int getTipoReporte() { return tipoReporte; }
    public void setTipoReporte(int tipoReporte) { this.tipoReporte = tipoReporte; }

    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }

    public int getIdUsuario() { return idUsuario; }
    public void setIdUsuario(int idUsuario) { this.idUsuario = idUsuario; }

    // MÉTODO UTILITARIO (NUEVO)
    public String getTipoTexto() {
        switch (tipoReporte) {
            case 1: return "Asistencias";
            case 2: return "Horas Trabajadas";
            case 3: return "Atrasos";
            case 4: return "Justificaciones";
            case 5: return "Licencias";
            default: return "Reporte " + tipoReporte;
        }
    }
}
