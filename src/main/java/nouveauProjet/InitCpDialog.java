package nouveauProjet;

import javafx.geometry.Insets;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

/**
 * Édition en texte du modèle d'initCP.bat.
 */
public class InitCpDialog extends Dialog<String> {

    public InitCpDialog(Window proprietaire, String texte) {
        initOwner(proprietaire);
        setTitle("Modèle initCP.bat");
        setHeaderText("Contenu du fichier initCP.bat");

        TextArea zone = new TextArea(texte);
        zone.setStyle("-fx-font-family: 'Consolas', monospace;");
        zone.setPrefColumnCount(80);
        zone.setPrefRowCount(24);
        VBox.setVgrow(zone, Priority.ALWAYS);
        Label aide = new Label(InitCp.AIDE);
        aide.setWrapText(true);
        VBox contenu = new VBox(8, zone, aide);
        contenu.setPadding(new Insets(4));

        ButtonType modele = new ButtonType("Modèle par défaut", ButtonBar.ButtonData.LEFT);
        getDialogPane().setContent(contenu);
        getDialogPane().getButtonTypes().addAll(modele, ButtonType.OK, ButtonType.CANCEL);
        // « Modèle par défaut » remplit la zone sans fermer la fenêtre
        getDialogPane().lookupButton(modele).addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
            zone.setText(InitCp.MODELE_PAR_DEFAUT);
            e.consume();
        });
        UiUtil.boutonsLisibles(getDialogPane());
        setResizable(true);
        setResultConverter(bouton -> bouton == ButtonType.OK ? zone.getText() : null);
    }
}
