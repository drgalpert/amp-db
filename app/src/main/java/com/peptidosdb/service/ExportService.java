package com.peptidosdb.service;

import com.peptidosdb.model.Peptido;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class ExportService {

    public void exportarListaAPeptidos(List<Peptido> peptidos, String rutaArchivo) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Péptidos");

            // Cabecera
            Row headerRow = sheet.createRow(0);
            String[] headers = {"ID", "Nombre Principal", "Nombres Alternativos", "Secuencia", "Longitud", 
                               "Organismo Fuente", "Estado", "Organismos Blanco", "Actividades", 
                               "Peso Molecular", "Carga Neta", "Uniprot ID", "DRAMP ID"};

            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Datos
            int rowNum = 1;
            for (Peptido p : peptidos) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(p.getId());
                row.createCell(1).setCellValue(p.getNombrePrincipal() != null ? p.getNombrePrincipal() : "");
                row.createCell(2).setCellValue(p.getNombresAlternativos() != null ? p.getNombresAlternativos() : "");
                row.createCell(3).setCellValue(p.getSecuencia() != null ? p.getSecuencia() : "");
                row.createCell(4).setCellValue(p.getLongitud() != null ? p.getLongitud() : 0);
                row.createCell(5).setCellValue(p.getOrganismoFuenteNombre() != null ? p.getOrganismoFuenteNombre() : "");
                row.createCell(6).setCellValue(p.getEstadoVerificacion() != null ? p.getEstadoVerificacion() : "");
                row.createCell(7).setCellValue(p.getOrganismosResumen() != null ? p.getOrganismosResumen() : "");
                row.createCell(8).setCellValue(p.getActividadesResumen() != null ? p.getActividadesResumen() : "");
                row.createCell(9).setCellValue(p.getPesoMolecular() != null ? p.getPesoMolecular().doubleValue() : 0);
                row.createCell(10).setCellValue(p.getCargaNeta() != null ? p.getCargaNeta() : 0);
                row.createCell(11).setCellValue(p.getUniprotId() != null ? p.getUniprotId() : "");
                row.createCell(12).setCellValue(p.getDrampId() != null ? p.getDrampId() : "");
            }

            // Autoajustar columnas
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // Guardar archivo
            try (FileOutputStream fileOut = new FileOutputStream(rutaArchivo)) {
                workbook.write(fileOut);
            }
        }
    }
}