package com.peptidosdb.service;

import com.peptidosdb.dao.UsuarioAppDAO;
import com.peptidosdb.model.UsuarioApp;
import com.peptidosdb.security.Sesion;

import java.sql.SQLException;

public class AuthService {

    private final UsuarioAppDAO usuarioAppDAO = new UsuarioAppDAO();

    /**
     * Autentica al usuario y, si es válido, abre la sesión con la conexión
     * JDBC correspondiente a su rol de negocio.
     *
     * @return el usuario autenticado, o null si las credenciales son inválidas.
     */
    public UsuarioApp iniciarSesion(String nombreUsuario, String password) throws SQLException {
        UsuarioApp usuario = usuarioAppDAO.autenticar(nombreUsuario, password);
        if (usuario == null) {
            return null;
        }
        Sesion.iniciar(usuario);
        return usuario;
    }

    public void cerrarSesion() {
        Sesion.cerrar();
    }
}
