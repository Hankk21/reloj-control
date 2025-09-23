package com.example.relojcontrol.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.relojcontrol.R;
import com.example.relojcontrol.models.Justificacion;
import com.example.relojcontrol.models.Licencia;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistorialAdapter extends RecyclerView.Adapter<HistorialAdapter.HistorialViewHolder> {
    private List<Object> historial;
    private OnHistorialClickListener listener;

    public interface OnHistorialClickListener {
        void onHistorialClick(Object item);
        void onDescargarArchivoClick(Object item);
    }

    public HistorialAdapter(List<Object> historial, OnHistorialClickListener listener) {
        this.historial = historial;
        this.listener = listener;
    }

    @NonNull
    @Override
    public HistorialViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_historial, parent, false);
        return new HistorialViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistorialViewHolder holder, int position) {
        Object item = historial.get(position);
        holder.bind(item, listener);
    }

    @Override
    public int getItemCount() {
        return historial.size();
    }

    public void updateData(List<Object> nuevoHistorial) {
        this.historial = nuevoHistorial;
        notifyDataSetChanged();
    }

    static class HistorialViewHolder extends RecyclerView.ViewHolder {
        private TextView tvFecha, tvEstado, tvMotivo, tvDetalle, tvTipo;
        private TextView tvFechaInicio, tvFechaFin;
        private LinearLayout layoutFechasLicencia, layoutDescargar;

        public HistorialViewHolder(@NonNull View itemView) {
            super(itemView);

            // Inicializar todas las vistas del layout
            tvFecha = itemView.findViewById(R.id.tv_fecha);
            tvEstado = itemView.findViewById(R.id.tv_estado);
            tvMotivo = itemView.findViewById(R.id.tv_motivo);
            tvDetalle = itemView.findViewById(R.id.tv_detalle);
            tvTipo = itemView.findViewById(R.id.tv_tipo);
            tvFechaInicio = itemView.findViewById(R.id.tv_fecha_inicio);
            tvFechaFin = itemView.findViewById(R.id.tv_fecha_fin);
            layoutFechasLicencia = itemView.findViewById(R.id.layout_fechas_licencia);
            layoutDescargar = itemView.findViewById(R.id.layout_descargar);
        }

        public void bind(final Object item, final OnHistorialClickListener listener) {
            if (item instanceof Justificacion) {
                bindJustificacion((Justificacion) item, listener);
            } else if (item instanceof Licencia) {
                bindLicencia((Licencia) item, listener);
            }

            itemView.setOnClickListener(v -> listener.onHistorialClick(item));
        }

        private void bindJustificacion(Justificacion justificacion, OnHistorialClickListener listener) {
            // Ocultar layout de licencias
            layoutFechasLicencia.setVisibility(View.GONE);

            // Configurar datos de justificación
            tvFecha.setText(formatearFecha(justificacion.getFecha()));
            tvMotivo.setText(justificacion.getMotivo());
            tvDetalle.setText(justificacion.getMotivo()); // Usar motivo como detalle

            // Configurar tipo
            tvTipo.setText("Justificación");

            // Configurar estado
            configurarEstado(justificacion.getIdEstado());

            // Configurar botón de descarga
            if (justificacion.getEvidencia() != null && !justificacion.getEvidencia().isEmpty()) {
                layoutDescargar.setVisibility(View.VISIBLE);
                layoutDescargar.setOnClickListener(v -> listener.onDescargarArchivoClick(justificacion));
            } else {
                layoutDescargar.setVisibility(View.GONE);
            }
        }

        private void bindLicencia(Licencia licencia, OnHistorialClickListener listener) {
            // Mostrar layout de licencias
            layoutFechasLicencia.setVisibility(View.VISIBLE);

            // Configurar datos de licencia
            tvFecha.setText(formatearFecha(licencia.getFechaInicio()));
            tvMotivo.setText("Licencia Médica");
            tvDetalle.setText("Licencia por enfermedad");

            // Configurar tipo
            tvTipo.setText("Licencia");

            // Configurar fechas de licencia
            tvFechaInicio.setText(formatearFecha(licencia.getFechaInicio()));
            tvFechaFin.setText(formatearFecha(licencia.getFechaFin()));

            // Configurar estado
            configurarEstado(licencia.getIdEstado());

            // Configurar botón de descarga
            if (licencia.getDocumento() != null && !licencia.getDocumento().isEmpty()) {
                layoutDescargar.setVisibility(View.VISIBLE);
                layoutDescargar.setOnClickListener(v -> listener.onDescargarArchivoClick(licencia));
            } else {
                layoutDescargar.setVisibility(View.GONE);
            }
        }

        private void configurarEstado(int idEstado) {
            switch (idEstado) {
                case 1: // Pendiente
                    tvEstado.setText("Pendiente");
                    tvEstado.setBackgroundResource(R.drawable.status_pendiente);
                    break;
                case 2: // Aceptado
                    tvEstado.setText("Aceptado");
                    tvEstado.setBackgroundResource(R.drawable.status_aceptado);
                    break;
                case 3: // Rechazado
                    tvEstado.setText("Rechazado");
                    tvEstado.setBackgroundResource(R.drawable.status_rechazado);
                    break;
                default:
                    tvEstado.setText("Desconocido");
                    tvEstado.setBackgroundResource(R.drawable.status_pendiente);
            }
        }

        private String formatearFecha(String fecha) {
            try {
                SimpleDateFormat formatoEntrada = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                SimpleDateFormat formatoSalida = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                Date date = formatoEntrada.parse(fecha);
                return formatoSalida.format(date);
            } catch (ParseException e) {
                return fecha;
            }
        }
    }
}