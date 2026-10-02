package nouveauProjet;

import java.io.File;
import java.nio.file.Files;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.stream.Stream;
import java.util.List;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ComboBox;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Separator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TableRow;
import javafx.scene.control.TextArea;
import javafx.scene.input.MouseButton;
import javafx.scene.text.Text;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

/**
 * Fenêtre principale : nom du projet, dossier, puis création de la base et des dossiers.
 */
public class App extends Application {

    private final Parametres parametres = Parametres.charger();

    private final TextField champProjet = new TextField();
    private final TextField champDossier = new TextField();
    private final TextField champInstallation = new TextField();
    private final ComboBox<String> listeInstallation = new ComboBox<>();
    private final ComboBox<String> comboProduit = new ComboBox<>();
    private final ComboBox<ServeurBase> comboServeur = new ComboBox<>();
    private boolean majEnCours;
    private final TableView<Path> tableContenu = new TableView<>();
    private final TextArea zoneLog = new TextArea();
    private final Label nomBase = new Label();
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

        champProjet.setPromptText("Nom du projet");
        champDossier.setPromptText("Destination du projet");
        champInstallation.setPromptText("Dossier d'installation");
        listeInstallation.setMaxWidth(Double.MAX_VALUE);
        Button parcourir = new Button("📁 Parcourir…");
        parcourir.setOnAction(e -> choisirDossier("Destination du projet", champDossier));
        Button parcourirInstallation = new Button("📁 Parcourir…");
        parcourirInstallation.setOnAction(e -> choisirDossier("Dossier d'installation", champInstallation));

        nomBase.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        GridPane formulaire = new GridPane();
        formulaire.setHgap(8);
        formulaire.setVgap(10);
        // Produit et serveur de base de données sur la même ligne
        HBox ligneProduit = new HBox(8, comboProduit, libelle("Serveur"), comboServeur);
        ligneProduit.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(comboProduit, Priority.ALWAYS);
        HBox.setHgrow(comboServeur, Priority.ALWAYS);
        // Paramétrage dans la même colonne que les boutons Parcourir, de même largeur pour les aligner
        formulaire.addRow(0, libelle("Produit"), ligneProduit, parametrage);
        formulaire.addRow(1, libelle("Nom du projet"), champProjet);
        formulaire.addRow(2, libelle("Installation"), champInstallation, parcourirInstallation);
        formulaire.addRow(3, libelle("Version"), listeInstallation);
        formulaire.add(tableContenu, 0, 4, 3, 1);
        formulaire.add(new Separator(), 0, 5, 3, 1);
        HBox ligneBase = new HBox(16, nomBase, interrupteurBase());
        ligneBase.setAlignment(Pos.CENTER_LEFT);
        formulaire.addRow(6, libelle("Base de données"), ligneBase);
        formulaire.addRow(7, libelle("Destination"), champDossier, parcourir);
        GridPane.setHgrow(ligneProduit, Priority.ALWAYS);
        comboServeur.setMaxWidth(Double.MAX_VALUE);
        majComboServeur();
        comboServeur.valueProperty().addListener((obs, ancien, nouveau) -> {
            if (!majEnCours && nouveau != null) {
                parametres.setServeur(nouveau);
                parametres.enregistrer();
                majResume();
            }
        });
        comboProduit.setMaxWidth(Double.MAX_VALUE);
        majComboProduit();
        comboProduit.valueProperty().addListener((obs, ancien, nouveau) -> {
            if (!majEnCours && nouveau != null) {
                parametres.setProduit(nouveau);
                parametres.enregistrer();
                majResume();
            }
        });
        initTableContenu();
        GridPane.setHgrow(tableContenu, Priority.ALWAYS);
        GridPane.setVgrow(tableContenu, Priority.ALWAYS);
        listeInstallation.valueProperty().addListener((obs, ancien, nouveau) -> majTableContenu());
        GridPane.setHgrow(champProjet, Priority.ALWAYS);
        GridPane.setHgrow(champDossier, Priority.ALWAYS);
        GridPane.setHgrow(champInstallation, Priority.ALWAYS);
        GridPane.setHgrow(listeInstallation, Priority.ALWAYS);
        champInstallation.textProperty().addListener((obs, ancien, nouveau) -> majListeInstallation());

        creer.setDefaultButton(true);
        // Gros bouton Créer, vert
        creer.setStyle("-fx-base: #2e9e4f; -fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");
        creer.setPrefSize(220, 48);
        creer.setOnAction(e -> creer());
        creer.disableProperty().bind(Bindings.or(champProjet.textProperty().isEmpty(),
                champDossier.textProperty().isEmpty()));
        message.setWrapText(true);
        message.setAlignment(Pos.CENTER);
        VBox pied = new VBox(6, creer); // le texte de progression n'est plus affiché : le log suffit
        pied.setAlignment(Pos.CENTER);

        // Le log est dans une fenêtre modale à part, ouverte à la création
        zoneLog.setEditable(false);
        zoneLog.setWrapText(true);

        BorderPane racine = new BorderPane(formulaire, null, null, pied, null);
        racine.setPadding(new Insets(16));
        BorderPane.setMargin(formulaire, new Insets(0, 0, 16, 0));

        champProjet.textProperty().addListener((obs, ancien, nouveau) -> majNomBase());
        majResume();

        for (int taille : new int[] { 16, 24, 32, 48, 64, 128, 256 }) {
            var flux = App.class.getResourceAsStream("/icones/nouveau-projet-" + taille + ".png");
            if (flux != null) {
                stage.getIcons().add(new Image(flux));
            }
        }
        stage.setTitle("Nouveau projet");
        stage.setScene(new Scene(racine, 760, 620));
        for (Button b : new Button[] { parametrage, parcourirInstallation, parcourir }) {
            b.setMaxWidth(Double.MAX_VALUE);
        }
        UiUtil.boutonsLisibles(racine);
        stage.show();
    }

    private void ouvrirParametres() {
        new ParametresDialog(fenetre, parametres).showAndWait();
        majComboProduit();
        majComboServeur();
        majResume();
    }

    private void choisirDossier(String titre, TextField champ) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle(titre);
        File actuel = new File(champ.getText());
        if (actuel.isDirectory()) {
            chooser.setInitialDirectory(actuel);
        }
        File choix = chooser.showDialog(fenetre);
        if (choix != null) {
            champ.setText(choix.getAbsolutePath());
        }
    }

    /** Interrupteur Oui / Non : créer ou non la base de données ; l'état est enregistré. */
    private HBox interrupteurBase() {
        javafx.scene.control.ToggleButton interrupteur = new javafx.scene.control.ToggleButton();
        interrupteur.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        interrupteur.setPrefWidth(60);
        interrupteur.selectedProperty().addListener((obs, ancien, oui) -> {
            interrupteur.setText(oui ? "Oui" : "Non");
            interrupteur.setStyle(oui ? "-fx-base: #2e9e4f;" : "-fx-base: #b0b0b0;");
            parametres.setCreerBase(oui);
            parametres.enregistrer();
        });
        interrupteur.setSelected(parametres.isCreerBase());
        // les propriétés ne changent pas si l'état initial est déjà « Oui » : forcer le texte de départ
        interrupteur.setText(interrupteur.isSelected() ? "Oui" : "Non");
        interrupteur.setStyle(interrupteur.isSelected() ? "-fx-base: #2e9e4f;" : "-fx-base: #b0b0b0;");
        HBox boite = new HBox(8, libelle("Créer la base"), interrupteur);
        boite.setAlignment(Pos.CENTER_LEFT);
        return boite;
    }

    private void majComboProduit() {
        majEnCours = true;
        comboProduit.getItems().setAll(parametres.getProduits());
        comboProduit.setValue(parametres.getProduit());
        majEnCours = false;
    }

    private void majComboServeur() {
        majEnCours = true;
        comboServeur.getItems().setAll(parametres.getServeurs());
        comboServeur.setValue(parametres.getServeur());
        majEnCours = false;
    }

    private void majResume() {
        majNomBase();
    }

    private void majNomBase() {
        String projet = champProjet.getText().trim();
        majChemins(projet);
        nomBase.setText(projet.isEmpty() || parametres.getProduit() == null ? ""
                : CreateurProjet.nomBase(parametres.getProduit(), projet));
    }

    /** Pré-remplit Dossier (Projets\produit\projet) et Installation (Installations\projet). */
    private void majChemins(String projet) {
        String produit = parametres.getProduit();
        if (projet.isEmpty()) {
            champDossier.setText("");
            champInstallation.setText("");
            return;
        }
        champDossier.setText(produit == null ? "" : joindre(parametres.getCheminProjets(), produit,
                CreateurProjet.nomPourBaseEtDossier(projet)));
        champInstallation.setText(joindre(parametres.getCheminInstallations(), projet));
    }

    private static String joindre(String base, String... parties) {
        try {
            Path p = Path.of(base.trim());
            for (String partie : parties) {
                p = p.resolve(partie);
            }
            return p.toString();
        } catch (RuntimeException e) {
            return "";
        }
    }

    private static Label libelle(String texte) {
        return new Label(texte);
    }

    private double largeurNomContenu;

    private void initTableContenu() {
        tableContenu.widthProperty().addListener((obs, ancien, nouveau) -> etirerColonneNom());
        tableContenu.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        tableContenu.setPrefHeight(170);
        // Sélection toujours en bleu, avec ou sans focus
        tableContenu.setStyle("-fx-selection-bar: #2f7ed8; -fx-selection-bar-non-focused: #2f7ed8; -fx-selection-bar-text: white;");
        // Un simple clic sélectionne ou désélectionne la ligne, sans Ctrl ni Maj
        tableContenu.setRowFactory(tv -> {
            TableRow<Path> ligne = new TableRow<>();
            ligne.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, e -> {
                if (e.getButton() == MouseButton.PRIMARY && !ligne.isEmpty()) {
                    int index = ligne.getIndex();
                    var modele = tableContenu.getSelectionModel();
                    if (modele.isSelected(index)) {
                        modele.clearSelection(index);
                    } else {
                        modele.select(index);
                    }
                    e.consume();
                }
            });
            return ligne;
        });
        tableContenu.setPlaceholder(new Label("Aucun contenu"));
        tableContenu.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

        TableColumn<Path, String> nom = new TableColumn<>("Nom");
        nom.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getFileName().toString()));
        TableColumn<Path, String> type = new TableColumn<>("Type");
        type.setCellValueFactory(c -> new SimpleStringProperty(Files.isDirectory(c.getValue()) ? "Dossier" : "Fichier"));
        TableColumn<Path, String> taille = new TableColumn<>("Taille");
        taille.setCellValueFactory(c -> new SimpleStringProperty(Files.isDirectory(c.getValue()) ? ""
                : tailleLisible(c.getValue())));
        TableColumn<Path, String> modifie = new TableColumn<>("Modifié");
        modifie.setCellValueFactory(c -> new SimpleStringProperty(LocalDateTime
                .ofInstant(dateModification(c.getValue()).toInstant(), ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))));
        tableContenu.getColumns().addAll(nom, type, taille, modifie);
    }

    /** Adapte la largeur de chaque colonne à son contenu (en-tête compris). */
    private void ajusterColonnes() {
        for (TableColumn<Path, ?> colonne : tableContenu.getColumns()) {
            double largeur = largeurTexte(colonne.getText());
            for (int i = 0; i < tableContenu.getItems().size(); i++) {
                Object valeur = colonne.getCellData(i);
                if (valeur != null) {
                    largeur = Math.max(largeur, largeurTexte(valeur.toString()));
                }
            }
            colonne.setPrefWidth(largeur + 24);
        }
        largeurNomContenu = tableContenu.getColumns().get(0).getPrefWidth();
        etirerColonneNom();
    }

    /** La colonne Nom (la première) occupe l'espace restant, sans réduire sa largeur de contenu. */
    private void etirerColonneNom() {
        if (tableContenu.getColumns().isEmpty()) {
            return;
        }
        double autres = 0;
        for (int i = 1; i < tableContenu.getColumns().size(); i++) {
            autres += tableContenu.getColumns().get(i).getPrefWidth();
        }
        double libre = tableContenu.getWidth() - autres - 20; // marge pour la barre de défilement et les bordures
        tableContenu.getColumns().get(0).setPrefWidth(Math.max(largeurNomContenu, libre));
    }

    private static double largeurTexte(String texte) {
        return new Text(texte).getLayoutBounds().getWidth();
    }

    private static String tailleLisible(Path p) {
        try {
            long ko = (Files.size(p) + 1023) / 1024;
            return ko + " Ko";
        } catch (IOException e) {
            return "";
        }
    }

    /** Affiche le contenu du dossier Installationersion choisi. */
    private void majTableContenu() {
        List<Path> contenu = new ArrayList<>();
        String version = listeInstallation.getValue();
        String texte = champInstallation.getText().trim();
        if (version != null && !texte.isEmpty()) {
            try (Stream<Path> flux = Files.list(Path.of(texte).resolve(version))) {
                flux.forEach(contenu::add);
            } catch (IOException | RuntimeException e) {
                // dossier illisible : tableau vide
            }
        }
        contenu.sort(Comparator.comparing((Path p) -> !Files.isDirectory(p))
                .thenComparing(p -> p.getFileName().toString(), String.CASE_INSENSITIVE_ORDER));
        tableContenu.getItems().setAll(contenu);
        ajusterColonnes();
    }

    private static FileTime dateModification(Path p) {
        try {
            return Files.getLastModifiedTime(p);
        } catch (IOException e) {
            return FileTime.fromMillis(0);
        }
    }

    /** Liste les sous-dossiers du dossier Installation, s'il existe. */
    private void majListeInstallation() {
        List<Path> dossiers = new ArrayList<>();
        String texte = champInstallation.getText().trim();
        if (!texte.isEmpty()) {
            try {
                Path installation = Path.of(texte);
                if (Files.isDirectory(installation)) {
                    try (DirectoryStream<Path> flux = Files.newDirectoryStream(installation, Files::isDirectory)) {
                        for (Path p : flux) {
                            dossiers.add(p);
                        }
                    }
                }
            } catch (IOException | RuntimeException e) {
                // chemin invalide ou illisible : liste vide
            }
        }
        // Les plus récents d'abord (date de dernière modification du dossier)
        dossiers.sort(Comparator.comparing(App::dateModification).reversed());
        List<String> noms = new ArrayList<>();
        for (Path p : dossiers) {
            noms.add(p.getFileName().toString());
        }
        listeInstallation.getItems().setAll(noms);
        listeInstallation.getSelectionModel().selectFirst();
    }

    private void creer() {
        ServeurBase serveur = parametres.getServeur();
        String produit = parametres.getProduit();
        String projet = champProjet.getText().trim();
        Path dossier;
        try {
            dossier = Path.of(champDossier.getText().trim());
        } catch (RuntimeException e) {
            afficherErreur("Le dossier du projet n'est pas un chemin valide.");
            return;
        }

        String erreur = CreateurProjet.verifierNom("Le nom du projet", projet);
        if (erreur == null && serveur == null) {
            erreur = "Configurez un serveur de base de données dans le paramétrage.";
        }
        if (erreur == null && produit == null) {
            erreur = "Choisissez un produit dans le paramétrage.";
        }
        if (erreur == null && Files.exists(dossier) && !Files.isDirectory(dossier)) {
            erreur = dossier + " n'est pas un dossier.";
        }
        if (erreur != null) {
            afficherErreur(erreur);
            return;
        }

        String base = CreateurProjet.nomBase(produit, projet);
        erreursIgnorees = 0;
        boolean creerLaBase = parametres.isCreerBase();
        // Archives et dump pris dans le dossier de la version choisie
        List<Path> selection = new ArrayList<>(tableContenu.getSelectionModel().getSelectedItems());
        List<Path> archives = new ArrayList<>();
        List<Path> dumps = new ArrayList<>();
        for (Path p : selection) {
            String nom = p.getFileName().toString().toLowerCase();
            if (nom.endsWith(".zip") || nom.endsWith(".7z")) {
                archives.add(p);
            } else if (nom.endsWith(".netisysdump")) {
                dumps.add(p);
            }
        }
        Path dump = dumps.isEmpty() ? null : dumps.get(0);
        // initCP.bat est généré depuis le modèle du paramétrage avant la restauration
        String modeleInitCp = parametres.getInitCp();
        String jdkChoisi = parametres.getJdk();
        String cheminJdk = parametres.getCheminJdk();
        String cheminTomcat = parametres.getCheminTomcat();
        String nomEffectif = CreateurProjet.nomPourBaseEtDossier(projet);
        java.util.function.Consumer<String> journal = this::log;
        Task<Void> tache = new Task<>() {
            @Override
            protected Void call() throws Exception {
                if (creerLaBase) {
                    updateMessage("Création de la base " + base + "…");
                    etape("Création de la base " + base, () -> CreateurProjet.creerBase(serveur, base), journal);
                } else {
                    journal.accept("Création de la base ignorée (interrupteur sur Non).");
                }
                updateMessage("Création des dossiers…");
                etape("Création des dossiers", () -> CreateurProjet.creerDossiers(dossier), journal);
                for (Path archive : archives) {
                    updateMessage("Décompression de " + archive.getFileName() + "…");
                    etape("Décompression de " + archive.getFileName(),
                            () -> CreateurProjet.decompresser(archive, dossier, journal), journal);
                }
                if (archives.isEmpty()) {
                    journal.accept("Aucune archive (.zip ou .7z) sélectionnée : rien à décompresser.");
                }
                if (modeleInitCp != null && !modeleInitCp.isBlank()) {
                    updateMessage("Écriture de initCP.bat…");
                    etape("Écriture de initCP.bat", () -> {
                        if (modeleInitCp.contains(InitCp.JAVA_HOME) && jdkChoisi == null) {
                            throw new java.io.IOException("Aucune version de Java choisie dans le paramétrage.");
                        }
                        java.util.Map<String, String> valeurs = new java.util.LinkedHashMap<>();
                        valeurs.put(InitCp.REF_CLIENT, nomEffectif + "_2");
                        valeurs.put(InitCp.PROJET, nomEffectif);
                        valeurs.put(InitCp.BASE, base);
                        valeurs.put(InitCp.JAVA_HOME, jdkChoisi == null ? "" : Path.of(cheminJdk, jdkChoisi).toString());
                        valeurs.put(InitCp.TOMCAT_HOME, cheminTomcat);
                        Path ecrit = InitCp.ecrire(dossier, InitCp.remplacer(modeleInitCp, valeurs));
                        journal.accept("initCP.bat écrit dans " + ecrit + ".");
                    }, journal);
                }
                updateMessage("Écriture de dump.bat…");
                etape("Écriture de dump.bat", () -> {
                    Path ecrit = CreateurProjet.ecrireDumpBat(dossier, base);
                    journal.accept("dump.bat écrit dans " + ecrit + ".");
                }, journal);
                if (dump != null) {
                    updateMessage("Restauration de " + dump.getFileName() + "…");
                    etape("Restauration de " + dump.getFileName(),
                            () -> CreateurProjet.restaurer(dossier, dump, journal), journal);
                } else {
                    journal.accept("Aucun dump .netisysDump sélectionné dans la liste : pas de restauration.");
                }
                return null;
            }
        };
        message.textProperty().bind(tache.messageProperty());
        tache.messageProperty().addListener((obs, ancien, nouveau) -> {
            if (nouveau != null && !nouveau.isEmpty()) {
                log(nouveau);
            }
        });
        creer.disableProperty().unbind();
        creer.setDisable(true);
        tache.setOnSucceeded(e -> {
            finTache();
            if (erreursIgnorees > 0) {
                message.setText("Terminé avec " + erreursIgnorees + " erreur(s) ignorée(s).");
                log("Création terminée avec " + erreursIgnorees + " erreur(s) ignorée(s) (voir ci-dessus).");
                return;
            }
            message.setText("Base " + base + " créée sur " + serveur + ".");
            log("Projet créé. " + (creerLaBase ? "Base " + base + " créée sur " + serveur + "."
                    : "Base " + base + " non créée (interrupteur sur Non).")
                    + " Dossiers créés dans " + dossier + " : " + String.join(", ", CreateurProjet.SOUS_DOSSIERS) + ".");
        });
        tache.setOnFailed(e -> {
            finTache();
            message.setText("");
            if (tache.getException() instanceof CreationArretee) {
                log("Création arrêtée par l'utilisateur.");
                message.setText("Création arrêtée.");
                return;
            }
            afficherErreur(tache.getException().getMessage());
        });
        ouvrirFenetreLog();
        Thread t = new Thread(tache, "creation-projet");
        t.setDaemon(true);
        t.start();
    }

    /** Étape de la création qui peut échouer : l'erreur est journalisée puis on demande de continuer ou d'arrêter. */
    @FunctionalInterface
    private interface Etape {
        void executer() throws Exception;
    }

    /** Levée quand l'utilisateur choisit « Arrêter » après une erreur. */
    private static final class CreationArretee extends Exception {
        private static final long serialVersionUID = 1L;
    }

    private int erreursIgnorees;

    private void etape(String nom, Etape etape, java.util.function.Consumer<String> journal) throws Exception {
        try {
            etape.executer();
        } catch (Exception ex) {
            String detail = ex.getMessage() == null ? ex.toString() : ex.getMessage();
            journal.accept("ERREUR (" + nom + ") : " + detail);
            if (!demanderContinuer(nom, detail)) {
                throw new CreationArretee();
            }
            erreursIgnorees++;
            journal.accept("Poursuite de la création malgré l'erreur.");
        }
    }

    /** Affiche l'erreur avec Continuer / Arrêter ; appelable depuis le thread de la tâche. */
    private boolean demanderContinuer(String nom, String detail) throws Exception {
        java.util.concurrent.FutureTask<Boolean> demande = new java.util.concurrent.FutureTask<>(() -> {
            ButtonType continuer = new ButtonType("Continuer", ButtonBar.ButtonData.YES);
            ButtonType arreter = new ButtonType("Arrêter", ButtonBar.ButtonData.NO);
            Alert alerte = new Alert(Alert.AlertType.ERROR, detail, continuer, arreter);
            alerte.initOwner(proprietaireBoites());
            alerte.setTitle("Erreur");
            alerte.setHeaderText(nom + " a échoué");
            return alerte.showAndWait().orElse(arreter) == continuer;
        });
        Platform.runLater(demande);
        return demande.get();
    }

    private javafx.stage.Stage fenetreLog;

    private javafx.stage.Window proprietaireBoites() {
        return fenetreLog != null && fenetreLog.isShowing() ? fenetreLog : fenetre;
    }

    /** Ouvre (ou rouvre) la fenêtre modale du log, vidée, au lancement de Créer. */
    private void ouvrirFenetreLog() {
        zoneLog.clear();
        fileLog.clear();
        if (fenetreLog == null) {
            javafx.scene.control.Button fermer = new javafx.scene.control.Button("Fermer");
            fermer.setOnAction(e -> fenetreLog.close());
            fermer.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
            HBox bas = new HBox(fermer);
            bas.setAlignment(Pos.CENTER_RIGHT);
            bas.setPadding(new Insets(8, 0, 0, 0));
            BorderPane contenu = new BorderPane(zoneLog, null, null, bas, null);
            contenu.setPadding(new Insets(12));
            fenetreLog = new Stage();
            fenetreLog.initOwner(fenetre);
            fenetreLog.initModality(javafx.stage.Modality.WINDOW_MODAL);
            fenetreLog.setTitle("Création du projet");
            fenetreLog.getIcons().setAll(fenetre.getIcons());
            fenetreLog.setScene(new Scene(contenu, 820, 520));
        }
        fenetreLog.show();
    }

    private void finTache() {
        message.textProperty().unbind();
        creer.disableProperty().bind(Bindings.or(champProjet.textProperty().isEmpty(),
                champDossier.textProperty().isEmpty()));
    }

    private final java.util.concurrent.ConcurrentLinkedQueue<String> fileLog = new java.util.concurrent.ConcurrentLinkedQueue<>();
    private final java.util.concurrent.atomic.AtomicBoolean viderPlanifie = new java.util.concurrent.atomic.AtomicBoolean();

    /**
     * Ajoute une ligne au log ; utilisable depuis n'importe quel thread. Les lignes sont regroupées avant
     * affichage (la sortie de RestoreDatabase peut être très longue) pour que l'interface reste réactive.
     */
    private void log(String texte) {
        fileLog.add(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "  " + texte + "\n");
        if (viderPlanifie.compareAndSet(false, true)) {
            Platform.runLater(this::viderLog);
        }
    }

    /** Affiche les lignes en attente d'un seul coup, sans arracher le défilement à l'utilisateur. */
    private void viderLog() {
        viderPlanifie.set(false);
        StringBuilder lot = new StringBuilder();
        String ligne;
        while ((ligne = fileLog.poll()) != null) {
            lot.append(ligne);
        }
        if (lot.length() == 0) {
            return;
        }
        javafx.scene.control.ScrollPane defilement = zoneLog.lookup(".scroll-pane") instanceof javafx.scene.control.ScrollPane sp
                ? sp : null;
        // suit la fin du log seulement si l'utilisateur y est déjà ; sinon garde sa position
        boolean enBas = defilement == null || defilement.getVvalue() >= 0.999;
        double position = zoneLog.getScrollTop();
        int caret = zoneLog.getCaretPosition();
        zoneLog.appendText(lot.toString());
        if (enBas) {
            zoneLog.setScrollTop(Double.MAX_VALUE);
        } else {
            zoneLog.positionCaret(caret);
            zoneLog.setScrollTop(position);
        }
    }

    private void afficherErreur(String texte) {
        log("ERREUR : " + texte);
        Alert alerte = new Alert(Alert.AlertType.ERROR);
        alerte.initOwner(proprietaireBoites());
        alerte.setHeaderText("Création impossible");
        alerte.setContentText(texte);
        alerte.showAndWait();
    }
}
