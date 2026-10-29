-- Función que valida si un trabajador puede ingresar a un barrio.
-- Retorna una sola fila: habilitado=true si pasa todas las verificaciones,
-- o habilitado=false con el motivo de rechazo al encontrar la primera falla.
-- Invocada desde GaritaRepository vía JdbcTemplate.

CREATE OR REPLACE FUNCTION verificar_acceso(p_trabajador_id BIGINT, p_barrio_id BIGINT)
RETURNS TABLE(habilitado BOOLEAN, motivo TEXT)
LANGUAGE plpgsql
STABLE
AS $$
DECLARE
    v_barrio_estado     VARCHAR(10);
    v_trabajador_baja   TIMESTAMPTZ;
    v_perfil_id         BIGINT;
    v_perfil_tipo       VARCHAR(15);
    v_perfil_estado     VARCHAR(15);
    v_habilit           BIGINT;
    v_tiene_art         BOOLEAN;
    v_art_vencida       BOOLEAN;
    v_tiene_seguro      BOOLEAN;
    v_seguro_vencido    BOOLEAN;
    v_ult_tipo          VARCHAR(10);
    v_ult_razon_social  VARCHAR(200);
    v_ult_nombre_mono   VARCHAR(100);
    v_ult_apellido_mono VARCHAR(100);
    v_ult_tipo_perfil   VARCHAR(15);
    v_nombre_vinculo    TEXT;
    v_dni               VARCHAR(10);
BEGIN
    -- ────────────────────────────────────────────────
    -- 1. Barrio activo
    -- ────────────────────────────────────────────────
    SELECT b.estado INTO v_barrio_estado
    FROM barrio b
    WHERE b.id = p_barrio_id;

    IF v_barrio_estado IS DISTINCT FROM 'ACTIVO' THEN
        RETURN QUERY SELECT FALSE, 'barrio inactivo'::TEXT;
        RETURN;
    END IF;

    -- ────────────────────────────────────────────────
    -- 2. Trabajador existe (obtenemos datos de una vez)
    -- ────────────────────────────────────────────────
    SELECT t.fecha_baja, t.perfil_id, t.dni
    INTO v_trabajador_baja, v_perfil_id, v_dni
    FROM trabajador t
    WHERE t.id = p_trabajador_id;

    IF NOT FOUND THEN
        RETURN QUERY SELECT FALSE, 'no existe'::TEXT;
        RETURN;
    END IF;

    -- ────────────────────────────────────────────────
    -- 3. Trabajador dado de baja en el vínculo
    -- ────────────────────────────────────────────────
    IF v_trabajador_baja IS NOT NULL THEN
        RETURN QUERY SELECT FALSE, 'baja en ese vínculo'::TEXT;
        RETURN;
    END IF;

    -- ────────────────────────────────────────────────
    -- 4. Perfil aprobado
    -- ────────────────────────────────────────────────
    SELECT p.estado, p.tipo
    INTO v_perfil_estado, v_perfil_tipo
    FROM perfil p
    WHERE p.id = v_perfil_id;

    IF v_perfil_estado IS DISTINCT FROM 'APROBADO' THEN
        IF v_perfil_tipo = 'EMPRESA' THEN
            RETURN QUERY SELECT FALSE, 'Empresa suspendida'::TEXT;
        ELSE
            RETURN QUERY SELECT FALSE, 'Monotributista suspendido'::TEXT;
        END IF;
        RETURN;
    END IF;

    -- ────────────────────────────────────────────────
    -- 5. Habilitación del perfil en el barrio
    -- ────────────────────────────────────────────────
    SELECT hb.perfil_id INTO v_habilit
    FROM habilitacion_barrio hb
    WHERE hb.perfil_id = v_perfil_id AND hb.barrio_id = p_barrio_id;

    IF NOT FOUND THEN
        RETURN QUERY SELECT FALSE, 'no habilitada en el barrio'::TEXT;
        RETURN;
    END IF;

    -- ────────────────────────────────────────────────
    -- 6. Documentación
    -- ────────────────────────────────────────────────
    SELECT EXISTS (
        SELECT 1 FROM documento d
        WHERE d.trabajador_id = p_trabajador_id AND d.tipo = 'ART'
    ) INTO v_tiene_art;

    IF NOT v_tiene_art THEN
        RETURN QUERY SELECT FALSE, 'documentación pendiente de carga'::TEXT;
        RETURN;
    END IF;

    SELECT EXISTS (
        SELECT 1 FROM documento d
        WHERE d.trabajador_id = p_trabajador_id AND d.tipo = 'ART'
          AND d.fecha_vencimiento < CURRENT_DATE
    ) INTO v_art_vencida;

    IF v_art_vencida THEN
        RETURN QUERY SELECT FALSE, 'documentación vencida (ART)'::TEXT;
        RETURN;
    END IF;

    SELECT EXISTS (
        SELECT 1 FROM documento d
        WHERE d.trabajador_id = p_trabajador_id AND d.tipo = 'SEGURO_VIDA'
    ) INTO v_tiene_seguro;

    IF NOT v_tiene_seguro THEN
        RETURN QUERY SELECT FALSE, 'documentación pendiente de carga'::TEXT;
        RETURN;
    END IF;

    SELECT EXISTS (
        SELECT 1 FROM documento d
        WHERE d.trabajador_id = p_trabajador_id AND d.tipo = 'SEGURO_VIDA'
          AND d.fecha_vencimiento < CURRENT_DATE
    ) INTO v_seguro_vencido;

    IF v_seguro_vencido THEN
        RETURN QUERY SELECT FALSE, 'documentación vencida (Seguro de Vida)'::TEXT;
        RETURN;
    END IF;

    -- ────────────────────────────────────────────────
    -- 7. Ingreso activo (por DNI en el barrio, cualquier vínculo)
    -- ────────────────────────────────────────────────
    -- Busca el último movimiento de CUALQUIER trabajador con el mismo DNI en ese barrio.
    SELECT m.tipo,
           p2.razon_social,
           p2.nombre,
           p2.apellido,
           p2.tipo
    INTO v_ult_tipo, v_ult_razon_social, v_ult_nombre_mono, v_ult_apellido_mono, v_ult_tipo_perfil
    FROM movimiento m
    JOIN trabajador t2  ON t2.id = m.trabajador_id
    JOIN perfil    p2   ON p2.id = t2.perfil_id
    WHERE t2.dni = v_dni
      AND m.barrio_id = p_barrio_id
    ORDER BY m.fecha_hora DESC
    LIMIT 1;

    IF FOUND AND v_ult_tipo = 'INGRESO' THEN
        IF v_ult_tipo_perfil = 'EMPRESA' THEN
            v_nombre_vinculo := v_ult_razon_social;
        ELSE
            v_nombre_vinculo := 'Monotributista ' || v_ult_nombre_mono || ' ' || v_ult_apellido_mono;
        END IF;
        RETURN QUERY SELECT FALSE,
            ('ya tiene un ingreso activo bajo ' || v_nombre_vinculo || ': debe registrarse su egreso primero')::TEXT;
        RETURN;
    END IF;

    -- ────────────────────────────────────────────────
    -- 8. Pasa todas las verificaciones
    -- ────────────────────────────────────────────────
    RETURN QUERY SELECT TRUE, ''::TEXT;
END;
$$;
