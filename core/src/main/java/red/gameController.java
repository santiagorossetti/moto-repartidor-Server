package red;

public interface gameController {

    void interactuar ( int IdJugador ,int tecla);
    void enviarNafta (float gas , int id);
    void enviarDinero (int dinero , int id);
    void enviarVida (int dinero , int id);
    void onResetMatch();


}
