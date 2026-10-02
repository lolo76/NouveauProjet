package nouveauProjet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Properties;
import java.util.Enumeration;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;

/**
 * Crée la base PostgreSQL et l'arborescence d'un nouveau projet.
 */
public final class CreateurProjet {

    public static final List<String> SOUS_DOSSIERS = List.of("telechargements", "stockageVisites", "_dumps_");

    private static final Pattern NOM_VALIDE = Pattern.compile("[A-Za-z0-9_-]+");
    private static final int LONGUEUR_MAX_NOM_BASE = 63;

    private CreateurProjet() {
    }

    /** Un projet dont le nom commence par CD est enregistré sous CG (base et dossier). */
    public static String nomPourBaseEtDossier(String projet) {
        if (projet.regionMatches(true, 0, "CD", 0, 2)) {
            return "CG" + projet.substring(2);
        }
        return projet;
    }

    public static String nomBase(String produit, String projet) {
        return produit + "_" + nomPourBaseEtDossier(projet) + "_2";
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

    public static String tester(ServeurBase serveur) throws SQLException {
        try (Connection c = connecter(serveur);
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(serveur.type().requeteVersion())) {
            rs.next();
            return rs.getString(1);
        }
    }

    public static void creerBase(ServeurBase serveur, String nomBase) throws SQLException {
        if (serveur.type() != TypeBase.POSTGRESQL) {
            throw new SQLException("La création de la base n'est gérée que pour PostgreSQL (serveur choisi : "
                    + serveur.type() + ").");
        }
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

    public static boolean estArchive(Path fichier) {
        String nom = fichier.getFileName().toString().toLowerCase();
        return nom.endsWith(".zip") || nom.endsWith(".7z");
    }

    /** Décompresse une archive .zip ou .7z dans le dossier de destination. */
    public static void decompresser(Path archive, Path destination, Consumer<String> journal) throws IOException {
        boolean sevenZ = archive.getFileName().toString().toLowerCase().endsWith(".7z");
        String type = sevenZ ? "7z" : "zip";
        journal.accept("Décompression [" + type + "] de " + archive.getFileName() + " dans " + destination + "…");
        Path racine = destination.toAbsolutePath().normalize();
        int nombre = sevenZ ? decompresser7z(archive, racine) : decompresserZip(archive, racine);
        journal.accept("Archive [" + type + "] " + archive.getFileName() + " décompressée : " + nombre
                + " fichier(s) extrait(s).");
    }

    private static int decompresser7z(Path archive, Path racine) throws IOException {
        int nombre = 0;
        try (SevenZFile sevenZ = SevenZFile.builder().setFile(archive.toFile()).get()) {
            SevenZArchiveEntry entree;
            while ((entree = sevenZ.getNextEntry()) != null) {
                Path cible = racine.resolve(entree.getName()).normalize();
                if (!cible.startsWith(racine)) {
                    throw new IOException("Entrée d'archive refusée (hors destination) : " + entree.getName());
                }
                if (entree.isDirectory()) {
                    Files.createDirectories(cible);
                } else {
                    Files.createDirectories(cible.getParent());
                    try (InputStream in = sevenZ.getInputStream(entree)) {
                        Files.copy(in, cible, StandardCopyOption.REPLACE_EXISTING);
                    }
                    nombre++;
                }
            }
        }
        return nombre;
    }

    private static int decompresserZip(Path archive, Path racine) throws IOException {
        int nombre = 0;
        try (ZipFile zip = new ZipFile(archive.toFile(), Charset.forName("windows-1252"))) {
            Enumeration<? extends ZipEntry> entrees = zip.entries();
            while (entrees.hasMoreElements()) {
                ZipEntry entree = entrees.nextElement();
                Path cible = racine.resolve(entree.getName()).normalize();
                if (!cible.startsWith(racine)) {
                    throw new IOException("Entrée d'archive refusée (hors destination) : " + entree.getName());
                }
                if (entree.isDirectory()) {
                    Files.createDirectories(cible);
                } else {
                    Files.createDirectories(cible.getParent());
                    try (InputStream in = zip.getInputStream(entree)) {
                        Files.copy(in, cible, StandardCopyOption.REPLACE_EXISTING);
                    }
                    nombre++;
                }
            }
        }
        return nombre;
    }

    /** Mémoire réservée à la JVM de restauration. */
    private static final String OPTIONS_MEMOIRE = "-Xms6g -Xmx6g";

    private static final String COMMANDE_RESTAURATION = "%JAVA_HOME%\\bin\\java.exe " + OPTIONS_MEMOIRE
            + " -classpath %CP% com.netisys.commun.moulinettes.dump.RestoreDatabase %1";

    /**
     * Écrit dump.bat dans le dossier _dumps_ : sauvegarde de la base dans un fichier daté
     * {@code <nomBase>_<aaaammjj>_<hhmmss>.netisysDump}, avec les variables d'initCP.bat.
     */
    public static Path ecrireDumpBat(Path destination, String nomBase) throws IOException {
        Path dumps = destination.resolve("_dumps_");
        Files.createDirectories(dumps);
        Path dump = dumps.resolve("dump.bat");
        String contenu = "@echo off\r\n"
                + "call initCP\r\n"
                + "SET CUR_TIME=%TIME:~0,2%%TIME:~3,2%%TIME:~6,2%\r\n"
                + "SET CUR_DATE_TIME=%DATE:~6,4%%DATE:~3,2%%DATE:~0,2%_%CUR_TIME: =0%\r\n"
                + "SET FICHIER_DUMP=" + nomBase + "_%CUR_DATE_TIME%.netisysDump\r\n"
                + "%JAVA_HOME%\\bin\\java.exe -classpath %CP% com.netisys.commun.moulinettes.dump.DumpDatabase %FICHIER_DUMP%\r\n";
        Files.write(dump, contenu.getBytes(Charset.forName("windows-1252")));
        return dump;
    }

    /** Écrit restore.bat : variables d'initCP.bat puis appel direct de la classe de restauration. */
    static Path ecrireRestoreBat(Path dumps) throws IOException {
        Path restore = dumps.resolve("restore.bat");
        String contenu = "@echo off\r\ncall initCP\r\n" + COMMANDE_RESTAURATION + "\r\n";
        Files.write(restore, contenu.getBytes(Charset.forName("windows-1252")));
        return restore;
    }

    /**
     * Copie le dump dans le dossier _dumps_ de la destination, écrit restore.bat (appel d'initCP puis de la classe
     * RestoreDatabase avec les variables d'initCP) et le lance avec le nom du dump.
     * initCP.bat doit être présent dans _dumps_ (écrit à partir du modèle du paramétrage ou venu des archives).
     */
    public static void restaurer(Path destination, Path dump, Consumer<String> journal)
            throws IOException, InterruptedException {
        Path dumps = destination.resolve("_dumps_");
        Files.createDirectories(dumps);
        if (!Files.isRegularFile(dumps.resolve("initCP.bat"))) {
            throw new IOException("initCP.bat introuvable dans " + dumps + " : "
                    + "le dump n'a pas été restauré (renseignez le modèle initCP dans le paramétrage).");
        }
        ecrireRestoreBat(dumps);
        journal.accept("restore.bat écrit dans " + dumps + " : " + COMMANDE_RESTAURATION);
        Path dumpLocal = dumps.resolve(dump.getFileName());
        Files.copy(dump, dumpLocal, StandardCopyOption.REPLACE_EXISTING);
        journal.accept("Restauration de " + dump.getFileName() + " avec restore.bat…");
        ProcessBuilder pb = new ProcessBuilder("cmd", "/c", "restore.bat", dump.getFileName().toString());
        pb.directory(dumps.toFile());
        pb.redirectErrorStream(true);
        Process process = pb.start();
        process.getOutputStream().close();
        try (BufferedReader lecteur = new BufferedReader(
                new InputStreamReader(process.getInputStream(), Charset.forName("windows-1252")))) {
            String ligne;
            while ((ligne = lecteur.readLine()) != null) {
                journal.accept("  " + ligne);
            }
        }
        int code = process.waitFor();
        if (code != 0) {
            throw new IOException("restore.bat s'est terminé avec le code " + code + ".");
        }
        journal.accept("Restauration terminée.");
    }

    private static Connection connecter(ServeurBase serveur) throws SQLException {
        Properties infos = new Properties();
        infos.setProperty("user", serveur.utilisateur());
        // Sans mot de passe, le pilote PostgreSQL le cherche dans %APPDATA%\postgresql\pgpass.conf
        if (serveur.motDePasse() != null && !serveur.motDePasse().isEmpty()) {
            infos.setProperty("password", serveur.motDePasse());
        }
        DriverManager.setLoginTimeout(5);
        return DriverManager.getConnection(serveur.urlJdbc(), infos);
    }
}
