package com.peptidosdb.dao;

import com.peptidosdb.model.EstabilidadClinica;
import com.peptidosdb.security.Sesion;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/** DAO de ESTABILIDAD_CLINICA: relación 1:1 con PÉPTIDO (peptido_id es UNIQUE). */
public class EstabilidadClinicaDAO {

    private Connection con() {
        return Sesion.actual().getConexion();
    }

    public EstabilidadClinica buscarPorPeptido(int peptidoId) throws SQLException {
        String sql = "SELECT * FROM ESTABILIDAD_CLINICA WHERE peptido_id = ?";
        try (PreparedStatement ps = con().prepareStatement(sql)) {
            ps.setInt(1, peptidoId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                EstabilidadClinica ec = new EstabilidadClinica();
                ec.setId(rs.getInt("id"));
                ec.setEstabilidadSuero(rs.getBigDecimal("estabilidad_suero"));
                ec.setEstadoClinico(rs.getString("estado_clinico"));
                ec.setToxicidadInVivo(rs.getString("toxicidad_in_vivo"));
                ec.setPeptidoId(rs.getInt("peptido_id"));
                return ec;
            }
        }
    }

    public void guardar(EstabilidadClinica ec) throws SQLException {
        boolean vacio = ec.getEstabilidadSuero() == null
                && (ec.getEstadoClinico() == null || ec.getEstadoClinico().isBlank())
                && (ec.getToxicidadInVivo() == null || ec.getToxicidadInVivo().isBlank());
        if (vacio) {
            eliminar(ec.getPeptidoId());
            return;
        }
        String sql = "INSERT INTO ESTABILIDAD_CLINICA (estabilidad_suero, estado_clinico, toxicidad_in_vivo, peptido_id) " +
                "VALUES (?, ?, ?, ?) " +
                "ON CONFLICT (peptido_id) DO UPDATE SET " +
                "estabilidad_suero = EXCLUDED.estabilidad_suero, estado_clinico = EXCLUDED.estado_clinico, " +
                "toxicidad_in_vivo = EXCLUDED.toxicidad_in_vivo";
        try (PreparedStatement ps = con().prepareStatement(sql)) {
            if (ec.getEstabilidadSuero() != null) {
                ps.setBigDecimal(1, ec.getEstabilidadSuero());
            } else {
                ps.setNull(1, Types.NUMERIC);
            }
            ps.setString(2, ec.getEstadoClinico());
            ps.setString(3, ec.getToxicidadInVivo());
            ps.setInt(4, ec.getPeptidoId());
            ps.executeUpdate();
        }
    }

    public void eliminar(int peptidoId) throws SQLException {
        try (PreparedStatement ps = con().prepareStatement("DELETE FROM ESTABILIDAD_CLINICA WHERE peptido_id = ?")) {
            ps.setInt(1, peptidoId);
            ps.executeUpdate();
        }
    }
}
