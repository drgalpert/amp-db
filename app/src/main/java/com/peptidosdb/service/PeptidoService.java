package com.peptidosdb.service;

import com.peptidosdb.dao.PeptidoDAO;
import com.peptidosdb.model.Peptido;
import com.peptidosdb.security.Sesion;

import java.sql.SQLException;
import java.util.List;

/**
 * Reglas de negocio sobre Péptido. Además de delegar en el DAO, verifica
 * permisos a nivel de aplicación (defensa en profundidad: PostgreSQL ya
 * rechazaría la operación por el rol, pero así evitamos el viaje a la BD
 * y damos un mensaje más claro en la interfaz).
 */
public class PeptidoService {

    private final PeptidoDAO peptidoDAO = new PeptidoDAO();

    public List<Peptido> buscar(String filtro) throws SQLException {
        return peptidoDAO.buscar(filtro);
    }

    public Peptido buscarPorId(int id) throws SQLException {
        return peptidoDAO.buscarPorId(id);
    }

    public int crear(Peptido p) throws SQLException {
        exigirPermisoEdicion();
        validar(p);
        return peptidoDAO.insertar(p);
    }

    public void actualizar(Peptido p) throws SQLException {
        exigirPermisoEdicion();
        validar(p);
        peptidoDAO.actualizar(p);
    }

    public void darDeBaja(int id) throws SQLException {
        exigirPermisoEdicion();
        peptidoDAO.darDeBaja(id);
    }

    private void validar(Peptido p) {
        if (p.getSecuencia() == null || p.getSecuencia().isBlank()) {
            throw new IllegalArgumentException("La secuencia es obligatoria");
        }
        if (!p.getSecuencia().matches("^[ACDEFGHIKLMNPQRSTVWYBXZ]+$")) {
            // La base de datos también valida esto con un trigger; se repite aquí
            // para dar feedback inmediato sin necesidad de ir a la BD.
            throw new IllegalArgumentException(
                    "Secuencia inválida: solo se permiten aminoácidos estándar + X + Z");
        }
        if (p.getNombrePrincipal() == null || p.getNombrePrincipal().isBlank()) {
            throw new IllegalArgumentException("El nombre principal es obligatorio");
        }
        if (p.getEstadoVerificacion() == null) {
            throw new IllegalArgumentException("El estado de verificación es obligatorio");
        }
    }

    private void exigirPermisoEdicion() {
        if (!Sesion.actual().puedeEditar()) {
            throw new SecurityException("Su rol (consultor) no tiene permisos de edición");
        }
    }
}
