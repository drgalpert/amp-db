package com.peptidosdb.model;

/** Artículo científico (tabla PUBLICACIÓN), compartido entre varios péptidos. */
public class Publicacion {

    private String pmid;
    private String doi;
    private String titulo;
    private String autores;
    private Integer anyo;
    private boolean existente; // true = ya estaba asociada a este péptido al cargar el formulario

    public Publicacion() {
    }

    public Publicacion(String pmid, String titulo, boolean existente) {
        this.pmid = pmid;
        this.titulo = titulo;
        this.existente = existente;
    }

    public String getPmid() {
        return pmid;
    }

    public void setPmid(String pmid) {
        this.pmid = pmid;
    }

    public String getDoi() {
        return doi;
    }

    public void setDoi(String doi) {
        this.doi = doi;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getAutores() {
        return autores;
    }

    public void setAutores(String autores) {
        this.autores = autores;
    }

    public Integer getAnyo() {
        return anyo;
    }

    public void setAnyo(Integer anyo) {
        this.anyo = anyo;
    }

    public boolean isExistente() {
        return existente;
    }

    public void setExistente(boolean existente) {
        this.existente = existente;
    }
}
