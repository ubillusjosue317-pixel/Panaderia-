-- Base de datos del sistema web de gestion de pedidos - Panaderia Piero Alonso
-- Modelo relacional de 7 tablas (ver seccion 5.2 del informe)
--
-- Nota: al ejecutar la aplicacion, Hibernate crea las tablas si no existen y
-- DatosIniciales carga roles, estados, categorias y el administrador.
-- Este script sirve para crear la BD manualmente con todas sus restricciones.

CREATE DATABASE IF NOT EXISTS panaderia_piero_alonso
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_spanish_ci;

USE panaderia_piero_alonso;

CREATE TABLE rol (
    id_rol      INT AUTO_INCREMENT PRIMARY KEY,
    nombre_rol  VARCHAR(30) NOT NULL UNIQUE
);

CREATE TABLE usuario (
    id_usuario      INT AUTO_INCREMENT PRIMARY KEY,
    id_rol          INT NOT NULL,
    nombre          VARCHAR(50)  NOT NULL,
    apellido        VARCHAR(50)  NOT NULL,
    email           VARCHAR(100) NOT NULL UNIQUE,
    telefono        VARCHAR(15),
    password        VARCHAR(255) NOT NULL,
    fecha_registro  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_usuario_rol FOREIGN KEY (id_rol) REFERENCES rol (id_rol)
);

CREATE TABLE categoria (
    id_categoria  INT AUTO_INCREMENT PRIMARY KEY,
    nombre        VARCHAR(50) NOT NULL,
    descripcion   TEXT
);

CREATE TABLE producto (
    id_producto   INT AUTO_INCREMENT PRIMARY KEY,
    id_categoria  INT NOT NULL,
    nombre        VARCHAR(100) NOT NULL,
    descripcion   TEXT,
    precio        DECIMAL(8,2) NOT NULL,
    imagen_url    VARCHAR(255),
    estado        BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_producto_categoria FOREIGN KEY (id_categoria) REFERENCES categoria (id_categoria),
    CONSTRAINT chk_producto_precio CHECK (precio >= 0)
);

CREATE TABLE estado_pedido (
    id_estado      INT AUTO_INCREMENT PRIMARY KEY,
    nombre_estado  VARCHAR(30) NOT NULL
);

CREATE TABLE pedido (
    id_pedido          INT AUTO_INCREMENT PRIMARY KEY,
    id_usuario         INT NOT NULL,
    id_estado          INT NOT NULL,
    fecha_pedido       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    total              DECIMAL(10,2) NOT NULL,
    tipo_entrega       VARCHAR(20) NOT NULL,
    direccion_entrega  VARCHAR(200),
    CONSTRAINT fk_pedido_usuario FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario),
    CONSTRAINT fk_pedido_estado  FOREIGN KEY (id_estado)  REFERENCES estado_pedido (id_estado),
    CONSTRAINT chk_pedido_total  CHECK (total >= 0)
);

CREATE TABLE detalle_pedido (
    id_detalle       INT AUTO_INCREMENT PRIMARY KEY,
    id_pedido        INT NOT NULL,
    id_producto      INT NOT NULL,
    cantidad         INT NOT NULL,
    precio_unitario  DECIMAL(8,2)  NOT NULL,
    subtotal         DECIMAL(10,2) NOT NULL,
    CONSTRAINT fk_detalle_pedido   FOREIGN KEY (id_pedido)   REFERENCES pedido (id_pedido),
    CONSTRAINT fk_detalle_producto FOREIGN KEY (id_producto) REFERENCES producto (id_producto),
    CONSTRAINT chk_detalle_cantidad CHECK (cantidad > 0)
);

-- Datos base
INSERT INTO rol (nombre_rol) VALUES ('ADMINISTRADOR'), ('CLIENTE');

INSERT INTO estado_pedido (nombre_estado)
VALUES ('Pendiente'), ('En preparación'), ('Listo'), ('Entregado'), ('Cancelado');

INSERT INTO categoria (nombre, descripcion) VALUES
    ('Panadería',  'Panes del día: francés, camote, yema, integral'),
    ('Pastelería', 'Tortas enteras y por porción'),
    ('Bollería',   'Croissants, cachitos y masas hojaldradas'),
    ('Salados',    'Empanadas, enrollados y bocaditos');
