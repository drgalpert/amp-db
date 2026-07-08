package com.peptidosdb.dao;

import com.peptidosdb.model.PeptidoOrganismoRow;
import com.peptidosdb.security.Sesion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/** DAO de PEPTIDO_ORGANISMO: relación N:M entre PÉPTIDO y ORGANISMO_BLANCO, con MIC como atributo. */
public class PeptidoOrganismoDAO {

    private Connection con() {
        return Sesion.actual().getConexion();
    }

    public List<PeptidoOrganismoRow> buscarPorPeptido(int peptidoId) throws SQLException {
        String sql = "SELECT po.organismo_id, ob.nombre_cientifico, po.mic_valor, po.mic_unidad " +
                "FROM PEPTIDO_ORGANISMO po JOIN ORGANISMO_BLANCO ob ON po.organismo_id = ob.id " +
                "WHERE po.peptido_id = ? ORDER BY ob.nombre_cientifico";
        List<PeptidoOrganismoRow> lista = new ArrayList<>();
        try (PreparedStatement ps = con().prepareStatement(sql)) {
            ps.setInt(1, peptidoId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    double micValor = rs.getDouble("mic_valor");
                    lista.add(new PeptidoOrganismoRow(
                            rs.getInt("organismo_id"),
                            rs.getString("nombre_cientifico"),
                            rs.wasNull() ? null : micValor,
                            rs.getString("mic_unidad"),
                            true // existente = ya estaba en la BD
                    ));
                }
            }
        }
        return lista;
    }

    /** Inserta o actualiza la asociación péptido-organismo (upsert por la PK compuesta). */
    public void guardar(int peptidoId, int organismoId, Double micValor, String micUnidad) throws SQLException {
        String sql = "INSERT INTO PEPTIDO_ORGANISMO (peptido_id, organismo_id, mic_valor, mic_unidad) " +
                "VALUES (?, ?, ?, ?) " +
                "ON CONFLICT (peptido_id, organismo_id) DO UPDATE SET " +
                "mic_valor = EXCLUDED.mic_valor, mic_unidad = EXCLUDED.mic_unidad";
        try (PreparedStatement ps = con().prepareStatement(sql)) {
            ps.setInt(1, peptidoId);
            ps.setInt(2, organismoId);
            if (micValor != null) {
                ps.setDouble(3, micValor);
            } else {
                ps.setNull(3, Types.NUMERIC);
            }
            ps.setString(4, micUnidad);
            ps.executeUpdate();
        }
    }

    public void eliminar(int peptidoId, int organismoId) throws SQLException {
        String sql = "DELETE FROM PEPTIDO_ORGANISMO WHERE peptido_id = ? AND organismo_id = ?";
        try (PreparedStatement ps = con().prepareStatement(sql)) {
            ps.setInt(1, peptidoId);
            ps.setInt(2, organismoId);
            ps.executeUpdate();
        }
    }
}
