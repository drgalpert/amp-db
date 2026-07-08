package com.peptidosdb.security;

import com.peptidosdb.dao.ConexionBD;
import com.peptidosdb.model.UsuarioApp;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Mantiene el usuario autenticado y una única conexión JDBC abierta con el
 * rol de PostgreSQL correspondiente, mientras dura la sesión de la app.
 * Es un singleton simple: en esta aplicación de escritorio solo hay un
 * usuario conectado a la vez por instancia de la app.
 */
public final class Sesion {

    private static Sesion instancia;

    private final UsuarioApp usuario;
    private final Connection conexion;

    private Sesion(UsuarioApp usuario, Connection conexion) {
        this.usuario = usuario;
        this.conexion = conexion;
    }

    public static void iniciar(UsuarioApp usuario) throws SQLException {
        Connection conexion = ConexionBD.obtenerConexion(usuario.getRolBd());
        instancia = new Sesion(usuario, conexion);
    }

    public static Sesion actual() {
        if (instancia == null) {
            throw new IllegalStateException("No hay ninguna sesión iniciada");
        }
        return instancia;
    }

    public static void cerrar() {
        if (instancia != null) {
            try {
                instancia.conexion.close();
            } catch (SQLException ignored) {
                // conexión ya cerrada o inválida
            }
            instancia = null;
        }
    }

    public UsuarioApp getUsuario() {
        return usuario;
    }

    public Connection getConexion() {
        return conexion;
    }

    public boolean puedeEditar() {
        return usuario.puedeEditar();
    }
}
