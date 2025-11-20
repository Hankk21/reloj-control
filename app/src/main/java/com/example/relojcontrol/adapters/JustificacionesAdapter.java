package com.example.relojcontrol.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.relojcontrol.R;
import com.example.relojcontrol.models.Justificacion;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class JustificacionesAdapter extends RecyclerView.Adapter<JustificacionesAdapter.ViewHolder> {

    private List<Justificacion> justificaciones;
    private Context context;

    public JustificacionesAdapter(List<Justificacion> justificaciones, Context context) {
        this.justificaciones = justificaciones;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_justificacion_historial, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Justificacion justificacion = justificaciones.get(position);

        // Motivo
        holder.tvMotivo.setText(justificacion.getMotivo());

        // Descripción
        if (justificacion.getDescripcion() != null && !justificacion.getDescripcion().isEmpty()) {
            holder.tvDescripcion.setText(justificacion.getDescripcion());
            holder.tvDescripcion.setVisibility(View.VISIBLE);
        } else {
            holder.tvDescripcion.setVisibility(View.GONE);
        }

        // Fecha a justificar
        holder.tvFechaJustificar.setText("Fecha: " + justificacion.getFechaJustificar());

        // Fecha de creación
        if (justificacion.getFechaCreacion() != null) {
            holder.tvFechaCreacion.setText("Solicitado: " + justificacion.getFechaCreacion());
        }

        // Estado
        String estadoTexto = justificacion.getEstadoTexto();
        holder.tvEstado.setText(estadoTexto);

        // Color según estado
        switch (justificacion.getIdEstado()) {
            case 1: // Registrado
                holder.tvEstado.setBackgroundResource(R.drawable.bg_estado_registrado);
                break;
            case 2: // Pendiente
                holder.tvEstado.setBackgroundResource(R.drawable.bg_estado_pendiente);
                break;
            case 3: // Aprobado
                holder.tvEstado.setBackgroundResource(R.drawable.bg_estado_aprobado);
                break;
            case 4: // Rechazado
                holder.tvEstado.setBackgroundResource(R.drawable.bg_estado_rechazado);
                break;
        }

        // Documento adjunto
        if (justificacion.tieneDocumento()) {
            holder.tvDocumento.setVisibility(View.VISIBLE);
        } else {
            holder.tvDocumento.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return justificaciones.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardView;
        TextView tvMotivo;
        TextView tvDescripcion;
        TextView tvFechaJustificar;
        TextView tvFechaCreacion;
        TextView tvEstado;
        TextView tvDocumento;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            tvMotivo = itemView.findViewById(R.id.tvMotivo);
            tvDescripcion = itemView.findViewById(R.id.tvDescripcion);
            tvFechaJustificar = itemView.findViewById(R.id.tvFechaJustificar);
            tvFechaCreacion = itemView.findViewById(R.id.tvFechaCreacion);
            tvEstado = itemView.findViewById(R.id.tvEstado);
            tvDocumento = itemView.findViewById(R.id.tvDocumento);
        }
    }
}
