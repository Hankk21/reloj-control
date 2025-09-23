package com.example.relojcontrol.adapters;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.relojcontrol.R;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ArchivoAdapter extends RecyclerView.Adapter<ArchivoAdapter.ArchivoViewHolder> {
    private List<Uri> archivos;
    private OnArchivoClickListener listener;

    public interface OnArchivoClickListener {
        void onArchivoClick(Uri archivo);
        void onEliminarArchivoClick(int position);
    }

    public ArchivoAdapter(List<Uri> archivos, OnArchivoClickListener listener) {
        this.archivos = archivos;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ArchivoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_archivo_adjunto, parent, false);
        return new ArchivoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ArchivoViewHolder holder, int position) {
        Uri archivo = archivos.get(position);
        holder.bind(archivo, position, listener);
    }

    @Override
    public int getItemCount() {
        return archivos.size();
    }

    static class ArchivoViewHolder extends RecyclerView.ViewHolder {
        private TextView tvNombreArchivo, tvTamaño, tvFechaSubida;
        private ImageButton ivEliminar;

        public ArchivoViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombreArchivo = itemView.findViewById(R.id.tv_nombre_archivo);
            tvTamaño = itemView.findViewById(R.id.tv_tamaño);
            tvFechaSubida = itemView.findViewById(R.id.tv_fecha_subida);
            ivEliminar = itemView.findViewById(R.id.iv_eliminar);
        }

        public void bind(final Uri archivo, final int position, final OnArchivoClickListener listener) {
            // Información del archivo
            String nombreArchivo = obtenerNombreArchivo(archivo);
            String tamaño = obtenerTamañoArchivo(archivo);
            String fechaSubida = obtenerFechaSubida();

            tvNombreArchivo.setText(nombreArchivo);
            tvTamaño.setText(tamaño);
            tvFechaSubida.setText(fechaSubida);

            // Click en el item para ver/previsualizar el archivo
            itemView.setOnClickListener(v -> listener.onArchivoClick(archivo));

            // Click en eliminar
            ivEliminar.setOnClickListener(v -> listener.onEliminarArchivoClick(position));
        }

        private String obtenerNombreArchivo(Uri archivo) {
            String path = archivo.getPath();
            if (path != null) {
                return path.substring(path.lastIndexOf("/") + 1);
            }
            return "documento.pdf";
        }

        private String obtenerTamañoArchivo(Uri archivo) {
            try {
                File file = new File(archivo.getPath());
                if (file.exists()) {
                    long size = file.length();
                    if (size < 1024) {
                        return size + " B";
                    } else if (size < 1024 * 1024) {
                        return String.format("%.1f KB", size / 1024.0);
                    } else {
                        return String.format("%.1f MB", size / (1024.0 * 1024.0));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return "N/A";
        }

        private String obtenerFechaSubida() {
            // Fecha actual como fecha de subida
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            return sdf.format(new Date());
        }
    }
}