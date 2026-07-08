package com.peptidosdb.dao;

import com.peptidosdb.model.UsuarioApp;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Abre conexiones JDBC usando las credenciales del rol de PostgreSQL que
 * corresponda al usuario de la aplicación que inició sesión. Esto asegura
 * que los privilegios (SELECT / INSERT / UPDATE / DELETE) los aplique
 * PostgreSQL mismo, no solo la lógica de la interfaz.
 */
public final class ConexionBD {

    private static final Properties CONFIG = new Properties();

    static {
        try (InputStream in = ConexionBD.class.getClassLoader()
                .getResourceAsStream("config/db.properties")) {
            if (in == null) {
                throw new IllegalStateException("No se encontró config/db.properties en el classpath");
            }
            CONFIG.load(in);
            Class.forName("org.postgresql.Driver");
        } catch (IOException | ClassNotFoundException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private ConexionBD() {
    }

    public static Connection obtenerConexion(UsuarioApp.Rol rol) throws SQLException {
        return conectarConRol(rol.name());
    }

    /** Conexión de solo-login, usada por UsuarioAppDAO antes de conocer el rol de negocio. */
    public static Connection obtenerConexionLogin() throws SQLException {
        return conectarConRol("app_login");
    }

    private static Connection conectarConRol(String nombreRol) throws SQLException {
        String host = CONFIG.getProperty("db.host");
        String port = CONFIG.getProperty("db.port");
        String dbName = CONFIG.getProperty("db.name");
        String user = CONFIG.getProperty("db.role." + nombreRol + ".user");
        String password = CONFIG.getProperty("db.role." + nombreRol + ".password", "");

        String url = "jdbc:postgresql://" + host + ":" + port + "/" + dbName;
        return DriverManager.getConnection(url, user, password);
    }
}
