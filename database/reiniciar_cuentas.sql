-- Borra TODAS las cuentas y lo que depende de ellas (pedidos, mensajes, auditoria).
-- Los productos, categorias y estados no se tocan.
-- Al reiniciar la app se vuelve a crear el administrador con su clave y su Authenticator nuevos.
DELETE FROM detalle_pedido;
DELETE FROM pedido;
DELETE FROM mensaje;
DELETE FROM auditoria;
DELETE FROM usuario;
