package com.peptidosdb.model;

/**
 * Fila combinada de actividad biológica adicional (antiviral, antifúngica o
 * anticáncer). Envuelve las tres tablas de catálogo + sus tres relaciones N:M,
 * ya que las tres comparten la misma forma "tipo + nombre identificador".
 */
public class ActividadRow {

    public enum Tipo { ANTIVIRAL, ANTIFUNGICA, ANTICANCER }

    private Integer id; // id en ACTIVIDAD_ANTIVIRAL / ANTIFUNGICA / ANTICANCER, según tipo
    private Tipo tipo;
    private String nombre; // nombre_virus / nombre_hongo / linea_celular
    private boolean existente;

    public ActividadRow() {
    }

    public ActividadRow(Integer id, Tipo tipo, String nombre, boolean existente) {
        this.id = id;
        this.tipo = tipo;
        this.nombre = nombre;
        this.existente = existente;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Tipo getTipo() {
        return tipo;
    }

    public void setTipo(Tipo tipo) {
        this.tipo = tipo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public boolean isExistente() {
        return existente;
    }

    public void setExistente(boolean existente) {
        this.existente = existente;
    }
}
