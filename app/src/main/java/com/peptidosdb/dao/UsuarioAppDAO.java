package com.peptidosdb.dao;

import com.peptidosdb.model.UsuarioApp;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HexFormat;

/**
 * Autenticación contra la tabla USUARIO_APP. Esta consulta se hace con una
 * conexión "de servicio" (rol consultor, que solo tiene SELECT) ya que aún
 * no existe una sesión: el usuario todavía no ha iniciado sesión.
 *
 * NOTA: para simplificar el ejemplo se usa SHA-256. En una aplicación real
 * se recomienda BCrypt o Argon2, que incluyen "salt" y son resistentes a
 * ataques de fuerza bruta con GPU.
 */
public class UsuarioAppDAO {

    public UsuarioApp autenticar(String nombreUsuario, String password) throws SQLException {
        String sql = "SELECT id, nombre_usuario, nombre_completo, password_hash, rol_bd, activo " +
                "FROM USUARIO_APP WHERE nombre_usuario = ? AND activo = TRUE";

        try (Connection con = ConexionBD.obtenerConexionLogin();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, nombreUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null; // usuario no existe o está inactivo
                }
                String hashAlmacenado = rs.getString("password_hash");
                if (!hashAlmacenado.equals(hashSha256(password))) {
                    return null; // contraseña incorrecta
                }

                UsuarioApp usuario = new UsuarioApp();
                usuario.setId(rs.getInt("id"));
                usuario.setNombreUsuario(rs.getString("nombre_usuario"));
                usuario.setNombreCompleto(rs.getString("nombre_completo"));
                usuario.setRolBd(UsuarioApp.Rol.valueOf(rs.getString("rol_bd")));
                usuario.setActivo(rs.getBoolean("activo"));
                return usuario;
            }
        }
    }

    public static String hashSha256(String texto) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(texto.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
