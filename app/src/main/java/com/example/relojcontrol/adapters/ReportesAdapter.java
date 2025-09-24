package com.example.relojcontrol.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.relojcontrol.R;
import com.example.relojcontrol.models.Reporte;

import java.util.List;

public class ReportesAdapter extends RecyclerView.Adapter<ReportesAdapter.ViewHolder> {

    private List<Reporte> reportesList;

    public ReportesAdapter(List<Reporte> reportesList) {
        this.reportesList = reportesList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_reporte, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Reporte reporte = reportesList.get(position);

        holder.tvTipo.setText("Tipo: " + reporte.getTipoTexto());
        holder.tvFecha.setText("Fecha: " + reporte.getFecha());
        holder.tvUsuario.setText("Usuario ID: " + reporte.getIdUsuario());
    }

    @Override
    public int getItemCount() {
        return reportesList.size();
    }

    public void updateData(List<Reporte> nuevosReportes) {
        reportesList.clear();
        reportesList.addAll(nuevosReportes);
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTipo, tvFecha, tvUsuario;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTipo = itemView.findViewById(R.id.tv_tipo);
            tvFecha = itemView.findViewById(R.id.tv_fecha);
            tvUsuario = itemView.findViewById(R.id.tv_usuario);
        }
    }
}