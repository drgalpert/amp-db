package com.peptidosdb.model;

/** Nombre alternativo de un péptido (tabla NOMBRE_ALTERNATIVO, 1:N respecto a PÉPTIDO). */
public class NombreAlternativo {

    private Integer id; // null => todavía no se ha guardado en la BD
    private String nombre;
    private Integer peptidoId;

    public NombreAlternativo() {
    }

    public NombreAlternativo(Integer id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Integer getPeptidoId() {
        return peptidoId;
    }

    public void setPeptidoId(Integer peptidoId) {
        this.peptidoId = peptidoId;
    }

    @Override
    public String toString() {
        return nombre;
    }
}
