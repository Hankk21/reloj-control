package com.example.relojcontrol.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.relojcontrol.R;
import com.example.relojcontrol.models.Justificacion;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class JustificacionesAdapter extends RecyclerView.Adapter<JustificacionesAdapter.ViewHolder> {

    private List<Justificacion> justificacionesList;
    private Context context;

    public JustificacionesAdapter(List<Justificacion> justificacionesList, Context context) {
        this.justificacionesList = justificacionesList;
        this.context = context;
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

        //motivo
        holder.tvMotivo.setText(justificacion.getMotivo());

        //descripcion
        if (justificacion.getDescripcion() != null && !justificacion.getDescripcion().isEmpty()) {

        }
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
            holder.tvEvide// Descripción
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

        public static class ViewHolder extends RecyclerView.ViewHolder {
            MaterialCardView cardView;
            TextView tvMotivo;
            TextView tvDescripcion;
            TextView tvFechaJustificar;
            TextView tvFechaCreacion;
            TextView tvEstado;
            TextView tvDocumento;

            public ViewHolder(@NonNull View itemView) {
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