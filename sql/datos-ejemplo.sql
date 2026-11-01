-- Datos opcionales de demostración (ejecutar DESPUÉS de lab6.sql).
USE lab6;

INSERT INTO customer (name, document, document_type) VALUES
    ('Juan Pérez Quispe', '12345678', 'DNI'),
    ('María Flores Rojas', '87654321', 'DNI'),
    ('Comercial Andina S.A.C.', '20123456789', 'RUC'),
    ('Distribuidora Sol del Perú E.I.R.L.', '20987654321', 'RUC');

INSERT INTO product (name, price, stock) VALUES
    ('Arroz extra 5 kg', 24.90, 120),
    ('Aceite vegetal 1 L', 9.50, 200),
    ('Azúcar rubia 1 kg', 4.20, 150),
    ('Leche evaporada (caja x 24)', 96.00, 40);
