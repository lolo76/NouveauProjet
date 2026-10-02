package nouveauProjet;

/**
 * Point d'entrée du jar exécutable : une classe qui n'étend pas Application
 * permet de lancer JavaFX depuis le classpath (java -jar).
 */
public final class Launcher {

    private Launcher() {
    }

    public static void main(String[] args) {
        App.main(args);
    }
}
