package nouveauareo;

/**
 * Une installation PostgreSQL présente sur le poste.
 */
public record ServeurPostgres(String version, String hote, int port, String utilisateur, String motDePasse) {

    public String urlJdbc(String base) {
        return "jdbc:postgresql://" + hote + ":" + port + "/" + base;
    }

    @Override
    public String toString() {
        return "PostgreSQL " + version + " (" + hote + ":" + port + ")";
    }
}
