-- Para búsquedas de similitud de cadenas (opcional, pero útil)
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- Para generar UUIDs en tablas como analysis_jobs (si se usa)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Función que valida que la secuencia contenga solo caracteres de aminoácidos estándar
CREATE OR REPLACE FUNCTION validar_secuencia_aa() 
RETURNS trigger AS $$
BEGIN
    -- Permite aminoácidos estándar + X (desconocido) + Z (Glu/Gln)
    IF NEW.secuencia !~ '^[ACDEFGHIKLMNPQRSTVWYBXZ]+$' THEN
        RAISE EXCEPTION 'Secuencia inválida (solo aminoácidos estándar + X + Z): %', NEW.secuencia;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION calcular_longitud()
RETURNS TRIGGER AS $$
BEGIN
    NEW.longitud := LENGTH(NEW.secuencia);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Refresca la marca temporal de última actualización en cada UPDATE
CREATE OR REPLACE FUNCTION actualizar_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.ultima_actualizacion := CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TABLE FUENTE_DATOS (
    nombre VARCHAR(50) PRIMARY KEY,
    url TEXT,
    version VARCHAR(20)
);

COMMENT ON TABLE FUENTE_DATOS IS 'Bases de datos externas de donde se extrae la información (APD, DRAMP, UniProt, etc.)';

CREATE TABLE PUBLICACIÓN (
    pmid VARCHAR(20) PRIMARY KEY,
    doi VARCHAR(255) UNIQUE,
    titulo TEXT,
    autores TEXT,
    anyo INTEGER CHECK (anyo >= 1800 AND anyo <= EXTRACT(YEAR FROM CURRENT_DATE))
);

COMMENT ON TABLE PUBLICACIÓN IS 'Artículos científicos que describen los péptidos';

CREATE TABLE FUENTE_ORGANISMO (
    id SERIAL PRIMARY KEY,
    nombre_cientifico VARCHAR(200) UNIQUE NOT NULL,
    reino VARCHAR(50)
);

COMMENT ON TABLE FUENTE_ORGANISMO IS 'Organismos productores de los péptidos (origen biológico)';

CREATE TABLE PÉPTIDO (
    id SERIAL PRIMARY KEY,
    secuencia TEXT NOT NULL UNIQUE,
    nombre_principal VARCHAR(200) NOT NULL,
    longitud SMALLINT NOT NULL,
    peso_molecular DECIMAL(10,2),
    carga_neta SMALLINT,
    hidrofobicidad DECIMAL(5,2),
    es_natural BOOLEAN,
    estado_verificacion VARCHAR(20) NOT NULL CHECK (estado_verificacion IN ('curado_literatura', 'validado_espectrometria', 'predicho', 'sintetico')),
    uniprot_id VARCHAR(20),
    dramp_id VARCHAR(20),
    organismo_fuente_id INTEGER REFERENCES FUENTE_ORGANISMO(id) ON DELETE SET NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_ingesta TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ultima_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON COLUMN PÉPTIDO.activo IS 'Soft-delete: false = registro dado de baja lógicamente, se conserva su historial asociado';

-- Triggers para validación y cálculo de longitud
CREATE TRIGGER trg_validar_secuencia_peptido
    BEFORE INSERT OR UPDATE ON PÉPTIDO
    FOR EACH ROW EXECUTE FUNCTION validar_secuencia_aa();

CREATE TRIGGER trg_calcular_longitud_peptido
    BEFORE INSERT OR UPDATE ON PÉPTIDO
    FOR EACH ROW EXECUTE FUNCTION calcular_longitud();

CREATE TRIGGER trg_actualizar_timestamp_peptido
    BEFORE UPDATE ON PÉPTIDO
    FOR EACH ROW EXECUTE FUNCTION actualizar_timestamp();

-- Índices
CREATE INDEX idx_peptido_uniprot ON PÉPTIDO(uniprot_id);
CREATE INDEX idx_peptido_dramp ON PÉPTIDO(dramp_id);
CREATE INDEX idx_peptido_longitud ON PÉPTIDO(longitud);
CREATE INDEX idx_peptido_estado ON PÉPTIDO(estado_verificacion);
CREATE INDEX idx_peptido_organismo ON PÉPTIDO(organismo_fuente_id);
CREATE INDEX idx_peptido_activo ON PÉPTIDO(activo);

-- Índice trigram para búsquedas de similitud de secuencia (requiere pg_trgm, ya habilitado arriba)
CREATE INDEX idx_peptido_secuencia_trgm ON PÉPTIDO USING gin (secuencia gin_trgm_ops);

-- Índice trigram para búsquedas aproximadas por nombre principal
CREATE INDEX idx_peptido_nombre_trgm ON PÉPTIDO USING gin (nombre_principal gin_trgm_ops);

CREATE TABLE NOMBRE_ALTERNATIVO (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(200) NOT NULL,
    peptido_id INTEGER NOT NULL REFERENCES PÉPTIDO(id) ON DELETE CASCADE,
    CONSTRAINT uq_nombre_alternativo_nombre_peptido UNIQUE (nombre, peptido_id)
);

CREATE INDEX idx_nombre_alt_peptido ON NOMBRE_ALTERNATIVO(peptido_id);

CREATE TABLE ORGANISMO_BLANCO (
    id SERIAL PRIMARY KEY,
    nombre_cientifico VARCHAR(200) UNIQUE NOT NULL,
    gram VARCHAR(10) CHECK (gram IN ('positive', 'negative', 'both')),
    categoria VARCHAR(50)
);

CREATE TABLE ACTIVIDAD_ANTIVIRAL (
    id SERIAL PRIMARY KEY,
    nombre_virus VARCHAR(200) UNIQUE NOT NULL,
    familia_viral VARCHAR(100)
);

CREATE TABLE ACTIVIDAD_ANTIFUNGICA (
    id SERIAL PRIMARY KEY,
    nombre_hongo VARCHAR(200) UNIQUE NOT NULL
);

CREATE TABLE ACTIVIDAD_ANTICANCER (
    id SERIAL PRIMARY KEY,
    linea_celular VARCHAR(200) UNIQUE NOT NULL,
    tipo_cancer VARCHAR(100)
);

CREATE TABLE ESTRUCTURA (
    id SERIAL PRIMARY KEY,
    tipo_estructura VARCHAR(50),
    pdb_id VARCHAR(10),
    metodo TEXT,
    ciclizacion VARCHAR(30),
    peptido_id INTEGER NOT NULL UNIQUE REFERENCES PÉPTIDO(id) ON DELETE CASCADE
);

CREATE INDEX idx_estructura_pdb ON ESTRUCTURA(pdb_id);

CREATE TABLE MODIFICACION_POSTRADUCCIONAL (
    id SERIAL PRIMARY KEY,
    tipo VARCHAR(50) NOT NULL,
    posicion VARCHAR(200),
    CONSTRAINT uq_modificacion_tipo_posicion UNIQUE (tipo, posicion)
);

CREATE TABLE ESTABILIDAD_CLINICA (
    id SERIAL PRIMARY KEY,
    estabilidad_suero DECIMAL(8,2),
    estado_clinico VARCHAR(50) CHECK (estado_clinico IN ('preclínica', 'fase1', 'fase2', 'fase3', 'aprobado', 'retirado')),
    toxicidad_in_vivo TEXT,
    peptido_id INTEGER NOT NULL UNIQUE REFERENCES PÉPTIDO(id) ON DELETE CASCADE
);

CREATE TABLE PEPTIDO_ORGANISMO (
    peptido_id INTEGER NOT NULL REFERENCES PÉPTIDO(id) ON DELETE CASCADE,
    organismo_id INTEGER NOT NULL REFERENCES ORGANISMO_BLANCO(id) ON DELETE CASCADE,
    mic_valor DECIMAL(10,3),
    mic_unidad VARCHAR(20) DEFAULT 'µg/mL',
    PRIMARY KEY (peptido_id, organismo_id)
);

COMMENT ON COLUMN PEPTIDO_ORGANISMO.mic_valor IS 'Valor numérico de la concentración inhibitoria mínima, separado de la unidad para permitir ordenar/filtrar/agregar directamente';

CREATE INDEX idx_peptido_organismo_peptido ON PEPTIDO_ORGANISMO(peptido_id);
CREATE INDEX idx_peptido_organismo_organismo ON PEPTIDO_ORGANISMO(organismo_id);

CREATE TABLE PEPTIDO_ANTIVIRAL (
    peptido_id INTEGER NOT NULL REFERENCES PÉPTIDO(id) ON DELETE CASCADE,
    antiviral_id INTEGER NOT NULL REFERENCES ACTIVIDAD_ANTIVIRAL(id) ON DELETE CASCADE,
    PRIMARY KEY (peptido_id, antiviral_id)
);

CREATE TABLE PEPTIDO_ANTIFUNGICO (
    peptido_id INTEGER NOT NULL REFERENCES PÉPTIDO(id) ON DELETE CASCADE,
    antifungico_id INTEGER NOT NULL REFERENCES ACTIVIDAD_ANTIFUNGICA(id) ON DELETE CASCADE,
    PRIMARY KEY (peptido_id, antifungico_id)
);

CREATE TABLE PEPTIDO_ANTICANCER (
    peptido_id INTEGER NOT NULL REFERENCES PÉPTIDO(id) ON DELETE CASCADE,
    anticancer_id INTEGER NOT NULL REFERENCES ACTIVIDAD_ANTICANCER(id) ON DELETE CASCADE,
    PRIMARY KEY (peptido_id, anticancer_id)
);

CREATE TABLE PEPTIDO_MODIFICACION (
    peptido_id INTEGER NOT NULL REFERENCES PÉPTIDO(id) ON DELETE CASCADE,
    modificacion_id INTEGER NOT NULL REFERENCES MODIFICACION_POSTRADUCCIONAL(id) ON DELETE CASCADE,
    PRIMARY KEY (peptido_id, modificacion_id)
);

CREATE TABLE PEPTIDO_PUBLICACION (
    peptido_id INTEGER NOT NULL REFERENCES PÉPTIDO(id) ON DELETE CASCADE,
    publicacion_pmid VARCHAR(20) NOT NULL REFERENCES PUBLICACIÓN(pmid) ON DELETE CASCADE,
    PRIMARY KEY (peptido_id, publicacion_pmid)
);

CREATE TABLE PEPTIDO_FUENTE (
    peptido_id INTEGER NOT NULL REFERENCES PÉPTIDO(id) ON DELETE CASCADE,
    fuente_nombre VARCHAR(50) NOT NULL REFERENCES FUENTE_DATOS(nombre) ON DELETE CASCADE,
    prioridad SMALLINT,  -- menor número = mayor prioridad/confiabilidad
    PRIMARY KEY (peptido_id, fuente_nombre)
);

-- Vista simplificada con nombres alternativos agregados
CREATE VIEW vista_peptido_completo AS
SELECT p.id, p.secuencia, p.nombre_principal,
       array_agg(na.nombre) AS nombres_alternativos,
       p.longitud, p.peso_molecular, p.carga_neta, p.hidrofobicidad,
       p.es_natural, p.estado_verificacion,
       p.uniprot_id, p.dramp_id,
       fo.nombre_cientifico AS organismo_fuente
FROM PÉPTIDO p
LEFT JOIN NOMBRE_ALTERNATIVO na ON p.id = na.peptido_id
LEFT JOIN FUENTE_ORGANISMO fo ON p.organismo_fuente_id = fo.id
WHERE p.activo = TRUE
GROUP BY p.id, fo.nombre_cientifico;

-- Vista de actividad por péptido (organismos blanco)
CREATE VIEW vista_actividad_peptido AS
SELECT p.id, p.nombre_principal,
       jsonb_agg(DISTINCT jsonb_build_object('organismo', ob.nombre_cientifico, 'mic_valor', po.mic_valor, 'mic_unidad', po.mic_unidad)) AS organismos,
       (SELECT jsonb_agg(DISTINCT av.nombre_virus) FROM PEPTIDO_ANTIVIRAL pv JOIN ACTIVIDAD_ANTIVIRAL av ON pv.antiviral_id = av.id WHERE pv.peptido_id = p.id) AS virus,
       (SELECT jsonb_agg(DISTINCT af.nombre_hongo) FROM PEPTIDO_ANTIFUNGICO pf JOIN ACTIVIDAD_ANTIFUNGICA af ON pf.antifungico_id = af.id WHERE pf.peptido_id = p.id) AS hongos
FROM PÉPTIDO p
LEFT JOIN PEPTIDO_ORGANISMO po ON p.id = po.peptido_id
LEFT JOIN ORGANISMO_BLANCO ob ON po.organismo_id = ob.id
WHERE p.activo = TRUE
GROUP BY p.id;

-- Usuarios de la aplicación (login propio de la app, separado de los roles de conexión de PostgreSQL)
CREATE TABLE USUARIO_APP (
    id SERIAL PRIMARY KEY,
    nombre_usuario VARCHAR(50) UNIQUE NOT NULL,
    nombre_completo VARCHAR(200),
    password_hash VARCHAR(255) NOT NULL,
    rol_bd VARCHAR(20) NOT NULL CHECK (rol_bd IN ('administrador', 'curador', 'consultor')),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE USUARIO_APP IS 'Cuentas individuales de la aplicación Java; rol_bd determina con qué rol de PostgreSQL (administrador/curador/consultor) se abre la conexión JDBC';

-- Rol técnico exclusivo para el login: solo puede leer USUARIO_APP
-- (no tiene acceso a las tablas de negocio). Lo usa la app antes de saber
-- qué rol de negocio (administrador/curador/consultor) le corresponde
-- al usuario que se está autenticando.
CREATE ROLE app_login WITH LOGIN PASSWORD '12345678';
GRANT USAGE ON SCHEMA public TO app_login;
GRANT SELECT ON USUARIO_APP TO app_login;

-- Crear roles
CREATE ROLE administrador WITH LOGIN PASSWORD '12345678' SUPERUSER CREATEDB CREATEROLE;
CREATE ROLE curador WITH LOGIN PASSWORD '12345678';
CREATE ROLE consultor WITH LOGIN PASSWORD '12345678';

-- Asignar privilegios al curador
GRANT USAGE ON SCHEMA public TO curador;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO curador;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO curador;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO curador;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT USAGE, SELECT ON SEQUENCES TO curador;

-- Asignar privilegios al consultor
GRANT USAGE ON SCHEMA public TO consultor;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO consultor;
GRANT SELECT ON ALL SEQUENCES IN SCHEMA public TO consultor;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO consultor;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON SEQUENCES TO consultor;

-- Otorgar permisos específicos sobre las vistas
GRANT SELECT ON vista_peptido_completo, vista_actividad_peptido TO consultor;

-- USUARIO_APP contiene password_hash: se revoca el acceso otorgado por las políticas
-- generales anteriores; solo el administrador (superusuario) puede gestionarla.
REVOKE ALL ON USUARIO_APP FROM curador;
REVOKE ALL ON USUARIO_APP FROM consultor;