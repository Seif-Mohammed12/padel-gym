module com.example.padelfrontend {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.json;


    opens com.example.padelfrontend to javafx.fxml;
    exports com.example.padelfrontend;
}