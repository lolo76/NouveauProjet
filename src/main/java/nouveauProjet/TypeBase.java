package nouveauProjet;

/**
 * Type de serveur de base de données géré par l'application.
 */
public enum TypeBase {

    POSTGRESQL("PostgreSQL", 5432, "postgres", "SHOW server_version"),
    ORACLE("Oracle", 1521, "XEPDB1", "SELECT banner FROM v$version FETCH FIRST 1 ROWS ONLY"),
    MYSQL("MySQL", 3306, "mysql", "SELECT VERSION()"),
    SQLSERVER("SQL Server", 1433, "master", "SELECT @@VERSION");

    private final String libelle;
    private final int portParDefaut;
    private final String baseParDefaut;
    private final String requeteVersion;

    TypeBase(String libelle, int portParDefaut, String baseParDefaut, String requeteVersion) {
        this.libelle = libelle;
        this.portParDefaut = portParDefaut;
        this.baseParDefaut = baseParDefaut;
        this.requeteVersion = requeteVersion;
    }

    public int portParDefaut() {
        return portParDefaut;
    }

    /** Base (ou service Oracle) à laquelle on se connecte pour administrer le serveur. */
    public String baseParDefaut() {
        return baseParDefaut;
    }

    public String requeteVersion() {
        return requeteVersion;
    }

    /** Libellé de la base de connexion : un service pour Oracle, une base pour les autres. */
    public String libelleBase() {
        return this == ORACLE ? "Service" : "Base";
    }

    public String urlJdbc(String hote, int port, String base) {
        return switch (this) {
            case POSTGRESQL -> "jdbc:postgresql://" + hote + ":" + port + "/" + base;
            case ORACLE -> "jdbc:oracle:thin:@//" + hote + ":" + port + "/" + base;
            case MYSQL -> "jdbc:mysql://" + hote + ":" + port + "/" + base;
            case SQLSERVER -> "jdbc:sqlserver://" + hote + ":" + port + ";databaseName=" + base
                    + ";encrypt=false;trustServerCertificate=true";
        };
    }

    public static TypeBase depuis(String valeur) {
        try {
            return valeur == null || valeur.isBlank() ? POSTGRESQL : valueOf(valeur.trim());
        } catch (IllegalArgumentException e) {
            return POSTGRESQL;
        }
    }

    @Override
    public String toString() {
        return libelle;
    }
}
