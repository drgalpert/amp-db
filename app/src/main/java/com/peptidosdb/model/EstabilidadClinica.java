package com.peptidosdb.model;

import java.math.BigDecimal;

/** Estabilidad y estado clínico del péptido (tabla ESTABILIDAD_CLINICA, 1:1 con PÉPTIDO). */
public class EstabilidadClinica {

    private Integer id;
    private BigDecimal estabilidadSuero;
    private String estadoClinico;
    private String toxicidadInVivo;
    private Integer peptidoId;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public BigDecimal getEstabilidadSuero() {
        return estabilidadSuero;
    }

    public void setEstabilidadSuero(BigDecimal estabilidadSuero) {
        this.estabilidadSuero = estabilidadSuero;
    }

    public String getEstadoClinico() {
        return estadoClinico;
    }

    public void setEstadoClinico(String estadoClinico) {
        this.estadoClinico = estadoClinico;
    }

    public String getToxicidadInVivo() {
        return toxicidadInVivo;
    }

    public void setToxicidadInVivo(String toxicidadInVivo) {
        this.toxicidadInVivo = toxicidadInVivo;
    }

    public Integer getPeptidoId() {
        return peptidoId;
    }

    public void setPeptidoId(Integer peptidoId) {
        this.peptidoId = peptidoId;
    }
}
