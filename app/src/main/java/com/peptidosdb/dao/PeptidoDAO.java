package com.peptidosdb.dao;

import com.peptidosdb.model.Peptido;
import com.peptidosdb.security.Sesion;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Acceso a datos de la tabla PÉPTIDO. Todas las operaciones usan la conexión
 * de la sesión activa, por lo que los privilegios reales los aplica
 * PostgreSQL según el rol (administrador / curador / consultor).
 */
public class PeptidoDAO {

    private Connection con() {
        return Sesion.actual().getConexion();
    }

    /**
     * Lista péptidos activos con información enriquecida (usa
     * vista_peptido_completo).
     */
    public List<Peptido> buscar(String filtro) throws SQLException {
        String sql = """
        SELECT 
            v.id, v.secuencia, v.nombre_principal, v.longitud, 
            v.peso_molecular, v.carga_neta, v.hidrofobicidad, 
            v.es_natural, v.estado_verificacion, v.uniprot_id, 
            v.dramp_id, v.organismo_fuente,
            array_to_string(v.nombres_alternativos, ', ') AS nombres_alternativos,
            
            -- Resumen de organismos blanco con MIC
            (SELECT string_agg(
                ob.nombre_cientifico || 
                CASE WHEN po.mic_valor IS NOT NULL 
                     THEN ' (' || po.mic_valor || po.mic_unidad || ')' 
                     ELSE '' 
                END, 
                ', '
            ) 
            FROM PEPTIDO_ORGANISMO po 
            JOIN ORGANISMO_BLANCO ob ON po.organismo_id = ob.id 
            WHERE po.peptido_id = v.id) AS organismos_resumen,
            
            -- Resumen de actividades
            (SELECT string_agg(DISTINCT 'Antiviral: ' || nombre_virus, ', ') 
             FROM PEPTIDO_ANTIVIRAL pa 
             JOIN ACTIVIDAD_ANTIVIRAL a ON pa.antiviral_id = a.id 
             WHERE pa.peptido_id = v.id) AS antiviral,
             
            (SELECT string_agg(DISTINCT 'Antifúngico: ' || nombre_hongo, ', ') 
             FROM PEPTIDO_ANTIFUNGICO pf 
             JOIN ACTIVIDAD_ANTIFUNGICA a ON pf.antifungico_id = a.id 
             WHERE pf.peptido_id = v.id) AS antifungico,
             
            (SELECT string_agg(DISTINCT 'Anticancer: ' || linea_celular, ', ') 
             FROM PEPTIDO_ANTICANCER pc 
             JOIN ACTIVIDAD_ANTICANCER a ON pc.anticancer_id = a.id 
             WHERE pc.peptido_id = v.id) AS anticancer
            
        FROM vista_peptido_completo v
        WHERE (? = '' OR v.nombre_principal ILIKE '%' || ? || '%' 
                    OR v.secuencia ILIKE '%' || ? || '%'
                    OR array_to_string(v.nombres_alternativos, ' ') ILIKE '%' || ? || '%')
        ORDER BY v.nombre_principal
        """;

        List<Peptido> resultado = new ArrayList<>();
        try (PreparedStatement ps = con().prepareStatement(sql)) {
            String f = filtro == null ? "" : filtro.trim();
            ps.setString(1, f);
            ps.setString(2, f);
            ps.setString(3, f);
            ps.setString(4, f);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    resultado.add(mapearEnriquecido(rs));
                }
            }
        }
        return resultado;
    }

    public Peptido buscarPorId(int id) throws SQLException {
        String sql = "SELECT p.*, fo.nombre_cientifico AS organismo_nombre " +
                "FROM PÉPTIDO p LEFT JOIN FUENTE_ORGANISMO fo ON p.organismo_fuente_id = fo.id " +
                "WHERE p.id = ?";
        try (PreparedStatement ps = con().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    /** Inserta un péptido nuevo y devuelve el id generado. */
    public int insertar(Peptido p) throws SQLException {
        String sql = "INSERT INTO PÉPTIDO (secuencia, nombre_principal, longitud, peso_molecular, " +
                "carga_neta, hidrofobicidad, es_natural, estado_verificacion, uniprot_id, dramp_id, " +
                "organismo_fuente_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        // longitud la recalcula el trigger trg_calcular_longitud_peptido, pero se exige NOT NULL
        // en la tabla, así que enviamos un valor provisional basado en la secuencia.

        try (PreparedStatement ps = con().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            cargarParametrosComunes(ps, p);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
                throw new SQLException("No se obtuvo el id generado para el péptido insertado");
            }
        }
    }

    public void actualizar(Peptido p) throws SQLException {
        String sql = "UPDATE PÉPTIDO SET secuencia = ?, nombre_principal = ?, longitud = ?, " +
                "peso_molecular = ?, carga_neta = ?, hidrofobicidad = ?, es_natural = ?, " +
                "estado_verificacion = ?, uniprot_id = ?, dramp_id = ?, organismo_fuente_id = ? " +
                "WHERE id = ?";
        try (PreparedStatement ps = con().prepareStatement(sql)) {
            cargarParametrosComunes(ps, p);
            ps.setInt(12, p.getId());
            ps.executeUpdate();
        }
    }

    /** Baja lógica: no se borra físicamente el registro (ver columna activo). */
    public void darDeBaja(int id) throws SQLException {
        String sql = "UPDATE PÉPTIDO SET activo = FALSE WHERE id = ?";
        try (PreparedStatement ps = con().prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public void reactivar(int id) throws SQLException {
        String sql = "UPDATE PÉPTIDO SET activo = TRUE WHERE id = ?";
        try (PreparedStatement ps = con().prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private void cargarParametrosComunes(PreparedStatement ps, Peptido p) throws SQLException {
        ps.setString(1, p.getSecuencia());
        ps.setString(2, p.getNombrePrincipal());
        ps.setShort(3, p.getSecuencia() != null ? (short) p.getSecuencia().length() : 0);
        setBigDecimalOrNull(ps, 4, p.getPesoMolecular());
        if (p.getCargaNeta() != null) {
            ps.setShort(5, p.getCargaNeta());
        } else {
            ps.setNull(5, java.sql.Types.SMALLINT);
        }
        setBigDecimalOrNull(ps, 6, p.getHidrofobicidad());
        if (p.getEsNatural() != null) {
            ps.setBoolean(7, p.getEsNatural());
        } else {
            ps.setNull(7, java.sql.Types.BOOLEAN);
        }
        ps.setString(8, p.getEstadoVerificacion());
        ps.setString(9, p.getUniprotId());
        ps.setString(10, p.getDrampId());
        if (p.getOrganismoFuenteId() != null) {
            ps.setInt(11, p.getOrganismoFuenteId());
        } else {
            ps.setNull(11, java.sql.Types.INTEGER);
        }
    }

    private void setBigDecimalOrNull(PreparedStatement ps, int idx, BigDecimal valor) throws SQLException {
        if (valor != null) {
            ps.setBigDecimal(idx, valor);
        } else {
            ps.setNull(idx, java.sql.Types.NUMERIC);
        }
    }

    private Peptido mapear(ResultSet rs) throws SQLException {
        Peptido p = new Peptido();
        p.setId(rs.getInt("id"));
        p.setSecuencia(rs.getString("secuencia"));
        p.setNombrePrincipal(rs.getString("nombre_principal"));
        p.setLongitud(rs.getShort("longitud"));
        p.setPesoMolecular(rs.getBigDecimal("peso_molecular"));
        short cargaNeta = rs.getShort("carga_neta");
        p.setCargaNeta(rs.wasNull() ? null : cargaNeta);
        p.setHidrofobicidad(rs.getBigDecimal("hidrofobicidad"));
        boolean esNatural = rs.getBoolean("es_natural");
        p.setEsNatural(rs.wasNull() ? null : esNatural);
        p.setEstadoVerificacion(rs.getString("estado_verificacion"));
        p.setUniprotId(rs.getString("uniprot_id"));
        p.setDrampId(rs.getString("dramp_id"));
        int organismoId = rs.getInt("organismo_fuente_id");
        p.setOrganismoFuenteId(rs.wasNull() ? null : organismoId);
        p.setOrganismoFuenteNombre(rs.getString("organismo_nombre"));
        p.setActivo(rs.getBoolean("activo"));
        Timestamp fechaIngesta = rs.getTimestamp("fecha_ingesta");
        if (fechaIngesta != null) p.setFechaIngesta(fechaIngesta.toLocalDateTime());
        Timestamp ultimaAct = rs.getTimestamp("ultima_actualizacion");
        if (ultimaAct != null) p.setUltimaActualizacion(ultimaAct.toLocalDateTime());
        return p;
    }
    
    private Peptido mapearEnriquecido(ResultSet rs) throws SQLException {
        Peptido p = new Peptido();
        p.setId(rs.getInt("id"));
        p.setSecuencia(rs.getString("secuencia"));
        p.setNombrePrincipal(rs.getString("nombre_principal"));
        p.setNombresAlternativos(rs.getString("nombres_alternativos"));
        p.setLongitud(rs.getShort("longitud"));
        p.setPesoMolecular(rs.getBigDecimal("peso_molecular"));
        short cargaNeta = rs.getShort("carga_neta");
        p.setCargaNeta(rs.wasNull() ? null : cargaNeta);
        p.setHidrofobicidad(rs.getBigDecimal("hidrofobicidad"));
        boolean esNatural = rs.getBoolean("es_natural");
        p.setEsNatural(rs.wasNull() ? null : esNatural);
        p.setEstadoVerificacion(rs.getString("estado_verificacion"));
        p.setUniprotId(rs.getString("uniprot_id"));
        p.setDrampId(rs.getString("dramp_id"));
        p.setOrganismoFuenteNombre(rs.getString("organismo_fuente"));
        p.setOrganismosResumen(rs.getString("organismos_resumen"));

        // Construye el resumen de actividades
        StringBuilder act = new StringBuilder();
        String antiviral = rs.getString("antiviral");
        String antifungico = rs.getString("antifungico");
        String anticancer = rs.getString("anticancer");

        if (antiviral != null) {
            act.append(antiviral).append("; ");
        }
        if (antifungico != null) {
            act.append(antifungico).append("; ");
        }
        if (anticancer != null) {
            act.append(anticancer);
        }

        p.setActividadesResumen(act.toString().replaceAll("; $", "")); // limpia último separador

        return p;
    }
}
