package com.peptidosdb.model;

/**
 * Cuenta de la aplicación (tabla USUARIO_APP). El campo rolBd indica con qué
 * rol de PostgreSQL debe abrirse la conexión JDBC para este usuario.
 */
public class UsuarioApp {

    public enum Rol {
        administrador, curador, consultor
    }

    private Integer id;
    private String nombreUsuario;
    private String nombreCompleto;
    private Rol rolBd;
    private boolean activo;

    public UsuarioApp() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public void setNombreUsuario(String nombreUsuario) {
        this.nombreUsuario = nombreUsuario;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public void setNombreCompleto(String nombreCompleto) {
        this.nombreCompleto = nombreCompleto;
    }

    public Rol getRolBd() {
        return rolBd;
    }

    public void setRolBd(Rol rolBd) {
        this.rolBd = rolBd;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public boolean puedeEditar() {
        return rolBd == Rol.administrador || rolBd == Rol.curador;
    }
}
