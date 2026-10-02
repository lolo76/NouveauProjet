package nouveauProjet;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Paramètres de l'application, enregistrés dans
 * %USERPROFILE%\.nouveau-projet\nouveau-projet.properties.
 */
public class Parametres {

    private static final Path FICHIER = Path.of(System.getProperty("user.home"), ".nouveau-projet",
            "nouveau-projet.properties");

    private final List<ServeurBase> serveurs = new ArrayList<>();
    private final List<String> produits = new ArrayList<>();
    private String cheminProjets = "D:\\Projets\\";
    private String cheminInstallations = "Q:\\Support\\Installations\\";
    private String cheminJdk = "C:\\JDK";
    private String jdk;
    private String cheminTomcat = "C:\\Tomcat70";
    private boolean creerBase = true;
    private String initCp;
    private int serveurSelectionne;
    private String produitSelectionne;

    public String getCheminProjets() {
        return cheminProjets;
    }

    public void setCheminProjets(String cheminProjets) {
        this.cheminProjets = cheminProjets;
    }

    public String getCheminInstallations() {
        return cheminInstallations;
    }

    public void setCheminInstallations(String cheminInstallations) {
        this.cheminInstallations = cheminInstallations;
    }

    /** Modèle d'initCP.bat (le modèle d'origine tant que rien n'a été enregistré). */
    public String getInitCp() {
        return initCp == null ? InitCp.MODELE_PAR_DEFAUT : initCp;
    }

    public void setInitCp(String initCp) {
        this.initCp = initCp;
    }

    /** Faut-il créer la base de données au clic sur Créer ? (Oui par défaut) */
    public boolean isCreerBase() {
        return creerBase;
    }

    public void setCreerBase(boolean creerBase) {
        this.creerBase = creerBase;
    }

    public String getCheminJdk() {
        return cheminJdk;
    }

    public void setCheminJdk(String cheminJdk) {
        this.cheminJdk = cheminJdk;
    }

    /** Nom du sous-dossier du JDK choisi dans le dossier des JDK (null : aucun). */
    public String getJdk() {
        return jdk;
    }

    public void setJdk(String jdk) {
        this.jdk = jdk;
    }

    public String getCheminTomcat() {
        return cheminTomcat;
    }

    public void setCheminTomcat(String cheminTomcat) {
        this.cheminTomcat = cheminTomcat;
    }

    /** Sous-dossiers du dossier des JDK qui contiennent bin\java.exe, triés par nom. */
    public static List<String> listerJdk(String dossierJdk) {
        List<String> noms = new ArrayList<>();
        try {
            Path racine = Path.of(dossierJdk.trim());
            if (Files.isDirectory(racine)) {
                try (java.util.stream.Stream<Path> flux = Files.list(racine)) {
                    flux.filter(p -> Files.isRegularFile(p.resolve("bin").resolve("java.exe")))
                            .forEach(p -> noms.add(p.getFileName().toString()));
                }
            }
        } catch (IOException | RuntimeException e) {
            // dossier illisible : liste vide
        }
        noms.sort(String.CASE_INSENSITIVE_ORDER);
        return noms;
    }

    /** Java 11 par défaut : le JDK 11 le plus récent par ordre alphabétique, sinon le premier. */
    public static String jdkParDefaut(String dossierJdk) {
        List<String> noms = listerJdk(dossierJdk);
        String choix = null;
        for (String nom : noms) {
            if (nom.matches("(?i).*jdk-11\\..*")) {
                choix = nom;
            }
        }
        return choix != null ? choix : (noms.isEmpty() ? null : noms.get(0));
    }

    public List<ServeurBase> getServeurs() {
        return serveurs;
    }

    public List<String> getProduits() {
        return produits;
    }

    public ServeurBase getServeur() {
        if (serveurs.isEmpty()) {
            return null;
        }
        return serveurs.get(Math.max(0, Math.min(serveurSelectionne, serveurs.size() - 1)));
    }

    public void setServeur(ServeurBase serveur) {
        serveurSelectionne = Math.max(0, serveurs.indexOf(serveur));
    }

    public String getProduit() {
        return produitSelectionne;
    }

    public void setProduit(String produit) {
        produitSelectionne = produit;
    }

    public static Parametres charger() {
        Parametres p = new Parametres();
        if (!Files.exists(FICHIER)) {
            p.serveurs.add(new ServeurBase(TypeBase.POSTGRESQL, "9", "localhost", 5432, "postgres", "postgres", ""));
            p.serveurs.add(new ServeurBase(TypeBase.POSTGRESQL, "15", "localhost", 5425, "postgres", "postgres", ""));
            p.produits.addAll(List.of("Areo", "Sireo", "Gecope"));
            p.produitSelectionne = "Areo";
            p.jdk = jdkParDefaut(p.cheminJdk);
            return p;
        }
        Properties props = new Properties();
        try (Reader r = Files.newBufferedReader(FICHIER, StandardCharsets.UTF_8)) {
            props.load(r);
        } catch (IOException e) {
            throw new IllegalStateException("Lecture impossible de " + FICHIER, e);
        }
        p.cheminProjets = props.getProperty("chemin.projets", p.cheminProjets);
        p.cheminInstallations = props.getProperty("chemin.installations", p.cheminInstallations);
        p.cheminJdk = props.getProperty("chemin.jdk", p.cheminJdk);
        p.cheminTomcat = props.getProperty("chemin.tomcat", p.cheminTomcat);
        p.jdk = props.getProperty("jdk", jdkParDefaut(p.cheminJdk));
        p.initCp = props.getProperty("initcp");
        p.creerBase = Boolean.parseBoolean(props.getProperty("creer.base", "true"));
        int nbServeurs = Integer.parseInt(props.getProperty("serveurs.nombre", "0"));
        for (int i = 0; i < nbServeurs; i++) {
            String prefixe = "serveur." + i + ".";
            // Anciens fichiers : pas de type (PostgreSQL) ni de base de connexion
            TypeBase type = TypeBase.depuis(props.getProperty(prefixe + "type"));
            p.serveurs.add(new ServeurBase(type,
                    props.getProperty(prefixe + "version", ""),
                    props.getProperty(prefixe + "hote", "localhost"),
                    Integer.parseInt(props.getProperty(prefixe + "port", String.valueOf(type.portParDefaut()))),
                    props.getProperty(prefixe + "base", type.baseParDefaut()),
                    props.getProperty(prefixe + "utilisateur", "postgres"),
                    props.getProperty(prefixe + "motDePasse", "")));
        }
        int nbProduits = Integer.parseInt(props.getProperty("produits.nombre", "0"));
        for (int i = 0; i < nbProduits; i++) {
            p.produits.add(props.getProperty("produit." + i));
        }
        p.serveurSelectionne = Integer.parseInt(props.getProperty("serveurs.selection", "0"));
        p.produitSelectionne = props.getProperty("produits.selection", p.produits.isEmpty() ? null : p.produits.get(0));
        return p;
    }

    public void enregistrer() {
        Properties props = new Properties();
        props.setProperty("chemin.projets", cheminProjets);
        props.setProperty("chemin.installations", cheminInstallations);
        props.setProperty("chemin.jdk", cheminJdk);
        props.setProperty("chemin.tomcat", cheminTomcat);
        props.setProperty("creer.base", String.valueOf(creerBase));
        if (initCp != null) {
            props.setProperty("initcp", initCp);
        }
        if (jdk != null) {
            props.setProperty("jdk", jdk);
        }
        props.setProperty("serveurs.nombre", String.valueOf(serveurs.size()));
        for (int i = 0; i < serveurs.size(); i++) {
            ServeurBase s = serveurs.get(i);
            String prefixe = "serveur." + i + ".";
            props.setProperty(prefixe + "type", s.type().name());
            props.setProperty(prefixe + "version", s.version());
            props.setProperty(prefixe + "hote", s.hote());
            props.setProperty(prefixe + "port", String.valueOf(s.port()));
            props.setProperty(prefixe + "base", s.base());
            props.setProperty(prefixe + "utilisateur", s.utilisateur());
            props.setProperty(prefixe + "motDePasse", s.motDePasse());
        }
        props.setProperty("serveurs.selection", String.valueOf(serveurSelectionne));
        props.setProperty("produits.nombre", String.valueOf(produits.size()));
        for (int i = 0; i < produits.size(); i++) {
            props.setProperty("produit." + i, produits.get(i));
        }
        if (produitSelectionne != null) {
            props.setProperty("produits.selection", produitSelectionne);
        }
        try {
            Files.createDirectories(FICHIER.getParent());
            try (Writer w = Files.newBufferedWriter(FICHIER, StandardCharsets.UTF_8)) {
                props.store(w, "Paramètres Nouveau-Projet");
            }
        } catch (IOException e) {
            throw new IllegalStateException("Écriture impossible de " + FICHIER, e);
        }
    }
}
