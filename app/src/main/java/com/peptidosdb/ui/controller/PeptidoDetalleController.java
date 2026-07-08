package com.peptidosdb.ui.controller;

import com.peptidosdb.dao.FuenteOrganismoDAO;
import com.peptidosdb.model.FuenteOrganismo;
import com.peptidosdb.model.Peptido;
import com.peptidosdb.service.PeptidoService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

/**
 * Controlador del formulario de detalle de péptido. La pestaña "General" se
 * maneja directamente aquí; el resto de las pestañas (información relacionada
 * en otras tablas) se delega a un controlador propio por pestaña, inyectado
 * automáticamente por FXMLLoader gracias a la convención fx:id="xxx" ->
 * campo "xxxController" cuando se usa <fx:include fx:id="xxx" .../>.
 */
public class PeptidoDetalleController {

    @FXML private Label etiquetaTitulo;
    @FXML private TabPane tabPane;
    @FXML private TextField campoNombre;
    @FXML private TextField campoSecuencia;
    @FXML private TextField campoPesoMolecular;
    @FXML private TextField campoCargaNeta;
    @FXML private TextField campoHidrofobicidad;
    @FXML private CheckBox checkEsNatural;
    @FXML private ComboBox<String> comboEstadoVerificacion;
    @FXML private TextField campoUniprotId;
    @FXML private TextField campoDrampId;
    @FXML private ComboBox<FuenteOrganismo> comboOrganismo;
    @FXML private Label etiquetaError;

    // Controladores de las pestañas de información relacionada, inyectados vía fx:include.
    @FXML private NombresAlternativosTabController nombresAltTabController;
    @FXML private EstructuraTabController estructuraTabController;
    @FXML private EstabilidadClinicaTabController estabilidadTabController;
    @FXML private ModificacionesTabController modificacionesTabController;
    @FXML private OrganismosBlancoTabController organismosTabController;
    @FXML private ActividadesTabController actividadesTabController;
    @FXML private PublicacionesTabController publicacionesTabController;
    @FXML private FuentesTabController fuentesTabController;

    private final PeptidoService peptidoService = new PeptidoService();
    private final FuenteOrganismoDAO fuenteOrganismoDAO = new FuenteOrganismoDAO();

    private Integer peptidoId; // null => modo creación

    @FXML
    private void initialize() {
        comboEstadoVerificacion.setItems(FXCollections.observableArrayList(
                "curado_literatura", "validado_espectrometria", "predicho", "sintetico"));
        try {
            List<FuenteOrganismo> organismos = fuenteOrganismoDAO.listarTodos();
            comboOrganismo.setItems(FXCollections.observableArrayList(organismos));
        } catch (SQLException e) {
            etiquetaError.setText("No se pudo cargar el catálogo de organismos: " + e.getMessage());
        }
    }

    /** Carga un péptido existente para editarlo, o deja el formulario vacío si id es null. */
    public void cargar(Integer id) {
        this.peptidoId = id;

        // Las pestañas de información relacionada dependen de un peptido_id ya
        // existente; se deshabilitan al crear un péptido nuevo.
        for (int i = 1; i < tabPane.getTabs().size(); i++) {
            tabPane.getTabs().get(i).setDisable(id == null);
        }

        if (id == null) {
            etiquetaTitulo.setText("Nuevo péptido");
            return;
        }
        etiquetaTitulo.setText("Editar péptido #" + id);
        try {
            Peptido p = peptidoService.buscarPorId(id);
            if (p == null) {
                etiquetaError.setText("El péptido ya no existe");
                return;
            }
            campoNombre.setText(p.getNombrePrincipal());
            campoSecuencia.setText(p.getSecuencia());
            campoPesoMolecular.setText(p.getPesoMolecular() != null ? p.getPesoMolecular().toString() : "");
            campoCargaNeta.setText(p.getCargaNeta() != null ? p.getCargaNeta().toString() : "");
            campoHidrofobicidad.setText(p.getHidrofobicidad() != null ? p.getHidrofobicidad().toString() : "");
            checkEsNatural.setSelected(Boolean.TRUE.equals(p.getEsNatural()));
            comboEstadoVerificacion.setValue(p.getEstadoVerificacion());
            campoUniprotId.setText(p.getUniprotId());
            campoDrampId.setText(p.getDrampId());
            if (p.getOrganismoFuenteId() != null) {
                comboOrganismo.getItems().stream()
                        .filter(o -> o.getId().equals(p.getOrganismoFuenteId()))
                        .findFirst()
                        .ifPresent(comboOrganismo::setValue);
            }

            nombresAltTabController.cargar(id);
            estructuraTabController.cargar(id);
            estabilidadTabController.cargar(id);
            modificacionesTabController.cargar(id);
            organismosTabController.cargar(id);
            actividadesTabController.cargar(id);
            publicacionesTabController.cargar(id);
            fuentesTabController.cargar(id);
        } catch (SQLException e) {
            etiquetaError.setText("Error al cargar el péptido: " + e.getMessage());
        }
    }

    @FXML
    private void onGuardar() {
        etiquetaError.setText("");
        try {
            Peptido p = (peptidoId != null) ? peptidoService.buscarPorId(peptidoId) : new Peptido();
            p.setNombrePrincipal(campoNombre.getText());
            p.setSecuencia(campoSecuencia.getText() != null ? campoSecuencia.getText().toUpperCase() : null);
            p.setPesoMolecular(parseDecimal(campoPesoMolecular.getText()));
            p.setCargaNeta(parseShort(campoCargaNeta.getText()));
            p.setHidrofobicidad(parseDecimal(campoHidrofobicidad.getText()));
            p.setEsNatural(checkEsNatural.isSelected());
            p.setEstadoVerificacion(comboEstadoVerificacion.getValue());
            p.setUniprotId(vacioComoNull(campoUniprotId.getText()));
            p.setDrampId(vacioComoNull(campoDrampId.getText()));
            p.setOrganismoFuenteId(comboOrganismo.getValue() != null ? comboOrganismo.getValue().getId() : null);

            if (peptidoId == null) {
                // Péptido nuevo: las pestañas relacionadas están deshabilitadas y vacías,
                // así que basta con crear el registro principal.
                peptidoService.crear(p);
            } else {
                peptidoService.actualizar(p);
                // Las pestañas relacionadas usan un patrón de "buffer": los cambios se
                // aplican recién aquí, junto con el guardado del péptido principal.
                nombresAltTabController.guardar(peptidoId);
                estructuraTabController.guardar(peptidoId);
                estabilidadTabController.guardar(peptidoId);
                modificacionesTabController.guardar(peptidoId);
                organismosTabController.guardar(peptidoId);
                actividadesTabController.guardar(peptidoId);
                publicacionesTabController.guardar(peptidoId);
                fuentesTabController.guardar(peptidoId);
            }
            cerrarVentana();
        } catch (NumberFormatException e) {
            etiquetaError.setText("Revise los campos numéricos (peso molecular, carga neta, hidrofobicidad)");
        } catch (IllegalArgumentException | SecurityException e) {
            etiquetaError.setText(e.getMessage());
        } catch (SQLException e) {
            etiquetaError.setText("Error al guardar en la base de datos: " + e.getMessage());
        }
    }

    @FXML
    private void onCancelar() {
        cerrarVentana();
    }

    private void cerrarVentana() {
        ((Stage) campoNombre.getScene().getWindow()).close();
    }

    private BigDecimal parseDecimal(String texto) {
        if (texto == null || texto.isBlank()) return null;
        return new BigDecimal(texto.trim().replace(",", "."));
    }

    private Short parseShort(String texto) {
        if (texto == null || texto.isBlank()) return null;
        return Short.parseShort(texto.trim());
    }

    private String vacioComoNull(String texto) {
        return (texto == null || texto.isBlank()) ? null : texto.trim();
    }
}
