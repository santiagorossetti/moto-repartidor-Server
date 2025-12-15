package com.motorepartidor.screens;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.utils.Null;
import com.motorepartidor.Main;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.math.Rectangle;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.motorepartidor.Main;
import com.motorepartidor.audio.AudioManager;
import com.motorepartidor.entities.Jugador;
import com.motorepartidor.entities.components.PlayerController;
import com.motorepartidor.input.GameInputProcessor;
import com.motorepartidor.ui.HUD;
import com.motorepartidor.ui.DeliveryIndicator;
import com.badlogic.gdx.graphics.Color;
import jdk.internal.org.jline.terminal.TerminalBuilder;
import red.gameController;
import red.hiloServidor;

public class GameScreen implements Screen , gameController {

    private Game game;
    private boolean initialized = false;
    private String chosenSpritePath;
    private String chosenSpritePath2;

    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;

    private OrthographicCamera camera1;
    private Viewport viewport1;
    private OrthographicCamera camera2;
    private Viewport viewport2;

    private HUD hud;

    private TiledMap tiledMap;
    private OrthogonalTiledMapRenderer tiledMapRenderer;
    private MapLayer collisionLayer;
    private MapLayer gasLayer;
    private MapLayer dealerLayer;
    private MapLayer entregasLayer;
    private final List<Rectangle> dealerAreas = new ArrayList<>();
    private final List<Rectangle> entregaAreas = new ArrayList<>();
    public hiloServidor servidor;




    private AudioManager audio;

    private int[] lastHint = new int[]{-1, -1};
    private final boolean[] lastGas = new boolean[]{false, false};

    // Jugadores como array
    private Jugador[] jugadores = new Jugador[2];
    private final boolean[] playerInGasArea = new boolean[2];
    private final boolean[] nearDealer = new boolean[2];
    private final boolean[] nearDrop = new boolean[2];
    private boolean playersAreColliding = false;
    private boolean[] playerIsCollidingObstacle = new boolean[2];

    private GameInputProcessor[] inputProcessors = new GameInputProcessor[2];;

    boolean[] eKeyHandled = new boolean[2];

    boolean[] gKeyHandled = new boolean[2];

    private static final String DEFAULT_SPRITE_PATH = "sprites/sprite.png";
    private static final String DEFAULT_SPRITE_PATH2 = "sprites/sprite2.png";



    private DeliveryIndicator p1Indicator = new DeliveryIndicator();
    private DeliveryIndicator p2Indicator = new DeliveryIndicator();


    private boolean matchEnding = false;
    private static final float UNIT_SCALE = 1 / 64f;
    private static final float VIRTUAL_WIDTH = 20f;
    private static final float VIRTUAL_HEIGHT = 15f;



    @Override
    public void interactuar(int IdJugador ,int tecla) {

        if (IdJugador < 0 || IdJugador >= inputProcessors.length) return;

        boolean presionado = tecla >= 0;
        int key = Math.abs(tecla);

        inputProcessors[IdJugador].procesarInput(key, presionado);



    }



    // Estado por jugador
    public static class ActiveDelivery {
        Rectangle target;
        boolean dangerous;
        int reward;


    }
    private ActiveDelivery p1Delivery = null;
    private ActiveDelivery p2Delivery = null;




    private final Random rng = new Random();

    public GameScreen(Game game, AudioManager audio , hiloServidor servidor) {
        this.game = game;
        this.audio = audio;
        this.chosenSpritePath = DEFAULT_SPRITE_PATH;
        this.chosenSpritePath2 = DEFAULT_SPRITE_PATH2;
        this.servidor = servidor;
    }

    @Override
    public void show() {

        if (initialized) {


            return;
        }

        // ESTO SOLO SE EJECUTA LA PRIMERA VEZ:

        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();

        hud = new HUD();


        try {
            if (this.audio != null) {
                this.audio.playMusic("audio/song.mp3", true, 0.1f);
            }
        } catch (Exception e) {
            Gdx.app.error("GameScreen", "Error al reproducir música de juego: audio/song.mp3", e);
        }

        tiledMap = new TmxMapLoader().load("map/Map.tmx");
        tiledMapRenderer = new OrthogonalTiledMapRenderer(tiledMap, UNIT_SCALE);

        collisionLayer = tiledMap.getLayers().get("colisiones");
        if (collisionLayer == null) {
            Gdx.app.error("GameScreen", "¡ERROR! Capa 'colisiones' no encontrada en el mapa.");
        }

        gasLayer = tiledMap.getLayers().get("Gasolina");
        if (gasLayer == null) {
            Gdx.app.error("GameScreen", "¡ERROR! Capa 'Gasolina' no encontrada en el mapa.");
        }

        // Ahora se crea el Jugador después de que las capas del mapa están cargadas.
        jugadores[0] = new Jugador(chosenSpritePath, 18, 36, new Vector2(1700, 500), collisionLayer , this , 0 );
        jugadores[1] = new Jugador(chosenSpritePath2, 18, 36, new Vector2(1700, 450), collisionLayer , this , 1);

        camera1 = new OrthographicCamera();
        viewport1 = new FitViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT, camera1);

        camera2 = new OrthographicCamera();
        viewport2 = new FitViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT, camera2);

        inputProcessors[0] = new GameInputProcessor();
        inputProcessors[1] = new GameInputProcessor();




        dealerLayer = tiledMap.getLayers().get("dealer");
        if (dealerLayer == null) {
            Gdx.app.error("GameScreen", "¡ERROR! Capa 'dealer' no encontrada en el mapa.");
        } else {
            for (MapObject obj : dealerLayer.getObjects()) {
                if (obj instanceof RectangleMapObject) {
                    dealerAreas.add(((RectangleMapObject) obj).getRectangle());
                }
            }
        }

        entregasLayer = tiledMap.getLayers().get("entregas");
        if (entregasLayer == null) {
            Gdx.app.error("GameScreen", "¡ERROR! Capa 'entregas' no encontrada en el mapa.");
        } else {
            for (MapObject obj : entregasLayer.getObjects()) {
                    if (obj instanceof RectangleMapObject) {
                    entregaAreas.add(((RectangleMapObject) obj).getRectangle());
                }
            }
        }

        p1Indicator.setColor(Color.CYAN);
        p2Indicator.setColor(Color.MAGENTA);

        // 2. Marcamos como inicializado para evitar que se ejecute de nuevo.
        initialized = true;
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // --- GASOLINA / INPUT ---
            playerInGasArea[0] = checkPlayerInGasArea(jugadores[0]);
            playerInGasArea[1] = checkPlayerInGasArea(jugadores[1]);

        for (int i = 0; i < 2; i++) {
            if (playerInGasArea[i] != lastGas[i]) {
                lastGas[i] = playerInGasArea[i];
                servidor.enviarGasHint(i, playerInGasArea[i] ? 1 : 0);
            }
        }


        for (int i = 0; i < jugadores.length; i++) {
            GameInputProcessor ip = inputProcessors[i];

            if (playerInGasArea[i] && ip.isEPressed() && !eKeyHandled[i]) {
                int falta = 100 - (int) jugadores[i].getGasolina();
                jugadores[i].restarDinero(falta);
                jugadores[i].recargarGasolina(100);

                enviarDinero(jugadores[i].getDinero(), i);
                enviarNafta(jugadores[i].getGasolina(), i);

                eKeyHandled[i] = true;
            } else if (!ip.isEPressed()) {
                eKeyHandled[i] = false;
            }
        }


        // --- PROXIMIDAD A ZONAS ---
        nearDealer[0] = isInAny(jugadores[0],dealerAreas);
        nearDealer[1] = isInAny(jugadores[1],dealerAreas);


        nearDrop[0]   = (p1Delivery != null) && jugadores[0].getBounds().overlaps(p1Delivery.target);
        nearDrop[1]   = (p2Delivery != null) && jugadores[1].getBounds().overlaps(p2Delivery.target);

        // --- HINT JUGADOR 0 ---
        int hint0 = 0;
        if (p1Delivery == null && nearDealer[0]) {
            hint0 = 1; // Aceptar pedido
        } else if (p1Delivery != null && nearDrop[0]) {
            hint0 = 2; // Entregar pedido
        }
        if (hint0 != lastHint[0]) {
            lastHint[0] = hint0;
            servidor.enviarHint(0, hint0);
        }

// --- HINT JUGADOR 1 ---
        int hint1 = 0;
        if (p2Delivery == null && nearDealer[1]) {
            hint1 = 1;
        } else if (p2Delivery != null && nearDrop[1]) {
            hint1 = 2;
        }
        if (hint1 != lastHint[1]) {
            lastHint[1] = hint1;
            servidor.enviarHint(1, hint1);
        }



        // Detecta si se presiona la G (por jugador)
        for (int i = 0; i < 2; i++) {
            GameInputProcessor ip = inputProcessors[i];

            if (ip.isGPressed()) {
                if (!gKeyHandled[i]) {

                    if (i == 0) {
                        // --- PLAYER 0 ---
                        if (p1Delivery == null && nearDealer[0]) {
                            p1Delivery = createDelivery();
                            if (p1Delivery != null) {
                                servidor.enviarUbicacionDelivery(
                                    p1Delivery.target, p1Delivery.dangerous, p1Delivery.reward, 0
                                );
                                if (this.audio != null) this.audio.playSound("audio/pickup.wav", 1f);
                            }
                        } else if (p1Delivery != null && nearDrop[0]) {
                            jugadores[0].sumarDinero(p1Delivery.reward);
                            enviarDinero(jugadores[0].getDinero(), 0);
                            servidor.enviarFinDelivery(0);
                            if (this.audio != null) this.audio.playSound("audio/deliver.wav", 1f);
                            p1Delivery = null;
                        }

                    } else if (i == 1) {
                        // --- PLAYER 1 ---
                        if (p2Delivery == null && nearDealer[1]) {
                            p2Delivery = createDelivery();
                            if (p2Delivery != null) {
                                servidor.enviarUbicacionDelivery(
                                    p2Delivery.target, p2Delivery.dangerous, p2Delivery.reward, 1
                                );
                                if (this.audio != null) this.audio.playSound("audio/pickup.wav", 1f);
                            }
                        } else if (p2Delivery != null && nearDrop[1]) {
                            jugadores[1].sumarDinero(p2Delivery.reward);
                            enviarDinero(jugadores[1].getDinero(), 1);
                            servidor.enviarFinDelivery(1);
                            if (this.audio != null) this.audio.playSound("audio/deliver.wav", 1f);
                            p2Delivery = null;
                        }
                    }

                    gKeyHandled[i] = true;
                }
            } else {
                gKeyHandled[i] = false;
            }
        }




        // Centro del destino -> ESCALADO a mundo
        if (p1Delivery != null && p1Delivery.target != null) {
            float cx = (p1Delivery.target.x + p1Delivery.target.width  * 0.5f) * UNIT_SCALE;
            float cy = (p1Delivery.target.y + p1Delivery.target.height * 0.5f) * UNIT_SCALE;
            p1Indicator.setTarget(cx, cy);
        } else {
            p1Indicator.clearTarget();
        }

        if (p2Delivery != null && p2Delivery.target != null) {
            float cx = (p2Delivery.target.x + p2Delivery.target.width  * 0.5f) * UNIT_SCALE;
            float cy = (p2Delivery.target.y + p2Delivery.target.height * 0.5f) * UNIT_SCALE;
            p2Indicator.setTarget(cx, cy);
        } else {
            p2Indicator.clearTarget();
        }

        // --- UPDATE JUGADORES ---
        for (int i = 0; i < jugadores.length; i++) {
            final GameInputProcessor ip = inputProcessors[i];

            jugadores[i].update(new PlayerController.PlayerInput() {
                @Override public boolean accelerate() { return ip.isUpPressed(); }
                @Override public boolean brake()      { return ip.isDownPressed(); }
                @Override public boolean turnLeft()   { return ip.isLeftPressed(); }
                @Override public boolean turnRight()  { return ip.isRightPressed(); }
            }, delta);
        }


        // --- COLISIONES / DAÑO ---
        boolean[] playerCollidingWithObstacle = new boolean[2];
        for(int i = 0; i < jugadores.length; i++) {
            playerCollidingWithObstacle[i] = checkPolygonCollisions(jugadores[i].getPolygon());
            if(playerCollidingWithObstacle[i] && !playerIsCollidingObstacle[i]) {
                jugadores[i].restarVida(1);
                enviarVida(jugadores[i].getVida() , i);
                playerIsCollidingObstacle[i] = true;
            }
            else if (!playerCollidingWithObstacle[i]) {
                playerIsCollidingObstacle[i] = false;
            }
        }

        if (Intersector.overlapConvexPolygons(jugadores[0].getPolygon(), jugadores[1].getPolygon())) {
            if (!playersAreColliding) {
                Gdx.app.log("GameScreen", "¡Colisión entre jugadores detectada!");
                jugadores[0].restarVida(10);
                jugadores[1].restarVida(10);
                enviarVida(jugadores[0].getVida() , 0);
                enviarVida(jugadores[1].getVida() , 1);
                playersAreColliding = true;
            }
        } else {
            playersAreColliding = false;
        }

        checkEndMatch();



        // --- RENDER SPLIT-SCREEN ---
        int halfW = Gdx.graphics.getWidth() / 2;
        int h = Gdx.graphics.getHeight();

        // ===== VIEWPORT IZQUIERDO (P1) =====
        Gdx.gl.glViewport(0, 0, halfW, h);

        // cámara 1 sigue a P1
        camera1.position.set(
            jugadores[0].getPosicion().x * UNIT_SCALE,
            jugadores[0].getPosicion().y * UNIT_SCALE,
            0f
        );
        camera1.update();

        tiledMapRenderer.setView(camera1);
        tiledMapRenderer.render();

        batch.setProjectionMatrix(camera1.combined);
        batch.begin();
        // si querés solo P1 acá, podés dejar solo jugador1
        jugadores[0].dibujar(batch);
        jugadores[1].dibujar(batch);
        batch.end();

        // Indicador P1 en su mitad
        float p1x = (jugadores[0].getBounds().x + jugadores[0].getBounds().width  * 1.5f) * UNIT_SCALE;
        float p1y = (jugadores[0].getBounds().y + jugadores[0].getBounds().height * 1.5f) * UNIT_SCALE;
        p1Indicator.renderWorld(p1x, p1y, camera1, delta);




        // ===== VIEWPORT DERECHO (P2) =====
        Gdx.gl.glViewport(halfW, 0, halfW, h);

        // cámara 2 sigue a P2
        camera2.position.set(
            jugadores[1].getPosicion().x * UNIT_SCALE,
            jugadores[1].getPosicion().y * UNIT_SCALE,
            0f
        );
        camera2.update();

        tiledMapRenderer.setView(camera2);
        tiledMapRenderer.render();

        batch.setProjectionMatrix(camera2.combined);
        batch.begin();
        // si querés solo P2 acá, podés dejar solo jugador2
        jugadores[0].dibujar(batch);
        jugadores[1].dibujar(batch);
        batch.end();

        // Indicador P2 en su mitad
        float p2x = (jugadores[1].getBounds().x + jugadores[1].getBounds().width  * 1.5f) * UNIT_SCALE;
        float p2y = (jugadores[1].getBounds().y + jugadores[1].getBounds().height * 1.5f) * UNIT_SCALE;
        p2Indicator.renderWorld(p2x, p2y, camera2, delta);

        // ===== VOLVER A PANTALLA COMPLETA (UI / DIVISOR) =====
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        // Línea roja central
        shapeRenderer.setProjectionMatrix(new Matrix4().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(1, 0, 0, 1);
        shapeRenderer.rect(Gdx.graphics.getWidth() / 2f - 2, 0, 4, Gdx.graphics.getHeight());
        shapeRenderer.end();

        // --- HUD ---
        hud.setP1NearDealer(nearDealer[0]);
        hud.setP2NearDealer(nearDealer[1]);
        hud.setP1NearDrop(nearDrop[0]);
        hud.setP2NearDrop(nearDrop[1]);
        hud.setDeliveryStatus1(
            (p1Delivery == null) ? "Pedido: ninguno"
                : (p1Delivery.dangerous ? "Pedido: PELIGROSO $" + p1Delivery.reward
                : "Pedido: Normal $" + p1Delivery.reward)
        );
        hud.setDeliveryStatus2(
            (p2Delivery == null) ? "Pedido: ninguno"
                : (p2Delivery.dangerous ? "Pedido: PELIGROSO $" + p2Delivery.reward
                : "Pedido: Normal $" + p2Delivery.reward)
        );
        hud.render(jugadores[0], jugadores[1], playerInGasArea[0], playerInGasArea[1]);


        if (jugadores[0] != null || jugadores[1] != null) {
            this.servidor.enviarPosicion(jugadores[0].getPosicion(), jugadores[1].getPosicion() ,jugadores[0].getAngulo() , jugadores[1].getAngulo());
        }
    }


    private boolean checkPlayerInGasArea(Jugador jugador) {
        if (gasLayer == null) return false;

        for (MapObject object : gasLayer.getObjects()) {
            if (object instanceof RectangleMapObject) {
                Rectangle gasRect = ((RectangleMapObject) object).getRectangle();
                if (jugador.getBounds().overlaps(gasRect)) {
                    return true;
                }
            }
        }
        return false;
    }

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

    private boolean isInAny(Jugador jugador, List<Rectangle> zonas) {
        Rectangle b = jugador.getBounds();
        for (Rectangle r : zonas) {
            if (b.overlaps(r)) return true;
        }
        return false;
    }

    private Rectangle randomEntrega() {
        if (entregaAreas.isEmpty()) return null;
        return entregaAreas.get(rng.nextInt(entregaAreas.size()));
    }

    public ActiveDelivery createDelivery() {
        ActiveDelivery d = new ActiveDelivery();
        d.target = randomEntrega();
        if (d.target == null) return null;
        d.dangerous = rng.nextFloat() < 0.25f; // 25% peligroso
        d.reward = d.dangerous ? 130 : 65;

        return d;
    }

    private void checkEndMatch() {
        if (jugadores == null || jugadores.length < 2) return;
        if (matchEnding) return;

        int vida1 = jugadores[0].getVida();
        int vida2 = jugadores[1].getVida();
        int plata1 = jugadores[0].getDinero();
        int plata2 = jugadores[1].getDinero();

        // Sigue jugando
        if ((vida1 > 0 && vida2 > 0) && (plata1 < 1000 && plata2 < 1000)) return;

        matchEnding = true;

        int winnerIndex;

        if ((vida1 <= 0 && vida2 <= 0) || (plata1 >= 1000 && plata2 >= 1000)) {
            winnerIndex = 3; // empate
        } else if (vida1 <= 0 || plata2 >= 1000) {
            winnerIndex = 2; // gana J2
        } else {
            winnerIndex = 1; // gana J1
        }

        endMatch(winnerIndex);
    }

    private void endMatch(int winnerIndex) {
        // 1) Avisar resultado
        servidor.enviarGameOver(winnerIndex);

        // 2) (Recomendado) avisar reset a clientes para limpiar HUD/delivery/hints
        servidor.enviarGlobal("Reset");

        // 3) Resetear el estado del server en el hilo de LibGDX (seguro)
        Gdx.app.postRunnable(() -> {
            onResetMatch();
            matchEnding = false;
        });
    }


    @Override
    public void onResetMatch() {
        // --- reset posiciones / ángulos ---
        jugadores[0].setPosicionInicial(new Vector2(1700, 500));
        jugadores[1].setPosicionInicial(new Vector2(1700, 450));
        jugadores[0].setAngulo(0f);
        jugadores[1].setAngulo(0f);

        // --- reset stats ---
        jugadores[0].setVida(100);
        jugadores[1].setVida(100);
        jugadores[0].setDinero(0);
        jugadores[1].setDinero(0);
        jugadores[0].setGasolina(100);
        jugadores[1].setGasolina(100);

        // --- reset deliveries ---
        p1Delivery = null;
        p2Delivery = null;

        // --- reset hints ---
        lastHint[0] = -1;
        lastHint[1] = -1;
        servidor.enviarHint(0, 0);
        servidor.enviarHint(1, 0);

        // --- reset flags colisiones ---
        playersAreColliding = false;
        playerIsCollidingObstacle[0] = false;
        playerIsCollidingObstacle[1] = false;

        // --- MUY IMPORTANTE: reset inputs (evita teclas pegadas) ---
        inputProcessors[0] = new GameInputProcessor();
        inputProcessors[1] = new GameInputProcessor();

        // --- reenviar stats a clientes ---
        enviarVida(jugadores[0].getVida(), 0);
        enviarVida(jugadores[1].getVida(), 1);
        enviarDinero(jugadores[0].getDinero(), 0);
        enviarDinero(jugadores[1].getDinero(), 1);
        enviarNafta(jugadores[0].getGasolina(), 0);
        enviarNafta(jugadores[1].getGasolina(), 1);
    }




    @Override
    public void resize(int width, int height) {
        viewport1.update(width / 2, height, false);
        viewport2.update(width / 2, height, false);
        hud.resize(width, height);
    }

    @Override public void pause() { }
    @Override public void resume() { }

    @Override
    public void hide() {
        Gdx.input.setInputProcessor(null);
        // Opcional: podés parar la música al salir de la pantalla si querés:
        // if (game.getAudio() != null) game.getAudio().stopMusic();
    }

    @Override
    public void dispose() {
        batch.dispose();
        tiledMap.dispose();
        tiledMapRenderer.dispose();
        for(int i = 0; i < jugadores.length; i++) {
            jugadores[i].dispose();
        }
        shapeRenderer.dispose();
        p1Indicator.dispose();
        p2Indicator.dispose();
        hud.dispose();
    }

    //Solo para enviar al cliente que si existe, comentar una vez finalizadas las pruebas


    @Override
    public void enviarNafta(float gas, int id) {

        if (id < 0 || id >= servidor.clientes.length) return;
        if (servidor.clientes[id] == null) return;

        int puerto = servidor.clientes[id].getPort();
        InetAddress ip = servidor.clientes[id].getIp();

        servidor.enviarGas(gas, id, ip, puerto);
    }



    @Override
    public void enviarDinero(int dinero, int id) {

        if (id < 0 || id >= servidor.clientes.length) return;
        if (servidor.clientes[id] == null) return;

        int puerto = servidor.clientes[id].getPort();
        InetAddress ip = servidor.clientes[id].getIp();

        servidor.enviarDinero(dinero, id, ip, puerto);
    }

    @Override
    public void enviarVida(int vida, int id) {

        if (id < 0 || id >= servidor.clientes.length) return;
        if (servidor.clientes[id] == null) return;

        int puerto = servidor.clientes[id].getPort();
        InetAddress ip = servidor.clientes[id].getIp();

        servidor.enviarVida(vida, id, ip, puerto);
    }


}
