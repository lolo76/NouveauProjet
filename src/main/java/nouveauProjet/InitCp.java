package nouveauProjet;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Modèle du fichier initCP.bat d'un projet (classpath et variables utilisés par dump.bat et restore.bat).
 * Le texte du modèle est modifiable dans le paramétrage ; les marqueurs entre accolades sont remplacés à la création.
 */
public final class InitCp {

    public static final String REF_CLIENT = "{REF_CLIENT}";
    public static final String PROJET = "{PROJET}";
    public static final String BASE = "{BASE}";
    public static final String JAVA_HOME = "{JAVA_HOME}";
    public static final String TOMCAT_HOME = "{TOMCAT_HOME}";

    /** Description des marqueurs, affichée dans la fenêtre d'édition. */
    public static final String AIDE = "Marqueurs remplacés à la création du projet :\n"
            + REF_CLIENT + " = nom du projet (CD devient CG) suivi de _2, " + PROJET + " = nom du projet (CD devient CG),\n"
            + BASE + " = nom de la base, " + JAVA_HOME + " = JDK choisi dans le paramétrage,\n"
            + TOMCAT_HOME + " = dossier Tomcat du paramétrage.\n"
            + "Le fichier est écrit dans le dossier _dumps_ de la destination avant la restauration ; "
            + "s'il est vide, initCP.bat n'est pas touché.";

    /** Modèle d'origine, tiré de l'initCP.bat d'un projet récent (CG71), avec _Constantes_Areo dans le classpath. */
    public static final String MODELE_PAR_DEFAUT = """
            @echo off

            set REF_CLIENT={REF_CLIENT}

            set JAVA_HOME="{JAVA_HOME}"
            set TOMCAT_HOME={TOMCAT_HOME}

            set CONSTANTES_NETISYS=C:\\eclipse\\workspace\\netisys-cvs\\java\\netisys-maven\\netisys-commons\\_Constantes_Netisys
            set CONSTANTES_AREO=C:\\eclipse\\workspace\\netisys-cvs\\java\\netisys-areo\\_Constantes_Areo
            set UTILS=C:\\eclipse\\workspace\\netisys-cvs\\java\\netisys-commons\\__Utils
            set GEOMETRIES=C:\\eclipse\\workspace\\netisys-cvs\\java\\netisys-commons\\_Geometries
            set GEOMETRIES_VOIES=C:\\eclipse\\workspace\\netisys-cvs\\java\\netisys-commons\\_GeometriesVoies
            set SERVEUR_NETISYS_WEBAPP=C:\\eclipse\\workspace\\netisys-cvs\\java\\netisys-commons\\_Serveur_Netisys
            set SERVEUR_ET_CLIENT_NETISYS=C:\\eclipse\\workspace\\netisys-cvs\\java\\netisys-commons\\_ServeurEtClient_Netisys
            set SERVEUR_ET_CLIENT_AREO=C:\\eclipse\\workspace\\netisys-cvs\\java\\netisys-areo\\_ServeurEtClient_Areo
            set AREO_WEBAPP=C:\\eclipse\\workspace\\netisys-cvs\\java\\netisys-areo\\Areo2
            set AREO_SPECIFIQUE_WEBAPP=C:\\eclipse\\workspace\\netisys-cvs\\java\\netisys-areo\\Areo_%REF_CLIENT%

            set CP=%TOMCAT_HOME%\\lib\\servlet-api.jar
            set CP=%CP%;%TOMCAT_HOME%\\lib\\jsp-api.jar

            set CP=%CP%;%CONSTANTES_NETISYS%\\bin
            set CP=%CP%;%CONSTANTES_AREO%\\bin
            set CP=%CP%;%AREO_SPECIFIQUE_WEBAPP%\\classes
            set CP=%CP%;%AREO_WEBAPP%\\classes
            set CP=%CP%;%SERVEUR_NETISYS_WEBAPP%\\classes
            set CP=%CP%;%SERVEUR_NETISYS_WEBAPP%\\lib\\*
            set CP=%CP%;%SERVEUR_ET_CLIENT_NETISYS%\\bin
            set CP=%CP%;%GEOMETRIES%\\bin
            set CP=%CP%;%UTILS%\\bin
            set CP=%CP%;%UTILS%\\lib\\*
            set CP=%CP%;%SERVEUR_ET_CLIENT_AREO%\\bin
            set CP=%CP%;%GEOMETRIES_VOIES%\\bin

            set CP=%CP%;%AREO_SPECIFIQUE_WEBAPP%\\lib\\ojdbc14.jar
            """;

    private InitCp() {
    }

    /** Remplace les marqueurs du modèle par leurs valeurs. */
    public static String remplacer(String modele, Map<String, String> valeurs) {
        String texte = modele;
        for (Map.Entry<String, String> valeur : valeurs.entrySet()) {
            texte = texte.replace(valeur.getKey(), valeur.getValue());
        }
        return texte;
    }

    /** Écrit initCP.bat (fins de ligne Windows, codage ANSI) dans le dossier _dumps_ de la destination. */
    public static Path ecrire(Path destination, String texte) throws IOException {
        Path dumps = destination.resolve("_dumps_");
        Files.createDirectories(dumps);
        Path fichier = dumps.resolve("initCP.bat");
        String windows = texte.replace("\r\n", "\n").replace("\n", "\r\n");
        Files.write(fichier, windows.getBytes(Charset.forName("windows-1252")));
        return fichier;
    }
}
