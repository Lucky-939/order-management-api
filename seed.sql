-- A primitive long version is a NOT NULL column with no default, so a seed without it fails
INSERT INTO products (id, name, price, stock, version) VALUES (1, 'Mechanical Keyboard', 89.99, 10, 0);
INSERT INTO products (id, name, price, stock, version) VALUES (2, 'Wireless Mouse', 29.50, 5, 0);
INSERT INTO products (id, name, price, stock, version) VALUES (3, 'USB-C Charging Cable', 12.00, 1, 0);
INSERT INTO products (id, name, price, stock, version) VALUES (4, 'Noise-Cancelling Headphones', 199.99, 15, 0);
