package com.peptidosdb.model;

/** Modificación postraduccional (tabla MODIFICACION_POSTRADUCCIONAL, catálogo N:M con PÉPTIDO). */
public class ModificacionPostraduccional {

    private Integer id; // null => todavía no persistida (par tipo/posición nuevo del buffer)
    private String tipo;
    private String posicion;

    public ModificacionPostraduccional() {
    }

    public ModificacionPostraduccional(Integer id, String tipo, String posicion) {
        this.id = id;
        this.tipo = tipo;
        this.posicion = posicion;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getPosicion() {
        return posicion;
    }

    public void setPosicion(String posicion) {
        this.posicion = posicion;
    }

    @Override
    public String toString() {
        return tipo + (posicion != null && !posicion.isBlank() ? " — " + posicion : "");
    }
}
