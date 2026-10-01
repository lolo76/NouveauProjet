package nouveauareo;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

/**
 * Fenêtre principale : nom du projet, dossier, puis création de la base et des dossiers.
 */
public class App extends Application {

    private final Parametres parametres = Parametres.charger();

    private final TextField champProjet = new TextField();
    private final TextField champDossier = new TextField();
    private final Label nomBase = new Label();
    private final Label resume = new Label();
    private final Label message = new Label();
    private final Button creer = new Button("Créer");

    private Stage fenetre;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        fenetre = stage;

        Button parametrage = new Button("⚙ Paramétrage");
        parametrage.setOnAction(e -> ouvrirParametres());
        Region espace = new Region();
        HBox.setHgrow(espace, Priority.ALWAYS);
        HBox entete = new HBox(8, resume, espace, parametrage);
        entete.setAlignment(Pos.CENTER_LEFT);

        champProjet.setPromptText("Nom du projet");
        champDossier.setPromptText("Dossier du projet");
        Button parcourir = new Button("Parcourir…");
        parcourir.setOnAction(e -> choisirDossier());

        nomBase.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        GridPane formulaire = new GridPane();
        formulaire.setHgap(8);
        formulaire.setVgap(10);
        formulaire.addRow(0, new Label("Nom du projet"), champProjet);
        formulaire.addRow(1, new Label("Dossier"), champDossier, parcourir);
        formulaire.addRow(2, new Label("Base de données"), nomBase);
        GridPane.setHgrow(champProjet, Priority.ALWAYS);
        GridPane.setHgrow(champDossier, Priority.ALWAYS);

        creer.setDefaultButton(true);
        creer.setOnAction(e -> creer());
        creer.disableProperty().bind(Bindings.or(champProjet.textProperty().isEmpty(),
                champDossier.textProperty().isEmpty()));
        message.setWrapText(true);
        HBox pied = new HBox(12, creer, message);
        pied.setAlignment(Pos.CENTER_LEFT);

        BorderPane racine = new BorderPane(formulaire, entete, null, pied, null);
        racine.setPadding(new Insets(16));
        BorderPane.setMargin(formulaire, new Insets(16, 0, 16, 0));

        champProjet.textProperty().addListener((obs, ancien, nouveau) -> majNomBase());
        majResume();

        stage.setTitle("Nouveau projet");
        stage.setScene(new Scene(racine, 620, 260));
        stage.show();
    }

    private void ouvrirParametres() {
        new ParametresDialog(fenetre, parametres).showAndWait();
        majResume();
    }

    private void choisirDossier() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Dossier du projet");
        File actuel = new File(champDossier.getText());
        if (actuel.isDirectory()) {
            chooser.setInitialDirectory(actuel);
        }
        File choix = chooser.showDialog(fenetre);
        if (choix != null) {
            champDossier.setText(choix.getAbsolutePath());
        }
    }

    private void majResume() {
        ServeurPostgres s = parametres.getServeur();
        resume.setText("Produit : " + (parametres.getProduit() == null ? "aucun" : parametres.getProduit())
                + "    " + (s == null ? "Aucun PostgreSQL configuré" : s.toString()));
        majNomBase();
    }

    private void majNomBase() {
        String projet = champProjet.getText().trim();
        nomBase.setText(projet.isEmpty() || parametres.getProduit() == null ? ""
                : CreateurProjet.nomBase(parametres.getProduit(), projet));
    }

    private void creer() {
        ServeurPostgres serveur = parametres.getServeur();
        String produit = parametres.getProduit();
        String projet = champProjet.getText().trim();
        Path dossierParent = Path.of(champDossier.getText().trim());
        Path dossier = dossierParent.resolve(projet);

        String erreur = CreateurProjet.verifierNom("Le nom du projet", projet);
        if (erreur == null && serveur == null) {
            erreur = "Configurez une installation PostgreSQL dans le paramétrage.";
        }
        if (erreur == null && produit == null) {
            erreur = "Choisissez un produit dans le paramétrage.";
        }
        if (erreur == null && Files.exists(dossierParent) && !Files.isDirectory(dossierParent)) {
            erreur = dossierParent + " n'est pas un dossier.";
        }
        if (erreur == null && Files.exists(dossier) && !Files.isDirectory(dossier)) {
            erreur = dossier + " n'est pas un dossier.";
        }
        if (erreur != null) {
            afficherErreur(erreur);
            return;
        }

        String base = CreateurProjet.nomBase(produit, projet);
        Task<Void> tache = new Task<>() {
            @Override
            protected Void call() throws Exception {
                updateMessage("Création de la base " + base + "…");
                CreateurProjet.creerBase(serveur, base);
                updateMessage("Création des dossiers…");
                CreateurProjet.creerDossiers(dossier);
                return null;
            }
        };
        message.textProperty().bind(tache.messageProperty());
        creer.disableProperty().unbind();
        creer.setDisable(true);
        tache.setOnSucceeded(e -> {
            finTache();
            message.setText("Base " + base + " créée sur " + serveur + ".");
            Alert ok = new Alert(Alert.AlertType.INFORMATION);
            ok.initOwner(fenetre);
            ok.setHeaderText("Projet créé");
            ok.setContentText("Base : " + base + "\nServeur : " + serveur + "\nDossiers créés dans " + dossier + " :\n  "
                    + String.join("\n  ", CreateurProjet.SOUS_DOSSIERS));
            ok.showAndWait();
        });
        tache.setOnFailed(e -> {
            finTache();
            message.setText("");
            afficherErreur(tache.getException().getMessage());
        });
        Thread t = new Thread(tache, "creation-projet");
        t.setDaemon(true);
        t.start();
    }

    private void finTache() {
        message.textProperty().unbind();
        creer.disableProperty().bind(Bindings.or(champProjet.textProperty().isEmpty(),
                champDossier.textProperty().isEmpty()));
    }

    private void afficherErreur(String texte) {
        Alert alerte = new Alert(Alert.AlertType.ERROR);
        alerte.initOwner(fenetre);
        alerte.setHeaderText("Création impossible");
        alerte.setContentText(texte);
        alerte.showAndWait();
    }
}
