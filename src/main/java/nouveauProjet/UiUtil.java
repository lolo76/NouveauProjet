package nouveauProjet;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.Region;

/**
 * Petits utilitaires d'interface.
 */
final class UiUtil {

    private UiUtil() {
    }

    /** Impose à tous les boutons du contenu une largeur minimale égale à celle de leur texte (jamais coupé). */
    static void boutonsLisibles(Node noeud) {
        if (noeud instanceof Button bouton) {
            bouton.setMinWidth(Region.USE_PREF_SIZE);
        }
        if (noeud instanceof TitledPane pane && pane.getContent() != null) {
            boutonsLisibles(pane.getContent());
        }
        if (noeud instanceof ScrollPane defilement && defilement.getContent() != null) {
            boutonsLisibles(defilement.getContent());
        }
        if (noeud instanceof Parent parent) {
            for (Node enfant : parent.getChildrenUnmodifiable()) {
                boutonsLisibles(enfant);
            }
        }
    }
}
