package com.peptidosdb.model;

/** Fila de la tabla de fuentes de un péptido (envuelve PEPTIDO_FUENTE + el nombre de FUENTE_DATOS). */
public class FuentePeptidoRow {

    private String nombreFuente;
    private Short prioridad;
    private boolean existente;

    public FuentePeptidoRow() {
    }

    public FuentePeptidoRow(String nombreFuente, Short prioridad, boolean existente) {
        this.nombreFuente = nombreFuente;
        this.prioridad = prioridad;
        this.existente = existente;
    }

    public String getNombreFuente() {
        return nombreFuente;
    }

    public void setNombreFuente(String nombreFuente) {
        this.nombreFuente = nombreFuente;
    }

    public Short getPrioridad() {
        return prioridad;
    }

    public void setPrioridad(Short prioridad) {
        this.prioridad = prioridad;
    }

    public boolean isExistente() {
        return existente;
    }

    public void setExistente(boolean existente) {
        this.existente = existente;
    }
}
