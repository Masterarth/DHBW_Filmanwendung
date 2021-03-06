package controller;

import classes.Movie;
import classes.MovieList;
import classes.OMDB;
import classes.PdfExport;
import classes.SQLite;
import classes.Search;
import classes.Statistic;
import classes.TableRow;
import classes.User;
import classes.XML;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

public class MainFXMLController implements Initializable {

    MovieList movies;
    OMDB omdb = new OMDB();
    SQLite sql = new SQLite();

    User user = null;

    private Search liveSearchService = new Search();
    private Movie currentMovie = null;

    @FXML
    private ListView searchlist;
    @FXML
    private TextField searchbar;
    @FXML
    private ImageView detailImage;
    @FXML
    private TextArea detailPlot;
    @FXML
    private TableView detailTable;
    @FXML
    public TabPane tabPane;
    @FXML
    private ImageView imageRow1;
    @FXML
    private ImageView imageRow2;
    @FXML
    private ImageView imageRow3;
    @FXML
    private StackPane imagePane;
    @FXML
    private Button btnDetailPlay;
    @FXML
    private Button btnDetailFav;
    @FXML
    private Button btnDetailBookmark;
    @FXML
    private Button btnFavDel;
    @FXML
    private Button btnFavPdf;
    @FXML
    private Button btnFavLooked;
    @FXML
    private Button btnBookmarkDel;
    @FXML
    private Button btnBookmarkToFav;
    @FXML
    private Button btnBookmarkPdf;
    @FXML
    private WebView webView;
    @FXML
    private Label lblWelcome;
    @FXML
    private TableView favoriteTable;
    @FXML
    private TableView bookmarkTable;
    @FXML
    private MenuButton userBtn;
    @FXML
    private Label lblFav;
    @FXML
    private Label lblBook;
    @FXML
    private Label lblSeenMovies;
    @FXML
    private Label lblUserRate;
    @FXML
    private PieChart pieChartImdbRating;
    @FXML
    private Button btnExport;
    @FXML
    private Button btnImport;

    public MainFXMLController() {
        this.movies = MovieList.getInstance();
    }

    //Öffnet ein POP-UP mit den Details zum Film in der Favliste
    @FXML
    public void onFavPressed(MouseEvent event) throws IOException {
        //erst bei doppelklick wird die methode weiter ausgeführt
        if (event.getClickCount() == 2) {
            Movie movie = (Movie) favoriteTable.getSelectionModel().getSelectedItem();
            if (movie != null) {
                popup(movie);

            }
        }
    }

    //Öffnet ein  POP-UP in der Merkliste.
    @FXML
    public void onBookmarkPressed(MouseEvent event) throws IOException {

        if (event.getClickCount() == 2) {
            Movie movie = (Movie) bookmarkTable.getSelectionModel().getSelectedItem();
            if (movie != null) {
                popup(movie);
            }
        }
    }

    //Exportiert den in der Detailansicht gewählten Film als PDF
    @FXML
    private void onPlay() throws IOException {
        if (currentMovie != null) {
            PdfExport pdf = new PdfExport();
            pdf.exportMovie(currentMovie);
        } else {
            error(1);
        }
    }

    //Livesuche 
    //buchstaben und zahlen werden getrennt und als titel oder jahr verstanden
    @FXML
    private void onSearch() {
        if (!searchbar.getText().isEmpty()) {
            liveSearchService.setOnRunning(new EventHandler<WorkerStateEvent>() {
                @Override
                public void handle(WorkerStateEvent t) {
                    ObservableList<String> items = FXCollections.observableArrayList("Ergebnisse werden geladen...");
                    searchlist.setVisible(true);
                    searchlist.setItems(items);
                }
            });

            liveSearchService.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
                @Override
                public void handle(WorkerStateEvent t) {
                    ArrayList<Movie> results_array = liveSearchService.getValue();
                    if (results_array != null) {
                        ObservableList<Movie> results_list = FXCollections.observableList(results_array);
                        searchlist.setItems(results_list);
                    } else {
                        ObservableList<String> items = FXCollections.observableArrayList("Keine Treffer gefunden...");
                        searchlist.setItems(items);
                    }
                }
            });

            String year = "";
            String title = "";
            String[] res = searchbar.getText().split("(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)");
            if (res.length > 1) {
                for (String t : res) {
                    if (t.matches(".*\\d.*")) {
                        year = t;
                    } else {
                        title = t;
                    }
                }
            } else {
                title = res[0];
            }

            liveSearchService.setTitle(title);
            liveSearchService.setYear(year);
            liveSearchService.restart();
        } else {
            searchlist.setVisible(false);
            searchlist.setItems(null);
        }
    }

    //fügt einen movie in die db ein
    private void sqlInsertToMovie(Movie movie) {
        sql.insert("Movie", movie.getMap());
    }

    //fügt einen eintrag in die zwischentabelle movielist ein 
    //zwischentabelle für user - movie 
    private void sqlInsertToMovielist(Movie movie) {
        Map<String, String> movielist = new HashMap<>();
        movielist.put("UserID", user.getId());
        movielist.put("imdbID", movie.getImdbID());
        movielist.put("MerkList", Boolean.toString(movie.isBookmark()));
        movielist.put("FavList", Boolean.toString(movie.isFavourite()));
        movielist.put("UserRate", movie.getUserRating());
        sql.insert("Movielist", movielist);
    }

    //aktualisiert einen eintrag in der zwischentabelle movielist
    private void sqlUpdateMovielist(Movie movie) {
        if (sql.exsists("Movielist", "UserID, imdbID", "UserID = '" + user.getId() + "' and imdbID = '" + movie.getImdbID() + "'") > 0) {
            Map<String, String> movielist = new HashMap<>();
            movielist.put("UserID", user.getId());
            movielist.put("imdbID", movie.getImdbID());
            movielist.put("MerkList", Boolean.toString(movie.isBookmark()));
            movielist.put("FavList", Boolean.toString(movie.isFavourite()));
            movielist.put("UserRate", movie.getUserRating());
            movielist.put("Looked", Boolean.toString(movie.isLooked()));
            sql.update("Movielist", movielist, "imdbID = '" + movie.getImdbID() + "'");
        }
    }

    //löscht einen eintrag aus der zwischentabelle movielist
    private void sqlDeleteFromMovielist(Movie movie) {
        if (!movie.isFavourite() && !movie.isBookmark()) {
            if (sql.exsists("Movielist", "UserID, imdbID", "UserID = '" + user.getId() + "' and imdbID = '" + movie.getImdbID() + "'") > 0) {
                sql.delete("Movielist", "UserID = '" + user.getId() + "' and imdbID = '" + movie.getImdbID() + "'");
            }
        }
    }

    //Setzt einen Film auf die Favoritenliste, oder entfernt ihn. Fehlermeldung falls kein Film gewählt.
    @FXML
    private void onFav() throws IOException {
        if (currentMovie != null) {
            this.currentMovie.setFavourite(true);
            movies.addMovie(this.currentMovie);
            loadMovie(currentMovie.getId());
            if (sql.exsists("Movie", "imdbID", "imdbID = '" + currentMovie.getImdbID() + "'") < 1) {
                sqlInsertToMovie(currentMovie);
            }
            if (sql.exsists("Movielist", "UserID, imdbID", "UserID = '" + user.getId() + "' and imdbID = '" + currentMovie.getImdbID() + "'") > 0) {
                sqlUpdateMovielist(currentMovie);
            } else {
                sqlInsertToMovielist(currentMovie);
            }

        } else {
            error(1);
        }
    }

    //Setzt einen Film auf die Merkliste, oder entfernt ihn. Fehlermeldung falls kein Film gewählt.
    @FXML
    private void onBookmark() throws IOException {
        if (currentMovie != null) {
            this.currentMovie.setBookmark(true);
            movies.addMovie(this.currentMovie);
            loadMovie(currentMovie.getId());
            if (sql.exsists("Movie", "imdbID", "imdbID = '" + currentMovie.getImdbID() + "'") < 1) {
                sqlInsertToMovie(currentMovie);
            }
            if (sql.exsists("Movielist", "UserID, imdbID", "UserID = '" + user.getId() + "' and imdbID = '" + currentMovie.getImdbID() + "'") > 0) {
                sqlUpdateMovielist(currentMovie);
            } else {
                sqlInsertToMovielist(currentMovie);
            }
        } else {
            error(1);
        }
    }

    @FXML
    private void loadBookmarks() {
        refreshBookmarks();
    }

    @FXML
    private void loadFavorites() {
        refreshFavorites();
    }

    //Lade die Filme in die Favoritenliste.
    public void refreshFavorites() {

        ArrayList<Movie> favorites = new ArrayList();

        for (Object element : movies.movies) {
            Movie movie = (Movie) element;
            if (movie.isFavourite() == true) {
                favorites.add(movie);
            }
        }

        ObservableList fav = FXCollections.observableList(favorites);

        TableColumn titleCol = new TableColumn("Titel");
        TableColumn yearCol = new TableColumn("Jahr");
        TableColumn genreCol = new TableColumn("Genre");
        TableColumn runCol = new TableColumn("Laufzeit");
        TableColumn ratingCol = new TableColumn("IMDB Wertung");
        TableColumn userRatCol = new TableColumn("Benutzer Wertung");

        titleCol.setCellValueFactory(new PropertyValueFactory<Movie, String>("Title"));
        yearCol.setCellValueFactory(new PropertyValueFactory<Movie, String>("Year"));
        genreCol.setCellValueFactory(new PropertyValueFactory<Movie, String>("Genre"));
        runCol.setCellValueFactory(new PropertyValueFactory<Movie, String>("Runtime"));
        ratingCol.setCellValueFactory(new PropertyValueFactory<Movie, String>("imdbRating"));
        userRatCol.setCellValueFactory(new PropertyValueFactory<Movie, String>("userRating"));

        favoriteTable.getColumns().setAll(titleCol, yearCol, genreCol, runCol, userRatCol, ratingCol);
        favoriteTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        favoriteTable.setItems(fav);
    }

    //Lade die Filme in die Merkliste.
    public void refreshBookmarks() {
        ArrayList<Movie> bookmarks = new ArrayList();

        for (Object element : movies.movies) {
            Movie movie = (Movie) element;
            if (movie.isBookmark() == true) {
                bookmarks.add(movie);
            }
        }

        ObservableList bookmark = FXCollections.observableList(bookmarks);
        TableColumn titleCol = new TableColumn("Titel");
        TableColumn yearCol = new TableColumn("Jahr");
        TableColumn genreCol = new TableColumn("Genre");
        TableColumn runCol = new TableColumn("Laufzeit");
        TableColumn ratingCol = new TableColumn("Rating");
        TableColumn userRatCol = new TableColumn("Benutzer Wertung");
        TableColumn lookedCol = new TableColumn("Gesehen");

        titleCol.setCellValueFactory(new PropertyValueFactory<Movie, String>("Title"));
        yearCol.setCellValueFactory(new PropertyValueFactory<Movie, String>("Year"));
        genreCol.setCellValueFactory(new PropertyValueFactory<Movie, String>("Genre"));
        runCol.setCellValueFactory(new PropertyValueFactory<Movie, String>("Runtime"));
        ratingCol.setCellValueFactory(new PropertyValueFactory<Movie, String>("imdbRating"));
        userRatCol.setCellValueFactory(new PropertyValueFactory<Movie, String>("userRating"));
        lookedCol.setCellValueFactory(new PropertyValueFactory<Movie, String>("looked"));
        lookedCol.setSortType(TableColumn.SortType.ASCENDING);

        bookmarkTable.getColumns().setAll(titleCol, yearCol, genreCol, userRatCol, runCol, ratingCol, lookedCol);
        bookmarkTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        bookmarkTable.setItems(bookmark);
        bookmarkTable.getSortOrder().add(lookedCol);
    }

    //Lässt die Ergebnisliste der Suche nach verlassen der Liste verschwinden.
    @FXML
    private void onCenterDragOver() {
        searchlist.setVisible(false);
    }

    //Wählt Film für Detailansicht aus Suchliste aus.
    @FXML
    private void onSearchlistClick(MouseEvent event
    ) {
        if (event.getClickCount() == 2) {
            Movie movie = (Movie) searchlist.getSelectionModel().getSelectedItem();
            if (movie != null) {
                searchbar.setText(movie.getTitle());
                loadMovie(movie.getId());
            }
        }
    }

    //Lade die Detailansicht.
    private void loadMovie(String id) {
        String imageUrl = null;
        Movie movie;
        try {

            movie = movies.getMovieById(id);

            if (movie == null) {
                movie = omdb.searchById(id);
            }

            if (movie != null) {

                currentMovie = movie;
                SingleSelectionModel<Tab> selectionModel = tabPane.getSelectionModel();
                selectionModel.select(0);

                detailImage.setImage(new Image(movie.getPoster()));
                detailPlot.setText(movie.getPlot());

                Pane header = (Pane) detailTable.lookup("TableHeaderRow");
                header.setMaxHeight(0);
                header.setMinHeight(0);
                header.setPrefHeight(0);
                header.setVisible(false);
                header.setManaged(false);

                List infoList = new ArrayList();
                infoList.add(new TableRow("Titel", movie.getTitle()));
                infoList.add(new TableRow("Dauer", movie.getRuntime()));
                infoList.add(new TableRow("Regiseur", movie.getDirector()));
                infoList.add(new TableRow("Schauspieler", movie.getActors()));
                infoList.add(new TableRow("Genre", movie.getGenre()));
                infoList.add(new TableRow("Veröffentlicht", movie.getReleased()));
                infoList.add(new TableRow("Jahr", movie.getYear()));
                infoList.add(new TableRow("Bewertung", movie.getImdbRating()));

                ObservableList data = FXCollections.observableList(infoList);
                detailTable.setItems(data);
                TableColumn nameCol = new TableColumn();
                TableColumn valueCol = new TableColumn();
                nameCol.setCellValueFactory(new PropertyValueFactory<TableRow, String>("name"));
                nameCol.setVisible(true);
                valueCol.setCellValueFactory(new PropertyValueFactory<TableRow, String>("value"));
                valueCol.setVisible(true);
                detailTable.getColumns().setAll(nameCol, valueCol);

                if (movie.isFavourite()) {
                    imageRow2.setVisible(true);
                } else {
                    imageRow2.setVisible(false);
                }

                if (movie.isBookmark()) {
                    imageRow3.setVisible(true);
                } else {
                    imageRow3.setVisible(false);
                }

                if (movie.isLooked()) {
                    imageRow1.setVisible(true);
                } else {
                    imageRow1.setVisible(false);
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(MainFXMLController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    //Lädt die vom Nutzer gespeicherten Filme aus der SQLLite Datenbank.
    private void loadMovies() {
        List movielistRS = sql.select("Movielist", "*", "UserID = '" + user.getId() + "'");

        for (Object movielistElement : movielistRS) {
            Map<String, Object> movielistRow = (Map<String, Object>) movielistElement;
            Movie movie = new Movie();
            movie.setImdbID((String) movielistRow.get("imdbID"));
            movie.setBookmark(Boolean.parseBoolean((String) movielistRow.get("MerkList")));
            movie.setFavourite(Boolean.parseBoolean((String) movielistRow.get("FavList")));
            movie.setUserRating((String) movielistRow.get("UserRate"));
            movie.setLooked(Boolean.parseBoolean((String) movielistRow.get("Looked")));
            List movieRS = sql.select("Movie", "*", "imdbID = '" + movie.getImdbID() + "'");
            for (Object movieElement : movieRS) {
                Map<String, Object> movieRow = (Map<String, Object>) movieElement;
                movie.setActors((String) movieRow.get("Actors"));
                movie.setGenre((String) movieRow.get("Genre"));
                movie.setImdbRating((String) movieRow.get("imdbRating"));
                movie.setPlot((String) movieRow.get("Plot"));
                movie.setPoster((String) movieRow.get("Poster"));
                movie.setReleased((String) movieRow.get("Released"));
                movie.setRuntime((String) movieRow.get("Runtime"));
                movie.setTitle((String) movieRow.get("Title"));
                movie.setYear((String) movieRow.get("Year"));
                movie.setDirector((String) movieRow.get("Director"));
            }
            movies.addMovie(movie);
        }
    }

    //initialmethode wird aufgerufen wenn controller geladen wird.
    @Override
    public void initialize(URL url, ResourceBundle rb) {

    }

    //Übergabe des in LoginFXMLController eingegebenen Benutzernamens.
    public void dataTransferLogin(User user) {
        userBtn.setText("Hallo " + user.getName());
        this.user = user;
        loadMovies();
    }

    // Exportiert die Favoritenliste als PDF. Fehlermeldung falls kein Film in Liste.
    @FXML
    public void pdfListExportFav() throws IOException {
        PdfExport pdf = new PdfExport();
        ArrayList<Movie> array = new ArrayList();

        for (Object element : movies.movies) {
            Movie movie = (Movie) element;
            if (movie.isFavourite() == true) {
                array.add(movie);
            }
        }
        if (array.size() > 0) {
            pdf.exportMovies(array, "Favoritenliste");
        } else {
            error(0);
        }
    }

    // Exportiert die Merkliste als PDF. Fehlermeldung falls kein Film in Liste.
    @FXML
    public void pdfListExportBook() throws IOException {
        PdfExport pdf = new PdfExport();
        ArrayList<Movie> array = new ArrayList();

        for (Object element : movies.movies) {
            Movie movie = (Movie) element;
            if (movie.isBookmark() == true) {
                array.add(movie);
            }
        }
        if (array.size() > 0) {
            pdf.exportMovies(array, "Merkliste");
        } else {
            error(0);
        }
    }

    //Entfernt gewählten Film aus Favoritenliste. Fehlermeldung wenn kein Film vorhanden.
    @FXML
    public void favoriteDelete() throws IOException {
        Movie movie = (Movie) favoriteTable.getSelectionModel().getSelectedItem();
        if (movie != null) {
            movie.setFavourite(false);
            movies.updateMovie(movie);
            loadFavorites();
            currentMovie = null;
            sqlUpdateMovielist(movie);
            sqlDeleteFromMovielist(movie);
        } else {
            error(1);
        }
    }

    //Entfernt gewählten Film aus Merkliste. Fehlermeldung wenn kein Film vorhanden.
    @FXML
    public void bookmarkDelete() throws IOException {
        Movie movie = (Movie) bookmarkTable.getSelectionModel().getSelectedItem();
        if (movie != null) {
            movie.setBookmark(false);
            movies.updateMovie(movie);
            loadBookmarks();
            currentMovie = null;
            sqlUpdateMovielist(movie);
            sqlDeleteFromMovielist(movie);
        } else {
            error(1);
        }
    }

    //Markiert einen Film auf der Merkliste als gesehen/ungesehen. Fehlermeldung wenn kein Film vorhanden.
    @FXML
    public void movieLooked() throws IOException {
        Movie movie = (Movie) bookmarkTable.getSelectionModel().getSelectedItem();
        if (movie != null) {
            if (movie.isLooked()) {
                movie.setLooked(false);
            } else {
                movie.setLooked(true);
            }
            movies.updateMovie(movie);
            loadBookmarks();
            sqlUpdateMovielist(movie);
        } else {
            error(1);
        }
    }

    //Verschiebt einen Film aus der Merkliste in die Favoritenliste. Fehlermeldung wenn kein Film vorhanden.
    @FXML
    public void movieToFav() throws IOException {
        Movie movie = (Movie) bookmarkTable.getSelectionModel().getSelectedItem();
        if (movie != null) {
            movie.setBookmark(false);
            movie.setFavourite(true);
            movies.updateMovie(movie);
            loadBookmarks();
            currentMovie = null;
            sqlUpdateMovielist(movie);
        } else {
            error(1);
        }
    }

    //Öffnet Popup Fenster. Aufgerufen durch Doppelklick in Favoriten- oder Merkliste (s.o.)
    private void popup(Movie movie) throws IOException {
        FXMLLoader fxmlLoader = null;
        fxmlLoader = new FXMLLoader(getClass().getResource("/view/PopupFXML.fxml"));
        Parent root = fxmlLoader.load();

        Stage stage = new Stage();
        Scene scene = new Scene(root);

        stage.initModality(Modality.WINDOW_MODAL);
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setScene(scene);
        stage.setTitle("Details");
        stage.show();

        PopupFXMLController popupController = (PopupFXMLController) fxmlLoader.getController();
        popupController.dataTransferMain(movie, user);
    }

    //Gibt Fehlermeldung aus, wenn kein Film ausgewählt, oder in der aktuellen Liste vorhanden ist. "label" legt fest welche Fehlermeldung gezeigt werden soll.
    private void error(int label) throws IOException {
        FXMLLoader fxmlLoader = null;
        fxmlLoader = new FXMLLoader(getClass().getResource("/view/ErrorFXML.fxml"));
        Parent root = fxmlLoader.load();

        Stage stage = new Stage();
        Scene scene = new Scene(root);

        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setScene(scene);
        stage.setTitle("Fehler");
        stage.show();

        ErrorFXMLController errorController = (ErrorFXMLController) fxmlLoader.getController();
        errorController.dataTransferError(label);
    }

    //Verantwortlich für die Tooltips der einzelnen Buttons auf der MainFXML.fxml.
    @FXML
    public void onFavEntered() {
        Tooltip fav = new Tooltip("Film zur Favoritenliste hinzufügen");
        Tooltip.install(btnDetailFav, fav);
    }

    @FXML
    private void onLogout() throws IOException {

        Stage stage = (Stage) userBtn.getScene().getWindow();
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/view/LoginFXML.fxml"));
        Parent root = fxmlLoader.load();

        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();

        movies.movies.clear();
    }

    @FXML
    public void onBookmarkEntered() {
        Tooltip bm = new Tooltip("Film zur Merkliste hinzufügen");
        Tooltip.install(btnDetailBookmark, bm);
    }

    @FXML
    public void onPlayEntered() {
        Tooltip pdf = new Tooltip("Film als PDF exportieren");
        Tooltip.install(btnDetailPlay, pdf);
    }

    @FXML
    public void onFavDelEntered() {
        Tooltip favdel = new Tooltip("Film aus Favoriten entfernen");
        Tooltip.install(btnFavDel, favdel);
    }

    @FXML
    public void onFavPdfEntered() {
        Tooltip favpdf = new Tooltip("Favoritenliste als PDF exportieren");
        Tooltip.install(btnFavPdf, favpdf);
    }

    @FXML
    public void onFavLookedEntered() {
        Tooltip favlooked = new Tooltip("Film als gesehen markieren");
        Tooltip.install(btnFavLooked, favlooked);
    }

    @FXML
    public void onBookmarkDelEntered() {
        Tooltip bookmarkdel = new Tooltip("Film aus Merkliste entfernen");
        Tooltip.install(btnBookmarkDel, bookmarkdel);
    }

    @FXML
    public void onBookmarkPdfEntered() {
        Tooltip bookmarkpdf = new Tooltip("Merkliste als PDF exportieren");
        Tooltip.install(btnBookmarkPdf, bookmarkpdf);
    }

    @FXML
    public void onBookmarkToFavEntered() {
        Tooltip bookmarktofav = new Tooltip("Film in Favoritenliste verschieben");
        Tooltip.install(btnBookmarkToFav, bookmarktofav);
    }

    @FXML
    public void onXmlExport() {
        Tooltip xmlExport = new Tooltip("Alle Filme exportieren");
        Tooltip.install(btnExport, xmlExport);

    }

    @FXML
    public void onXmlImport() {
        Tooltip xmlImport = new Tooltip("XML-Datei importieren");
        Tooltip.install(btnImport, xmlImport);
    }

    @FXML
    public void loadStatistic() {

        Statistic stats = new Statistic();

        //Kreisdiagramm
        List movieListRS = sql.select("Movielist", "imdbID", "UserID = '" + user.getId() + "'");

        //Array initialisieren
        int movieRating[] = new int[11];

        for (Object movielistElement : movieListRS) {
            //Die einzelnen Elemente werden gemappt
            Map<String, String> movielistRow = (Map<String, String>) movielistElement;
            List movieRS = sql.select("Movie", "imdbRating", "imdbID = '" + movielistRow.get("imdbID") + "'");

            for (Object movieElement : movieRS) {
                Map<String, String> movieRow = (Map<String, String>) movieElement;
                //imdbRating in double umwandeln, dann runden und in int casten
                int groupNumber = (int) Math.rint(Double.parseDouble(movieRow.get("imdbRating")));
                //Counter, welcher für jede Bewertung die entsprechende Gruppe hochzählt
                movieRating[groupNumber] = movieRating[groupNumber] + 1;
            }
        }

        ArrayList<PieChart.Data> data = new ArrayList<>();
        for (int j = 1; j < movieRating.length; j++) {
            if (movieRating[j] != 0) {
                //Daten in die Arraylist übertragen
                data.add(new PieChart.Data(String.valueOf(j) + "-Bewertung", movieRating[j]));
            }
        }

        //Datenübergabe an ObservableList, Befüllung des Diagramms und Benennung des Titels
        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList(data);
        pieChartImdbRating.setData(pieChartData);
        pieChartImdbRating.setTitle("Verteilung der Imdb-Bewertungen auf " + movieListRS.size() + " Filme");

        //Statistic Table
        //SQL-Abfragen
        List bookListRS = sql.select("Movielist", "imdbID", "UserID = '" + user.getId() + "' AND MerkList = 'true'");
        List favListRS = sql.select("Movielist", "imdbID", "UserID = '" + user.getId() + "' AND FavList = 'true'");
        List bookSeenRS = sql.select("Movielist", "imdbID", "UserID = '" + user.getId() + "' AND MerkList = 'true' AND Looked = 'true'");
        List userRateRS = sql.select("Movielist", "UserRate", "UserID = '" + user.getId() + "'");

        //Befüllung der Statistik-Labels mit der Statistik-Klasse
        lblBook.setText(String.valueOf(stats.getNumber(bookListRS)));
        lblFav.setText(String.valueOf(stats.getNumber(favListRS)));
        lblSeenMovies.setText(String.valueOf(stats.getNumber(bookSeenRS)));
        lblUserRate.setText(String.valueOf(stats.getAverage(userRateRS)) + "/5");
    }

    @FXML
    //Exportiert eine XML-Datei mit allen Filmen des aktuell angemeldeten Nutzers
    public void xmlExport() {
        //XML Objekt Instanziierung
        XML xml = new XML();
        JFileChooser chooser = new JFileChooser();
        //Pop-Up für die Pfadwahl
        chooser.setCurrentDirectory(new java.io.File("."));
        chooser.setDialogTitle("Pfad wählen");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.setApproveButtonText("Exportieren");
        //Bei Klick auf Exportieren (Im Dialogfenster) exportiere XML
        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            try {
                //Instanz der JAXBContext-Klasse erstellen
                JAXBContext jaxbContext = JAXBContext.newInstance(MovieList.class);
                //Aufruf der Methode exportXml in der KLasse xml inklusive Übergabe der Parameter
                xml.exportXml(movies, jaxbContext, chooser.getSelectedFile() + "/movieExport.xml");
            } catch (JAXBException ex) {
                Logger.getLogger(MainFXMLController.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    //Liest eine xml-Datei ein
    @FXML
    public void xmlImport() {
        XML xml = new XML();
        //Öffnet ein Pop-Up, in welchem der Pfad zur xml-Datei angegeben wird
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(new java.io.File("."));
        chooser.setDialogTitle("XML wählen");
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        //Filter, damit nur xml-Dateien angezeigt werden
        FileFilter filter = new FileNameExtensionFilter("XML File", "xml");
        chooser.setFileFilter(filter);
        chooser.setApproveButtonText("Importieren");
        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            try {
                //Instanz der JAXBContext-Klasse erstellen
                JAXBContext jaxbContext = JAXBContext.newInstance(MovieList.class);
                //Die Methode importXml aus der Klasse xml wird aufgerufen und der zurückgegebene Wert in ein Obejekt des Typs MovieList gecastet
                MovieList movielist = (MovieList) xml.importXml(jaxbContext, chooser.getSelectedFile().toString());
                //movielist aufteilen
                for (Object element : movielist.movies) {
                    Movie movie = (Movie) element;
                    //Überprüfung, ob der movie schon geladen ist (Wenn nicht, hinzufügen)
                    if (movies.movieExists(movie) == -1) {
                        movies.addMovie(movie);
                        //Überprüfung, ob der movie schon in der SQL-Datenbank vorhanden ist (Wenn nicht, hinzufügen)
                        if (sql.exsists("Movie", "imdbID", "imdbID = '" + movie.getImdbID() + "'") < 1) {
                            sqlInsertToMovie(movie);
                        }
                        sqlInsertToMovielist(movie);
                    }
                }

            } catch (JAXBException ex) {
                Logger.getLogger(MainFXMLController.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
