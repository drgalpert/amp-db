package com.peptidosdb.model;

/** Organismo productor del péptido (tabla FUENTE_ORGANISMO). */
public class FuenteOrganismo {

    private Integer id;
    private String nombreCientifico;
    private String reino;

    public FuenteOrganismo() {
    }

    public FuenteOrganismo(Integer id, String nombreCientifico, String reino) {
        this.id = id;
        this.nombreCientifico = nombreCientifico;
        this.reino = reino;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNombreCientifico() {
        return nombreCientifico;
    }

    public void setNombreCientifico(String nombreCientifico) {
        this.nombreCientifico = nombreCientifico;
    }

    public String getReino() {
        return reino;
    }

    public void setReino(String reino) {
        this.reino = reino;
    }

    @Override
    public String toString() {
        return nombreCientifico;
    }
}
