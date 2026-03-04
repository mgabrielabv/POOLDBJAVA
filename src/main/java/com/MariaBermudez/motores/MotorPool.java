package com.MariaBermudez.motores;

import com.MariaBermudez.modelos.Ajustes;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class MotorPool implements EstrategiaConexion {
    private static volatile MotorPool instancia;
    private final BlockingQueue<ConnectionHolder> pool;
    private final List<ConnectionHolder> holders = new ArrayList<>();
    private final Ajustes ajustes;
    private final int maxPoolSize;
    private volatile boolean cerrado = false;

    private static class ConnectionHolder {
        volatile Connection real;
        final AtomicBoolean inUse = new AtomicBoolean(false);

        ConnectionHolder(Connection real) {
            this.real = real;
        }
    }

    // Constructor singleton
    private MotorPool(Ajustes ajustes) {
        this.ajustes = Objects.requireNonNull(ajustes);
        this.maxPoolSize = Math.max(1, ajustes.limitePool());
        this.pool = new ArrayBlockingQueue<>(this.maxPoolSize);

        // Prellenar el pool con holders
        for (int i = 0; i < this.maxPoolSize; i++) {
            try {
                Connection real = crearConexionReal();
                ConnectionHolder holder = new ConnectionHolder(real);
                holders.add(holder);
                pool.offer(holder);
            } catch (SQLException e) {
                // Si falla la creación, lanzamos una excepción en tiempo de inicialización
                throw new RuntimeException("Error creando conexiones del pool", e);
            }
        }
    }

    public static MotorPool getInstance(Ajustes ajustes) {
        if (instancia == null) {
            synchronized (MotorPool.class) {
                if (instancia == null) {
                    instancia = new MotorPool(ajustes);
                }
            }
        }
        return instancia;
    }

    private Connection crearConexionReal() throws SQLException {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException ignored) {
            // El driver puede ser cargado automáticamente por DriverManager
        }
        return DriverManager.getConnection(ajustes.url(), ajustes.usuario(), ajustes.clave());
    }

    private Connection crearProxyForHolder(final ConnectionHolder holder) {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                String name = method.getName();
                if ("close".equals(name)) {
                    // Intentar devolver el holder al pool solo si estaba en uso
                    if (holder.inUse.compareAndSet(true, false)) {
                        if (!cerrado) {
                            // intentar devolver al pool, si falla cerrar la conexión real
                            boolean offered = pool.offer(holder);
                            if (!offered) {
                                try {
                                    if (holder.real != null && !holder.real.isClosed()) {
                                        holder.real.close();
                                    }
                                } catch (Exception ignored) {
                                }
                            }
                        } else {
                            try {
                                if (holder.real != null && !holder.real.isClosed()) {
                                    holder.real.close();
                                }
                            } catch (Exception ignored) {
                            }
                        }
                    }
                    return null;
                }
                // Delegate el resto de métodos al real actual
                try {
                    Connection r = holder.real;
                    if (r == null) {
                        throw new SQLException("Conexión no disponible");
                    }
                    return method.invoke(r, args);
                } catch (Throwable t) {
                    throw t.getCause() == null ? t : t.getCause();
                }
            }
        };

        return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
                new Class[]{Connection.class}, handler);
    }

    @Override
    public Connection obtenerConexion() throws SQLException {
        if (cerrado) {
            throw new SQLException("Pool de conexiones cerrado");
        }
        try {
            ConnectionHolder holder = pool.poll(10, TimeUnit.SECONDS);
            if (holder == null) {
                throw new SQLException("Timeout: no hay conexiones disponibles en el pool");
            }

            // Marcar como en uso
            holder.inUse.set(true);

            // Validar la conexión real y reemplazar si está cerrada o inválida
            try {
                Connection real = holder.real;
                if (real == null || real.isClosed() || !real.isValid(2)) {
                    synchronized (holder) {
                        // verificar otra vez dentro del lock
                        Connection current = holder.real;
                        if (current == null || current.isClosed() || !current.isValid(2)) {
                            try {
                                if (current != null && !current.isClosed()) {
                                    current.close();
                                }
                            } catch (Exception ignored) {
                            }
                            Connection nueva = crearConexionReal();
                            holder.real = nueva;
                        }
                    }
                }
            } catch (SQLException sqle) {
                // Si la validación falla, intentar crear una nueva conexión
                synchronized (holder) {
                    try {
                        if (holder.real != null && !holder.real.isClosed()) {
                            holder.real.close();
                        }
                    } catch (Exception ignored) {
                    }
                    holder.real = crearConexionReal();
                }
            }

            // Crear y devolver un proxy que delega al holder
            return crearProxyForHolder(holder);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SQLException("Interrumpido al obtener conexión del pool", e);
        }
    }

    @Override
    public void cerrar() {
        cerrado = true;
        // Vaciar pool y cerrar las conexiones reales
        for (ConnectionHolder h : holders) {
            try {
                if (h.real != null && !h.real.isClosed()) {
                    h.real.close();
                }
            } catch (Exception ignored) {
            }
        }
        pool.clear();
        holders.clear();

        synchronized (MotorPool.class) {
            instancia = null;
        }
    }
}