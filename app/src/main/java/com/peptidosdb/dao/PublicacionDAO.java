package com.peptidosdb.dao;

import com.peptidosdb.model.Publicacion;
import com.peptidosdb.security.Sesion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** DAO de PUBLICACIÓN (entidad compartida) y su relación N:M PEPTIDO_PUBLICACION. */
public class PublicacionDAO {

    private Connection con() {
        return Sesion.actual().getConexion();
    }

    /** Publicaciones asociadas a un péptido. */
    public List<Publicacion> buscarPorPeptido(int peptidoId) throws SQLException {
        String sql = "SELECT p.pmid, p.doi, p.titulo, p.autores, p.anyo " +
                "FROM PUBLICACIÓN p JOIN PEPTIDO_PUBLICACION pp ON p.pmid = pp.publicacion_pmid " +
                "WHERE pp.peptido_id = ? ORDER BY p.anyo DESC NULLS LAST";
        List<Publicacion> lista = new ArrayList<>();
        try (PreparedStatement ps = con().prepareStatement(sql)) {
            ps.setInt(1, peptidoId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Publicacion pub = new Publicacion();
                    pub.setPmid(rs.getString("pmid"));
                    pub.setDoi(rs.getString("doi"));
                    pub.setTitulo(rs.getString("titulo"));
                    pub.setAutores(rs.getString("autores"));
                    int anyo = rs.getInt("anyo");
                    pub.setAnyo(rs.wasNull() ? null : anyo);
                    pub.setExistente(true);
                    lista.add(pub);
                }
            }
        }
        return lista;
    }

    /** Asegura que exista una fila mínima en PUBLICACIÓN para el pmid (no pisa datos ya
     *  cargados por enrich_pubmed.py si la publicación ya existía). */
    public void asegurarExiste(String pmid) throws SQLException {
        String sql = "INSERT INTO PUBLICACIÓN (pmid) VALUES (?) ON CONFLICT (pmid) DO NOTHING";
        try (PreparedStatement ps = con().prepareStatement(sql)) {
            ps.setString(1, pmid);
            ps.executeUpdate();
        }
    }

    public void asociar(int peptidoId, String pmid) throws SQLException {
        String sql = "INSERT INTO PEPTIDO_PUBLICACION (peptido_id, publicacion_pmid) VALUES (?, ?) " +
                "ON CONFLICT DO NOTHING";
        try (PreparedStatement ps = con().prepareStatement(sql)) {
            ps.setInt(1, peptidoId);
            ps.setString(2, pmid);
            ps.executeUpdate();
        }
    }

    /** Quita la asociación péptido-publicación (no borra la publicación, que puede estar
     *  referenciada por otros péptidos). */
    public void desasociar(int peptidoId, String pmid) throws SQLException {
        String sql = "DELETE FROM PEPTIDO_PUBLICACION WHERE peptido_id = ? AND publicacion_pmid = ?";
        try (PreparedStatement ps = con().prepareStatement(sql)) {
            ps.setInt(1, peptidoId);
            ps.setString(2, pmid);
            ps.executeUpdate();
        }
    }
}
