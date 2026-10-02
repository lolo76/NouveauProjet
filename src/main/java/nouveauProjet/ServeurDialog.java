package nouveauProjet;

import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.stage.Window;

/**
 * Fenêtre de modification des réglages d'une base de données (PostgreSQL, Oracle, MySQL ou SQL Server).
 * Le mot de passe est affiché en clair.
 */
public class ServeurDialog extends Dialog<ServeurBase> {

    private final ComboBox<TypeBase> comboType = new ComboBox<>();
    private final TextField champVersion = new TextField();
    private final TextField champHote = new TextField();
    private final TextField champPort = new TextField();
    private final TextField champBase = new TextField();
    private final TextField champUtilisateur = new TextField();
    private final TextField champMotDePasse = new TextField();
    private final Label libelleBase = new Label();
    private final Label message = new Label();

    private TypeBase typePrecedent;

    /** @param serveur le serveur à modifier, ou null pour en ajouter un nouveau */
    public ServeurDialog(Window proprietaire, ServeurBase serveur) {
        initOwner(proprietaire);
        setTitle(serveur == null ? "Ajouter une base de données" : "Modifier une base de données");
        setHeaderText("Réglages de la base de données");

        comboType.getItems().setAll(TypeBase.values());
        comboType.setPrefWidth(180);
        TypeBase type = serveur == null ? TypeBase.POSTGRESQL : serveur.type();
        comboType.setValue(type);
        typePrecedent = type;
        if (serveur == null) {
            champHote.setText("localhost");
            champPort.setText(String.valueOf(type.portParDefaut()));
            champBase.setText(type.baseParDefaut());
        } else {
            champVersion.setText(serveur.version());
            champHote.setText(serveur.hote());
            champPort.setText(String.valueOf(serveur.port()));
            champBase.setText(serveur.base());
            champUtilisateur.setText(serveur.utilisateur());
            champMotDePasse.setText(serveur.motDePasse());
        }
        majLibelleBase();
        comboType.valueProperty().addListener((obs, ancien, nouveau) -> changerType(nouveau));

        GridPane formulaire = new GridPane();
        formulaire.setHgap(8);
        formulaire.setVgap(8);
        formulaire.setPadding(new Insets(4));
        formulaire.addRow(0, new Label("Type"), comboType);
        formulaire.addRow(1, new Label("Version"), champVersion);
        formulaire.addRow(2, new Label("Hôte"), champHote);
        formulaire.addRow(3, new Label("Port"), champPort);
        formulaire.addRow(4, libelleBase, champBase);
        formulaire.addRow(5, new Label("Utilisateur"), champUtilisateur);
        formulaire.addRow(6, new Label("Mot de passe"), champMotDePasse);
        formulaire.add(message, 0, 7, 2, 1);
        // Champs de saisie étroits : la fenêtre reste compacte
        champVersion.setPrefColumnCount(8);
        champHote.setPrefColumnCount(18);
        champPort.setPrefColumnCount(6);
        champBase.setPrefColumnCount(18);
        champUtilisateur.setPrefColumnCount(14);
        champMotDePasse.setPrefColumnCount(14);
        for (TextField champ : new TextField[] { champVersion, champHote, champPort, champBase, champUtilisateur,
                champMotDePasse }) {
            champ.setMaxWidth(Region.USE_PREF_SIZE);
        }
        message.setWrapText(true);
        message.setStyle("-fx-text-fill: #b00020;");

        getDialogPane().setContent(formulaire);
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        getDialogPane().setPrefWidth(340);
        setResizable(true);

        // OK reste ouvert tant que le formulaire est invalide
        getDialogPane().lookupButton(ButtonType.OK).addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
            if (lire() == null) {
                e.consume();
            }
        });
        UiUtil.boutonsLisibles(getDialogPane());
        setResultConverter(bouton -> bouton == ButtonType.OK ? lire() : null);
    }

    /** Au changement de type, le port et la base par défaut suivent s'ils n'ont pas été personnalisés. */
    private void changerType(TypeBase nouveau) {
        if (nouveau == null) {
            return;
        }
        if (champPort.getText().trim().equals(String.valueOf(typePrecedent.portParDefaut()))) {
            champPort.setText(String.valueOf(nouveau.portParDefaut()));
        }
        if (champBase.getText().trim().equals(typePrecedent.baseParDefaut())) {
            champBase.setText(nouveau.baseParDefaut());
        }
        typePrecedent = nouveau;
        majLibelleBase();
    }

    private void majLibelleBase() {
        libelleBase.setText(comboType.getValue().libelleBase());
    }

    private ServeurBase lire() {
        int port;
        try {
            port = Integer.parseInt(champPort.getText().trim());
        } catch (NumberFormatException ex) {
            message.setText("Le port doit être un nombre.");
            return null;
        }
        if (champHote.getText().isBlank() || champBase.getText().isBlank()) {
            message.setText("L'hôte et " + (comboType.getValue() == TypeBase.ORACLE ? "le service" : "la base")
                    + " sont obligatoires.");
            return null;
        }
        message.setText("");
        return new ServeurBase(comboType.getValue(), champVersion.getText().trim(), champHote.getText().trim(), port,
                champBase.getText().trim(), champUtilisateur.getText().trim(), champMotDePasse.getText());
    }
}
