package com.peptidosdb.model;

/** Estructura tridimensional del péptido (tabla ESTRUCTURA, 1:1 con PÉPTIDO). */
public class Estructura {

    private Integer id;
    private String tipoEstructura;
    private String pdbId;
    private String metodo;
    private String ciclizacion;
    private Integer peptidoId;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTipoEstructura() {
        return tipoEstructura;
    }

    public void setTipoEstructura(String tipoEstructura) {
        this.tipoEstructura = tipoEstructura;
    }

    public String getPdbId() {
        return pdbId;
    }

    public void setPdbId(String pdbId) {
        this.pdbId = pdbId;
    }

    public String getMetodo() {
        return metodo;
    }

    public void setMetodo(String metodo) {
        this.metodo = metodo;
    }

    public String getCiclizacion() {
        return ciclizacion;
    }

    public void setCiclizacion(String ciclizacion) {
        this.ciclizacion = ciclizacion;
    }

    public Integer getPeptidoId() {
        return peptidoId;
    }

    public void setPeptidoId(Integer peptidoId) {
        this.peptidoId = peptidoId;
    }
}
