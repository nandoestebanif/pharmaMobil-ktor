-- =====================================================================
-- V4 (perfil h2): datos de ejemplo para probar la app sin Oracle.
-- La base es en memoria: se vuelve a llenar en cada arranque.
-- =====================================================================

insert into categorias (nombre, descripcion, estado, fecha_creacion) values
  ('Analgesicos', 'Alivio del dolor y la fiebre', true, current_timestamp),
  ('Antibioticos', 'Tratamiento de infecciones bacterianas', true, current_timestamp),
  ('Antialergicos', 'Control de reacciones alergicas', true, current_timestamp),
  ('Vitaminas', 'Suplementos vitaminicos', true, current_timestamp);

insert into productos (nombre, precio, stock, estado, categoria_id, fecha_creacion) values
  ('Paracetamol 500mg',   3.50, 120, true,  1, current_timestamp),
  ('Ibuprofeno 400mg',    5.20,  80, true,  1, current_timestamp),
  ('Naproxeno 550mg',     7.80,   6, true,  1, current_timestamp),
  ('Amoxicilina 500mg',   8.90,  45, true,  2, current_timestamp),
  ('Azitromicina 500mg', 18.50,   3, true,  2, current_timestamp),
  ('Loratadina 10mg',     2.40,  60, true,  3, current_timestamp),
  ('Cetirizina 10mg',     3.10,   0, true,  3, current_timestamp),
  ('Vitamina C 1g',      12.00,  35, true,  4, current_timestamp),
  ('Complejo B',          9.60,  25, true,  4, current_timestamp),
  ('Clorfenamina 4mg',    1.20,  15, false, 3, current_timestamp);

insert into clientes (dni, nombres, apellidos, email, telefono, direccion, estado, fecha_creacion) values
  ('45678912', 'Maria', 'Torres Rojas', 'maria.torres@correo.pe', '988776655', 'Av. Los Olivos 123, Lima', true, current_timestamp),
  ('46789123', 'Pedro', 'Salazar Rios', 'pedro.salazar@correo.pe', '977665544', 'Jr. Union 456, Lima', true, current_timestamp);
