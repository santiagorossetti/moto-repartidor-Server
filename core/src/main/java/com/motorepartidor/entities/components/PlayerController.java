package com.motorepartidor.entities.components;

import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.motorepartidor.entities.Jugador;
import red.hiloServidor;
import red.gameController;

public class PlayerController {

    public interface PlayerInput {
        boolean accelerate();
        boolean brake();
        boolean turnLeft();
        boolean turnRight();
    }

    // Parámetros de ajuste para el movimiento
    private static final float ACELERACION = 150f;
    private static final float FRENO = 600f;
    private static final float DESACELERACION_NATURAL = 400f;
    private static final float GIRO_VELOCIDAD = 160f;
    private static final float VELOCIDAD_MAXIMA = 250f;
    private static final float VELOCIDAD_MINIMA = -150f;
    private gameController juego;

    // Referencia a la capa de colisiones para su uso en este componente
    private MapLayer collisionLayer;

    // Constructor que recibe la capa de colisiones
    public PlayerController(MapLayer collisionLayer , gameController gameController ) {
        this.collisionLayer = collisionLayer;
        this.juego = gameController;

    }

    public void update(Jugador jugador, PlayerInput input, float dt) {

            float velocidadActual = jugador.getVelocidad();
            float anguloActual = jugador.getAngulo();
            float gasolinaActual = jugador.getGasolina();

            // Definimos una velocidad de "reserva" (por ejemplo, el 15% de la máxima)
            float VELOCIDAD_RESERVA = VELOCIDAD_MAXIMA * 0.15f;

            // 1. Rotación del jugador (Sin cambios)
            if (input.turnLeft()) {
                anguloActual += GIRO_VELOCIDAD * dt;
            }
            if (input.turnRight()) {
                anguloActual -= GIRO_VELOCIDAD * dt;
            }

            // 2. Aceleración / Freno
            if (input.accelerate()) {
                velocidadActual += ACELERACION * dt;
            } else if (input.brake()) {
                velocidadActual -= FRENO * dt;
            } else {
                // Desaceleración natural
                if (velocidadActual > 0) {
                    velocidadActual = Math.max(0, velocidadActual - DESACELERACION_NATURAL * dt);
                } else if (velocidadActual < 0) {
                    velocidadActual = Math.min(0, velocidadActual + DESACELERACION_NATURAL * dt);
                }
            }

            // --- NUEVA LÓGICA: Penalización por falta de gasolina ---
            if (gasolinaActual <= 0) {
                // Si vamos más rápido que la velocidad de reserva, desaceleramos forzosamente
                if (velocidadActual > VELOCIDAD_RESERVA) {
                    velocidadActual -= (DESACELERACION_NATURAL * 2) * dt; // Se frena más rápido hasta llegar al límite bajo
                }
                // Si intentamos acelerar, no dejamos pasar del límite de reserva
                if (velocidadActual > VELOCIDAD_RESERVA) {
                    velocidadActual = VELOCIDAD_RESERVA;
                }
            }
            // -------------------------------------------------------

            // 3. Clamps de velocidad normales
            if (velocidadActual > VELOCIDAD_MAXIMA) {
                velocidadActual = VELOCIDAD_MAXIMA;
            }
            if (velocidadActual < VELOCIDAD_MINIMA) {
                velocidadActual = VELOCIDAD_MINIMA;
            }

            // 4. Consumo de gasolina
            if (Math.abs(velocidadActual) > 0 && gasolinaActual > 0) { // Agregado && gasolina > 0 para no calcular si ya es 0
                float consumo = (Math.abs(velocidadActual) * 0.005f + (input.accelerate() ? 0.02f : 0f)) * dt;
                gasolinaActual -= consumo;
                if (gasolinaActual < 0) {
                    gasolinaActual = 0;
                }
                juego.enviarNafta(gasolinaActual , jugador.id);
            }

            // 5. Integración de posición y colisiones
            // CAMBIO: Quitamos el "if (gasolinaActual > 0)" que envolvía todo este bloque.
            // Ahora el movimiento ocurre siempre, pero la velocidad ya fue limitada arriba.

            float rad = (float) Math.toRadians(anguloActual + 90);
            Vector2 movimiento = new Vector2((float) Math.cos(rad), (float) Math.sin(rad)).scl(velocidadActual * dt);

            Vector2 oldPos = new Vector2(jugador.getPosicion());

            // Probar movimiento en X
            jugador.getPosicion().x += movimiento.x;
            jugador.getPolygon().setPosition(jugador.getPosicion().x, jugador.getPosicion().y);
            jugador.getPolygon().setRotation(anguloActual);

            if (checkPolygonCollisions(jugador.getPolygon())) {
                jugador.getPosicion().x = oldPos.x;
            }

            // Probar movimiento en Y
            jugador.getPosicion().y += movimiento.y;
            jugador.getPolygon().setPosition(jugador.getPosicion().x, jugador.getPosicion().y);
            jugador.getPolygon().setRotation(anguloActual);

            if (checkPolygonCollisions(jugador.getPolygon())) {
                jugador.getPosicion().y = oldPos.y;
            }

            // 6. Actualizar las propiedades del jugador
            jugador.setAngulo(anguloActual);
            jugador.setVelocidad(velocidadActual);

            // Solo actualizamos la gasolina si cambió, aunque el setter debería manejarlo bien
            jugador.gastarGasolina(Math.max(0, jugador.getGasolina() - gasolinaActual)); // Aseguramos consistencia
            // Nota: Dependiendo de cómo funcione tu método gastarGasolina,
            // podrías necesitar simplemente: jugador.setGasolina(gasolinaActual);
        }

    // Método para detectar colisiones con los polígonos del mapa
    private boolean checkPolygonCollisions(Polygon polygon) {
        if (collisionLayer == null) return false;

        float[] playerVertices = polygon.getTransformedVertices();
        for (MapObject object : collisionLayer.getObjects()) {
            if (object instanceof RectangleMapObject) {
                Rectangle collisionRect = ((RectangleMapObject) object).getRectangle();

                if (!polygon.getBoundingRectangle().overlaps(collisionRect)) continue;

                for (int i = 0; i < playerVertices.length; i += 2) {
                    if (collisionRect.contains(playerVertices[i], playerVertices[i + 1])) return true;
                }

                float rectX = collisionRect.x;
                float rectY = collisionRect.y;
                float rectWidth = collisionRect.width;
                float rectHeight = collisionRect.height;

                if (polygon.contains(rectX, rectY) ||
                    polygon.contains(rectX + rectWidth, rectY) ||
                    polygon.contains(rectX + rectWidth, rectY + rectHeight) ||
                    polygon.contains(rectX, rectY + rectHeight)) return true;
            }
        }
        return false;
    }
}
