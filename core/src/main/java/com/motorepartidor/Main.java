package com.motorepartidor;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.motorepartidor.audio.AudioManager;
import com.motorepartidor.screens.GameScreen;
import red.hiloServidor;

/*

  Ahora mantiene un único AudioManager compartido.
*/
public class Main extends Game {


    private GameScreen gameScreen;

    private AudioManager audio; // AudioManager global


    hiloServidor server = new hiloServidor();



    @Override
    public void create() {
        // Inicializar el AudioManager una sola vez
        audio = new AudioManager();

        server.start();

        // Pantalla inicial
        GameScreen gameScreen  = new GameScreen(this, this.audio , this.server);
        this.server.setGameController(gameScreen);
        setScreen(gameScreen);
    }

    /** Acceso global al AudioManager. */
    public AudioManager getAudio() {
        return audio;
    }



    @Override
    public void dispose() {
        server.terminarServidor();

        super.dispose();
        //if (mainMenuScreen != null) mainMenuScreen.dispose();
        if (gameScreen != null) gameScreen.dispose();

        // Liberar el audio global al cerrar el juego
        if (audio != null) {
            try {
                audio.dispose();
            } catch (Exception e) {
                Gdx.app.error("Main", "Error liberando AudioManager", e);
            }
        }
    }



}
