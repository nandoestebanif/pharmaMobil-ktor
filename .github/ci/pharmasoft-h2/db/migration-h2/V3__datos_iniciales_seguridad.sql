-- =====================================================================
-- V3 (perfil h2): mismos datos que db/migration/V3 (systimestamp -> current_timestamp)
--
-- Las contrasenias van cifradas con BCrypt (BCryptPasswordEncoder):
--   admin     -> Admin123
--   vendedor  -> Vendedor123
--   almacen   -> Almacen123
-- Cambiarlas despues del primer ingreso.
-- El ultimo empleado no tiene usuario (la relacion es opcional).
-- =====================================================================

insert into empleados (dni, nombres, apellidos, cargo, email, telefono, estado, fecha_creacion)
values ('70123456', 'Carlos', 'Ramirez Torres', 'Administrador', 'carlos.ramirez@pharma.pe', '987654321', true, current_timestamp);

insert into empleados (dni, nombres, apellidos, cargo, email, telefono, estado, fecha_creacion)
values ('71234567', 'Lucia', 'Fernandez Quispe', 'Vendedora', 'lucia.fernandez@pharma.pe', '987111222', true, current_timestamp);

insert into empleados (dni, nombres, apellidos, cargo, email, telefono, estado, fecha_creacion)
values ('72345678', 'Jorge', 'Mendoza Huaman', 'Almacenero', 'jorge.mendoza@pharma.pe', '987333444', true, current_timestamp);

insert into empleados (dni, nombres, apellidos, cargo, email, telefono, estado, fecha_creacion)
values ('73456789', 'Rosa', 'Castillo Vargas', 'Quimica farmaceutica', 'rosa.castillo@pharma.pe', null, true, current_timestamp);

-- Usuarios (se enlazan al empleado por DNI)
insert into usuarios (username, password, estado, empleado_id, fecha_creacion)
select 'admin', '$2a$10$yAlQiMu80YDvbdFlElZ5aOLQa/EA2WT05zww41NXklYWcHtw/Ou1O', true, id, current_timestamp
from empleados where dni = '70123456';

insert into usuarios (username, password, estado, empleado_id, fecha_creacion)
select 'vendedor', '$2a$10$/zBrjcNCd6C9p3RZIUAbAeCObkBaoygc2GIYn.WZ1gDyM2uJUQHhq', true, id, current_timestamp
from empleados where dni = '71234567';

insert into usuarios (username, password, estado, empleado_id, fecha_creacion)
select 'almacen', '$2a$10$OMCdP3idpWxXD6oHD2lXge0M3x1qo/Nz4T43ko9wFgLaeyVwerGbm', true, id, current_timestamp
from empleados where dni = '72345678';

-- Roles de cada usuario
insert into usuario_roles (usuario_id, rol_id)
select u.id, r.id from usuarios u, roles r where u.username = 'admin' and r.nombre = 'ADMIN';

insert into usuario_roles (usuario_id, rol_id)
select u.id, r.id from usuarios u, roles r where u.username = 'vendedor' and r.nombre = 'VENDEDOR';

insert into usuario_roles (usuario_id, rol_id)
select u.id, r.id from usuarios u, roles r where u.username = 'almacen' and r.nombre = 'ALMACENERO';
