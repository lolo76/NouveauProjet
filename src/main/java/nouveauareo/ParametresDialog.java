package nouveauareo;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

/**
 * Choix de l'installation PostgreSQL et du produit.
 */
public class ParametresDialog extends Dialog<ButtonType> {

    private final ObservableList<ServeurPostgres> serveurs;
    private final ObservableList<String> produits;
    private final ListView<ServeurPostgres> listeServeurs;
    private final ComboBox<String> comboProduit;

    private final TextField champVersion = new TextField();
    private final TextField champHote = new TextField();
    private final TextField champPort = new TextField();
    private final TextField champUtilisateur = new TextField();
    private final PasswordField champMotDePasse = new PasswordField();
    private final Label messageServeur = new Label();

    public ParametresDialog(Window proprietaire, Parametres parametres) {
        initOwner(proprietaire);
        setTitle("Paramétrage");
        setHeaderText("Installation PostgreSQL et produit");

        serveurs = FXCollections.observableArrayList(parametres.getServeurs());
        produits = FXCollections.observableArrayList(parametres.getProduits());

        listeServeurs = new ListView<>(serveurs);
        listeServeurs.setPrefHeight(120);
        listeServeurs.getSelectionModel().selectedItemProperty().addListener((obs, ancien, s) -> afficher(s));

        comboProduit = new ComboBox<>(produits);
        comboProduit.setMaxWidth(Double.MAX_VALUE);

        VBox contenu = new VBox(12, sectionServeurs(), sectionProduits());
        contenu.setPadding(new Insets(4));
        getDialogPane().setContent(contenu);
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        getDialogPane().setPrefWidth(520);

        listeServeurs.getSelectionModel().select(parametres.getServeur());
        comboProduit.getSelectionModel().select(parametres.getProduit());

        setResultConverter(bouton -> {
            if (bouton == ButtonType.OK) {
                parametres.getServeurs().clear();
                parametres.getServeurs().addAll(serveurs);
                parametres.getProduits().clear();
                parametres.getProduits().addAll(produits);
                parametres.setServeur(listeServeurs.getSelectionModel().getSelectedItem());
                parametres.setProduit(comboProduit.getValue());
                parametres.enregistrer();
            }
            return bouton;
        });
    }

    private TitledPane sectionServeurs() {
        GridPane formulaire = new GridPane();
        formulaire.setHgap(8);
        formulaire.setVgap(6);
        formulaire.addRow(0, new Label("Version"), champVersion, new Label("Port"), champPort);
        formulaire.addRow(1, new Label("Hôte"), champHote, new Label("Utilisateur"), champUtilisateur);
        formulaire.addRow(2, new Label("Mot de passe"), champMotDePasse);
        champVersion.setPrefColumnCount(6);
        champPort.setPrefColumnCount(6);

        Button ajouter = new Button("Ajouter");
        ajouter.setOnAction(e -> {
            ServeurPostgres s = lireFormulaire();
            if (s != null) {
                serveurs.add(s);
                listeServeurs.getSelectionModel().select(s);
            }
        });
        Button modifier = new Button("Modifier");
        modifier.setOnAction(e -> {
            int index = listeServeurs.getSelectionModel().getSelectedIndex();
            ServeurPostgres s = lireFormulaire();
            if (index >= 0 && s != null) {
                serveurs.set(index, s);
                listeServeurs.getSelectionModel().select(index);
            }
        });
        Button supprimer = new Button("Supprimer");
        supprimer.setOnAction(e -> {
            int index = listeServeurs.getSelectionModel().getSelectedIndex();
            if (index >= 0) {
                serveurs.remove(index);
            }
        });
        Button tester = new Button("Tester la connexion");
        tester.setOnAction(e -> tester());

        HBox boutons = new HBox(6, ajouter, modifier, supprimer, tester);
        messageServeur.setWrapText(true);

        VBox boite = new VBox(8, new Label("Installation utilisée (sélectionnez-la dans la liste) :"),
                listeServeurs, formulaire, boutons, messageServeur);
        TitledPane pane = new TitledPane("PostgreSQL", boite);
        pane.setCollapsible(false);
        return pane;
    }

    private TitledPane sectionProduits() {
        TextField nouveauProduit = new TextField();
        nouveauProduit.setPromptText("Nouveau produit");
        Button ajouter = new Button("Ajouter");
        ajouter.setOnAction(e -> {
            String nom = nouveauProduit.getText().trim();
            String erreur = CreateurProjet.verifierNom("Le nom du produit", nom);
            if (erreur != null) {
                messageServeur.setText(erreur);
                return;
            }
            if (!produits.contains(nom)) {
                produits.add(nom);
            }
            comboProduit.getSelectionModel().select(nom);
            nouveauProduit.clear();
        });
        Button supprimer = new Button("Supprimer");
        supprimer.setOnAction(e -> {
            String produit = comboProduit.getValue();
            if (produit != null) {
                produits.remove(produit);
                comboProduit.getSelectionModel().selectFirst();
            }
        });
        HBox.setHgrow(comboProduit, Priority.ALWAYS);
        HBox.setHgrow(nouveauProduit, Priority.ALWAYS);
        VBox boite = new VBox(8,
                new HBox(6, comboProduit, supprimer),
                new HBox(6, nouveauProduit, ajouter));
        TitledPane pane = new TitledPane("Produit", boite);
        pane.setCollapsible(false);
        return pane;
    }

    private void afficher(ServeurPostgres s) {
        if (s == null) {
            return;
        }
        champVersion.setText(s.version());
        champHote.setText(s.hote());
        champPort.setText(String.valueOf(s.port()));
        champUtilisateur.setText(s.utilisateur());
        champMotDePasse.setText(s.motDePasse());
        messageServeur.setText("");
    }

    private ServeurPostgres lireFormulaire() {
        int port;
        try {
            port = Integer.parseInt(champPort.getText().trim());
        } catch (NumberFormatException ex) {
            messageServeur.setText("Le port doit être un nombre.");
            return null;
        }
        if (champVersion.getText().isBlank() || champHote.getText().isBlank()) {
            messageServeur.setText("La version et l'hôte sont obligatoires.");
            return null;
        }
        messageServeur.setText("");
        return new ServeurPostgres(champVersion.getText().trim(), champHote.getText().trim(), port,
                champUtilisateur.getText().trim(), champMotDePasse.getText());
    }

    private void tester() {
        ServeurPostgres s = lireFormulaire();
        if (s == null) {
            return;
        }
        messageServeur.setText("Connexion à " + s + "…");
        Task<String> tache = new Task<>() {
            @Override
            protected String call() throws Exception {
                return CreateurProjet.tester(s);
            }
        };
        tache.setOnSucceeded(e -> messageServeur.setText("Connexion réussie, version du serveur : " + tache.getValue()));
        tache.setOnFailed(e -> messageServeur.setText("Échec : " + tache.getException().getMessage()));
        Thread t = new Thread(tache, "test-connexion");
        t.setDaemon(true);
        t.start();
    }
}
