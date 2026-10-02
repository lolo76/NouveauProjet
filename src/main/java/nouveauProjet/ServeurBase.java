package nouveauProjet;

/**
 * Un serveur de base de données (PostgreSQL, Oracle, MySQL ou SQL Server) présent sur le poste ou le réseau.
 * {@code base} est la base (ou le service Oracle) utilisée pour se connecter au serveur.
 */
public record ServeurBase(TypeBase type, String version, String hote, int port, String base, String utilisateur,
        String motDePasse) {

    public String urlJdbc() {
        return type.urlJdbc(hote, port, base);
    }

    @Override
    public String toString() {
        return type + " " + version + " (" + hote + ":" + port + ")";
    }
}
