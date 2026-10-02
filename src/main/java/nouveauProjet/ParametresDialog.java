package nouveauProjet;

import java.io.File;
import java.util.List;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;

/**
 * Paramétrage : général (chemins, Java, Tomcat, produit), bases de données et modèle initCP.bat.
 */
public class ParametresDialog extends Dialog<ButtonType> {

    private final ObservableList<ServeurBase> serveurs;
    private final ObservableList<String> produits;
    private final ComboBox<ServeurBase> comboServeur;
    private final ComboBox<String> comboProduit;

    private final TextField champProjets = new TextField();
    private final TextField champInstallations = new TextField();
    private final TextField champJdk = new TextField();
    private final TextField champTomcat = new TextField();
    private final ComboBox<String> comboJdk = new ComboBox<>();
    private final Label messageServeur = new Label();
    private final Label messageProduit = new Label();
    private String initCp;

    public ParametresDialog(Window proprietaire, Parametres parametres) {
        initOwner(proprietaire);
        setTitle("Paramétrage");
        setHeaderText("Paramétrage de l'application");

        serveurs = FXCollections.observableArrayList(parametres.getServeurs());
        produits = FXCollections.observableArrayList(parametres.getProduits());
        initCp = parametres.getInitCp();

        comboServeur = new ComboBox<>(serveurs);
        comboServeur.setMaxWidth(Double.MAX_VALUE);
        comboProduit = new ComboBox<>(produits);
        comboProduit.setMaxWidth(Double.MAX_VALUE);

        VBox contenu = new VBox(12, sectionGenerale(), sectionBases(), sectionInitCp());
        contenu.setPadding(new Insets(4));
        getDialogPane().setContent(contenu);
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        getDialogPane().setPrefWidth(660);
        setResizable(true);

        // Champs de saisie moins larges
        for (TextField champ : new TextField[] { champProjets, champInstallations, champJdk, champTomcat }) {
            champ.setPrefColumnCount(26);
            champ.setMaxWidth(Region.USE_PREF_SIZE);
        }
        UiUtil.boutonsLisibles(getDialogPane());
        champProjets.setText(parametres.getCheminProjets());
        champInstallations.setText(parametres.getCheminInstallations());
        champJdk.setText(parametres.getCheminJdk());
        champTomcat.setText(parametres.getCheminTomcat());
        majListeJdk(parametres.getJdk());
        champJdk.textProperty().addListener((obs, ancien, nouveau) -> majListeJdk(comboJdk.getValue()));
        comboServeur.getSelectionModel().select(parametres.getServeur());
        comboProduit.getSelectionModel().select(parametres.getProduit());

        setResultConverter(bouton -> {
            if (bouton == ButtonType.OK) {
                parametres.setCheminProjets(champProjets.getText().trim());
                parametres.setCheminInstallations(champInstallations.getText().trim());
                parametres.setCheminJdk(champJdk.getText().trim());
                parametres.setCheminTomcat(champTomcat.getText().trim());
                parametres.setJdk(comboJdk.getValue());
                parametres.getServeurs().clear();
                parametres.getServeurs().addAll(serveurs);
                parametres.getProduits().clear();
                parametres.getProduits().addAll(produits);
                parametres.setServeur(comboServeur.getValue());
                parametres.setProduit(comboProduit.getValue());
                parametres.setInitCp(initCp);
                parametres.enregistrer();
            }
            return bouton;
        });
    }

    /** Chemins, Java, Tomcat et produit, regroupés. */
    private TitledPane sectionGenerale() {
        comboJdk.setPrefWidth(220);
        Button parcourirJdk = new Button("📁 Parcourir…");
        parcourirJdk.setOnAction(e -> choisirDossier("Dossier des JDK", champJdk));
        Button parcourirTomcat = new Button("📁 Parcourir…");
        parcourirTomcat.setOnAction(e -> choisirDossier("Dossier de Tomcat", champTomcat));

        GridPane formulaire = new GridPane();
        formulaire.setHgap(8);
        formulaire.setVgap(6);
        // Colonne des libellés assez large pour les lire en entier
        javafx.scene.layout.ColumnConstraints colonneLibelles = new javafx.scene.layout.ColumnConstraints();
        colonneLibelles.setMinWidth(150);
        colonneLibelles.setPrefWidth(150);
        formulaire.getColumnConstraints().add(colonneLibelles);
        formulaire.addRow(0, new Label("Projets"), champProjets);
        formulaire.addRow(1, new Label("Installations"), champInstallations);
        // La liste des versions de Java est à droite du dossier des JDK, sans libellé
        HBox ligneJdk = new HBox(8, champJdk, comboJdk);
        ligneJdk.setAlignment(Pos.CENTER_LEFT);
        formulaire.addRow(2, new Label("Dossier des JDK"), ligneJdk, parcourirJdk);
        formulaire.addRow(3, new Label("Tomcat"), champTomcat, parcourirTomcat);
        // La liste des produits remplace le libellé « Produit » : même largeur que les autres libellés
        formulaire.addRow(4, comboProduit, ligneProduit());
        formulaire.add(messageProduit, 1, 5);

        TitledPane pane = new TitledPane("Général", formulaire);
        pane.setCollapsible(false);
        return pane;
    }

    /** Le réglage des produits tient sur une ligne : liste, nouveau produit, Ajouter, Supprimer. */
    private HBox ligneProduit() {
        TextField nouveauProduit = new TextField();
        nouveauProduit.setPromptText("Nouveau produit");
        Button ajouter = new Button("Ajouter");
        ajouter.setOnAction(e -> {
            String nom = nouveauProduit.getText().trim();
            String erreur = CreateurProjet.verifierNom("Le nom du produit", nom);
            messageProduit.setText(erreur == null ? "" : erreur);
            if (erreur != null) {
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
        HBox.setHgrow(nouveauProduit, Priority.ALWAYS);
        HBox ligne = new HBox(6, nouveauProduit, ajouter, supprimer);
        ligne.setAlignment(Pos.CENTER_LEFT);
        return ligne;
    }

    /** Choix de la base de données en liste déroulante, avec les 4 boutons. */
    private TitledPane sectionBases() {
        Button ajouter = new Button("Ajouter");
        ajouter.setOnAction(e -> {
            ServeurBase s = new ServeurDialog(getDialogPane().getScene().getWindow(), null).showAndWait().orElse(null);
            if (s != null) {
                serveurs.add(s);
                comboServeur.getSelectionModel().select(s);
            }
        });
        Button modifier = new Button("Modifier");
        modifier.setOnAction(e -> modifierServeur());
        Button supprimer = new Button("Supprimer");
        supprimer.setOnAction(e -> {
            int index = comboServeur.getSelectionModel().getSelectedIndex();
            if (index >= 0) {
                serveurs.remove(index);
                comboServeur.getSelectionModel().selectFirst();
            }
        });
        Button tester = new Button("Tester la connexion");
        tester.setOnAction(e -> tester());

        HBox.setHgrow(comboServeur, Priority.ALWAYS);
        HBox ligne = new HBox(6, comboServeur, ajouter, modifier, supprimer, tester);
        ligne.setAlignment(Pos.CENTER_LEFT);
        messageServeur.setWrapText(true);

        TitledPane pane = new TitledPane("Base de données", new VBox(8, ligne, messageServeur));
        pane.setCollapsible(false);
        return pane;
    }

    private TitledPane sectionInitCp() {
        Button editer = new Button("Éditer initCP…");
        editer.setOnAction(e -> {
            String texte = new InitCpDialog(getDialogPane().getScene().getWindow(), initCp).showAndWait().orElse(null);
            if (texte != null) {
                initCp = texte;
            }
        });
        Label aide = new Label("Modèle du fichier initCP.bat écrit dans le dossier _dumps_ à la création.");
        aide.setWrapText(true);
        HBox ligne = new HBox(10, editer, aide);
        ligne.setAlignment(Pos.CENTER_LEFT);
        TitledPane pane = new TitledPane("initCP", ligne);
        pane.setCollapsible(false);
        return pane;
    }

    private void modifierServeur() {
        int index = comboServeur.getSelectionModel().getSelectedIndex();
        if (index < 0) {
            messageServeur.setText("Sélectionnez d'abord une base dans la liste.");
            return;
        }
        ServeurBase s = new ServeurDialog(getDialogPane().getScene().getWindow(), serveurs.get(index))
                .showAndWait().orElse(null);
        if (s != null) {
            serveurs.set(index, s);
            comboServeur.getSelectionModel().select(index);
        }
    }

    private void tester() {
        ServeurBase s = comboServeur.getValue();
        if (s == null) {
            messageServeur.setText("Sélectionnez d'abord une base dans la liste.");
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

    /** Recharge la liste des JDK du dossier saisi en gardant le choix s'il y existe encore. */
    private void majListeJdk(String choix) {
        List<String> noms = Parametres.listerJdk(champJdk.getText());
        comboJdk.getItems().setAll(noms);
        if (choix != null && noms.contains(choix)) {
            comboJdk.setValue(choix);
        } else {
            comboJdk.setValue(Parametres.jdkParDefaut(champJdk.getText()));
        }
    }

    private void choisirDossier(String titre, TextField champ) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle(titre);
        File actuel = new File(champ.getText().trim());
        if (actuel.isDirectory()) {
            chooser.setInitialDirectory(actuel);
        }
        File choix = chooser.showDialog(getDialogPane().getScene().getWindow());
        if (choix != null) {
            champ.setText(choix.getAbsolutePath());
        }
    }
}
