module com.example.ep1 {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.ep1 to javafx.fxml;
    exports com.example.ep1;
}