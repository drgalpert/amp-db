package com.peptidosdb.model;

/**
 * Fila de la tabla de organismos blanco de un péptido, para uso exclusivo de
 * la interfaz (no es una tabla propia: envuelve PEPTIDO_ORGANISMO + el
 * nombre del organismo, que viene de un JOIN con ORGANISMO_BLANCO).
 */
public class PeptidoOrganismoRow {

    private Integer organismoId;
    private String nombreOrganismo;
    private Double micValor;
    private String micUnidad;
    private boolean existente; // true = ya estaba guardado en la BD al cargar el formulario

    public PeptidoOrganismoRow() {
    }

    public PeptidoOrganismoRow(Integer organismoId, String nombreOrganismo,
                               Double micValor, String micUnidad, boolean existente) {
        this.organismoId = organismoId;
        this.nombreOrganismo = nombreOrganismo;
        this.micValor = micValor;
        this.micUnidad = micUnidad;
        this.existente = existente;
    }

    public Integer getOrganismoId() {
        return organismoId;
    }

    public void setOrganismoId(Integer organismoId) {
        this.organismoId = organismoId;
    }

    public String getNombreOrganismo() {
        return nombreOrganismo;
    }

    public void setNombreOrganismo(String nombreOrganismo) {
        this.nombreOrganismo = nombreOrganismo;
    }

    public Double getMicValor() {
        return micValor;
    }

    public void setMicValor(Double micValor) {
        this.micValor = micValor;
    }

    public String getMicUnidad() {
        return micUnidad;
    }

    public void setMicUnidad(String micUnidad) {
        this.micUnidad = micUnidad;
    }

    public boolean isExistente() {
        return existente;
    }

    public void setExistente(boolean existente) {
        this.existente = existente;
    }
}
