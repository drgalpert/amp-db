package com.peptidosdb.dao;

import com.peptidosdb.model.OrganismoBlanco;
import com.peptidosdb.security.Sesion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class OrganismoBlancoDAO {

    private Connection con() {
        return Sesion.actual().getConexion();
    }

    public List<OrganismoBlanco> listarTodos() throws SQLException {
        String sql = "SELECT id, nombre_cientifico, gram, categoria FROM ORGANISMO_BLANCO ORDER BY nombre_cientifico";
        List<OrganismoBlanco> lista = new ArrayList<>();
        try (PreparedStatement ps = con().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                OrganismoBlanco o = new OrganismoBlanco();
                o.setId(rs.getInt("id"));
                o.setNombreCientifico(rs.getString("nombre_cientifico"));
                o.setGram(rs.getString("gram"));
                o.setCategoria(rs.getString("categoria"));
                lista.add(o);
            }
        }
        return lista;
    }
}
