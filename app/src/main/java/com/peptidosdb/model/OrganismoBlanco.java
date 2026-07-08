package com.peptidosdb.model;

/** Organismo blanco de la actividad antimicrobiana (tabla ORGANISMO_BLANCO). */
public class OrganismoBlanco {

    private Integer id;
    private String nombreCientifico;
    private String gram; // 'positive', 'negative', 'both'
    private String categoria;

    public OrganismoBlanco() {
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

    public String getGram() {
        return gram;
    }

    public void setGram(String gram) {
        this.gram = gram;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    @Override
    public String toString() {
        return nombreCientifico;
    }
}
