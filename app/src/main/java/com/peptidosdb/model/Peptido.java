package com.peptidosdb.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Representa un registro de la tabla PÉPTIDO.
 */
public class Peptido {

    private Integer id;
    private String secuencia;
    private String nombrePrincipal;
    private String nombresAlternativos;  // ej: "AMP1, Defensin-2, ..."
    private Short longitud;
    private BigDecimal pesoMolecular;
    private Short cargaNeta;
    private BigDecimal hidrofobicidad;
    private Boolean esNatural;
    private String estadoVerificacion;
    private String uniprotId;
    private String drampId;
    private Integer organismoFuenteId;
    private String organismoFuenteNombre; // solo lectura, para mostrar en tablas/formularios
    private Boolean activo = Boolean.TRUE;
    private LocalDateTime fechaIngesta;
    private LocalDateTime ultimaActualizacion;
    private String organismosResumen;     // ej: "E. coli (0.5 µg/mL), S. aureus"
    private String actividadesResumen;    // ej: "Antiviral: HIV; Anticancer: HeLa"

    public Peptido() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getSecuencia() {
        return secuencia;
    }

    public void setSecuencia(String secuencia) {
        this.secuencia = secuencia;
    }

    public String getNombrePrincipal() {
        return nombrePrincipal;
    }

    public void setNombrePrincipal(String nombrePrincipal) {
        this.nombrePrincipal = nombrePrincipal;
    }
    
    public String getNombresAlternativos() {
        return nombresAlternativos;
    }

    public void setNombresAlternativos(String nombresAlternativos) {
        this.nombresAlternativos = nombresAlternativos;
    }

    public Short getLongitud() {
        return longitud;
    }

    public void setLongitud(Short longitud) {
        this.longitud = longitud;
    }

    public BigDecimal getPesoMolecular() {
        return pesoMolecular;
    }

    public void setPesoMolecular(BigDecimal pesoMolecular) {
        this.pesoMolecular = pesoMolecular;
    }

    public Short getCargaNeta() {
        return cargaNeta;
    }

    public void setCargaNeta(Short cargaNeta) {
        this.cargaNeta = cargaNeta;
    }

    public BigDecimal getHidrofobicidad() {
        return hidrofobicidad;
    }

    public void setHidrofobicidad(BigDecimal hidrofobicidad) {
        this.hidrofobicidad = hidrofobicidad;
    }

    public Boolean getEsNatural() {
        return esNatural;
    }

    public void setEsNatural(Boolean esNatural) {
        this.esNatural = esNatural;
    }

    public String getEstadoVerificacion() {
        return estadoVerificacion;
    }

    public void setEstadoVerificacion(String estadoVerificacion) {
        this.estadoVerificacion = estadoVerificacion;
    }

    public String getUniprotId() {
        return uniprotId;
    }

    public void setUniprotId(String uniprotId) {
        this.uniprotId = uniprotId;
    }

    public String getDrampId() {
        return drampId;
    }

    public void setDrampId(String drampId) {
        this.drampId = drampId;
    }

    public Integer getOrganismoFuenteId() {
        return organismoFuenteId;
    }

    public void setOrganismoFuenteId(Integer organismoFuenteId) {
        this.organismoFuenteId = organismoFuenteId;
    }

    public String getOrganismoFuenteNombre() {
        return organismoFuenteNombre;
    }

    public void setOrganismoFuenteNombre(String organismoFuenteNombre) {
        this.organismoFuenteNombre = organismoFuenteNombre;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    public LocalDateTime getFechaIngesta() {
        return fechaIngesta;
    }

    public void setFechaIngesta(LocalDateTime fechaIngesta) {
        this.fechaIngesta = fechaIngesta;
    }

    public LocalDateTime getUltimaActualizacion() {
        return ultimaActualizacion;
    }

    public void setUltimaActualizacion(LocalDateTime ultimaActualizacion) {
        this.ultimaActualizacion = ultimaActualizacion;
    }

    public String getOrganismosResumen() {
        return organismosResumen;
    }

    public void setOrganismosResumen(String organismosResumen) {
        this.organismosResumen = organismosResumen;
    }

    public String getActividadesResumen() {
        return actividadesResumen;
    }

    public void setActividadesResumen(String actividadesResumen) {
        this.actividadesResumen = actividadesResumen;
    }
    
    @Override
    public String toString() {
        return nombrePrincipal + " (" + secuencia + ")";
    }
}
