package com.example.relojcontrol.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.relojcontrol.R;
import com.example.relojcontrol.models.Justificacion;

import java.util.List;

public class JustificacionesAdapter extends RecyclerView.Adapter<JustificacionesAdapter.ViewHolder> {

    private List<Justificacion> justificacionesList;

    public JustificacionesAdapter(List<Justificacion> justificacionesList) {
        this.justificacionesList = justificacionesList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_justificacion, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Justificacion justificacion = justificacionesList.get(position);

        holder.tvUsuario.setText(justificacion.getInfoUsuario());
        holder.tvFecha.setText("Fecha: " + justificacion.getFecha());
        holder.tvMotivo.setText("Motivo: " + justificacion.getMotivo());
        holder.tvEstado.setText("Estado: " + justificacion.getEstadoTexto());

        // Color según estado
        int colorEstado = getColorEstado(justificacion.getIdEstado(), holder.itemView);
        holder.tvEstado.setTextColor(colorEstado);

        // Mostrar evidencia si existe
        if (justificacion.tieneEvidencia()) {
            holder.tvEvidencia.setText("Evidencia: Sí");
            holder.tvEvidencia.setVisibility(View.VISIBLE);
        } else {
            holder.tvEvidencia.setVisibility(View.GONE);
        }
    }

    private int getColorEstado(int idEstado, View view) {
        switch (idEstado) {
            case 2: return ContextCompat.getColor(view.getContext(), R.color.success_color);
            case 3: return ContextCompat.getColor(view.getContext(), R.color.error_color);
            case 1:
            default: return ContextCompat.getColor(view.getContext(), R.color.warning_color);
        }
    }

    @Override
    public int getItemCount() {
        return justificacionesList.size();
    }

    public void updateData(List<Justificacion> nuevasJustificaciones) {
        justificacionesList.clear();
        justificacionesList.addAll(nuevasJustificaciones);
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvUsuario, tvFecha, tvMotivo, tvEstado, tvEvidencia;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUsuario = itemView.findViewById(R.id.tv_usuario);
            tvFecha = itemView.findViewById(R.id.tv_fecha);
            tvMotivo = itemView.findViewById(R.id.tv_motivo);
            tvEstado = itemView.findViewById(R.id.tv_estado);
            tvEvidencia = itemView.findViewById(R.id.tv_evidencia);
        }
    }
}