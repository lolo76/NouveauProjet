package nouveauareo;

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
 * %USERPROFILE%\.nouveau-areo\parametres.properties.
 */
public class Parametres {

    private static final Path FICHIER = Path.of(System.getProperty("user.home"), ".nouveau-areo",
            "parametres.properties");

    private final List<ServeurPostgres> serveurs = new ArrayList<>();
    private final List<String> produits = new ArrayList<>();
    private int serveurSelectionne;
    private String produitSelectionne;

    public List<ServeurPostgres> getServeurs() {
        return serveurs;
    }

    public List<String> getProduits() {
        return produits;
    }

    public ServeurPostgres getServeur() {
        if (serveurs.isEmpty()) {
            return null;
        }
        return serveurs.get(Math.max(0, Math.min(serveurSelectionne, serveurs.size() - 1)));
    }

    public void setServeur(ServeurPostgres serveur) {
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
            p.serveurs.add(new ServeurPostgres("9", "localhost", 5432, "postgres", ""));
            p.serveurs.add(new ServeurPostgres("15", "localhost", 5425, "postgres", ""));
            p.produits.addAll(List.of("Areo", "Sireo", "Gecope"));
            p.produitSelectionne = "Areo";
            return p;
        }
        Properties props = new Properties();
        try (Reader r = Files.newBufferedReader(FICHIER, StandardCharsets.UTF_8)) {
            props.load(r);
        } catch (IOException e) {
            throw new IllegalStateException("Lecture impossible de " + FICHIER, e);
        }
        int nbServeurs = Integer.parseInt(props.getProperty("serveurs.nombre", "0"));
        for (int i = 0; i < nbServeurs; i++) {
            String prefixe = "serveur." + i + ".";
            p.serveurs.add(new ServeurPostgres(
                    props.getProperty(prefixe + "version", ""),
                    props.getProperty(prefixe + "hote", "localhost"),
                    Integer.parseInt(props.getProperty(prefixe + "port", "5432")),
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
        props.setProperty("serveurs.nombre", String.valueOf(serveurs.size()));
        for (int i = 0; i < serveurs.size(); i++) {
            ServeurPostgres s = serveurs.get(i);
            String prefixe = "serveur." + i + ".";
            props.setProperty(prefixe + "version", s.version());
            props.setProperty(prefixe + "hote", s.hote());
            props.setProperty(prefixe + "port", String.valueOf(s.port()));
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
                props.store(w, "Paramètres Nouveau-Areo");
            }
        } catch (IOException e) {
            throw new IllegalStateException("Écriture impossible de " + FICHIER, e);
        }
    }
}
