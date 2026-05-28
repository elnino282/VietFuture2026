UPDATE plots
SET soil_type = CASE soil_type
    WHEN 'LOAM' THEN 'FERRALSOLS'
    WHEN 'Loam' THEN 'FERRALSOLS'
    WHEN 'CLAY' THEN 'CHERNOZEMS'
    WHEN 'Clay' THEN 'CHERNOZEMS'
    WHEN 'SANDY' THEN 'FLUVISOLS'
    WHEN 'Sandy' THEN 'FLUVISOLS'
    WHEN 'SILT' THEN 'PODZOL'
    WHEN 'Silt' THEN 'PODZOL'
    WHEN 'Peat' THEN 'PEAT'
    WHEN 'Peaty' THEN 'PEAT'
    WHEN 'CHALK' THEN 'ARENOSOLS'
    WHEN 'Chalk' THEN 'ARENOSOLS'
    WHEN 'Chalky' THEN 'ARENOSOLS'
    ELSE soil_type
END
WHERE soil_type IN (
    'LOAM', 'Loam',
    'CLAY', 'Clay',
    'SANDY', 'Sandy',
    'SILT', 'Silt',
    'Peat', 'Peaty',
    'CHALK', 'Chalk', 'Chalky'
);
