package nouveauareo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * Crée la base PostgreSQL et l'arborescence d'un nouveau projet.
 */
public final class CreateurProjet {

    public static final List<String> SOUS_DOSSIERS = List.of("telechargements", "stockageVisites", "_dumps_");

    private static final Pattern NOM_VALIDE = Pattern.compile("[A-Za-z0-9_-]+");
    private static final int LONGUEUR_MAX_NOM_BASE = 63;

    private CreateurProjet() {
    }

    public static String nomBase(String produit, String projet) {
        return produit + "_" + projet + "_2";
    }

    /** Retourne un message d'erreur, ou null si le nom est utilisable. */
    public static String verifierNom(String libelle, String valeur) {
        if (valeur == null || valeur.isBlank()) {
            return libelle + " est obligatoire.";
        }
        if (!NOM_VALIDE.matcher(valeur).matches()) {
            return libelle + " ne doit contenir que des lettres sans accent, des chiffres, _ ou -.";
        }
        return null;
    }

    public static String tester(ServeurPostgres serveur) throws SQLException {
        try (Connection c = connecter(serveur);
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SHOW server_version")) {
            rs.next();
            return rs.getString(1);
        }
    }

    public static void creerBase(ServeurPostgres serveur, String nomBase) throws SQLException {
        if (nomBase.length() > LONGUEUR_MAX_NOM_BASE) {
            throw new SQLException("Le nom de base dépasse " + LONGUEUR_MAX_NOM_BASE + " caractères : " + nomBase);
        }
        try (Connection c = connecter(serveur)) {
            try (PreparedStatement ps = c.prepareStatement("SELECT 1 FROM pg_database WHERE datname = ?")) {
                ps.setString(1, nomBase);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        throw new SQLException("La base " + nomBase + " existe déjà sur " + serveur + ".");
                    }
                }
            }
            // template0 est obligatoire pour imposer un codage et une collation différents du modèle
            String sql = "CREATE DATABASE \"" + nomBase + "\""
                    + " WITH TEMPLATE = template0"
                    + " ENCODING = 'LATIN9'"
                    + " LC_COLLATE = 'C'"
                    + " LC_CTYPE = 'C'"
                    + " TABLESPACE = pg_default"
                    + " CONNECTION LIMIT = -1";
            try (Statement st = c.createStatement()) {
                st.execute(sql);
            }
        }
    }

    public static void creerDossiers(Path dossier) throws IOException {
        for (String sousDossier : SOUS_DOSSIERS) {
            Files.createDirectories(dossier.resolve(sousDossier));
        }
    }

    private static Connection connecter(ServeurPostgres serveur) throws SQLException {
        Properties infos = new Properties();
        infos.setProperty("user", serveur.utilisateur());
        // Sans mot de passe, le pilote le cherche dans %APPDATA%\postgresql\pgpass.conf
        if (serveur.motDePasse() != null && !serveur.motDePasse().isEmpty()) {
            infos.setProperty("password", serveur.motDePasse());
        }
        infos.setProperty("loginTimeout", "5");
        return DriverManager.getConnection(serveur.urlJdbc("postgres"), infos);
    }
}
